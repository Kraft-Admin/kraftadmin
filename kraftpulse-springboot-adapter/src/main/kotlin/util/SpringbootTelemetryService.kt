//package util
//
//import config.KraftPulseSpringKraftAdminProperties
//import json.KraftJsonSerializer
//import model.PulseExceptionEntry
//import telementary.KraftTelemetryService
//import telemetry.SQLiteTelemetryProvider
//import org.slf4j.LoggerFactory
//import org.springframework.scheduling.annotation.Async
//import org.springframework.scheduling.annotation.Scheduled
//import org.springframework.web.client.RestTemplate
//import telemetry.KraftTelemetryEvent
//import java.util.concurrent.ConcurrentLinkedQueue
//
////@Service
////@ConditionalOnMissingBean(KraftTelemetryService::class)
////class SpringBootTelemetryService(
////    private val properties: SpringKraftAdminProperties,
////    private val serializer: KraftJsonSerializer,
////    private val commonStore: SQLiteTelemetryProvider = SQLiteTelemetryProvider(
////        serializer = serializer
////    )
////) : KraftTelemetryService {
//
//class SpringBootTelemetryService(
//    private val properties: KraftPulseSpringKraftAdminProperties,
//    private val serializer: KraftJsonSerializer,
//    private val commonStore: SQLiteTelemetryProvider = SQLiteTelemetryProvider(
//        serializer = serializer
//    )
//) : KraftTelemetryService {
//
//    private val logger = LoggerFactory.getLogger(SpringBootTelemetryService::class.java)
//    private val restTemplate = RestTemplate()
//
//    // RAM buffer for the instant Svelte Dashboard (Pulse)
//    private val liveBuffer = ConcurrentLinkedQueue<KraftTelemetryEvent>()
//    private val maxBufferSize = 500
//
//    @Async("kraftTelemetryExecutor")
//    override fun record(event: KraftTelemetryEvent) {
//        // 1. Maintain Local Pulse (In-Memory)
//        if (liveBuffer.size >= maxBufferSize) liveBuffer.poll()
//        liveBuffer.add(event)
//
//        // 2. Durable Persistence (SQLite)
//        // We bypass the in-memory cloudBuffer and go straight to disk
//        commonStore.save(event)
//
////        logger.info("TELEMETRY CAPTURED: {} | {}ms | Actor: {}", event.action, event.durationMs, event.actor)
//    }
//
//    @Scheduled(fixedRate = 10000) // Every 10 seconds
//    override fun flushToCloud() {
//        // 3. Fetch pending events from SQLite instead of the RAM cloudBuffer
//        val pendingEvents = commonStore.fetchBatch(limit = 100)
//
//        if (pendingEvents.isEmpty()) return
//
//        val batch = TelemetryBatch(events = pendingEvents)
//
//        try {
//            val cloudUrl = "${properties.telemetryConfig.cloudUrl}/api/telemetry/ingest"
//            val response = restTemplate.postForEntity(cloudUrl, batch, String::class.java)
//
//            if (response.statusCode.is2xxSuccessful) {
//                // 4. Success: Clear the outbox for these specific IDs
//                commonStore.deleteBatch(pendingEvents.map { it.id })
//                logger.debug("Successfully emitted ${pendingEvents.size} events to Kraftpulse")
//            }
//        } catch (e: Exception) {
//            logger.warn("Cloud Emission Failed: {}. Events remain in SQLite for retry.", e.message)
//        }
//    }
//
//    override fun recordException(entry: PulseExceptionEntry) {
//        logger.info("Recording exception: {}", entry)
//        // 1. Save entry
////        commonStore.save(entry)
//
//        // 2. 7-Day Janitor (Prune 1% of the time to avoid overhead)
//        if (java.util.concurrent.ThreadLocalRandom.current().nextInt(100) == 0) {
//            val cutoff = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
////            commonStore.execute("DELETE FROM pulse_exceptions WHERE timestamp < ?", cutoff)
////            commonStore.execute("DELETE FROM pulse_requests WHERE timestamp < ?", cutoff)
//        }
//    }
//
//    override fun getPulse(limit: Int): List<KraftTelemetryEvent> =
//        liveBuffer.toList().takeLast(limit).reversed()
//
//    override fun purge() = liveBuffer.clear()
//}

package util

import analytics.TelemetryWithQueries
import config.KraftPulseSpringKraftAdminProperties
import json.KraftJsonSerializer
import model.KraftHttpClientEvent
import model.KraftTaskEvent
import model.PulseExceptionEntry
import telementary.KraftTelemetryService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.web.client.RestTemplate
import telemetry.KraftTelemetryEvent
import telemetry.SQLiteTelemetryProvider
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ThreadLocalRandom

open class SpringBootTelemetryService(
    private val properties: KraftPulseSpringKraftAdminProperties,
    private val serializer: KraftJsonSerializer,
    private val commonStore: SQLiteTelemetryProvider
) : KraftTelemetryService {

    private val logger = LoggerFactory.getLogger(SpringBootTelemetryService::class.java)
    private val restTemplate = RestTemplate()

    // RAM buffer for the instant Svelte Dashboard (Pulse)
    private val liveBuffer = ConcurrentLinkedQueue<KraftTelemetryEvent>()
    private val maxBufferSize = 500

    @Async("kraftTelemetryExecutor")
    override fun record(event: KraftTelemetryEvent) {
        // 1. Maintain Local Pulse (In-Memory)
        if (liveBuffer.size >= maxBufferSize) liveBuffer.poll()
        liveBuffer.add(event)

        // 2. Durable Persistence (SQLite Core Telemetry)
        commonStore.save(event)
    }

    @Async("kraftTelemetryExecutor")
    override fun recordException(exceptionData: PulseExceptionEntry) {
        logger.debug("Recording exception for trace: {}", exceptionData.traceId)

        // 1. Durable Persistence (SQLite Exceptions Table)
        commonStore.saveException(exceptionData)

        // 2. 7-Day Janitor (Prune 1% of the time to avoid overhead)
        // We now use the unified pruneOldEvents from commonStore
        if (ThreadLocalRandom.current().nextInt(100) == 0) {
            commonStore.pruneOldEvents(retentionDays = 7)
            logger.info("KraftPulse: Maintenance cycle completed (Pruned old records)")
        }
    }

    override fun recordTaskEvent(taskEvent: KraftTaskEvent) {
        logger.debug("Recording task-event for trace: {}", taskEvent.traceId)
        commonStore.saveTask(taskEvent)
        if (ThreadLocalRandom.current().nextInt(100) == 0) {
            commonStore.pruneOldEvents(retentionDays = 7)
            logger.info("KraftPulse: Maintenance cycle completed (Pruned old records)")
        }
    }

    override fun recordHttpClientEvent(event: KraftHttpClientEvent) {
        logger.info("outbound http request $event")
        commonStore.saveHttpClientEvent(event)
        if (ThreadLocalRandom.current().nextInt(100) == 0) {
            commonStore.pruneOldEvents(retentionDays = 7)
            logger.info("KraftPulse: Maintenance cycle completed (Pruned old records)")
        }
    }

    @Scheduled(fixedRate = 1200000) // Every 120 seconds
    override fun flushToCloud() {
        // 1. Fetch pending events from SQLite (Outbox Pattern)
        val pendingEvents = commonStore.fetchBatch(limit = 100)
        if (pendingEvents.isEmpty()) return

        val batch = TelemetryBatch(events = pendingEvents)

        try {
            val cloudUrl = "${properties.telemetryConfig.cloudUrl}/api/telemetry/ingest"
            val response = restTemplate.postForEntity(cloudUrl, batch, String::class.java)

            if (response.statusCode.is2xxSuccessful) {
                // 2. Success: Clear the outbox for these specific IDs
                commonStore.deleteBatch(pendingEvents.map { it.id })
                logger.debug("Successfully emitted ${pendingEvents.size} events to Kraftpulse Cloud")
            }
        } catch (e: Exception) {
            logger.warn("Cloud Emission Failed: {}. Events remain in SQLite for retry.", e.message)
        }
    }

    override fun getPulse(limit: Int): List<KraftTelemetryEvent> =
        liveBuffer.toList().takeLast(limit).reversed()

    override fun purge() = liveBuffer.clear()


    override fun getDashboardOverview(limit: Int): List<TelemetryWithQueries> {
        return commonStore.fetchLatestWithQueries(limit)
    }

    override fun getComprehensiveDeepDive(traceId: String): Map<String, Any?> {
        return commonStore.fetchComprehensiveDeepDive(traceId)
    }

    override fun <T> getPageData(table: String, limit: Int, offset: Int, clazz: Class<T>): List<T> {
        return commonStore.fetchAllPaged(table, limit, offset, clazz)
    }

    // Consolidated maintenance helper to reduce boilerplate noise
    private fun triggerJanitor() {
        if (ThreadLocalRandom.current().nextInt(100) == 0) {
            commonStore.pruneOldEvents(retentionDays = 7)
            logger.info("KraftPulse: Maintenance cycle completed (Pruned old records)")
        }
    }



}

data class TelemetryBatch(
    val events: List<KraftTelemetryEvent>
)

