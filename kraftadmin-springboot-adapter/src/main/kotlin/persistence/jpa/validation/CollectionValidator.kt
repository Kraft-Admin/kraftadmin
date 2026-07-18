package persistence.jpa.validation

import com.kraftadmin.annotations.KraftAdminField
import jakarta.persistence.ElementCollection
import persistence.error.PersistenceErrorDetails
import persistence.error.PersistenceException
import java.lang.reflect.Field
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

/**
 * Validates size constraints (min/max items) on @ElementCollection fields
 * ONLY — scalar/embeddable collections owned entirely by this entity
 * (tags, attributes, warehouses, etc.). Relation-backed collections
 * (@ManyToMany, @OneToMany) are RelationshipValidator's responsibility,
 * since they reference OTHER entities rather than holding inline values.
 */
class CollectionValidator : PersistenceValidator {

    private data class CollectionField(
        val field: Field,
        val label: String,
        val min: Int?,
        val max: Int?
    )

    override fun validate(context: ValidationContext<*>) {
        val fields = ValidatorMetadataCache.get(javaClass, context.entityClass) {
            resolveCollectionFields(context.entityClass)
        }
        if (fields.isEmpty()) return

        for (cf in fields) {
            cf.field.isAccessible = true
            val value = cf.field.get(context.entity)
            val size = (value as? Collection<*>)?.size
                ?: (value as? Map<*, *>)?.size
                ?: 0

            if (cf.min != null && size < cf.min) {
                throw PersistenceException(
                    PersistenceErrorDetails(
                        "invalid_value",
                        "'${cf.label}' must have at least ${cf.min} item(s)."
                    )
                )
            }
            if (cf.max != null && size > cf.max) {
                throw PersistenceException(
                    PersistenceErrorDetails(
                        "invalid_value",
                        "'${cf.label}' must have at most ${cf.max} item(s)."
                    )
                )
            }
        }
    }

    private fun resolveCollectionFields(entityClass: KClass<*>): List<CollectionField> {
        val result = mutableListOf<CollectionField>()

        entityClass.memberProperties.forEach { prop ->
            val field = prop.javaField ?: return@forEach
            if (!field.isAnnotationPresent(ElementCollection::class.java)) return@forEach

            val admin = field.getAnnotation(KraftAdminField::class.java)
//            val min = admin?.let { runCatching { it.minItems.takeIf { v -> v >= 0 } }.getOrNull() }
//            val max = admin?.let { runCatching { it.maxItems.takeIf { v -> v >= 0 } }.getOrNull() }

            // Skip building the entry entirely if no bounds are set —
            // keeps the cached list free of fields with nothing to check.
//            if (min != null || max != null) {
//                result += CollectionField(field, humanize(field.name), min, max)
//            }
        }

        return result
    }

}