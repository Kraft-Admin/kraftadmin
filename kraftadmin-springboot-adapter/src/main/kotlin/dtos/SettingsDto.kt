package dtos

import config.KraftAdminProperties
import com.fasterxml.jackson.annotation.JsonProperty


/**
 * What the UI is allowed to SEE. Secrets (telemetry apiKey/secretKey,
 * basicAuth password) and the full requiredRoles/protectedRoutes maps are
 * never included here — allow-list, not a "remember to redact" list.
 */
class PublicKraftAdminSettings {
    var basePath: String = ""
    var title: String = ""
    var logoUrl: String? = null
    var version: String = ""
    var theme: Theme = Theme()
    var storage: Storage = Storage()
    var security: SecurityDisplay = SecurityDisplay()
    var pagination: Pagination = Pagination()
    var features: Features = Features()
    var localeConfig: Locale = Locale()
    var telemetryConfig: Telemetry = Telemetry()

    class Theme {
        var primaryColor: String = ""
        var darkMode: Boolean = false
    }

    class Storage {
        var uploadDir: String = ""
        var publicUrlPrefix: String = ""
    }

    class SecurityDisplay {
        var cookieName: String = ""
        var sessionExpiryMinutes: Long = 0
    }

    class Pagination {
        var defaultPageSize: Int = 0
        var maxPageSize: Int = 0
    }

    class Features {
        var allowDelete: Boolean = false
        var showTimestamps: Boolean = false
        var readOnly: Boolean = false
    }

    class Locale {
        var defaultLanguage: String = ""
        var timezone: String = ""
    }

    class Telemetry {
        var cloudUrl: String = ""
        var enabled: Boolean = false
    }
}

fun KraftAdminProperties.toPublicSettings(): PublicKraftAdminSettings {
    val publicSettings = PublicKraftAdminSettings()

    // Assign top-level properties
    publicSettings.basePath = this.basePath
    publicSettings.title = this.title
    publicSettings.logoUrl = this.logoUrl
    publicSettings.version = this.version

    // Assign nested object properties
    publicSettings.theme.apply {
        primaryColor = this@toPublicSettings.theme.primaryColor
        darkMode = this@toPublicSettings.theme.darkMode
    }

    publicSettings.storage.apply {
        uploadDir = this@toPublicSettings.storage.uploadDir
        publicUrlPrefix = this@toPublicSettings.storage.publicUrlPrefix
    }

    publicSettings.security.apply {
        cookieName = this@toPublicSettings.security.cookieName
        sessionExpiryMinutes = this@toPublicSettings.security.sessionExpiryMinutes
    }

    publicSettings.pagination.apply {
        defaultPageSize = this@toPublicSettings.pagination.defaultPageSize
        maxPageSize = this@toPublicSettings.pagination.maxPageSize
    }

    publicSettings.features.apply {
        allowDelete = this@toPublicSettings.features.allowDelete
        showTimestamps = this@toPublicSettings.features.showTimestamps
        readOnly = this@toPublicSettings.features.readOnly
    }

    publicSettings.localeConfig.apply {
        defaultLanguage = this@toPublicSettings.localeConfig.defaultLanguage
        timezone = this@toPublicSettings.localeConfig.timezone
    }

    publicSettings.telemetryConfig.apply {
        cloudUrl = this@toPublicSettings.telemetryConfig.cloudUrl
        enabled = this@toPublicSettings.telemetryConfig.enabled
    }

    return publicSettings
}

/**
 * What the UI is allowed to CHANGE. Every field is nullable/omittable —
 * null means "leave unchanged," not "reset to default." basePath, enabled,
 * version, and the entire security.* block are structurally absent: there
 * is no field here a client could populate to touch them, no matter what
 * gets sent in the request body.
 */

class SettingsUpdateRequest {
    @JsonProperty("title") var title: String? = null
    @JsonProperty("logoUrl") var logoUrl: String? = null
    @JsonProperty("theme") var theme: ThemeUpdate? = null
    @JsonProperty("storage") var storage: StorageUpdate? = null
    @JsonProperty("pagination") var pagination: PaginationUpdate? = null
    @JsonProperty("features") var features: FeatureUpdate? = null
    @JsonProperty("localeConfig") var localeConfig: LocaleUpdate? = null
    @JsonProperty("telemetryConfig") var telemetryConfig: TelemetryUpdate? = null

    class ThemeUpdate {
        @JsonProperty("primaryColor") var primaryColor: String? = null
        @JsonProperty("darkMode") var darkMode: Boolean? = null
    }

    class StorageUpdate {
        @JsonProperty("uploadDir") var uploadDir: String? = null
        @JsonProperty("publicUrlPrefix") var publicUrlPrefix: String? = null
    }

    class PaginationUpdate {
        @JsonProperty("defaultPageSize") var defaultPageSize: Int? = null
        @JsonProperty("maxPageSize") var maxPageSize: Int? = null
    }

    class FeatureUpdate {
        @JsonProperty("allowDelete") var allowDelete: Boolean? = null
        @JsonProperty("showTimestamps") var showTimestamps: Boolean? = null
        @JsonProperty("readOnly") var readOnly: Boolean? = null
    }

    class LocaleUpdate {
        @JsonProperty("defaultLanguage") var defaultLanguage: String? = null
        @JsonProperty("timezone") var timezone: String? = null
    }

    class TelemetryUpdate {
        @JsonProperty("cloudUrl") var cloudUrl: String? = null
        @JsonProperty("enabled") var enabled: Boolean? = null
    }
}