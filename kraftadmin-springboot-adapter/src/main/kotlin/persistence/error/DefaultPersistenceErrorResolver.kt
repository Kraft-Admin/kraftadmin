package persistence.error

import com.kraftadmin.logging.KraftAdminLogging
import jakarta.persistence.EntityExistsException
import jakarta.persistence.EntityNotFoundException
import jakarta.persistence.PersistenceException as JpaPersistenceException
import org.hibernate.PropertyValueException
import org.hibernate.exception.ConstraintViolationException
import org.hibernate.exception.DataException
import org.hibernate.exception.JDBCConnectionException
import org.hibernate.exception.LockAcquisitionException
import org.hibernate.exception.SQLGrammarException
import org.hibernate.exception.GenericJDBCException
import java.sql.SQLException

class DefaultPersistenceErrorResolver : PersistenceErrorResolver {

    private val logger = KraftAdminLogging.logger(javaClass)


    override fun resolve(
        resource: String,
        exception: Throwable
    ): PersistenceErrorDetails {

        if (exception is PersistenceException) {
            return exception.details
        }

        // 2. Also handle if the exception is wrapped in an InvocationTargetException/Transaction
        val cause = exception.cause
        if (cause is PersistenceException) {
            return cause.details
        }

        return when {

            ExceptionUtils.hasCause<SQLGrammarException>(exception) ->
                resolveSqlGrammarError(resource, exception)

            ExceptionUtils.hasCause<JDBCConnectionException>(exception) ->
                PersistenceErrorDetails(
                    "database_unavailable",
                    "Could not reach the database. Please try again shortly."
                )

            ExceptionUtils.hasCause<LockAcquisitionException>(exception) ->
                PersistenceErrorDetails(
                    "locked",
                    "$resource is currently being modified by another process. Please try again."
                )

            ExceptionUtils.hasCause<PropertyValueException>(exception) ->
                PersistenceErrorDetails(
                    "required_field",
                    "Please fill in all required fields."
                )

            ExceptionUtils.hasCause<DataException>(exception) ->
                PersistenceErrorDetails(
                    "invalid_value",
                    "One or more values are invalid."
                )

            ExceptionUtils.hasCause<EntityExistsException>(exception) ->
                PersistenceErrorDetails(
                    "already_exists",
                    "$resource already exists."
                )

            ExceptionUtils.hasCause<EntityNotFoundException>(exception) ->
                PersistenceErrorDetails(
                    "not_found",
                    "$resource was not found."
                )

            ExceptionUtils.hasCause<ConstraintViolationException>(exception) ->
                resolveConstraintViolation(resource, exception)

            ExceptionUtils.hasCause<GenericJDBCException>(exception) ->
                resolveGenericJdbcError(resource, exception)

            ExceptionUtils.hasCause<SQLException>(exception) ->
                resolveGenericSqlError(resource, exception)

            ExceptionUtils.hasCause<JpaPersistenceException>(exception) ->
                PersistenceErrorDetails(
                    "persistence_error",
                    "A database error occurred while processing $resource."
                )

            else ->
                PersistenceErrorDetails(
                    "save_failed",
                    "Unable to save $resource."
                )
        }
    }

    private fun resolveSqlGrammarError(
        resource: String,
        exception: Throwable
    ): PersistenceErrorDetails {
        val sqlEx = ExceptionUtils.findCause<SQLException>(exception)
        return when (sqlEx?.sqlState) {
            "42703" -> PersistenceErrorDetails(
                "schema_mismatch",
                "$resource could not be loaded due to a data model mismatch. Please contact support."
            )
            "42P01" -> PersistenceErrorDetails(
                "schema_mismatch",
                "$resource references a table that does not exist. Please contact support."
            )
            "42601" -> PersistenceErrorDetails(
                "query_error",
                "An internal query error occurred while processing $resource."
            )
            else -> PersistenceErrorDetails(
                "schema_mismatch",
                "$resource could not be processed due to a database schema issue. Please contact support."
            )
        }
    }

    private fun resolveGenericJdbcError(
        resource: String,
        exception: Throwable
    ): PersistenceErrorDetails {
        val sqlEx = ExceptionUtils.findCause<SQLException>(exception)
        return classifyBySqlState(resource, sqlEx) ?: PersistenceErrorDetails(
            "database_error",
            "A database error occurred while processing $resource."
        )
    }

    private fun resolveGenericSqlError(
        resource: String,
        exception: Throwable
    ): PersistenceErrorDetails {
        val sqlEx = ExceptionUtils.findCause<SQLException>(exception)
        return classifyBySqlState(resource, sqlEx) ?: PersistenceErrorDetails(
            "database_error",
            "A database error occurred while processing $resource."
        )
    }

    private fun classifyBySqlState(resource: String, sqlEx: SQLException?): PersistenceErrorDetails? {
        val state = sqlEx?.sqlState ?: return null
        return when {
            state.startsWith("42") -> PersistenceErrorDetails(
                "schema_mismatch",
                "$resource could not be processed due to a database schema issue. Please contact support."
            )
            state.startsWith("23") -> PersistenceErrorDetails(
                "constraint_violation",
                "The operation violates a database constraint."
            )
            state.startsWith("08") -> PersistenceErrorDetails(
                "database_unavailable",
                "Could not reach the database. Please try again shortly."
            )
            else -> null
        }
    }

    private fun resolveConstraintViolation(
        resource: String,
        exception: Throwable
    ): PersistenceErrorDetails {
        val constraint = ExceptionUtils.findCause<ConstraintViolationException>(exception)
        val name = constraint?.constraintName?.lowercase().orEmpty()

        return when {
            name.contains("pkey") ->
                PersistenceErrorDetails("duplicate_id", "A record with this identifier already exists.")
            name.contains("unique") ->
                PersistenceErrorDetails("duplicate_value", "A record with the same value already exists.")
            name.contains("foreign") ->
                PersistenceErrorDetails("foreign_key", "This record is referenced by other data.")
            else ->
                PersistenceErrorDetails("constraint_violation", "The operation violates a database constraint.")
        }
    }

}