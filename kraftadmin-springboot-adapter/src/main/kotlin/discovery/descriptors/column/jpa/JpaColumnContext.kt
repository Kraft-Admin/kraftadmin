package discovery.descriptors.column.jpa

import discovery.descriptors.column.resolvers.FileResolver
import validation.JakartaValidationExtractor
import kotlin.reflect.KClass

class JpaColumnContext(

    val entityClass: KClass<*>

) {

    val validationExtractor = JakartaValidationExtractor()

    val annotationResolver = JpaAnnotationResolver()

    val typeResolver = JpaTypeResolver(this)

    val lookupResolver = JpaLookupResolver(annotationResolver)

    val validationResolver = ValidationResolver()

    val fileResolver = FileResolver()

    val visibilityResolver = JpaVisibilityResolver()

    val subColumnBuilder = JpaSubColumnBuilder(
        annotationResolver,
        typeResolver = typeResolver,
        validationResolver = validationResolver,
        fileResolver = fileResolver,
    )

}