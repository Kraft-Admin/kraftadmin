package model

// ---------------------------------------------------------------------------
// QueryEvent — the detailed record of a single database operation.
// Populated by the JPA/JDBC interceptor and handed to QueryPulseInterceptor.
// ---------------------------------------------------------------------------

data class QueryEvent(
    val traceId: String, // <--- Link to KraftTelemetryEvent.traceId
    // The SQL as it was sent to the database (with ? placeholders)
    val sql: String,

    // Bound parameter values in order — kept as strings to avoid serialization issues
    val parameters: List<String?> = emptyList(),

    // Query classification
    val queryType: QueryType,

    // Which JPA entity / table was the primary target (best-effort)
    val entityName: String? = null,
    val tableName: String? = null,

    // Timing — callers fill startedAt, interceptor computes durationMs
    val startedAt: Long,                    // System.currentTimeMillis()
    val durationMs: Long,

    // Outcome
    val rowsAffected: Int = 0,             // For INSERT/UPDATE/DELETE
    val rowsReturned: Int = 0,             // For SELECT

    // Performance signals
    val isSlowQuery: Boolean = false,       // True if > configured threshold
    val isPotentialNPlusOne: Boolean = false,

    // Error detail — null when query succeeded
    val error: QueryError? = null,

    // Full JDBC connection metadata (useful for multi-datasource setups)
    val dataSource: String = "primary",
    val databaseProduct: String? = null,   // e.g. "PostgreSQL", "MySQL"
    val schema: String? = null
)

enum class QueryType {
    SELECT,
    INSERT,
    UPDATE,
    DELETE,
    CALL,          // Stored procedure
    DDL,           // CREATE / ALTER / DROP (should be rare in prod)
    BATCH,         // executeBatch()
    UNKNOWN
}

data class QueryError(
    val sqlState: String?,
    val errorCode: Int,
    val message: String,
    val exceptionClass: String
)

// ---------------------------------------------------------------------------
// N+1 Detection Support
// Tracks how many times the same query pattern fires in one request context.
// ---------------------------------------------------------------------------

data class QueryPattern(
    val normalizedSql: String,             // SQL with literals stripped
    val entityName: String?,
    var count: Int = 1
)