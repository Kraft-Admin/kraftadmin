package interceptor

import com.kraftadmin.model.GeoData
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import model.KraftTaskEvent
import model.PulseExceptionEntry
import org.slf4j.LoggerFactory
import security.AdminUserDTO
import security.SecurityProviderChain
import telemetry.KraftTelemetryService
import com.kraftadmin.model.KraftTelemetryEvent
import com.kraftadmin.model.RequestDetails
import com.kraftadmin.model.TelemetryType
import util.PulseContextHolder

class PulseTelemetryCaptor(
    private val telemetryService: KraftTelemetryService,
    private val securityChain: SecurityProviderChain
) {
    private val logger = LoggerFactory.getLogger(PulseTelemetryCaptor::class.java)

    /**
     * Captures the high-level Request metrics (Latency, User, Metadata)
     */
    fun captureRequest(request: HttpServletRequest, response: HttpServletResponse) {
        val traceId = request.getAttribute("traceId") as? String ?: return
        val startTime = request.getAttribute("startTime") as? Long ?: return
        val duration = System.currentTimeMillis() - startTime

        if (request.requestURI == "/error") return

        val currentUser = securityChain.resolveCurrentUser()
        val userAgent = request.getHeader("User-Agent") ?: ""

        // Map the expanded RequestDetails
        val requestDetails = RequestDetails(
            method = request.method,
            path = request.requestURI,
            fullUrl = request.requestURL.toString() + (request.queryString?.let { "?$it" } ?: ""),
            ipAddress = request.remoteAddr,
            userAgent = userAgent,
            referer = request.getHeader("Referer"),
            origin = request.getHeader("Origin"),
            deviceType = parseDeviceType(userAgent),
            browser = parseBrowser(userAgent),
            os = parseOS(userAgent),
            controller = request.getAttribute("controllerName") as? String, // Assuming set in your interceptor
            handlerMethod = request.getAttribute("handlerMethod") as? String,
            routePattern = request.getAttribute("routePattern") as? String,
            locale = request.locale.toString(),
            timezone = null // Requires JS-side header or session context
        )

        telemetryService.record(
            KraftTelemetryEvent(
                traceId = traceId,
                type = TelemetryType.HTTP_REQUEST,
                resource = request.requestURI,
                action = request.method,
                durationMs = duration,
                status = response.status,
                actor = currentUser?.let {
                    AdminUserDTO(it.name, it.username, it.roles, it.initials, it.avatar, it.isBridgeMode)
                },
                ipAddress = request.remoteAddr,
                userAgent = userAgent,
                referer = request.getHeader("Referer"),
                request = requestDetails, // Injecting the object here
                // Note: GeoData usually requires a GeoIP lookup service (e.g., MaxMind)
                geolocation = resolveGeoData(request),
            )
        )
    }

    fun captureException(request: HttpServletRequest, response: HttpServletResponse, ex: Exception?) {
        // Deduplication check
        if (request.getAttribute("pulse_exception_captured") == true) return

        val error = ex ?: request.getAttribute("jakarta.servlet.error.exception") as? Throwable
        if (error == null && response.status < 400) return

        val context = PulseContextHolder.get()

        // Extracting request headers into a Map
        val headers = request.headerNames.toList().associateWith { request.getHeader(it) }

        // Extracting query params
        val queryParams = request.parameterMap.mapValues { it.value.toList() }

        // Generate stack summary (first 5 lines)
        val stackSummary = error?.stackTraceToString()?.lineSequence()?.take(5)?.joinToString("\n") ?: "N/A"

        val entry = PulseExceptionEntry(
            traceId = context?.traceId ?: "N/A",
            tenantId = context?.tenantId,
            userId = context?.userId,
            exceptionClass = error?.javaClass?.name ?: "HTTP_${response.status}",
            message = error?.message ?: "Handled Error",
            stackTrace = error?.stackTraceToString() ?: "N/A",
            stackSummary = stackSummary,
            path = request.requestURI,
            method = request.method,
            statusCode = response.status,
            requestHeaders = headers,
            queryParams = queryParams,
            hostName = System.getenv("HOSTNAME") ?: "unknown-host",
            environment = System.getenv("APP_ENV") ?: "prod",
            version = System.getenv("APP_VERSION") ?: "1.0.0",
            isHandled = error != null,
            metadata = mapOf("params" to queryParams)
        )

        telemetryService.recordException(entry)
        request.setAttribute("pulse_exception_captured", true)
        logger.info("Captured error for trace [${entry.traceId}]")
    }


    /**
     * Tracks the lifecycle of background executions (Scheduled crons, Async jobs, listeners)
     */
    fun recordTask(task: KraftTaskEvent) {
        logger.info("logging task event $task")
        telemetryService.recordTaskEvent(task)
        logger.info("Task Logged [${task.type}] - ${task.name} (${task.status}) in ${task.durationMs}ms")
    }

    fun recordBackgroundException(entry: PulseExceptionEntry) {
        telemetryService.recordException(entry)
        logger.info("Captured background exception for trace [${entry.traceId}] from task pipeline.")
    }

    fun recordOutboundHttp(event: model.KraftHttpClientEvent) {
        // Forward directly to your commonStore persistence layer through the telemetryService contract
        telemetryService.recordHttpClientEvent(event)
        logger.info("Logged Outbound HTTP [${event.method}] -> ${event.url} (${event.statusCode}) in ${event.durationMs}ms")
    }


    // Simple parsing logic (Consider using a library like 'uap-java' for production)
    private fun parseDeviceType(ua: String): String = when {
        ua.contains("Mobile") -> "Mobile"
        ua.contains("Tablet") -> "Tablet"
        else -> "Desktop"
    }

    private fun parseBrowser(ua: String): String = when {
        ua.contains("Firefox") -> "Firefox"
        ua.contains("Chrome") -> "Chrome"
        else -> "Unknown"
    }

    private fun parseOS(ua: String): String = when {
        ua.contains("Android") -> "Android"
        ua.contains("iPhone") -> "iOS"
        ua.contains("Windows") -> "Windows"
        else -> "Other"
    }

    private fun resolveGeoData(request: HttpServletRequest): GeoData {
        // Example: If using Cloudflare
        val country = request.getHeader("CF-IPCountry")
        return GeoData(country = country, city = null, lat = null, lon = null)
    }

}