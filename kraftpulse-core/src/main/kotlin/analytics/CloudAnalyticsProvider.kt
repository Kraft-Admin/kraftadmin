package analytics

import analytics.TelemetryWithQueries
import json.KraftJsonSerializer
import model.KraftHttpClientEvent
import model.KraftTaskEvent
import model.PulseExceptionEntry
import model.QueryEvent
import telemetry.KraftTelemetryEvent
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CompletableFuture

class CloudAnalyticsProvider(
    private val apiKey: String,
    private val secretKey: String,
    private val serializer: KraftJsonSerializer,
    private val baseUrl: String
) : AnalyticsProvider {

    companion object {
        private val httpClient: HttpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()
    }

    // ─── Telemetry Writing Methods (Asynchronous & Non-blocking) ─────────────

    override fun track(event: KraftTelemetryEvent) {
        sendAsyncPost("/telemetry", event)
    }

    override fun saveTelemetryEvent(event: KraftTelemetryEvent) {
        sendAsyncPost("/telemetry", event)
    }

    override fun save(event: QueryEvent) {
        sendAsyncPost("/queries", event)
    }

    override fun saveException(exceptionData: PulseExceptionEntry) {
        sendAsyncPost("/exceptions", exceptionData)
    }

    override fun saveTask(taskEvent: KraftTaskEvent) {
        sendAsyncPost("/tasks", taskEvent)
    }

    override fun saveHttpClientEvent(event: KraftHttpClientEvent) {
        sendAsyncPost("/http-client-events", event)
    }

    // ─── Analytics Read Methods (Synchronous Aggregations) ───────────────────

    override fun getTrafficTrend(
        interval: TimeInterval,
        range: TimeRange,
        filter: TelemetryFilter
    ): List<TrafficPoint> {
        val queryParams = buildString {
            append("?interval=${interval.name}")
            append("&start=${range.start.toEpochMilli()}")
            append("&end=${range.end.toEpochMilli()}")
            filter.resource?.let { append("&resource=$it") }
            filter.actor?.let { append("&actor=$it") }
        }
        return sendGetList("/analytics/traffic-trend$queryParams", TrafficPoint::class.java)
    }

    override fun getTopResources(limit: Int, sortBy: SortMetric): List<ResourceStats> {
        val path = "/analytics/top-resources?limit=$limit&sortBy=${sortBy.name}"
        return sendGetList(path, ResourceStats::class.java)
    }

    override fun getStatusBreakdown(filter: TelemetryFilter): Map<Int, Long> {
        val queryParams = buildString {
            append("?")
            filter.resource?.let { append("resource=$it&") }
            filter.actor?.let { append("actor=$it") }
        }
        val responseJson = sendGetRaw("/analytics/status-breakdown$queryParams")
        return try {
            @Suppress("UNCHECKED_CAST")
            serializer.fromJson(responseJson, Map::class.java) as Map<Int, Long>
        } catch (e: Exception) {
            emptyMap()
        }
    }

    override fun getLatencyPercentiles(resource: String?, range: TimeRange): LatencyReport {
        val queryParams = buildString {
            append("?start=${range.start.toEpochMilli()}&end=${range.end.toEpochMilli()}")
            resource?.let { append("&resource=$it") }
        }
        return sendGetSingle("/analytics/latency-percentiles$queryParams", LatencyReport::class.java)
    }

    override fun getRegionalDistribution(range: TimeRange): Map<String, Long> {
        val path = "/analytics/regional-distribution?start=${range.start.toEpochMilli()}&end=${range.end.toEpochMilli()}"
        val responseJson = sendGetRaw(path)
        return try {
            @Suppress("UNCHECKED_CAST")
            serializer.fromJson(responseJson, Map::class.java) as Map<String, Long>
        } catch (e: Exception) {
            emptyMap()
        }
    }

    override fun getSummary(range: TimeRange): AnalyticsSummary {
        val path = "/analytics/summary?start=${range.start.toEpochMilli()}&end=${range.end.toEpochMilli()}"
        return sendGetSingle(path, AnalyticsSummary::class.java)
    }

    override fun getQueriesForTrace(traceId: String): List<QueryEvent> {
        return sendGetList("/traces/$traceId/queries", QueryEvent::class.java)
    }

    override fun getLatestWithDetails(limit: Int): List<TelemetryWithQueries> {
        return sendGetList("/traces/latest?limit=$limit", TelemetryWithQueries::class.java)
    }

    override fun fetchLatestWithQueries(limit: Int): List<TelemetryWithQueries> {
        return getLatestWithDetails(limit)
    }

    override fun fetchComprehensiveDeepDive(traceId: String): Map<String, Any?> {
        val responseJson = sendGetRaw("/traces/$traceId/deep-dive")
        return try {
            @Suppress("UNCHECKED_CAST")
            serializer.fromJson(responseJson, Map::class.java) as Map<String, Any?>
        } catch (e: Exception) {
            emptyMap()
        }
    }

    override fun <T> fetchAllPaged(table: String, limit: Int, offset: Int, clazz: Class<T>): List<T> {
        return sendGetList("/raw/$table?limit=$limit&offset=$offset", clazz)
    }

    // ─── Private Non-Framework Native HTTP Utilities ────────────────────────
    private fun buildRequest(path: String): HttpRequest.Builder {
        return HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl$path"))
            .timeout(Duration.ofSeconds(5))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            // ─── HEADERS EXPLICITLY SENT HERE ───
            .header("X-Pulse-API-Key", apiKey.toString())
            .header("X-Pulse-Secret-Key", secretKey.toString())
    }

    /**
     * Executes non-blocking async POST requests for fire-and-forget ingestion pipelines.
     */
    private fun sendAsyncPost(path: String, payload: Any): CompletableFuture<Void> {
        val jsonBody = serializer.toJson(payload)
        val request = buildRequest(path)
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build()

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenAccept { response ->
                if (response.statusCode() !in 200..299) {
                    System.err.println("❌ CloudAnalytics Ingestion Failure [$path]: Status Code ${response.statusCode()} - ${response.body()}")
                }
            }
            .exceptionally { ex ->
                System.err.println("❌ CloudAnalytics Disconnect on [$path]: ${ex.message}")
                null
            }
    }

    /**
     * Executes synchronous GET requests returning raw payload strings.
     */
    private fun sendGetRaw(path: String): String {
        val request = buildRequest(path).GET().build()
        return try {
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() in 200..299) {
                response.body()
            } else {
                System.err.println("⚠️ CloudAnalytics Read Failure [$path]: Status Code ${response.statusCode()}")
                "{}"
            }
        } catch (e: Exception) {
            System.err.println("❌ CloudAnalytics HTTP Read Error on [$path]: ${e.message}")
            "{}"
        }
    }

    private fun <T> sendGetSingle(path: String, clazz: Class<T>): T {
        val rawJson = sendGetRaw(path)
        return serializer.fromJson(rawJson, clazz)
    }

    private fun <T> sendGetList(path: String, clazz: Class<T>): List<T> {
        val rawJson = sendGetRaw(path)
        return try {
            // Evaluates JSON array tokens to build custom type conversions safely
            val structure = serializer.fromJson(rawJson, Array<Any>::class.java)
            structure.map { serializer.fromJson(serializer.toJson(it), clazz) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}