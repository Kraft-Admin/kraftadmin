package persistence.jpa.fetch

import api.utils.ObjectResponse
import api.utils.ResourceRow
import jakarta.persistence.*
import org.slf4j.LoggerFactory
import persistence.jpa.metadata.AssociationResolver
import persistence.jpa.metadata.PropertyResolver
import persistence.jpa.util.HibernateUtil
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

/**
 * Fetches related entity collections for a single-entity detail view.
 * Only called from FetchById — never from FetchAll (would cause N+1).
 *
 * For each @OneToMany / @ManyToMany field on the entity:
 *   - Initializes the collection (already done by ensureLobsInitialized)
 *   - Takes at most [limit] items
 *   - Maps each to a lightweight RelatedRow (id + display label + key fields)
 *   - Returns a map of fieldName → RelatedCollection
 */
class RelatedResourceFetcher(private val limit: Int = 10) {

    private val logger = LoggerFactory.getLogger(RelatedResourceFetcher::class.java)

    data class RelatedItem(
        val id: String,
        val displayLabel: String,
        val values: Map<String, Any?>    // key simple-type fields only — no deep recursion
    )

    data class RelatedCollection(
        val fieldName: String,
        val entityType: String,         // simple class name — UI uses this for navigation
        val items: List<RelatedItem>,
        val totalInMemory: Int,         // how many were actually loaded (may be > limit)
        val limited: Boolean            // true if we cut the list at [limit]
    )

    /**
     * Extracts all collection relations from [entity] and maps them
     * to RelatedCollection instances.
     */
    fun fetch(entity: Any): Map<String, RelatedCollection> {
        val result = mutableMapOf<String, RelatedCollection>()

        entity::class.memberProperties.forEach { prop ->
            val field = prop.javaField ?: return@forEach
            if (PropertyResolver.shouldSkip(field)) return@forEach

            val isSingle = field.isAnnotationPresent(ManyToOne::class.java) ||
                    field.isAnnotationPresent(OneToOne::class.java)
            val isCollection = field.isAnnotationPresent(OneToMany::class.java) ||
                    field.isAnnotationPresent(ManyToMany::class.java)

            if (!isSingle && !isCollection) return@forEach

            val rawValue = try {
                field.isAccessible = true
                field.get(entity)
            } catch (e: Exception) { return@forEach }

            val proxyValue = HibernateUtil.unproxy(rawValue) ?: return@forEach

            // Normalize: If it's not a collection, wrap it in a list
            val itemsList = when (proxyValue) {
                is Collection<*> -> proxyValue.toList()
                else -> listOf(proxyValue)
            }

            if (itemsList.isEmpty()) return@forEach

            val sliced = itemsList.take(limit)
            val relatedItems = sliced.mapNotNull { mapToRelatedItem(it!!) }

            val entityTypeName = sliced.firstOrNull()
                ?.let { HibernateUtil.unproxy(it)?.javaClass?.simpleName } ?: field.name

            result[prop.name] = RelatedCollection(
                fieldName = prop.name,
                entityType = entityTypeName,
                items = relatedItems,
                totalInMemory = itemsList.size,
                limited = itemsList.size > limit
            )
        }
        return result
    }

    private fun mapToRelatedItem(item: Any): RelatedItem? {
        val real = HibernateUtil.unproxy(item) ?: return null

        val id = AssociationResolver.extractId(real)?.toString() ?: return null
        val label = AssociationResolver.resolveDisplayLabel(real) ?: id

        // ✅ Only map simple-type fields — no relations, no collections
        // Prevents infinite recursion and keeps the payload lightweight
        val values = mutableMapOf<String, Any?>()
        real::class.memberProperties.forEach { prop ->
            val field = prop.javaField ?: return@forEach
            if (PropertyResolver.shouldSkip(field)) return@forEach

            // Skip any relation/collection/embedded — simple types only
            if (field.isAnnotationPresent(ManyToOne::class.java) ||
                field.isAnnotationPresent(OneToOne::class.java) ||
                field.isAnnotationPresent(OneToMany::class.java) ||
                field.isAnnotationPresent(ManyToMany::class.java
//                field.isAnnotationPresent(Embedded::class.java) ||
//                field.isAnnotationPresent(ElementCollection::class.java
                )
            ) return@forEach

            val classifier = prop.returnType.classifier
            if (!PropertyResolver.isSimpleType(
                    classifier as? kotlin.reflect.KClass<*> ?: return@forEach
                )
            ) return@forEach

            try {
                field.isAccessible = true
                values[prop.name] = field.get(real)
            } catch (e: Exception) {
                logger.debug("Could not read field ${field.name} on ${real::class.simpleName}")
            }
        }

        return RelatedItem(id = id, displayLabel = label, values = values)
    }
}