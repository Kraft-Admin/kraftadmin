package persistence.jpa.save

import org.slf4j.LoggerFactory
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

/**
 * Creates new entity instances. Tries primary constructor first,
 * falls back to no-arg constructor for Java entities.
 */
class EntityInstantiator<T : Any>(private val entityClass: KClass<T>) {

    private val logger = LoggerFactory.getLogger(EntityInstantiator::class.java)

    fun newInstance(): T {
        return try {
            entityClass.primaryConstructor
                ?.takeIf { it.parameters.all { p -> p.isOptional } }
                ?.callBy(emptyMap())
                ?: entityClass.java.getDeclaredConstructor().also { it.isAccessible = true }.newInstance()
        } catch (e: Exception) {
            logger.error("Could not instantiate ${entityClass.simpleName}: ${e.message}", e)
            throw IllegalStateException("Cannot create instance of ${entityClass.simpleName}", e)
        }
    }
}