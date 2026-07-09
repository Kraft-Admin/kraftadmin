package exception

import actions.KraftActionResponse
import com.kraftadmin.KraftAdmin
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.lang.reflect.InvocationTargetException

@RestControllerAdvice
class KraftAdminExceptionHandler {

    // Business rule violations from action handlers — 400
    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(e: IllegalStateException): ResponseEntity<KraftActionResponse.Builder?> {
        KraftAdmin.logger.warn("Action blocked by business rule: {}", e.message)
        return ResponseEntity.badRequest().body(
            KraftActionResponse.fail(e.message ?: "Action not allowed")
        )
    }

    // Reflection wrapper from method.invoke() — unwrap and re-dispatch
    @ExceptionHandler(InvocationTargetException::class)
    fun handleInvocation(e: InvocationTargetException): ResponseEntity<KraftActionResponse.Builder?> {
        val cause = e.targetException ?: e.cause ?: e
        KraftAdmin.logger.debug("InvocationTargetException unwrapped: {}", cause::class.simpleName)

        return when (cause) {
            is IllegalStateException -> ResponseEntity.badRequest().body(
                KraftActionResponse.fail(cause.message ?: "Action not allowed")
            )
            is IllegalArgumentException -> ResponseEntity.badRequest().body(
                KraftActionResponse.fail(cause.message ?: "Invalid input")
            )
            else -> ResponseEntity.internalServerError().body(
                KraftActionResponse.fail(
                    cause.message ?: "An unexpected error occurred"
                )
            )
        }
    }

    // Catch-all — but structured to still return KraftActionResponse shape
    // the frontend always gets { success, message } regardless of error type
    @ExceptionHandler(Exception::class)
    fun handleGeneral(e: Exception): ResponseEntity<KraftActionResponse.Builder?> {
        // If it wraps an InvocationTargetException, unwrap it
        val real = when (e) {
            is InvocationTargetException -> e.targetException ?: e.cause ?: e
            else -> e
        }

        val status = when (real) {
            is IllegalStateException,
            is IllegalArgumentException -> HttpStatus.BAD_REQUEST
            else -> HttpStatus.INTERNAL_SERVER_ERROR
        }

        KraftAdmin.logger.error(
            "KraftAdmin unhandled exception [{}]: {}",
            real::class.simpleName, real.message, real
        )

        return ResponseEntity.status(status).body(
            KraftActionResponse.fail(real.message ?: "An unexpected error occurred")
        )
    }
}