package util

import interceptors.PulseContextProvider
import interceptors.QueryPulseInterceptor
import model.PulseContext
import model.QueryEvent
import model.QueryStatus
import net.ttddyy.dsproxy.ExecutionInfo
import net.ttddyy.dsproxy.QueryInfo
import net.ttddyy.dsproxy.listener.QueryExecutionListener
import org.springframework.stereotype.Component

/**
 * The JPA Sniffer: Intercepts SQL at the DataSource level.
 * It is architecture-agnostic and focused purely on query metrics.
 */
@Component
class JpaPulseQueryListener(
    private val interceptor: QueryPulseInterceptor,
    private val contextProvider: PulseContextProvider
) : QueryExecutionListener {

    override fun beforeQuery(execInfo: ExecutionInfo, queryInfoList: MutableList<QueryInfo>) {
        // No-op: timing is handled by dsproxy ExecutionInfo
        println("Timing started")
    }

    override fun afterQuery(execInfo: ExecutionInfo, queryInfoList: MutableList<QueryInfo>) {
        println("afterQuery: $execInfo")
        // Sniffer strategy: Fallback to SYSTEM context if provider fails or app is non-web
        val context = contextProvider.currentContext() ?: PulseContext.SYSTEM_DEFAULT

        queryInfoList.forEach { query ->
//            val event = QueryEvent(
//                statement = query.query,
//                operation = determineOperation(query.query),
//                durationMs = execInfo.elapsedTime,
//                // Result extraction for updates/deletes
//                rowsAffected = extractRowsAffected(execInfo),
//                status = mapStatus(execInfo)
//            )
//
//            // Immediate handoff to the core's async buffer
//            interceptor.onQuery(context, event)
        }
        println("afterQuery: $queryInfoList")
    }

    private fun mapStatus(execInfo: ExecutionInfo): QueryStatus {
        return when {
            execInfo.isSuccess -> QueryStatus.SUCCESS
            // Specific check for timeouts if the driver supports it
            execInfo.throwable?.message?.contains("timeout", ignoreCase = true) == true -> QueryStatus.TIMEOUT
            else -> QueryStatus.DATABASE_ERROR
        }
    }

    private fun extractRowsAffected(execInfo: ExecutionInfo): Long {
        val result = execInfo.result
        return if (result is Number) result.toLong() else 0L
    }

    private fun determineOperation(sql: String): String {
        // Clean and grab first word for high-speed parsing
        val normalized = sql.trimStart().take(10).uppercase()
        return when {
            normalized.startsWith("SELECT") -> "SELECT"
            normalized.startsWith("INSERT") -> "INSERT"
            normalized.startsWith("UPDATE") -> "UPDATE"
            normalized.startsWith("DELETE") -> "DELETE"
            normalized.startsWith("CALL")   -> "PROCEDURE"
            else -> "SQL_EXEC"
        }
    }
}