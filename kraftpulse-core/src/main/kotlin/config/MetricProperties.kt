package config

/**
 * Controls the optional Micrometer bridge. Independent of telemetryConfig —
 * a consumer can run KraftPulse's own SQLite/cloud telemetry AND/OR the
 * Micrometer bridge, any combination, since they're separate write paths.
 */
class MetricsProperties(
    // Master switch. If false, MicrometerTelemetryBridge is never invoked
    // even if a MeterRegistry bean is present — useful if a consumer wants
    // KraftPulse's own dashboard only, without polluting their existing
    // Prometheus namespace.
    var enabled: Boolean = true,

    // Common metric name prefix — lets consumers avoid collisions if they
    // already have a "kraft" or "telemetry" namespaced metric elsewhere.
    var metricPrefix: String = "kraft.telemetry",

    // Per-event-type toggles — lets a consumer opt out of high-cardinality
    // or high-volume metrics (e.g. SQL query timing) while keeping others.
    var recordRequests: Boolean = true,
    var recordExceptions: Boolean = true,
    var recordTasks: Boolean = true,
    var recordHttpClientCalls: Boolean = true,
    var recordQueries: Boolean = true
)