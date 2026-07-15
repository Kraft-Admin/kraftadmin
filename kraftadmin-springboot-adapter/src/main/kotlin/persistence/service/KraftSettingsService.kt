//package persistence.service
//
//import config.KraftAdminProperties
//import json.KraftJsonSerializer
//import jakarta.annotation.PostConstruct
//import org.slf4j.LoggerFactory
//import org.springframework.beans.factory.annotation.Value
//import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
//import org.springframework.stereotype.Service
//import java.io.File
//
//@Service
//@ConditionalOnProperty(prefix = "kraftadmin", name = ["enabled"], havingValue = "true")
//class KraftSettingsService(
//    private val properties: KraftAdminProperties,
//    private val serializer: KraftJsonSerializer,
//    @param:Value("\${kraftadmin.settings-file:kraft-settings.json}") private val fileName: String
//) {
//    private val logger = LoggerFactory.getLogger(KraftSettingsService::class.java)
//
//    private val settingsFile: File = File(System.getProperty("user.dir"), fileName)
//
//    @PostConstruct
//    fun init() {
//        if (settingsFile.exists()) {
//            try {
//                val overrides = serializer.fromJson(
//                    settingsFile.readText(),
//                    KraftAdminProperties::class.java
//                )
//                merge(overrides)
//                logger.info("KraftAdmin: UI Settings synchronized from ${settingsFile.absolutePath}")
//            } catch (e: Exception) {
//                logger.error("Failed to load existing Kraft settings from ${settingsFile.name}", e)
//            }
//        } else {
//            try {
//                logger.info("KraftAdmin: No settings file found. Creating default at ${settingsFile.absolutePath}")
//                saveToFile()
//            } catch (e: Exception) {
//                logger.error("Could not bootstrap default settings file", e)
//            }
//        }
//    }
//
//    private fun saveToFile() {
//        settingsFile.writeText(serializer.toJson(properties))
//    }
//
//    fun updateSettings(newProps: KraftAdminProperties): KraftAdminProperties {
//        merge(newProps)
//        try {
//            saveToFile()
//            logger.info("Successfully persisted settings to ${settingsFile.absolutePath}")
//        } catch (e: Exception) {
//            logger.error("Failed to write settings update to disk", e)
//        }
//        return properties
//    }
//
//    open fun getCurrentProperties(): KraftAdminProperties = properties
//
//    private fun merge(source: KraftAdminProperties) {
//        // unchanged — no serializer involvement here
//        if (source.basePath.isNotBlank()) properties.basePath = source.basePath
//        if (source.title.isNotBlank()) properties.title = source.title
//        source.logoUrl?.let { if (it.isNotBlank()) properties.logoUrl = it }
//        if (source.theme.primaryColor.isNotBlank()) properties.theme.primaryColor = source.theme.primaryColor
//        properties.theme.darkMode = source.theme.darkMode
//        if (source.storage.uploadDir.isNotBlank()) properties.storage.uploadDir = source.storage.uploadDir
//        if (source.storage.publicUrlPrefix.isNotBlank()) properties.storage.publicUrlPrefix = source.storage.publicUrlPrefix
//        if (source.security.cookieName.isNotBlank()) properties.security.cookieName = source.security.cookieName
//        if (source.security.sessionExpiryMinutes > 0) properties.security.sessionExpiryMinutes = source.security.sessionExpiryMinutes
//        if (source.security.requiredRoles.isNotEmpty()) properties.security.requiredRoles = source.security.requiredRoles
//        if (source.security.protectedRoutes.isNotEmpty()) properties.security.protectedRoutes = source.security.protectedRoutes
//        if (source.pagination.defaultPageSize > 0) properties.pagination.defaultPageSize = source.pagination.defaultPageSize
//        if (source.pagination.maxPageSize > 0) properties.pagination.maxPageSize = source.pagination.maxPageSize
//        properties.features.allowDelete = source.features.allowDelete
//        properties.features.showTimestamps = source.features.showTimestamps
//        properties.features.readOnly = source.features.readOnly
//        if (source.localeConfig.defaultLanguage.isNotBlank()) properties.localeConfig.defaultLanguage = source.localeConfig.defaultLanguage
//        if (source.localeConfig.timezone.isNotBlank()) properties.localeConfig.timezone = source.localeConfig.timezone
//        if (source.telemetryConfig.cloudUrl.isNotBlank()) properties.telemetryConfig.cloudUrl = source.telemetryConfig.cloudUrl
//        properties.telemetryConfig.enabled = source.telemetryConfig.enabled
//    }
//}



package persistence.service

import config.KraftAdminProperties
import dtos.PublicKraftAdminSettings
import dtos.SettingsUpdateRequest
import dtos.toPublicSettings
import json.KraftJsonSerializer
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.io.File

@Service
@ConditionalOnProperty(prefix = "kraftadmin", name = ["enabled"], havingValue = "true")
class KraftSettingsService(
    private val properties: KraftAdminProperties,
    private val serializer: KraftJsonSerializer,
    @param:Value("\${kraftadmin.settings-file:kraft-settings.json}") private val fileName: String
) {
    private val logger = LoggerFactory.getLogger(KraftSettingsService::class.java)
    private val settingsFile: File = File(System.getProperty("user.dir"), fileName)

    @PostConstruct
    fun init() {
        logger.debug("KraftAdmin: bootstrapping settings (title='{}')", properties.title)

        if (settingsFile.exists()) {
            try {
                val overrides = serializer.fromJson(
                    settingsFile.readText(),
                    SettingsUpdateRequest::class.java
                )
                applyUpdate(overrides)
                logger.info("KraftAdmin: UI Settings synchronized from ${settingsFile.absolutePath}")
            } catch (e: Exception) {
                logger.error("Failed to load existing Kraft settings from ${settingsFile.name}", e)
            }
        } else {
            try {
                logger.info("KraftAdmin: No settings file found. Creating default at ${settingsFile.absolutePath}")
                saveToFile()
            } catch (e: Exception) {
                logger.error("Could not bootstrap default settings file", e)
            }
        }
    }

    /**
     * Persists only the editable subset — never secrets, never security
     * config. The settings file on disk is safe to commit-ignore-list-forget
     * about; it structurally cannot contain credentials.
     */
    private fun saveToFile() {
        settingsFile.writeText(serializer.toJson(toUpdateSnapshot()))
    }

    private fun toUpdateSnapshot(): SettingsUpdateRequest = SettingsUpdateRequest(
        title = properties.title,
        logoUrl = properties.logoUrl,
        theme = SettingsUpdateRequest.ThemeUpdate(properties.theme.primaryColor, properties.theme.darkMode),
        storage = SettingsUpdateRequest.StorageUpdate(properties.storage.uploadDir, properties.storage.publicUrlPrefix),
        pagination = SettingsUpdateRequest.PaginationUpdate(properties.pagination.defaultPageSize, properties.pagination.maxPageSize),
        features = SettingsUpdateRequest.FeatureUpdate(properties.features.allowDelete, properties.features.showTimestamps, properties.features.readOnly),
        localeConfig = SettingsUpdateRequest.LocaleUpdate(properties.localeConfig.defaultLanguage, properties.localeConfig.timezone),
        telemetryConfig = SettingsUpdateRequest.TelemetryUpdate(properties.telemetryConfig.cloudUrl, properties.telemetryConfig.enabled)
    )

    fun updateSettings(request: SettingsUpdateRequest): PublicKraftAdminSettings {
        applyUpdate(request)
        try {
            saveToFile()
            logger.info("Successfully persisted settings to ${settingsFile.absolutePath}")
        } catch (e: Exception) {
            logger.error("Failed to write settings update to disk", e)
        }
        return properties.toPublicSettings()
    }

    fun getPublicSettings(): PublicKraftAdminSettings = properties.toPublicSettings()

    /**
     * Every line here is opt-in: a field is only ever touched if the
     * request explicitly supplied it AND (for strings) it's non-blank.
     * There is no code path that can reach properties.security or
     * properties.enabled/basePath — SettingsUpdateRequest has no fields
     * for them, so this isn't "we chose not to merge them," it's "there is
     * nothing here that could."
     */
    private fun applyUpdate(source: SettingsUpdateRequest) {
        source.title?.takeIf { it.isNotBlank() }?.let { properties.title = it }
        source.logoUrl?.let { properties.logoUrl = it.ifBlank { null } }

        source.theme?.primaryColor?.takeIf { it.isNotBlank() }?.let { properties.theme.primaryColor = it }
        source.theme?.darkMode?.let { properties.theme.darkMode = it }

        source.storage?.uploadDir?.takeIf { it.isNotBlank() }?.let { properties.storage.uploadDir = it }
        source.storage?.publicUrlPrefix?.takeIf { it.isNotBlank() }?.let { properties.storage.publicUrlPrefix = it }

        source.pagination?.defaultPageSize?.takeIf { it > 0 }?.let { properties.pagination.defaultPageSize = it }
        source.pagination?.maxPageSize?.takeIf { it > 0 }?.let { properties.pagination.maxPageSize = it }

        source.features?.allowDelete?.let { properties.features.allowDelete = it }
        source.features?.showTimestamps?.let { properties.features.showTimestamps = it }
        source.features?.readOnly?.let { properties.features.readOnly = it }

        source.localeConfig?.defaultLanguage?.takeIf { it.isNotBlank() }?.let { properties.localeConfig.defaultLanguage = it }
        source.localeConfig?.timezone?.takeIf { it.isNotBlank() }?.let { properties.localeConfig.timezone = it }

        source.telemetryConfig?.cloudUrl?.takeIf { it.isNotBlank() }?.let { properties.telemetryConfig.cloudUrl = it }
        source.telemetryConfig?.enabled?.let { properties.telemetryConfig.enabled = it }
    }
}