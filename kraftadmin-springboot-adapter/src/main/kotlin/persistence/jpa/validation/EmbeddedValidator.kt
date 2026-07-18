package persistence.jpa.validation

import com.kraftadmin.annotations.KraftAdminField
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import persistence.error.PersistenceErrorDetails
import persistence.error.PersistenceException
import java.lang.reflect.Field
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

class EmbeddedValidator : PersistenceValidator {

    private data class EmbeddedField(val field: Field, val label: String)
    private data class RequiredSubField(val field: Field, val label: String, val checkBlank: Boolean)

    override fun validate(context: ValidationContext<*>) {
        val embeddedFields = ValidatorMetadataCache.get(javaClass, context.entityClass) {
            resolveEmbeddedFields(context.entityClass)
        }
        if (embeddedFields.isEmpty()) return

        for (ef in embeddedFields) {
            ef.field.isAccessible = true
            val embeddedInstance = ef.field.get(context.entity) ?: continue // null embeddable: RequiredFieldValidator's job if it's itself required

            val subFields = ValidatorMetadataCache.get(
                EmbeddedValidator::class.java, // shared sub-cache key
                embeddedInstance::class
            ) {
                resolveRequiredSubFields(embeddedInstance::class)
            }

            if (subFields != null) {
                for (sf in subFields) {
                    sf.field.isAccessible = true
                    val value = sf.field.get(embeddedInstance)
                    val missing = value == null || (sf.checkBlank && value is String && value.isBlank())

                    if (missing) {
                        throw PersistenceException(
                            PersistenceErrorDetails(
                                "required_field",
                                "'${ef.label} → ${sf.label}' is required."
                            )
                        )
                    }
                }
            }
        }
    }

    private fun resolveEmbeddedFields(entityClass: KClass<*>): List<EmbeddedField> {
        val result = mutableListOf<EmbeddedField>()
        entityClass.memberProperties.forEach { prop ->
            val field = prop.javaField ?: return@forEach
            if (field.isAnnotationPresent(Embedded::class.java)) {
                result += EmbeddedField(field, humanize(field.name))
            }
        }
        return result
    }

    private fun resolveRequiredSubFields(embeddableClass: KClass<*>): List<RequiredSubField> {
        val result = mutableListOf<RequiredSubField>()
        embeddableClass.memberProperties.forEach { prop ->
            val field = prop.javaField ?: return@forEach

            // Nested @Embedded-within-@Embedded (e.g. Address.geo): recurse
            // by treating it like any other embeddable at validate() time —
            // this cache only tracks ONE level of required-ness per call,
            // but validate() re-enters via the outer loop naturally since
            // every embeddable instance gets its own cache key by its own
            // runtime KClass.
            if (field.isAnnotationPresent(Embedded::class.java)) return@forEach

            val admin = field.getAnnotation(KraftAdminField::class.java)
            val column = field.getAnnotation(Column::class.java)
            val required = admin?.required == true || column?.nullable == false
            if (!required) return@forEach

            result += RequiredSubField(field, humanize(field.name), field.type == String::class.java)
        }
        return result
    }

}