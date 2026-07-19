package security

import com.kraftadmin.logging.KraftAdminLogging
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

class AdminSecurityFilter(
    private val chain: SecurityProviderChain,
    private val loginPagePath: String = "/admin/#/auth/login",
    private val securityConfig: AdminSecurityConfig,
) : Filter {

    private val logger = KraftAdminLogging.logger(javaClass)

    private val safeMethods = setOf("GET", "HEAD", "OPTIONS")

    override fun doFilter(
        request: ServletRequest,
        response: ServletResponse,
        filterChain: FilterChain,
    ) {
        val httpRequest = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse
        val uri = httpRequest.requestURI
        val method = httpRequest.method.uppercase()

        val isAuthApi = uri.startsWith("/admin/api/auth/")
        val isStaticAsset = uri.contains("/admin/assets/") ||
                uri.startsWith("/admin/files/") ||
                uri.endsWith(".js") ||
                uri.endsWith(".css") ||
                uri.endsWith(".ico")

        val isUnauthenticatedPath = uri in UNAUTHENTICATED_PATHS ||
                uri == "/admin" ||
                uri == "/admin/"

        if (isAuthApi || isStaticAsset || isUnauthenticatedPath) {
            filterChain.doFilter(request, response)
            return
        }

        val adminRequest = httpRequest.toAdminRequest()
        val principal = chain.authenticate(adminRequest)

        if (principal == null) {
            handleUnauthenticated(httpRequest, httpResponse)
            return
        }


        val globalRoles = securityConfig.requiredRoles

        if (globalRoles.isEmpty()) {
            writeForbidden(httpResponse, "Access control misconfigured.")
            return
        }

        val hasGlobalAccess = principal.roles.any { it in globalRoles }
        if (!hasGlobalAccess) {
            writeForbidden(httpResponse, "You do not have the required permissions.")
            return
        }

        val routeRoles = resolveRouteRoles(uri)
        if (routeRoles != null && method !in safeMethods) {
            val hasRouteAccess = principal.roles.any { it in routeRoles }
            if (!hasRouteAccess) {
                writeForbidden(httpResponse, "You have read-only access to this resource.")
                return
            }
        }

        val context = AdminSecurityContext(principal)
        httpRequest.setAttribute(PRINCIPAL_ATTRIBUTE, principal)
        httpRequest.setAttribute(CONTEXT_ATTRIBUTE, context)

        try {
            filterChain.doFilter(request, response)
        } catch (e: AdminAccessDeniedException) {
            httpResponse.status = HttpServletResponse.SC_FORBIDDEN
            httpResponse.contentType = "application/json"
            httpResponse.writer.write("{\"error\":\"Forbidden\",\"detail\":\"${e.message}\"}")
        }
    }

    private fun resolveRouteRoles(uri: String): Set<String>? {
        return securityConfig.protectedRoutes.entries
            .filter { (prefix, _) -> uri.startsWith(prefix.removeSuffix("/**")) }
            .maxByOrNull { (prefix, _) -> prefix.length }
            ?.value
    }

    private fun writeForbidden(response: HttpServletResponse, message: String) {
        response.status = HttpServletResponse.SC_FORBIDDEN
        response.contentType = "application/json"
        val safeMessage = message.replace("\"", "\\\"")
        response.writer.write("{\"error\":\"Forbidden\",\"message\":\"$safeMessage\"}")
    }

    private fun handleUnauthenticated(
        request: HttpServletRequest,
        response: HttpServletResponse
    ) {
        val uri = request.requestURI
        val acceptsHtml = request.getHeader("Accept")?.contains("text/html") == true
        val isApiRequest = uri.contains("/api/")

        if (acceptsHtml && !isApiRequest) {
            if (uri != "/admin/" && uri != "/admin") {
                response.sendRedirect("/admin/")
            } else {
                response.status = 401
            }
        } else {
            response.status = 401
            response.contentType = "application/json"
            response.writer.write("{\"error\":\"Unauthorized\",\"message\":\"Session expired or invalid\"}")
        }
    }


    companion object {
        const val PRINCIPAL_ATTRIBUTE = "kraftadmin.principal"
        const val CONTEXT_ATTRIBUTE = "kraftadmin.context"

        val UNAUTHENTICATED_PATHS = setOf(
            "/admin/",
            "/admin",
            "/admin/index.html",
            "/admin/api/auth/login",
            "/admin/api/auth/logout"
        )
    }
}

private fun HttpServletRequest.toAdminRequest(): AdminRequest {
    val headers = headerNames.asSequence()
        .associateWith { getHeader(it) }
        .toMutableMap()

    cookies?.firstOrNull { it.name == "adminlib_session" }?.let {
        headers["X-Admin-Session"] = it.value
    }

    return AdminRequest(method, requestURI, headers)
}