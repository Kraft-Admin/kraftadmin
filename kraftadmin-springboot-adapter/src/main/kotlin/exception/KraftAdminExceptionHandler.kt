package exception

import api.responses.KraftOperationResponse
import com.kraftadmin.logging.KraftAdminLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import persistence.error.PersistenceException
import java.lang.reflect.InvocationTargetException

@RestControllerAdvice
class KraftAdminExceptionHandler {

    private val logger = KraftAdminLogging.logger(javaClass)


    // ── Structured persistence errors (fetch/save/delete) — status derived
    //    from the resolved error code, not flattened to a generic 500 ──────
    @ExceptionHandler(PersistenceException::class)
    fun handlePersistence(e: PersistenceException): ResponseEntity<KraftOperationResponse<Any?>> {
        val status = statusForCode(e.details.code)

        if (status.is5xxServerError) {
            logger.error(
                "KraftAdmin persistence error [{}]: {}",  e
            )
        } else {
            logger.warn(
                "KraftAdmin persistence error [{}]: {}", e.details.code, e.details.message
            )
        }

        return ResponseEntity.status(status).body(
            KraftOperationResponse(
                success = false,
                message = e.details.message,
                data = null
            )
        )
    }

    // Business rule violations from action handlers — 400
    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(e: IllegalStateException): ResponseEntity<KraftOperationResponse<Any?>> {
        logger.warn("Action blocked by business rule: {}", e.message)
        return ResponseEntity.badRequest().body(
            KraftOperationResponse(
                success = false,
                message = e.message ?: "Action not allowed"
            )
        )
    }

    // Invalid arguments not otherwise caught (e.g. malformed path/query params) — 400
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(e: IllegalArgumentException): ResponseEntity<KraftOperationResponse<Any?>> {
        return ResponseEntity.badRequest().body(
            KraftOperationResponse(
                success = false,
                message = e.message ?: "Invalid input"
            )
        )
    }

    // Reflection wrapper from method.invoke() — unwrap and re-dispatch
    @ExceptionHandler(InvocationTargetException::class)
    fun handleInvocation(e: InvocationTargetException): ResponseEntity<KraftOperationResponse<Any?>> {
        val cause = e.targetException ?: e.cause ?: e
        return when (cause) {
            is PersistenceException -> handlePersistence(cause)
            is IllegalStateException -> handleIllegalState(cause)
            is IllegalArgumentException -> handleIllegalArgument(cause)
            else -> ResponseEntity.internalServerError().body(
                KraftOperationResponse(
                    success = false,
                    message = cause.message ?: "An unexpected error occurred"
                )
            )
        }
    }

    // Catch-all — every unhandled exception still returns the SAME shape
    // { success, message, data, errors } so the frontend never has to guess.
    @ExceptionHandler(Exception::class)
    fun handleGeneral(e: Exception): ResponseEntity<KraftOperationResponse<Any?>> {
        val real = when (e) {
            is InvocationTargetException -> e.targetException ?: e.cause ?: e
            else -> e
        }

        return when (real) {
            is PersistenceException -> handlePersistence(real)
            is IllegalStateException -> handleIllegalState(real)
            is IllegalArgumentException -> handleIllegalArgument(real)
            else -> {
                ResponseEntity.internalServerError().body(
                    KraftOperationResponse(
                        success = false,
                        message = real.message ?: "An unexpected error occurred"
                    )
                )
            }
        }
    }

    // ── Maps a resolved PersistenceErrorDetails.code to the correct HTTP
    // status, matching the categories DefaultPersistenceErrorResolver
    // actually produces. Keep this in sync if you add new codes there. ──
    private fun statusForCode1(code: String): HttpStatus = when (code) {
        "not_found" -> HttpStatus.NOT_FOUND

        "already_exists",
        "duplicate_id",
        "duplicate_value",
        "foreign_key",
        "constraint_violation",
        "locked" -> HttpStatus.CONFLICT

        "required_field",
        "invalid_value" -> HttpStatus.BAD_REQUEST

        "database_unavailable" -> HttpStatus.SERVICE_UNAVAILABLE

        "schema_mismatch",
        "query_error",
        "database_error",
        "persistence_error",
        "save_failed" -> HttpStatus.INTERNAL_SERVER_ERROR

        else -> HttpStatus.INTERNAL_SERVER_ERROR
    }

    private fun statusForCode(code: String): HttpStatus = when (code) {
        "not_found" -> HttpStatus.NOT_FOUND
        "already_exists",
        "duplicate_id",
        "duplicate_value", // Validation error
        "foreign_key",
        "constraint_violation",
        "required_field",
        "invalid_value" -> HttpStatus.BAD_REQUEST // 400 is appropriate for validation
        "locked" -> HttpStatus.CONFLICT
        "database_unavailable" -> HttpStatus.SERVICE_UNAVAILABLE
        // Change "save_failed" if it is actually a validation error wrapper
        "save_failed" -> HttpStatus.BAD_REQUEST
        else -> HttpStatus.INTERNAL_SERVER_ERROR
    }


}