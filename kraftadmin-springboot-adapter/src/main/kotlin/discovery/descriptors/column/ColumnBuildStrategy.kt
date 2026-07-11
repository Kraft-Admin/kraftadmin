package discovery.descriptors.column

import com.kraftadmin.spi.KraftAdminColumn
import com.kraftadmin.ui_descriptors.ColumnDescriptor
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * Strategy responsible for building column descriptors for a
 * specific persistence provider.
 *
 * Implementations:
 * - JpaColumnBuildStrategy
 * - MongoColumnBuildStrategy
 * - R2dbcColumnBuildStrategy
 */
interface ColumnBuildStrategy {

    fun buildColumns(
        entityClass: KClass<*>,
        properties: List<KProperty1<out Any, *>>
    ): List<KraftAdminColumn>


}