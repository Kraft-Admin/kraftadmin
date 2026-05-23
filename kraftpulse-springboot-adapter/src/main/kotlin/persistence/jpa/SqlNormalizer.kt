package persistence.jpa

/**
 * Strips literal values from SQL so structurally identical queries
 * (differing only in parameter values) map to the same pattern key.
 *
 * e.g. "SELECT * FROM user WHERE id = 42"
 *   → "SELECT * FROM user WHERE id = ?"
 *
 * Used for N+1 detection and slow query grouping.
 */
object SqlNormalizer {

    // Matches: numeric literals, single-quoted strings, IN lists
    private val NUMBER_LITERAL = Regex("""\b\d+(\.\d+)?\b""")
    private val STRING_LITERAL = Regex("""'[^']*'""")
    private val IN_LIST        = Regex("""\bIN\s*\([^)]+\)""", RegexOption.IGNORE_CASE)
    private val WHITESPACE     = Regex("""\s+""")

    fun normalize(sql: String): String {
        return sql
            .let { IN_LIST.replace(it, "IN (?)") }
            .let { STRING_LITERAL.replace(it, "?") }
            .let { NUMBER_LITERAL.replace(it, "?") }
            .let { WHITESPACE.replace(it, " ") }
            .trim()
            .uppercase()
    }

    /**
     * Infers [QueryType] from the first keyword in the SQL statement.
     */
    fun inferQueryType(sql: String): model.QueryType {
        val trimmed = sql.trimStart()
        return when {
            trimmed.startsWith("SELECT", ignoreCase = true)   -> model.QueryType.SELECT
            trimmed.startsWith("INSERT", ignoreCase = true)   -> model.QueryType.INSERT
            trimmed.startsWith("UPDATE", ignoreCase = true)   -> model.QueryType.UPDATE
            trimmed.startsWith("DELETE", ignoreCase = true)   -> model.QueryType.DELETE
            trimmed.startsWith("CALL",   ignoreCase = true)   -> model.QueryType.CALL
            trimmed.startsWith("CREATE", ignoreCase = true)   -> model.QueryType.DDL
            trimmed.startsWith("ALTER",  ignoreCase = true)   -> model.QueryType.DDL
            trimmed.startsWith("DROP",   ignoreCase = true)   -> model.QueryType.DDL
            else                                               -> model.QueryType.UNKNOWN
        }
    }

    /**
     * Attempts to extract the primary table name from simple SELECT/INSERT/UPDATE/DELETE.
     * Not foolproof — works for common single-table patterns.
     */
    fun extractTableName(sql: String): String? {
        val upper = sql.uppercase().trim()
        return when {
            upper.startsWith("SELECT") ->
                Regex("""FROM\s+(\w+)""", RegexOption.IGNORE_CASE).find(sql)?.groupValues?.get(1)
            upper.startsWith("INSERT") ->
                Regex("""INTO\s+(\w+)""", RegexOption.IGNORE_CASE).find(sql)?.groupValues?.get(1)
            upper.startsWith("UPDATE") ->
                Regex("""UPDATE\s+(\w+)""", RegexOption.IGNORE_CASE).find(sql)?.groupValues?.get(1)
            upper.startsWith("DELETE") ->
                Regex("""FROM\s+(\w+)""", RegexOption.IGNORE_CASE).find(sql)?.groupValues?.get(1)
            else -> null
        }
    }
}