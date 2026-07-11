package discovery.descriptors.column.jpa

import java.lang.reflect.Field
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaGetter

class JpaAnnotationResolver {

    /**
     * Resolves an annotation from:
     *
     * 1. Java field
     * 2. Kotlin getter
     * 3. Kotlin property
     * 4. Declared Kotlin member property
     * 5. Java setter
     */
    fun <T : Annotation> resolveAnnotation(
        javaField: Field?,
        prop: KProperty<*>?,
        annotationClass: KClass<T>
    ): T? {

        javaField?.getAnnotation(annotationClass.java)?.let {
            return it
        }

        prop?.javaGetter
            ?.getAnnotation(annotationClass.java)
            ?.let {
                return it
            }

        prop?.annotations
            ?.filterIsInstance(annotationClass.java)
            ?.firstOrNull()
            ?.let {
                return it
            }

        prop?.let { property ->

            runCatching {

                javaField?.declaringClass
                    ?.kotlin
                    ?.memberProperties
                    ?.find { it.name == property.name }
                    ?.annotations
                    ?.filterIsInstance(annotationClass.java)
                    ?.firstOrNull()

            }.getOrNull()

        }?.let {
            return it
        }

        javaField?.declaringClass
            ?.methods
            ?.firstOrNull {
                it.name == "set${javaField.name.replaceFirstChar { c -> c.uppercase() }}"
            }
            ?.getAnnotation(annotationClass.java)
            ?.let {
                return it
            }

        return null
    }

    fun hasOneToOne(
        field: Field,
        property: KProperty<*>
    ): Boolean =
        hasRelation(field, property, OneToOne::class)

    fun hasManyToOne(
        field: Field,
        property: KProperty<*>
    ): Boolean =
        hasRelation(field, property, ManyToOne::class)

    fun hasOneToMany(
        field: Field,
        property: KProperty<*>
    ): Boolean =
        hasRelation(field, property, OneToMany::class)

    fun hasManyToMany(
        field: Field,
        property: KProperty<*>
    ): Boolean =
        hasRelation(field, property, ManyToMany::class)

    private fun hasRelation(
        field: Field,
        property: KProperty<*>,
        annotation: KClass<out Annotation>
    ): Boolean {

        return field.isAnnotationPresent(annotation.java)
                || property.javaGetter?.isAnnotationPresent(annotation.java) == true
                || property.annotations.any {
            it.annotationClass == annotation
        }

    }

}