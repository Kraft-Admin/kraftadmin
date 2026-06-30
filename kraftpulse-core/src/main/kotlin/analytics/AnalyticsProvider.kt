package analytics

import model.KraftHttpClientEvent
import model.KraftTaskEvent
import model.PulseExceptionEntry
import model.QueryEvent
import com.kraftadmin.model.KraftTelemetryEvent
import java.time.Instant

// --- SUPPORTING MODELS ---

data class TelemetryFilter(
    val resource: String? = null,
    val actor: String? = null,
    val statusGroup: Int? = null // e.g., 5 for 5xx errors
)

data class TelemetryWithQueries(
    val event: KraftTelemetryEvent,
    val queries: List<QueryEvent>
)

data class LatencyReport(val p50: Double, val p95: Double, val p99: Double, val avg: Double)

data class ResourceStats(
    val resource: String,
    val requestCount: Long,
    val errorRate: Double,
    val avgLatency: Double
)

enum class TimeInterval { MINUTELY, HOURLY, DAILY }
enum class SortMetric { REQUEST_COUNT, ERROR_RATE, LATENCY }
data class TimeRange(val start: Instant, val end: Instant)
data class TrafficPoint(val timestamp: Long, val count: Int)


/**
 * Read-only analytics queries — satisfied by either local SQLite
 * or a remote ClickHouse-backed cloud API, depending on config.
 */
interface AnalyticsReader {
    fun getTrafficTrend(interval: TimeInterval = TimeInterval.HOURLY, range: TimeRange, filter: TelemetryFilter = TelemetryFilter()): List<TrafficPoint>
    fun getTopResources(limit: Int = 10, sortBy: SortMetric = SortMetric.REQUEST_COUNT): List<ResourceStats>
    fun getStatusBreakdown(filter: TelemetryFilter): Map<Int, Long>
    fun getLatencyPercentiles(resource: String?, range: TimeRange): LatencyReport
    fun getRegionalDistribution(range: TimeRange): Map<String, Long>
    fun getSummary(range: TimeRange): AnalyticsSummary
    fun getQueriesForTrace(traceId: String): List<QueryEvent>
    fun getLatestWithDetails(limit: Int): List<TelemetryWithQueries>
    fun fetchLatestWithQueries(limit: Int): List<TelemetryWithQueries>
    fun fetchComprehensiveDeepDive(traceId: String): Map<String, Any?>
    fun <T> fetchAllPaged(table: String, limit: Int, offset: Int, clazz: Class<T>): List<T>
}

/**
 * Local-only durable write path. This is ALWAYS SQLite, regardless of
 * whether the cloud provider is active — it's the outbox, not the destination.
 * There is no CloudWriter equivalent; cloud writes only ever happen via
 * batched flush, never per-event.
 */
interface TelemetryWriter {
    fun track(event: KraftTelemetryEvent)
    fun save(event: QueryEvent)
    fun saveTelemetryEvent(event: KraftTelemetryEvent)
    fun saveException(exceptionData: PulseExceptionEntry)
    fun saveTask(taskEvent: KraftTaskEvent)
    fun saveHttpClientEvent(event: KraftHttpClientEvent)
}
