package analytics

import model.KraftHttpClientEvent
import model.KraftTaskEvent
import model.PulseExceptionEntry
import model.QueryEvent
import telemetry.SQLiteTelemetryProvider
import telemetry.KraftTelemetryEvent
import java.time.ZoneId

class LocalAnalyticsProvider(
    private val sqLiteTelemetryProvider: SQLiteTelemetryProvider,
    private val timeZone: ZoneId = ZoneId.systemDefault(),
) : AnalyticsProvider {

    override fun track(event: KraftTelemetryEvent) {
        sqLiteTelemetryProvider.save(event)
    }

    override fun getTrafficTrend(
        interval: TimeInterval,
        range: TimeRange,
        filter: TelemetryFilter
    ): List<TrafficPoint> {
        return sqLiteTelemetryProvider.fetchTrafficTrend(interval, range, filter, timeZone)
    }

    override fun getTopResources(limit: Int, sortBy: SortMetric): List<ResourceStats> {
        return sqLiteTelemetryProvider.fetchTopResources(limit, sortBy)
    }

    override fun getStatusBreakdown(filter: TelemetryFilter): Map<Int, Long> {
        return sqLiteTelemetryProvider.fetchStatusBreakdown(filter)
    }

    override fun getLatencyPercentiles(resource: String?, range: TimeRange): LatencyReport {
        return sqLiteTelemetryProvider.fetchLatencyPercentiles(resource, range)
    }

    override fun getRegionalDistribution(range: TimeRange): Map<String, Long> {
        return sqLiteTelemetryProvider.fetchRegionalDistribution(range)
    }

    override fun getSummary(range: TimeRange): AnalyticsSummary {
        return sqLiteTelemetryProvider.fetchSummary(range)
    }

    override fun save(event: QueryEvent) {
        sqLiteTelemetryProvider.save(event)
    }

    override fun saveTelemetryEvent(event: KraftTelemetryEvent) {
        sqLiteTelemetryProvider.save(event)
    }

    override fun getQueriesForTrace(traceId: String): List<QueryEvent> {
        return sqLiteTelemetryProvider.fetchQueriesForTrace(traceId)
    }

    override fun getLatestWithDetails(limit: Int): List<TelemetryWithQueries> {
        return sqLiteTelemetryProvider.fetchLatestWithQueries(limit)
    }

    override fun saveException(exceptionData: PulseExceptionEntry) {
        sqLiteTelemetryProvider.saveException(exceptionData)
    }

    override fun saveTask(taskEvent: KraftTaskEvent) {
        sqLiteTelemetryProvider.saveTask(taskEvent)
    }

    override fun saveHttpClientEvent(event: KraftHttpClientEvent) {
        sqLiteTelemetryProvider.saveHttpClientEvent(event)
    }

    override fun fetchLatestWithQueries(limit: Int): List<TelemetryWithQueries> {
        return sqLiteTelemetryProvider.fetchLatestWithQueries(limit)
    }

    override fun fetchComprehensiveDeepDive(traceId: String): Map<String, Any?> {
        return sqLiteTelemetryProvider.fetchComprehensiveDeepDive(traceId)
    }

    override fun <T> fetchAllPaged(table: String, limit: Int, offset: Int, clazz: Class<T>): List<T> {
        return sqLiteTelemetryProvider.fetchAllPaged(table, limit, offset, clazz)
    }
}