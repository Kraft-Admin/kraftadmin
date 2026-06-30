package telemetry.micrometer

import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.simple.SimpleConfig
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import java.util.concurrent.ExecutorService
import java.util.concurrent.ConcurrentHashMap

/**
 * KraftPulse's internal MeterRegistry.
 * Automatically bootstraps JVM, System, and Performance metrics.
 */
object KraftPulseMeters {

    private val registry: MeterRegistry = SimpleMeterRegistry(SimpleConfig.DEFAULT, Clock.SYSTEM).apply {
        // Auto-bind system-level metrics
        JvmMemoryMetrics().bindTo(this)
        JvmGcMetrics().bindTo(this)
        JvmThreadMetrics().bindTo(this)
        ProcessorMetrics().bindTo(this)
    }

    // Optional: Registry for tracking external executors/caches dynamically
    private val monitoredExecutors = ConcurrentHashMap<String, ExecutorService>()

    fun registry(): MeterRegistry = registry

    /** * Helper to register thread pools for monitoring.
     * Call this from your configuration or service init.
     */
    fun monitorExecutor(executor: ExecutorService, name: String) {
        if (!monitoredExecutors.containsKey(name)) {
            ExecutorServiceMetrics.monitor(registry, executor, name)
            monitoredExecutors[name] = executor
        }
    }

    fun snapshot(): List<Map<String, Any?>> {
        return registry.meters.map { meter ->
            mapOf(
                "name" to meter.id.name,
                "type" to meter.id.type.name,
                "tags" to meter.id.tags.associate { it.key to it.value },
                "measurements" to meter.measure().associate { it.statistic.name to it.value }
            )
        }
    }
}