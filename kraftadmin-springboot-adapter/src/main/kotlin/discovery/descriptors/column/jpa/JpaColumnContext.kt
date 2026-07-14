package discovery.descriptors.column.jpa

import util.JakartaValidationExtractor
import kotlin.reflect.KClass

class JpaColumnContext(

    val entityClass: KClass<*>

) {

    val validationExtractor = JakartaValidationExtractor()

    val annotationResolver = JpaAnnotationResolver()

    val typeResolver = JpaTypeResolver(this)

    val lookupResolver = JpaLookupResolver(annotationResolver)

    val validationResolver = JpaValidationResolver()

    val fileResolver = JpaFileResolver()

    val visibilityResolver = JpaVisibilityResolver()

    val subColumnBuilder = JpaSubColumnBuilder(
        annotationResolver,
        typeResolver = typeResolver,
        validationResolver = validationResolver,
        fileResolver = fileResolver,
    )

}