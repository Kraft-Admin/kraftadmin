package com.kraftadmin.config

import analytics.AnalyticsProvider
import analytics.CloudAnalyticsProvider
import analytics.LocalAnalyticsProvider
import config.KraftPulseSpringKraftAdminProperties
import config.TelemetryProvider
import json.KraftJsonSerializer
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import telemetry.SQLiteTelemetryProvider
import util.JacksonKraftJsonSerializer

//@Configuration
//@ConditionalOnProperty(prefix = "kraftpulse", name = ["enabled"], havingValue = "true", matchIfMissing = false)
class KraftAnalyticsConfiguration(
    private val properties: KraftPulseSpringKraftAdminProperties
) {

    private val logger = LoggerFactory.getLogger(javaClass)

//    @Bean
    // Only activates this fallback bean setup if telemetry is explicitly enabled in configurations
//    @ConditionalOnProperty(prefix = "kraftpulse.telemetry-config", name = ["enabled"], havingValue = "true", matchIfMissing = false)
    fun analyticsProvider(sqliteProvider: SQLiteTelemetryProvider?): AnalyticsProvider {
        val config = properties.telemetryConfig
        val providerType = config.provider ?: TelemetryProvider.LOCAL

        logger.info("KraftPulse: Initializing analytics runtime mapping context pipeline. Resolved provider target: $providerType")

        return when (providerType) {
            TelemetryProvider.CLOUD -> {
                val apiKey = config.apiKey!!
                val secretKey = config.secretKey!!

//                if (apiKey.isNullOrBlank() || secretKey.isNullOrBlank()) {
//                    throw IllegalStateException(
//                        "KraftPulse Error: Telemetry provider configured for [CLOUD] but missing mandatory authentication credentials (apiKey/secretKey)."
//                    )
//                }

                logger.info("KraftPulse: Wiring CloudAnalyticsProvider sink telemetry target.")
                CloudAnalyticsProvider(
                    apiKey, secretKey,
                    serializer = JacksonKraftJsonSerializer(),
                    baseUrl = "${properties.telemetryConfig.cloudUrl}/api/telemetry/ingest"
                )
            }
            TelemetryProvider.LOCAL -> {
                if (sqliteProvider == null) {
                    throw IllegalStateException(
                        "KraftPulse Error: Telemetry provider configured for [LOCAL] but SQLiteTelemetryProvider instance was missing in context space."
                    )
                }

                logger.info("KraftPulse: Wiring LocalAnalyticsProvider (SQLite-backed) fallback tracking database target.")
                LocalAnalyticsProvider(sqliteProvider)
            }
        }
    }
}