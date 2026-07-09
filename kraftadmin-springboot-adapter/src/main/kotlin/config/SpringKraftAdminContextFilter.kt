package config

import com.kraftadmin.context.KraftAdminContext
import com.kraftadmin.context.KraftAdminContextHolder
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import security.AdminSecurityContext
import security.SecurityProviderChain
import java.util.UUID

@Component
class SpringKraftAdminContextFilter(
    private val securityProviderChain: SecurityProviderChain
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {

        try {

            val principal = securityProviderChain.resolveCurrentUser()

            KraftAdminContextHolder.set(
                KraftAdminContext(
                    traceId = UUID.randomUUID().toString(),
                    actorUsername = principal?.username,
                    actorRoles = principal?.roles
                        ?.map { it }
                        ?.toSet()
                        ?: emptySet(),
                    tenantId = request.getHeader("X-Tenant-Id"),
                    ipAddress = request.remoteAddr,
                    userAgent = request.getHeader("User-Agent")
                )
            )

            chain.doFilter(request, response)

        } finally {
            KraftAdminContextHolder.clear()
        }
    }
}