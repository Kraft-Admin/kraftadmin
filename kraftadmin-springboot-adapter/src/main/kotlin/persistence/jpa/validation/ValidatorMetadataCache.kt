package persistence.jpa.validation

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * Shared memoization cache for reflection-derived validation metadata.
 *
 * Each validator computes its "which fields matter, and how" answer ONCE
 * per entity class, on first use. Every subsequent validate() call for that
 * class is a map lookup + direct field reads — no repeated annotation
 * scanning, no repeated hierarchy walking. Keyed by (validator class, entity
 * class) so different validators never collide even if they cache the same
 * shape of data.
 */
internal object ValidatorMetadataCache {
    private val caches = ConcurrentHashMap<Class<*>, ConcurrentHashMap<KClass<*>, Any>>()

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(
        validatorClass: Class<*>,
        entityClass: KClass<*>,
        compute: () -> T
    ): T {
        val cache = caches.getOrPut(validatorClass) { ConcurrentHashMap() }
        return cache.getOrPut(entityClass) { compute() } as T
    }
}

internal fun humanize(name: String): String =
    name.replace(Regex("([a-z0-9])([A-Z])"), "$1 $2").replaceFirstChar { it.uppercase() }