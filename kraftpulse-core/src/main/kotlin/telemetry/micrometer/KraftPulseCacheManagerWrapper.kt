package telemetry.micrometer

/**
 * Wraps any Spring `org.springframework.cache.Cache` (Spring's universal
 * abstraction over Caffeine/Redis/EhCache/etc.) so hit/miss tracking works
 * regardless of WHICH cache provider the consumer chose — as long as they
 * use @Cacheable/@CacheEvict through Spring's CacheManager, this covers it.
 *
 * This is OPTIONAL — only activated if Spring's cache abstraction classes
 * are on the classpath, detected via reflection so kraftpulse-core has
 * no hard dependency on spring-context-support.
 */
object KraftPulseCacheManagerWrapper {

    /**
     * Wraps a Spring CacheManager bean (passed as Any to avoid a hard
     * spring-context-support dependency in core) so every cache it manages
     * gets hit/miss tracking transparently.
     */
    fun wrap(cacheManager: Any): Any {
        return try {
            val cacheManagerClass = Class.forName("org.springframework.cache.CacheManager")
            if (!cacheManagerClass.isInstance(cacheManager)) return cacheManager

            java.lang.reflect.Proxy.newProxyInstance(
                cacheManagerClass.classLoader,
                arrayOf(cacheManagerClass)
            ) { _, method, args ->
                if (method.name == "getCache" && args != null) {
                    val cacheName = args[0] as String
                    val realCache = method.invoke(cacheManager, *args)
                    realCache?.let { wrapCache(it, cacheName) }
                } else {
                    method.invoke(cacheManager, *(args ?: emptyArray()))
                }
            }
        } catch (e: ClassNotFoundException) {
            cacheManager // Spring cache abstraction not present — return unwrapped
        }
    }

    private fun wrapCache(realCache: Any, cacheName: String): Any {
        val cacheClass = Class.forName("org.springframework.cache.Cache")
        return java.lang.reflect.Proxy.newProxyInstance(
            cacheClass.classLoader,
            arrayOf(cacheClass)
        ) { _, method, args ->
            when (method.name) {
                "get" -> {
                    val result = method.invoke(realCache, *(args ?: emptyArray()))
                    if (result != null) KraftPulseCacheMeters.recordHit(cacheName)
                    else KraftPulseCacheMeters.recordMiss(cacheName)
                    result
                }
                "put" -> {
                    KraftPulseCacheMeters.recordPut(cacheName)
                    method.invoke(realCache, *(args ?: emptyArray()))
                }
                "evict" -> {
                    KraftPulseCacheMeters.recordEviction(cacheName)
                    method.invoke(realCache, *(args ?: emptyArray()))
                }
                else -> method.invoke(realCache, *(args ?: emptyArray()))
            }
        }
    }
}