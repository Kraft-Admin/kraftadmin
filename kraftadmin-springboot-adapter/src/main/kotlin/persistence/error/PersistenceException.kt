package persistence.error

/**
 * The actual throwable used inside the persistence layer. Carries the
 * resolved, user-safe details plus the original cause for logging/tracing.
 * Controllers/advice catch THIS, never the raw Hibernate/JDBC exception.
 */
class PersistenceException(
    val details: PersistenceErrorDetails,
    cause: Throwable? = null
) : RuntimeException(details.message, cause)