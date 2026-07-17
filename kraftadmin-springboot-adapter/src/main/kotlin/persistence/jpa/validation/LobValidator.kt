package persistence.jpa.validation

import jakarta.persistence.Column
import jakarta.persistence.Lob
import persistence.error.PersistenceErrorDetails
import persistence.error.PersistenceException
import java.lang.reflect.Field
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

class LobValidator : PersistenceValidator {

    companion object {
        // Fallback ceiling when no explicit @Column(length=...) is set.
        private const val DEFAULT_MAX_LENGTH = 10 * 1024 * 1024 // 10 MB
    }

    private data class LobField(val field: Field, val label: String, val maxLength: Int)

    override fun validate(context: ValidationContext<*>) {
        val fields = ValidatorMetadataCache.get(javaClass, context.entityClass) {
            resolveLobFields(context.entityClass)
        }
        if (fields.isEmpty()) return

        for (lf in fields) {
            lf.field.isAccessible = true
            val value = lf.field.get(context.entity)

            val length = when (value) {
                is String -> value.length
                is ByteArray -> value.size
                else -> continue // unsupported LOB type — skip rather than guess
            }

            if (length > lf.maxLength) {
                throw PersistenceException(
                    PersistenceErrorDetails(
                        "invalid_value",
                        "'${lf.label}' exceeds the maximum allowed size (${lf.maxLength} bytes)."
                    )
                )
            }
        }
    }

    private fun resolveLobFields(entityClass: KClass<*>): List<LobField> {
        val result = mutableListOf<LobField>()

        entityClass.memberProperties.forEach { prop ->
            val field = prop.javaField ?: return@forEach
            if (!field.isAnnotationPresent(Lob::class.java)) return@forEach

            val column = field.getAnnotation(Column::class.java)
            val maxLength = column?.length?.takeIf { it > 0 } ?: DEFAULT_MAX_LENGTH

            result += LobField(field, humanize(field.name), maxLength)
        }

        return result
    }

}