package discovery.descriptors.column

import com.kraftadmin.spi.KraftAdminColumn
import com.kraftadmin.logging.KraftAdminLogging
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.superclasses

/**
 * Entry point used by ResourceGenerator.
 *
 * Handles common reflection work before delegating provider-specific
 * column generation to the selected strategy.
 */
class KraftColumnBuilder(
    private val strategy: ColumnBuildStrategy
) {

    private val logger = KraftAdminLogging.logger(javaClass)

    init {
        logger.info("KraftColumnBuilder init with strategy: $strategy")
    }

    fun build(entityClass: KClass<*>): List<KraftAdminColumn> {
        return strategy.buildColumns(
            entityClass,
            getAllProperties(entityClass)
        )
    }

    /**
     * Collect inherited properties while avoiding duplicates.
     */
    private fun getAllProperties(
        kClass: KClass<*>
    ): List<KProperty1<out Any, *>> {


        val properties = mutableListOf<KProperty1<out Any, *>>()

        var current: KClass<*>? = kClass

        while (current != null && current != Any::class) {

            current.memberProperties.forEach { property ->
                if (properties.none { it.name == property.name }) {
                    @Suppress("UNCHECKED_CAST")
                    properties += property as KProperty1<out Any, *>
                }
            }

            current = current.superclasses.firstOrNull()
        }

        return properties
    }
}