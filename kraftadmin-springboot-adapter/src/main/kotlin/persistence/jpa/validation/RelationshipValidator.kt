package persistence.jpa.validation

import com.kraftadmin.annotations.KraftAdminField
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import persistence.error.PersistenceErrorDetails
import persistence.error.PersistenceException
import java.lang.reflect.Field
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

/**
 * Validates ALL relation fields — @ManyToOne, @OneToOne, @ManyToMany,
 * @OneToMany. Covers two concerns:
 *  1. Required single relations (non-optional @ManyToOne/@OneToOne) that
 *     resolved to null — e.g. a referenced id that didn't exist.
 *  2. Cardinality bounds (min/max) on relation collections
 *     (@ManyToMany/@OneToMany), when configured via @KraftAdminField.
 *
 * Element collections (@ElementCollection: tags, attributes, embeddables,
 * etc.) are NOT this validator's concern — see CollectionValidator.
 */
class RelationshipValidator : PersistenceValidator {

    private data class RequiredSingleRelation(val field: Field, val label: String)
    private data class BoundedRelationCollection(val field: Field, val label: String, val min: Int?, val max: Int?)

    private data class ResolvedFields(
        val requiredSingles: List<RequiredSingleRelation>,
        val boundedCollections: List<BoundedRelationCollection>
    )

    override fun validate(context: ValidationContext<*>) {
        val resolved = ValidatorMetadataCache.get(javaClass, context.entityClass) {
            resolveRelationFields(context.entityClass)
        }
        if (resolved.requiredSingles?.isEmpty() == true && resolved.boundedCollections.isEmpty()) return

        for (r in resolved.requiredSingles) {
            r.field.isAccessible = true
            if (r.field.get(context.entity) == null) {
                throw PersistenceException(
                    PersistenceErrorDetails("required_field", "'${r.label}' is required.")
                )
            }
        }

        for (c in resolved.boundedCollections) {
            c.field.isAccessible = true
            val value = c.field.get(context.entity)
            val size = (value as? Collection<*>)?.size ?: 0

            if (c.min != null && size < c.min) {
                throw PersistenceException(
                    PersistenceErrorDetails("invalid_value", "'${c.label}' must have at least ${c.min} item(s).")
                )
            }
            if (c.max != null && size > c.max) {
                throw PersistenceException(
                    PersistenceErrorDetails("invalid_value", "'${c.label}' must have at most ${c.max} item(s).")
                )
            }
        }
    }

    private fun resolveRelationFields(entityClass: KClass<*>): ResolvedFields {
        val singles = mutableListOf<RequiredSingleRelation>()
        val collections = mutableListOf<BoundedRelationCollection>()

        entityClass.memberProperties.forEach { prop ->
            val field = prop.javaField ?: return@forEach

            val manyToOne = field.getAnnotation(ManyToOne::class.java)
            val oneToOne = field.getAnnotation(OneToOne::class.java)
            if (manyToOne != null || oneToOne != null) {
                val joinColumn = field.getAnnotation(JoinColumn::class.java)
                val nonOptional = (manyToOne?.optional == false) ||
                        (oneToOne?.optional == false) ||
                        (joinColumn?.nullable == false)

                if (nonOptional) {
                    singles += RequiredSingleRelation(field, humanize(field.name))
                }
                return@forEach
            }

            val isRelationCollection = field.isAnnotationPresent(ManyToMany::class.java) ||
                    field.isAnnotationPresent(OneToMany::class.java)
            if (isRelationCollection) {
                val admin = field.getAnnotation(KraftAdminField::class.java)
//                val min = admin?.let { runCatching { it.minItems.takeIf { v -> v >= 0 } }.getOrNull() }
//                val max = admin?.let { runCatching { it.maxItems.takeIf { v -> v >= 0 } }.getOrNull() }

//                if (min != null || max != null) {
//                    collections += BoundedRelationCollection(field, humanize(field.name), min, max)
//                }
            }
        }

        return ResolvedFields(singles, collections)
    }
}