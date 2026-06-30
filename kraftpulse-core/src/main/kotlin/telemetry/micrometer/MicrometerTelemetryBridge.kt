package telemetry.telemetry.micrometer

import com.kraftadmin.model.KraftTelemetryEvent
import config.MetricsProperties
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import model.KraftHttpClientEvent
import model.KraftTaskEvent
import model.PulseExceptionEntry
import model.QueryEvent
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * Translates KraftPulse's domain events into Micrometer metrics.
 * Lives in kraftpulse-core so ANY adapter (Spring/Ktor/Spark/Javalin)
 * can use it if a MeterRegistry happens to be available — this is
 * NOT Spring-specific, only depends on micrometer-core.
 *
 * All methods are no-ops if registry is null — safe to call unconditionally.
 */
class MicrometerTelemetryBridge(
    private val registry: MeterRegistry?,
    private val config: MetricsProperties = MetricsProperties()
) {
    fun recordRequest(event: KraftTelemetryEvent) {
        registry ?: return
        registry.timer(
            "kraft.telemetry.request.duration",
            Tags.of(
                "resource", event.resource ?: "unknown",
                "method", event.action ?: "unknown",
                "status", event.status.toString(),
                "outcome", outcomeOf(event.status)
            )
        ).record(event.durationMs, TimeUnit.MILLISECONDS)

        registry.counter(
            "kraft.telemetry.request.count",
            Tags.of("resource", event.resource ?: "unknown", "outcome", outcomeOf(event.status))
        ).increment()
    }

    fun recordException(entry: PulseExceptionEntry) {
        registry ?: return
        registry.counter(
            "kraft.telemetry.exceptions",
            Tags.of(
                "exception", entry.exceptionClass ?: "unknown",
                "path", entry.path ?: "unknown",
                "status", entry.statusCode.toString()
            )
        ).increment()
    }

    fun recordTask(task: KraftTaskEvent) {
        registry ?: return
        registry.timer(
            "kraft.telemetry.task.duration",
//            Tags.of(
//                "name", task.name ?: "unknown",
//                "type", task.type ?: "unknown",
//                "status", task.status ?: "unknown"
//            )
        ).record(task.durationMs, TimeUnit.MILLISECONDS)
    }

    fun recordHttpClientCall(event: KraftHttpClientEvent) {
        registry ?: return
        registry.timer(
            "kraft.telemetry.http_client.duration",
            Tags.of(
                "host", hostOf(event.url),
                "method", event.method ?: "unknown",
                "status", event.statusCode.toString()
            )
        ).record(event.durationMs, TimeUnit.MILLISECONDS)
    }

    fun recordQuery(query: QueryEvent) {
        registry ?: return
        registry.timer(
            "kraft.telemetry.query.duration",
//            Tags.of(
//                "table", query.tableName ?: "unknown",
//                "type", query.queryType ?: "unknown",
//                "slow", query.isSlowQuery.toString()
//            )
        ).record(query.durationMs, TimeUnit.MILLISECONDS)

        if (query.isPotentialNPlusOne) {
            registry.counter(
                "kraft.telemetry.query.n_plus_one_suspected",
                Tags.of("table", query.tableName ?: "unknown")
            ).increment()
        }
    }

    private fun outcomeOf(status: Int): String = when {
        status >= 500 -> "SERVER_ERROR"
        status >= 400 -> "CLIENT_ERROR"
        status >= 300 -> "REDIRECTION"
        status >= 200 -> "SUCCESS"
        else -> "UNKNOWN"
    }

    private fun hostOf(url: String?): String =
        try { URI(url ?: "").host ?: "unknown" } catch (e: Exception) { "unknown" }
}