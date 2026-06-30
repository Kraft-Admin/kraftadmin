package telemetry.telemetry

interface KraftTelemetryProvider {
    fun recordLatency(name: String, duration: Long, tags: Map<String, String>)
    fun recordException(exception: Throwable, context: Map<String, Any>)
}