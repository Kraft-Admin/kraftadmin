package telemetry.micrometer

import io.micrometer.core.instrument.Tags
import model.KraftHttpClientEvent
import model.KraftTaskEvent
import model.PulseExceptionEntry
import model.QueryEvent
import com.kraftadmin.model.KraftTelemetryEvent
import java.util.concurrent.TimeUnit

object KraftPulseTelemetryMeters {

    private val registry = KraftPulseMeters.registry()

    fun recordRequest(event: KraftTelemetryEvent) {
        registry.timer(
            "kraft.request.duration",
            Tags.of("resource", event.resource ?: "unknown", "method", event.action ?: "unknown", "outcome", outcomeOf(event.status))
        ).record(event.durationMs, TimeUnit.MILLISECONDS)

        registry.counter(
            "kraft.request.count",
            Tags.of("resource", event.resource ?: "unknown", "outcome", outcomeOf(event.status))
        ).increment()
    }

    fun recordException(entry: PulseExceptionEntry) {
        registry.counter(
            "kraft.exceptions",
            Tags.of("exception", entry.exceptionClass ?: "unknown", "path", entry.path ?: "unknown")
        ).increment()
    }


    fun recordTask(task: KraftTaskEvent) {
        registry.timer(
            "kraft.task.duration",
            Tags.of(
                "name", task.name,
                "type", task.type.toString() ?: "unknown",
                "status", task.status.toString() ?: "unknown"
            )
        ).record(task.durationMs, TimeUnit.MILLISECONDS)
    }

    fun recordHttpClientCall(event: KraftHttpClientEvent) {
        registry.timer(
            "kraft.http_client.duration",
            Tags.of("host", hostOf(event.url), "method", event.method ?: "unknown", "status", event.statusCode.toString())
        ).record(event.durationMs, TimeUnit.MILLISECONDS)

        registry.counter(
            "kraft.http_client.count",
            Tags.of("host", hostOf(event.url), "status", event.statusCode.toString())
        ).increment()
    }

    fun recordQuery(query: QueryEvent) {
        registry.timer(
            "kraft.query.duration",
            Tags.of("table", query.tableName ?: "unknown", "slow", query.isSlowQuery.toString())
        ).record(query.durationMs, TimeUnit.MILLISECONDS)
    }

    private fun outcomeOf(status: Int): String = when {
        status >= 500 -> "SERVER_ERROR"; status >= 400 -> "CLIENT_ERROR"; status >= 200 -> "SUCCESS"; else -> "UNKNOWN"
    }
    private fun hostOf(url: String?): String =
        try { java.net.URI(url ?: "").host ?: "unknown" } catch (e: Exception) { "unknown" }
}