package discovery.descriptors.column.jpa

import com.kraftadmin.annotations.KraftAdminField
import com.kraftadmin.enums.FormInputType
import jakarta.persistence.ElementCollection
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded
import jakarta.persistence.ManyToMany
import jakarta.persistence.OneToMany
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField
import java.time.LocalDate
import java.time.LocalDateTime

class JpaTypeResolver(
    private val context: JpaColumnContext
) {

    /**
     * Resolves the UI type together with its default value.
     *
     * This is intentionally a direct extraction from ResourceGenerator.
     */
    fun resolveTypeAndDefault(
        prop: KProperty1<*, *>,
        isOneToOne: Boolean,
        isManyToOne: Boolean,
        isManyToMany: Boolean,
        isOneToMany: Boolean
    ): Pair<FormInputType, Any?> {

        val field = prop.javaField
        val classifier = prop.returnType.classifier as? KClass<*>

        return when {

            field?.isAnnotationPresent(KraftAdminField::class.java) == true -> {
                val annotation = field.getAnnotation(KraftAdminField::class.java)
                annotation.inputType to null
            }

            isOneToOne || isManyToOne -> {
                FormInputType.RELATION to null
            }

            isManyToMany || isOneToMany -> {
                FormInputType.MULTI_RELATION to emptyList<String>()
            }

            field?.isAnnotationPresent(Embedded::class.java) == true ||
                    classifier?.java?.isAnnotationPresent(Embeddable::class.java) == true -> {

                FormInputType.OBJECT to createDefaultMapForClass(classifier)
            }

            field?.isAnnotationPresent(ElementCollection::class.java) == true -> {
                FormInputType.ARRAY to emptyList<Any>()
            }

            List::class.java.isAssignableFrom(field?.type)
                    && field?.isAnnotationPresent(OneToMany::class.java) != true
                    && field?.isAnnotationPresent(ManyToMany::class.java) != true -> {

                FormInputType.ARRAY to emptyList<Any>()
            }

            classifier?.isSubclassOf(Enum::class) == true ->
                FormInputType.SELECT to null

            classifier == String::class ->
                FormInputType.TEXT to ""

            classifier == Boolean::class ->
                FormInputType.CHECKBOX to false

            classifier?.isSubclassOf(Number::class) == true ->
                FormInputType.NUMBER to 0

            classifier == LocalDate::class ->
                FormInputType.DATE to null

            classifier == LocalDateTime::class ->
                FormInputType.DATETIME to null

            else ->
                FormInputType.TEXT to null
        }
    }

    /**
     * Builds the default value map for @Embedded objects.
     */
    fun createDefaultMapForClass(
        kClass: KClass<*>?
    ): Map<String, Any?> {

        if (kClass == null) {
            return emptyMap()
        }

        val values = mutableMapOf<String, Any?>()

        try {

            kClass.memberProperties.forEach { property ->

                val (_, defaultValue) = resolveTypeAndDefault(
                    property,
                    isOneToOne = false,
                    isManyToOne = false,
                    isManyToMany = false,
                    isOneToMany = false
                )

                values[property.name] = defaultValue
            }

        } catch (_: Exception) {
        }

        return values
    }

}