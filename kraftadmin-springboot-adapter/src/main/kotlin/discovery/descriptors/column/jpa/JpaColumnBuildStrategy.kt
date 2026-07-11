//package discovery.descriptors.column.jpa
//
//
//import discovery.descriptors.column.ColumnBuildStrategy
//import com.kraftadmin.ui_descriptors.ColumnDescriptor
//import org.slf4j.LoggerFactory
//import kotlin.reflect.KClass
//import kotlin.reflect.KProperty1
//
//class JpaColumnBuildStrategy : ColumnBuildStrategy {
//
//    val logger = LoggerFactory.getLogger(JpaColumnBuildStrategy::class.java)
//
//    init {
//        logger.info("Creating column build strategy for JPA column")
//    }
//
//    fun buildColumns1(
//        entityClass: KClass<*>,
//        properties: List<KProperty1<out Any, *>>
//    ): List<ColumnDescriptor> {
//        logger.info("Creating column build strategy for JPA column with properties: $properties and entityClass: $entityClass")
//        return emptyList()
//    }
//
//    override fun buildColumns(
//        entityClass: KClass<*>,
//        properties: List<KProperty1<out Any, *>>
//    ): List<ColumnDescriptor> {
//
//        return properties.mapNotNull {
//            columnResolver.resolve(entityClass, it)
//        }
//    }
//
//}


package discovery.descriptors.column.jpa

import com.kraftadmin.spi.KraftAdminColumn
import discovery.descriptors.column.ColumnBuildStrategy
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

class JpaColumnBuildStrategy : ColumnBuildStrategy {

    private val logger = LoggerFactory.getLogger(javaClass)

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
            subColumnBuilder = context.subColumnBuilder
        )

//        logger.info("Building columns for {} {}", entityClass.simpleName, columnResolver.resolve(entityClass, properties[0]))

        val columns = properties.mapNotNull {
            columnResolver.resolve(entityClass, it)
        }
//
//        columns.forEach {
//            logger.info("column building: {}", it)
//        }

        return columns
    }


}