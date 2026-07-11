package discovery.descriptors.column.jpa

import com.kraftadmin.annotations.KraftAdminField
import com.kraftadmin.annotations.KraftAdminLookup
import com.kraftadmin.ui_descriptors.LookupDescriptor
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

class JpaLookupResolver(
    private val annotationResolver: JpaAnnotationResolver
) {

    fun buildLookup(
        property: KProperty1<out Any, *>,
        javaField: java.lang.reflect.Field,
        targetEntityClass: KClass<*>?
    ): LookupDescriptor? {

        if (targetEntityClass == null) {
            return null
        }

        val fieldAnnotation = annotationResolver.resolveAnnotation(
            javaField,
            property,
            KraftAdminLookup::class
        )

        val lookupAnnotation =
            fieldAnnotation
                ?: targetEntityClass.java.getAnnotation(KraftAdminLookup::class.java)

        val searchField = when {

            lookupAnnotation != null &&
                    lookupAnnotation.displayField.isNotBlank() -> {

                lookupAnnotation.displayField
            }

            else -> discoverDefaultSearchField(targetEntityClass)
        }

        val lookupKey =
            if (
                lookupAnnotation != null &&
                lookupAnnotation.lookupKey.isNotBlank()
            ) {
                lookupAnnotation.lookupKey
            } else {
                "id"
            }

        val displayField =
            if (
                lookupAnnotation != null &&
                lookupAnnotation.displayField.isNotBlank()
            ) {
                lookupAnnotation.displayField
            } else {
                ""
            }

        return LookupDescriptor(
            targetEntity = targetEntityClass.simpleName ?: "Unknown",
            searchField = searchField,
            displayField = displayField,
            lookupKey = lookupKey
        )
    }

    /**
     * Attempts to automatically discover a suitable display field.
     *
     * Priority:
     *
     * 1. @KraftAdminField(displayField = true)
     * 2. title
     * 3. name
     * 4. label
     * 5. provider
     * 6. First String property
     * 7. id
     */
    private fun discoverDefaultSearchField(
        targetClass: KClass<*>
    ): String {

        val properties = targetClass.memberProperties

        val manuallyMarked = properties.find {

            it.javaField
                ?.getAnnotation(KraftAdminField::class.java)
                ?.displayField == true
        }

        if (manuallyMarked != null) {
            return manuallyMarked.name
        }

        val commonField = properties.find {

            it.name.lowercase() in listOf(
                "title",
                "name",
                "label",
                "provider"
            )
        }

        if (commonField != null) {
            return commonField.name
        }

        val firstString = properties.find {

            it.returnType.classifier == String::class &&
                    !it.name.contains("Id")
        }

        return firstString?.name ?: "id"
    }
}