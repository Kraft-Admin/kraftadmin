package controller

import analytics.AnalyticsReader
import analytics.LatencyReport
import analytics.ResourceStats
import analytics.SortMetric
import analytics.TelemetryFilter
import model.KraftHttpClientEvent
import model.KraftTaskEvent
import model.PulseExceptionEntry
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import telemetry.KraftTelemetryService
import analytics.TelemetryWithQueries
import analytics.TimeInterval
import analytics.TimeRange
import analytics.TrafficPoint
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import telemetry.micrometer.KraftPulseMeters
import java.time.Instant
import java.time.temporal.ChronoUnit

//@RestController
//@RequestMapping("/admin/api/monitoring")
//@ConditionalOnExpression(
//    "\${kraftpulse.enabled:false} and \${kraftpulse.telemetry-config.enabled:false}"
//)
//class KraftSpringMonitoringController(
//    private val telemetryService: AnalyticsReader
//) {
//
//    @GetMapping("/dashboard")
//    fun getDashboardOverview(@RequestParam(defaultValue = "50") limit: Int): List<TelemetryWithQueries> {
//        val httpCounter = meterRegistry?.find("http.server.requests")?.counter()?.count() ?: 0.0
//        return telemetryService.getDashboardOverview(limit)
//    }
//
//    @GetMapping("/traces/{traceId}")
//    fun getTraceDeepDive(@PathVariable traceId: String): Map<String, Any?> {
//        return telemetryService.getComprehensiveDeepDive(traceId)
//    }
//
//    @GetMapping("/exceptions")
//    fun getExceptionsPage(
//        @RequestParam(defaultValue = "20") limit: Int,
//        @RequestParam(defaultValue = "0") offset: Int
//    ): List<PulseExceptionEntry> {
//        return telemetryService.getPageData("kraft_exceptions", limit, offset, PulseExceptionEntry::class.java)
//    }
//
//    @GetMapping("/tasks")
//    fun getTasksPage(
//        @RequestParam(defaultValue = "20") limit: Int,
//        @RequestParam(defaultValue = "0") offset: Int
//    ): List<KraftTaskEvent> {
//        return telemetryService.getPageData("kraft_tasks", limit, offset, KraftTaskEvent::class.java)
//    }
//
//    @GetMapping("/outbound-http")
//    fun getOutboundHttpPage(
//        @RequestParam(defaultValue = "20") limit: Int,
//        @RequestParam(defaultValue = "0") offset: Int
//    ): List<KraftHttpClientEvent> {
//        return telemetryService.getPageData("kraft_http_client_events", limit, offset, KraftHttpClientEvent::class.java)
//    }
//
//    /**
//     * Aggregated traffic pulse with interval control (MINUTELY, HOURLY, DAILY)
//     */
//    @GetMapping("/traffic/trend")
//    fun getTrafficTrend(
//        @RequestParam(defaultValue = "24") hours: Int,
//        @RequestParam(defaultValue = "HOURLY") interval: TimeInterval
//    ): List<TrafficPoint> {
//        val range = TimeRange(Instant.now().minus(hours.toLong(), ChronoUnit.HOURS), Instant.now())
////        return analyticsProvider.getTrafficTrend(interval, range, TelemetryFilter())
//        return emptyList()
//    }
//
//    /**
//     * Returns P50, P95, and P99 latencies for the system or a specific resource.
//     */
//    @GetMapping("/latency/report")
//    fun getLatencyReport(
//        @RequestParam(required = false) resource: String?,
//        @RequestParam(defaultValue = "24") hours: Int
//    ): LatencyReport? {
//        val range = TimeRange(Instant.now().minus(hours.toLong(), ChronoUnit.HOURS), Instant.now())
////        return analyticsProvider.getLatencyPercentiles(resource, range)
//        return null
//    }
//
//    /**
//     * Top resources by traffic, error rate, or latency.
//     */
//    @GetMapping("/resources/top")
//    fun getTopResources(
//        @RequestParam(defaultValue = "10") limit: Int,
//        @RequestParam(defaultValue = "REQUEST_COUNT") sortBy: SortMetric
//    ): List<ResourceStats> {
////        return analyticsProvider.getTopResources(limit, sortBy)
//        return emptyList()
//    }
//
//    /**
//     * Distribution of status codes for the pie chart / health indicators.
//     */
//    @GetMapping("/distribution/status")
//    fun getStatusDistribution(@RequestParam(required = false) resource: String?): Map<Int, Long> {
////        return analyticsProvider.getStatusBreakdown(TelemetryFilter(resource = resource))
//        return emptyMap()
//    }
//}

@RestController
@RequestMapping("/admin/api/monitoring")
@ConditionalOnExpression(
    "\${kraftpulse.enabled:false} and \${kraftpulse.telemetry-config.enabled:false}"
)
class KraftSpringMonitoringController(
    private val analyticsReader: AnalyticsReader,
) {
    val logger: Logger = LoggerFactory.getLogger(KraftSpringMonitoringController::class.java)

    @GetMapping("/dashboard")
    fun getDashboardOverview(@RequestParam(defaultValue = "50") limit: Int): List<TelemetryWithQueries> {
        // You can use the registry here to augment the response if needed
//        val httpCounter = meterRegistry?.find("http.server.requests")?.counter()?.count() ?: 0.0
//        println("http counter = $httpCounter, meterRegistry ${meterRegistry?.meters}")
        logger.info("fetching latest with queries")
        return analyticsReader.fetchLatestWithQueries(limit)
    }

    // ✅ New — the URL to actually inspect what's accumulating right now
    @GetMapping("/metrics/internal")
    fun getInternalMetrics(): Map<String, Any?> {
        val snapshot = KraftPulseMeters.snapshot()

        // Custom Logic: Add an "Alerts" section to your output
        val alerts = mutableListOf<String>()
        val threadMetric = snapshot.find { it["name"] == "executor.active" }
        // If active threads > 90% of pool size, add to alerts

        return mapOf(
            "meterCount" to snapshot.size,
            "systemHealth" to if (alerts.isEmpty()) "OPTIMAL" else "DEGRADED",
            "activeAlerts" to alerts,
            "meters" to snapshot
        )
    }

    @GetMapping("/traces/{traceId}")
    fun getTraceDeepDive(@PathVariable traceId: String): Map<String, Any?> {
        logger.info("Trace $traceId")
        return analyticsReader.fetchComprehensiveDeepDive(traceId)
    }

    @GetMapping("/exceptions")
    fun getExceptionsPage(
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int
    ): List<PulseExceptionEntry> {
        logger.info("Get exceptions page")
        return analyticsReader.fetchAllPaged("kraft_exceptions", limit, offset, PulseExceptionEntry::class.java)
    }

    @GetMapping("/tasks")
    fun getTasksPage(
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int
    ): List<KraftTaskEvent> {
        logger.info("Get tasks page")
        val tasks = analyticsReader.fetchAllPaged("kraft_tasks", limit, offset, KraftTaskEvent::class.java)
        logger.info("found tasks $tasks")
        return tasks
    }

    @GetMapping("/outbound-http")
    fun getOutboundHttpPage(
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int
    ): List<KraftHttpClientEvent> {
        logger.info("Get outbound http page")
        return analyticsReader.fetchAllPaged("kraft_http_client_events", limit, offset, KraftHttpClientEvent::class.java)
    }

    @GetMapping("/traffic/trend")
    fun getTrafficTrend(
        @RequestParam(defaultValue = "24") hours: Int,
        @RequestParam(defaultValue = "HOURLY") interval: TimeInterval
    ): List<TrafficPoint> {
        val range = TimeRange(Instant.now().minus(hours.toLong(), ChronoUnit.HOURS), Instant.now())
        return analyticsReader.getTrafficTrend(interval, range)
    }

    @GetMapping("/latency/report")
    fun getLatencyReport(
        @RequestParam(required = false) resource: String?,
        @RequestParam(defaultValue = "24") hours: Int
    ): LatencyReport {
        val range = TimeRange(Instant.now().minus(hours.toLong(), ChronoUnit.HOURS), Instant.now())
        return analyticsReader.getLatencyPercentiles(resource, range)
    }

    @GetMapping("/resources/top")
    fun getTopResources(
        @RequestParam(defaultValue = "10") limit: Int,
        @RequestParam(defaultValue = "REQUEST_COUNT") sortBy: SortMetric
    ): List<ResourceStats> {
        return analyticsReader.getTopResources(limit, sortBy)
    }

    @GetMapping("/distribution/status")
    fun getStatusDistribution(@RequestParam(required = false) resource: String?): Map<Int, Long> {
        return analyticsReader.getStatusBreakdown(TelemetryFilter(resource = resource))
    }
}