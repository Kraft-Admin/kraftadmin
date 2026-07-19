package config

import com.kraftadmin.logging.KraftAdminLogging
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment
import security.AdminSecurityConfig
import security.AdminSecurityFilter
import security.AdminSecurityProvider
import security.AdminSessionStore
import security.BuiltinBasicAuthProvider
import security.SecurityProviderChain
import security.SessionConfig
import security.SessionSecurityProvider
import security.SpringSecurityAdapter

@AutoConfiguration
@ConditionalOnProperty(prefix = "kraftadmin", name = ["enabled"], havingValue = "true")
class KraftAdminSpringSecurityConfig(
    private val properties: KraftAdminProperties,
    private val env: Environment,
) {

    private val log = KraftAdminLogging.logger(javaClass)


    @Bean
    @ConditionalOnMissingBean
    fun adminSecurityConfig(): AdminSecurityConfig {
        val configuredRoles = properties.security.requiredRoles

        require(configuredRoles.isNotEmpty()) {
            "kraftadmin.security.required-roles is not configured. " +
                    "This application must explicitly define which role(s) grant access " +
                    "to the admin panel — there is no safe default. " +
                    "Set e.g. kraftadmin.security.required-roles[0]=ROLE_ADMIN in your configuration."
        }

        val basePath = properties.basePath.removeSuffix("/")

        val normalizedProtectedRoutes = properties.security.protectedRoutes
            .mapKeys { (pattern, _) ->
                if (pattern.startsWith(basePath)) pattern
                else "$basePath${if (pattern.startsWith("/")) "" else "/"}$pattern"
            }

        return AdminSecurityConfig(
            requiredRoles = configuredRoles,
            protectedRoutes = normalizedProtectedRoutes,
            frameworkAdapterFactory = { SpringSecurityAdapter() },
            frameworkSecurityActiveCheck = { isSpringSecurityActive() },
        )
    }

    @Bean
    fun adminSessionStore(config: AdminSecurityConfig): AdminSessionStore =
        AdminSessionStore(config.sessionConfig)

    @Bean
    fun sessionConfig(): SessionConfig = SessionConfig()


    @Bean
    fun builtinBasicAuthProvider(): BuiltinBasicAuthProvider {
        val basicAuthConfig = properties.security.basicAuth
        return BuiltinBasicAuthProvider(basicAuthConfig)
    }

    /**
     * The unified security chain used by both the Filter and the AuthController.
     */
    @Bean
    fun securityProviderChain(
        config: AdminSecurityConfig,
        sessionStore: AdminSessionStore,
        builtinProvider: BuiltinBasicAuthProvider
    ): SecurityProviderChain {
        val providers = mutableListOf<AdminSecurityProvider>()

        if (isSpringSecurityActive()) {
            // ONLY use the adapter.
            // This stops the library from trying to manage its own "admin" user.
            providers.add(SpringSecurityAdapter())
        } else {
            // No parent security? Use our standalone session + basic auth.
            providers.add(SessionSecurityProvider(sessionStore))
            providers.add(builtinProvider)
        }

        return SecurityProviderChain(providers.sortedBy { it.priority })
    }

    @Bean
    fun adminSecurityFilter(
        chain: SecurityProviderChain
    ): FilterRegistrationBean<AdminSecurityFilter> {
        val registration = FilterRegistrationBean(AdminSecurityFilter(
            chain,
            securityConfig = adminSecurityConfig()
        ))
        registration.addUrlPatterns("/admin/*")

        // Use a positive number to ensure we are well outside
        // the Spring Security internal filter chain range.
        registration.order = 100
        return registration
    }

    @Bean
    @ConditionalOnMissingBean
    fun kraftAdminContextFilter(
        securityProviderChain: SecurityProviderChain
    ) = SpringKraftAdminContextFilter(securityProviderChain)

    companion object {
        @JvmStatic
        fun isSpringSecurityActive(): Boolean = try {
            Class.forName(
                "org.springframework.security.web.SecurityFilterChain",
                false,
                KraftAdminSpringSecurityConfig::class.java.classLoader
            )
            true
        } catch (_: ClassNotFoundException) {
            false
        }
    }
}