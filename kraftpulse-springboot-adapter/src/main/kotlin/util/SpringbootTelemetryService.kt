package util

import analytics.AnalyticsProvider
import analytics.TelemetryWithQueries
import config.KraftPulseSpringKraftAdminProperties
import config.TelemetryProvider
import json.KraftJsonSerializer
import model.KraftHttpClientEvent
import model.KraftTaskEvent
import model.PulseExceptionEntry
import telemetry.KraftTelemetryService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.web.client.RestTemplate
import telemetry.KraftTelemetryEvent
import telemetry.SQLiteTelemetryProvider
import java.util.concurrent.ConcurrentLinkedQueue

open class SpringBootTelemetryService(
    private val properties: KraftPulseSpringKraftAdminProperties,
    private val serializer: KraftJsonSerializer,
    private val analyticsProvider: AnalyticsProvider,
    private val commonStore: SQLiteTelemetryProvider,
    private val restTemplate: RestTemplate // Inject the bean rather than initializing locally
) : KraftTelemetryService {

    private val logger = LoggerFactory.getLogger(SpringBootTelemetryService::class.java)
    private val liveBuffer = ConcurrentLinkedQueue<KraftTelemetryEvent>()
    private val maxBufferSize = 500

    @Async("kraftTelemetryExecutor")
    override fun record(event: KraftTelemetryEvent) {
        if (liveBuffer.size >= maxBufferSize) liveBuffer.poll()
        liveBuffer.add(event)
        commonStore.save(event)
    }

    @Async("kraftTelemetryExecutor")
    override fun recordException(exceptionData: PulseExceptionEntry) {
        commonStore.saveException(exceptionData)
    }

    @Async("kraftTelemetryExecutor")
    override fun recordTaskEvent(taskEvent: KraftTaskEvent) {
        commonStore.saveTask(taskEvent)
    }

    @Async("kraftTelemetryExecutor")
    override fun recordHttpClientEvent(event: KraftHttpClientEvent) {
        commonStore.saveHttpClientEvent(event)
    }

    // ─── ClickHouse Optimized Batch Synchronization Loop ───────────────────
    @Scheduled(fixedRate = 10000) // Every 10 seconds
    override fun flushToCloud() {
        // Guard Check
        if (properties.telemetryConfig.provider == TelemetryProvider.LOCAL) {
            return
        }

        val pendingEvents = commonStore.fetchBatch(limit = 300)
        if (pendingEvents.isEmpty()) return

        val traceIdsToUpdate = pendingEvents.map { it.traceId }.distinct()

        val pendingQueries = commonStore.fetchQueriesForTraces(traceIdsToUpdate)
        val pendingExceptions = commonStore.fetchExceptionsForTraces(traceIdsToUpdate)
        val pendingTasks = commonStore.fetchTasksForTraces(traceIdsToUpdate)
        val pendingHttpEvents = commonStore.fetchHttpClientEventsForTraces(traceIdsToUpdate)

        val batch = ClickHouseTelemetryBatch(
            events = pendingEvents,
            queries = pendingQueries,
            exceptions = pendingExceptions,
            tasks = pendingTasks,
            httpClientEvents = pendingHttpEvents
        )

        try {
            val targetBaseUrl = properties.telemetryConfig.cloudUrl ?: "http://localhost:8090"
            val cloudUrl = "${targetBaseUrl.removeSuffix("/")}/api/telemetry/ingest"

            // This now carries X-Pulse-API-Key and X-Pulse-Secret-Key automatically!
            val response = restTemplate.postForEntity(cloudUrl, batch, String::class.java)

            if (response.statusCode.is2xxSuccessful) {
                commonStore.markAsSynced(traceIdsToUpdate)
                logger.info("Successfully flushed batch: ${pendingEvents.size} traces to Ktor receiver.")
            }
        } catch (e: Exception) {
            if (e.message?.contains("400") == true) {
                logger.error("Drop Poison Pill Batch: Schema validation rejected payload. Clearing outbox boundaries.")
                commonStore.markAsSynced(traceIdsToUpdate)
            } else {
                logger.warn("ClickHouse Emission Failed: {}. Traces remain safely in SQLite for retry.", e.message)
            }
        }
    }

    override fun getPulse(limit: Int): List<KraftTelemetryEvent> = liveBuffer.toList().takeLast(limit).reversed()
    override fun purge() = liveBuffer.clear()
    override fun getDashboardOverview(limit: Int): List<TelemetryWithQueries> = analyticsProvider.fetchLatestWithQueries(limit)
    override fun getComprehensiveDeepDive(traceId: String): Map<String, Any?> = analyticsProvider.fetchComprehensiveDeepDive(traceId)
    override fun <T> getPageData(table: String, limit: Int, offset: Int, clazz: Class<T>): List<T> = fetchAllPaged(table, limit, offset, clazz)
    override fun <T> fetchAllPaged(table: String, limit: Int, offset: Int, clazz: Class<T>): List<T> = analyticsProvider.fetchAllPaged(table, limit, offset, clazz)
}

/**
 * Universal ClickHouse Ingestion Batch Model
 */
data class ClickHouseTelemetryBatch(
    val events: List<KraftTelemetryEvent>,
    val queries: List<model.QueryEvent> = emptyList(),
    val exceptions: List<PulseExceptionEntry> = emptyList(),
    val tasks: List<KraftTaskEvent> = emptyList(),
    val httpClientEvents: List<KraftHttpClientEvent> = emptyList()
)