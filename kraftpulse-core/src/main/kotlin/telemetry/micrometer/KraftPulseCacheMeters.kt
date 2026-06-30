package telemetry.micrometer

import io.micrometer.core.instrument.Tags
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Provider-agnostic cache instrumentation. Doesn't depend on any specific
 * cache library — consumers call recordHit/recordMiss/recordEviction
 * directly, OR we wrap their CacheManager (Spring abstraction) automatically
 * if present, OR we use reflection to detect Caffeine/Redis-backed caches
 * and pull their native stats.
 */
object KraftPulseCacheMeters {

    private val registry = KraftPulseMeters.registry()
    private val knownCaches = ConcurrentHashMap<String, Boolean>()

    fun recordHit(cacheName: String) {
        registry.counter("kraft.cache.hits", Tags.of("cache", cacheName)).increment()
    }

    fun recordMiss(cacheName: String) {
        registry.counter("kraft.cache.misses", Tags.of("cache", cacheName)).increment()
    }

    fun recordEviction(cacheName: String) {
        registry.counter("kraft.cache.evictions", Tags.of("cache", cacheName)).increment()
    }

    fun recordPut(cacheName: String) {
        registry.counter("kraft.cache.puts", Tags.of("cache", cacheName)).increment()
    }

    /**
     * Reflection-based size sampler — works for ANY cache exposing a
     * `size()`/`estimatedSize()` method, without compiling against the
     * specific cache library. Called periodically (e.g. every 30s) by
     * whatever scheduler the adapter provides.
     */
    fun sampleSize(cacheName: String, cacheInstance: Any) {
        val size = tryInvokeNumeric(cacheInstance, "estimatedSize")
            ?: tryInvokeNumeric(cacheInstance, "size")
            ?: return

        registry.gauge("kraft.cache.size", Tags.of("cache", cacheName), size)
    }

    private fun tryInvokeNumeric(target: Any, methodName: String): Double? {
        return try {
            val method = target.javaClass.getMethod(methodName)
            when (val result = method.invoke(target)) {
                is Number -> result.toDouble()
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}