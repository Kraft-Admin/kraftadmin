package persistence.jpa.fetch

import com.kraftadmin.logging.KraftAdminLogging
import jakarta.persistence.*
import persistence.jpa.metadata.AssociationResolver
import persistence.jpa.metadata.JpaEntityMetadata
import persistence.jpa.metadata.PropertyResolver
import persistence.jpa.util.HibernateUtil
import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
import java.lang.reflect.WildcardType
import kotlin.reflect.KClass
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

    private val logger = KraftAdminLogging.logger(javaClass)


    data class RelatedItem(
        val id: String,
        val displayLabel: String,
        val values: Map<String, Any?>    // key simple-type fields only — no deep recursion
    )

    data class RelatedCollection(
        val fieldName: String,
        val entityType: String,
        val items: List<RelatedItem>,
        val totalInMemory: Int,
        val limited: Boolean,
        val lookupKey: String,
        val displayField: String,
        val searchableFields: List<String>
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

            // 1. Resolve the related class
            val relatedKClass = resolveRelatedClass(field) ?: return@forEach

            // 2. THE FINAL FIX: Check if it's actually an Entity
            val isEntity = relatedKClass.java.isAnnotationPresent(Entity::class.java)


            // If it's NOT an entity (like String, Integer, etc.), ignore it completely
            if (!isEntity) return@forEach

            // 3. Determine if it's a relation (ManyToOne, OneToMany, etc.)
            // This ensures we don't accidentally process non-relation fields
            val isRelation = field.isAnnotationPresent(ManyToOne::class.java) ||
                    field.isAnnotationPresent(OneToOne::class.java) ||
                    field.isAnnotationPresent(OneToMany::class.java) ||
                    field.isAnnotationPresent(ManyToMany::class.java)

            if (!isRelation) return@forEach

            // 4. Prepare the items
            val rawValue = try {
                field.isAccessible = true
                field.get(entity)
            } catch (e: Exception) { null }

            val proxyValue = HibernateUtil.unproxy(rawValue)
            val itemsList = when (proxyValue) {
                is Collection<*> -> proxyValue.filterNotNull()
                null -> emptyList()
                else -> listOf(proxyValue)
            }

            val sliced = itemsList.take(limit)
            val relatedItems = sliced.mapNotNull { mapToRelatedItem(it) }

            result[prop.name] = RelatedCollection(
                fieldName = prop.name,
                entityType = relatedKClass.simpleName ?: "Unknown",
                items = relatedItems,
                totalInMemory = itemsList.size,
                limited = itemsList.size > limit,
                lookupKey = JpaEntityMetadata(relatedKClass).idField,
                displayField = JpaEntityMetadata(relatedKClass).displayField,
                searchableFields = JpaEntityMetadata(relatedKClass).searchableFields
            )
            
        }
        return result
    }

    /**
     * Extracts the generic class from a Field (e.g., List<BlogPost> -> BlogPost)
     */
    private fun resolveRelatedClass(field: Field): KClass<*>? {
        // Single-valued relation
        if (!Collection::class.java.isAssignableFrom(field.type)) {
            return field.type.kotlin
        }

        val parameterized = field.genericType as? ParameterizedType
            ?: return null

        val argument = parameterized.actualTypeArguments.firstOrNull()
            ?: return null

        return when (argument) {
            is Class<*> -> argument.kotlin

            is ParameterizedType ->
                (argument.rawType as? Class<*>)?.kotlin

            is WildcardType ->
                (argument.upperBounds.firstOrNull() as? Class<*>)?.kotlin

            else -> null
        }
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