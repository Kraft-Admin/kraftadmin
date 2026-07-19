package discovery.descriptors.column.jpa

import com.kraftadmin.spi.KraftAdminColumn
import discovery.descriptors.column.ColumnBuildStrategy
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

class JpaColumnBuildStrategy : ColumnBuildStrategy {


    override fun buildColumns(
        entityClass: KClass<*>,
        properties: List<KProperty1<out Any, *>>
    ): List<KraftAdminColumn> {

        val context = JpaColumnContext(entityClass)

        val columnResolver = JpaColumnResolver(
            annotationResolver = JpaAnnotationResolver(),
            typeResolver = JpaTypeResolver(context),
            lookupResolver = JpaLookupResolver(context.annotationResolver),
            validationResolver = JpaValidationResolver(),
            fileResolver = JpaFileResolver(),
            visibilityResolver = JpaVisibilityResolver(),
            subColumnBuilder = context.subColumnBuilder,
            elementCollectionResolver = ElementCollectionResolver
        )

        val columns = properties.mapNotNull {
            columnResolver.resolve(entityClass, it)
        }

        return columns
    }


}