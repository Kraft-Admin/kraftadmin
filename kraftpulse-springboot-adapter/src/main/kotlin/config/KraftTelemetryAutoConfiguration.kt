//package config
//
//import analytics.AnalyticsProvider
//import analytics.CloudAnalyticsProvider
//import analytics.LocalAnalyticsProvider
//import json.KraftJsonSerializer
//import org.slf4j.LoggerFactory
//import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
//import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
//import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
//import org.springframework.context.ApplicationEventPublisher
//import org.springframework.context.annotation.Bean
//import org.springframework.context.annotation.Configuration
//import org.springframework.context.annotation.Primary
//import org.springframework.core.env.Environment
//import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
//import telemetry.KraftTelemetryService
//import telemetry.SQLiteTelemetryProvider
//import telemetry.NoOpTelemetryService
//import util.JacksonKraftJsonSerializer
//import util.SpringBootTelemetryService
//import java.util.concurrent.Executor
//
//@Configuration
//class KraftTelemetryAutoConfiguration(
//    private val environment: Environment,
//    private val properties: KraftPulseSpringKraftAdminProperties
//) {
//
//    private val logger = LoggerFactory.getLogger(javaClass)
//
//    @Bean(destroyMethod = "close")
//    @ConditionalOnMissingBean
//    @ConditionalOnExpression(
//        "\${kraftpulse.enabled:true} and \${kraftpulse.telemetry-config.enabled:true}"
//    )
//    fun sqliteTelemetryProvider(
//        publisher: ApplicationEventPublisher
//    ): SQLiteTelemetryProvider {
//        // Resolve database location path safely
//        val path = properties.telemetryConfig.path ?: ".kraft-telemetry.db"
//
//        logger.info("KraftPulse: Initializing embedded engine storage target at -> $path")
//        val provider = SQLiteTelemetryProvider(
//            appName = environment.getProperty("spring.application.name") ?: "KraftPulse",
//            serializer = kraftPulseJsonSerializer()
//            // Pass the local path parameter to your provider constructor here if required
//        )
//
//        // The Bridge: SQLite Write -> Spring Event -> Controller SSE / Analytics
//        provider.onEventPersisted = { event ->
//            publisher.publishEvent(event)
//        }
//
//        return provider
//    }
//
//    @Bean
//    @Primary
//    @ConditionalOnExpression(
//        "\${kraftpulse.enabled:true} and \${kraftpulse.telemetry-config.enabled:true}"
//    )
//    fun telemetryService(
//        kraftJsonSerializer: KraftJsonSerializer,
//        provider: SQLiteTelemetryProvider,
//        analyticsProvider: AnalyticsProvider
//    ): KraftTelemetryService {
//        return SpringBootTelemetryService(
//            properties = properties,
//            serializer = kraftJsonSerializer,
//            commonStore = provider,
//            analyticsProvider = analyticsProvider,
//        )
//    }
//
//    @Bean(name = ["kraftTelemetryExecutor"])
//    @ConditionalOnExpression(
//        "\${kraftpulse.enabled:true} and \${kraftpulse.telemetry-config.enabled:true}"
//    )
//    fun kraftTelemetryExecutor(): Executor {
//        val executor = ThreadPoolTaskExecutor()
//        executor.corePoolSize = 2
//        executor.maxPoolSize = 5
//        executor.setQueueCapacity(500)
//        executor.setThreadNamePrefix("KraftTelemetry-")
//        executor.initialize()
//        return executor
//    }
//
//    /**
//     * The Fallback: This satisfies components like the Auditor or ErrorAttributes
//     * when the real telemetryService is disabled in YAML.
//     */
//    @Bean
//    @ConditionalOnMissingBean(KraftTelemetryService::class)
//    fun noOpTelemetryService(): KraftTelemetryService = NoOpTelemetryService()
//
//    @Bean
//    @Primary
//    fun kraftPulseJsonSerializer(): KraftJsonSerializer = JacksonKraftJsonSerializer()
//
//    @Bean
//    @ConditionalOnProperty(prefix = "kraftpulse.telemetry-config", name = ["enabled"], havingValue = "true", matchIfMissing = false)
//    @ConditionalOnMissingBean(AnalyticsProvider::class)
//    fun analyticsProvider(sqliteProvider: SQLiteTelemetryProvider?): AnalyticsProvider {
//        val config = properties.telemetryConfig
//        val providerType = config.provider ?: TelemetryProvider.LOCAL
//
//        logger.info("KraftPulse: Initializing analytics runtime mapping context pipeline. Resolved provider target: $providerType")
//
//        return when (providerType) {
//            TelemetryProvider.CLOUD -> {
//                val apiKey = config.apiKey ?: throw IllegalStateException(
//                    "KraftPulse Error: Telemetry provider configured for [CLOUD] but missing mandatory apiKey authentication key."
//                )
//                val secretKey = config.secretKey ?: throw IllegalStateException(
//                    "KraftPulse Error: Telemetry provider configured for [CLOUD] but missing mandatory secretKey authentication key."
//                )
//
//                // Dynamic environment parsing logic for url mapping
//                val targetBaseUrl = config.cloudUrl
//                    ?: environment.getProperty("kraftpulse.telemetry-config.cloud-url")
//                    ?: "http://localhost:8090"
//
//                // Cleanly remove any trailing slashes to enforce predictable endpoint formats
//                val resolvedIngestUrl = "${targetBaseUrl.removeSuffix("/")}/api/telemetry/ingest"
//
//                logger.info("KraftPulse: Wiring CloudAnalyticsProvider target sink -> $resolvedIngestUrl")
//                CloudAnalyticsProvider(
//                    apiKey = apiKey,
//                    secretKey = secretKey,
//                    serializer = kraftPulseJsonSerializer(),
//                    baseUrl = resolvedIngestUrl
//                )
//            }
//            TelemetryProvider.LOCAL -> {
//                if (sqliteProvider == null) {
//                    throw IllegalStateException(
//                        "KraftPulse Error: Telemetry provider configured for [LOCAL] but SQLiteTelemetryProvider instance was missing in context space."
//                    )
//                }
//
//                logger.info("KraftPulse: Wiring LocalAnalyticsProvider (SQLite-backed) fallback tracking database target.")
//                LocalAnalyticsProvider(sqliteProvider)
//            }
//        }
//    }
//}

package config

import analytics.AnalyticsProvider
import analytics.CloudAnalyticsProvider
import analytics.LocalAnalyticsProvider
import json.KraftJsonSerializer
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.env.Environment
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.web.client.RestTemplate
import telemetry.KraftTelemetryService
import telemetry.SQLiteTelemetryProvider
import telemetry.NoOpTelemetryService
import util.JacksonKraftJsonSerializer
import util.SpringBootTelemetryService
import java.util.concurrent.Executor

@Configuration
class KraftTelemetryAutoConfiguration(
    private val environment: Environment,
    private val properties: KraftPulseSpringKraftAdminProperties
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Bean
    @ConditionalOnExpression(
        "\${kraftpulse.enabled:true} and \${kraftpulse.telemetry-config.enabled:true}"
    )
    fun telemetryRestTemplate(): RestTemplate {
        val restTemplate = RestTemplate()
        val config = properties.telemetryConfig

        // Automatically inject the API Key and Secret Key headers into every outgoing sync request
        restTemplate.interceptors.add(ClientHttpRequestInterceptor { request, body, execution ->
            request.headers.add("Content-Type", "application/json")
            request.headers.add("Accept", "application/json")
            config.apiKey?.let { request.headers.add("X-Pulse-API-Key", it) }
            config.secretKey?.let { request.headers.add("X-Pulse-Secret-Key", it) }
            execution.execute(request, body)
        })

        return restTemplate
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    @ConditionalOnExpression(
        "\${kraftpulse.enabled:true} and \${kraftpulse.telemetry-config.enabled:true}"
    )
    fun sqliteTelemetryProvider(
        publisher: ApplicationEventPublisher
    ): SQLiteTelemetryProvider {
        val path = properties.telemetryConfig.path ?: ".kraft-telemetry.db"
        val provider = SQLiteTelemetryProvider(
            appName = environment.getProperty("spring.application.name") ?: "KraftPulse",
            serializer = kraftPulseJsonSerializer()
        )
        provider.onEventPersisted = { event -> publisher.publishEvent(event) }
        return provider
    }

    @Bean
    @Primary
    @ConditionalOnExpression(
        "\${kraftpulse.enabled:true} and \${kraftpulse.telemetry-config.enabled:true}"
    )
    fun telemetryService(
        kraftJsonSerializer: KraftJsonSerializer,
        provider: SQLiteTelemetryProvider,
        analyticsProvider: AnalyticsProvider,
        telemetryRestTemplate: RestTemplate // Inject the secure RestTemplate here
    ): KraftTelemetryService {
        return SpringBootTelemetryService(
            properties = properties,
            serializer = kraftJsonSerializer,
            commonStore = provider,
            analyticsProvider = analyticsProvider,
            restTemplate = telemetryRestTemplate // Pass it along
        )
    }

    @Bean(name = ["kraftTelemetryExecutor"])
    @ConditionalOnExpression(
        "\${kraftpulse.enabled:true} and \${kraftpulse.telemetry-config.enabled:true}"
    )
    fun kraftTelemetryExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 2
        executor.maxPoolSize = 5
        executor.setQueueCapacity(500)
        executor.setThreadNamePrefix("KraftTelemetry-")
        executor.initialize()
        return executor
    }

    @Bean
    @ConditionalOnMissingBean(KraftTelemetryService::class)
    fun noOpTelemetryService(): KraftTelemetryService = NoOpTelemetryService()

    @Bean
    @Primary
    fun kraftPulseJsonSerializer(): KraftJsonSerializer = JacksonKraftJsonSerializer()

    @Bean
    @ConditionalOnProperty(prefix = "kraftpulse.telemetry-config", name = ["enabled"], havingValue = "true", matchIfMissing = false)
    @ConditionalOnMissingBean(AnalyticsProvider::class)
    fun analyticsProvider(sqliteProvider: SQLiteTelemetryProvider?): AnalyticsProvider {
        val config = properties.telemetryConfig
        val providerType = config.provider ?: TelemetryProvider.LOCAL

        return when (providerType) {
            TelemetryProvider.CLOUD -> {
                val apiKey = config.apiKey ?: throw IllegalStateException("Missing apiKey")
                val secretKey = config.secretKey ?: throw IllegalStateException("Missing secretKey")
                val targetBaseUrl = config.cloudUrl ?: "http://localhost:8090"

                CloudAnalyticsProvider(
                    apiKey = apiKey,
                    secretKey = secretKey,
                    serializer = kraftPulseJsonSerializer(),
                    baseUrl = targetBaseUrl.removeSuffix("/")
                )
            }
            TelemetryProvider.LOCAL -> {
                if (sqliteProvider == null) throw IllegalStateException("Missing SQLiteTelemetryProvider")
                LocalAnalyticsProvider(sqliteProvider)
            }
        }
    }
}