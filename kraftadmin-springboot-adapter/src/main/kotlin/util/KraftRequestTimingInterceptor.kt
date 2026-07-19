package util

import com.kraftadmin.logging.KraftAdminLogging
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.servlet.HandlerInterceptor

class KraftRequestTimingInterceptor : HandlerInterceptor {

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {

        request.setAttribute("__kraft_start", System.nanoTime())
        return true
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?
    ) {
        val start = request.getAttribute("__kraft_start") as? Long ?: return

        val elapsedMs = (System.nanoTime() - start) / 1_000_000.0

        logger.info(
            "{} {} completed in {} ms",
            request.method,
            request.requestURI,
            "%.2f".format(elapsedMs)
        )
    }

    companion object {
        private val logger = KraftAdminLogging.logger(javaClass)

    }
}