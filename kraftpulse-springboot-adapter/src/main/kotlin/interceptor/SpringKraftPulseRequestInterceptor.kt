package interceptor

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import model.PulseContext
import org.springframework.web.servlet.HandlerInterceptor
import util.PulseContextHolder

class SpringKraftPulseRequestInterceptor(
    private val captor: PulseTelemetryCaptor
) : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val traceId = request.getHeader("X-Kraft-Trace-Id") ?: java.util.UUID.randomUUID().toString()

        PulseContextHolder.set(PulseContext(
            traceId = traceId,
            tenantId = request.getHeader("X-Tenant-Id") ?: "default"
        ))

        request.setAttribute("traceId", traceId)
        request.setAttribute("startTime", System.currentTimeMillis())
        return true
    }

    override fun afterCompletion(request: HttpServletRequest, response: HttpServletResponse, handler: Any, ex: Exception?) {
        try {
            // 1. Log the Request performance/metadata
            captor.captureRequest(request, response)

            // 2. Log the Exception (if any)
            captor.captureException(request, response, ex)
        } finally {
            PulseContextHolder.clear()
        }
    }


}