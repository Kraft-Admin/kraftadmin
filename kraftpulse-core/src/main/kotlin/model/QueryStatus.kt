package model

enum class QueryStatus {
    /** The query completed successfully within the expected lifecycle. */
    SUCCESS,

    /** The database returned an error (e.g., Syntax error, Constraint violation). */
    DATABASE_ERROR,

    /** The connection timed out or was lost before completion. */
    TIMEOUT,

    /** The transaction was explicitly rolled back by the application logic. */
    ROLLED_BACK,

    /** The query was killed/cancelled by the server or a circuit breaker. */
    CANCELLED
}