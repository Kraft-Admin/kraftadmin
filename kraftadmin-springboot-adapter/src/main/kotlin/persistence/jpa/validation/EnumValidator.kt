package persistence.jpa.validation

import com.kraftadmin.annotations.KraftAdminField
import jakarta.persistence.Column
import jakarta.persistence.Id
import jakarta.persistence.Version
import persistence.error.PersistenceErrorDetails
import persistence.error.PersistenceException
import java.lang.reflect.Field
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

class EnumValidator : PersistenceValidator {

    private data class EnumField(val field: Field, val label: String, val required: Boolean)

    override fun validate(context: ValidationContext<*>) {
        val fields = ValidatorMetadataCache.get(javaClass, context.entityClass) {
            resolveEnumFields(context.entityClass)
        }

        for (ef in fields) {
            ef.field.isAccessible = true
            val value = ef.field.get(context.entity)

            if (value == null && ef.required) {
                throw PersistenceException(
                    PersistenceErrorDetails(
                        "invalid_value",
                        "'${ef.label}' has an invalid or unrecognized value."
                    )
                )
            }
        }
    }

    private fun resolveEnumFields(entityClass: KClass<*>): List<EnumField> {
        val result = mutableListOf<EnumField>()

        entityClass.memberProperties.forEach { prop ->
            val field = prop.javaField ?: return@forEach
            if (!field.type.isEnum) return@forEach
            if (field.isAnnotationPresent(Id::class.java)) return@forEach
            if (field.isAnnotationPresent(Version::class.java)) return@forEach

            val admin = field.getAnnotation(KraftAdminField::class.java)
            val column = field.getAnnotation(Column::class.java)
            val required = admin?.required == true || column?.nullable == false

            result += EnumField(field, humanize(field.name), required)
        }

        return result
    }

}