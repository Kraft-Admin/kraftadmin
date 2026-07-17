package persistence.error

/**
 * Structured, resolvable description of what went wrong — safe to serialize
 * straight to the client. Produced by PersistenceErrorResolver.resolve().
 */
data class PersistenceErrorDetails(
    val code: String,
    val message: String
)