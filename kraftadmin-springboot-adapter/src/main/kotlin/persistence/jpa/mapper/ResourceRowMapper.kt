package persistence.jpa.mapper

import api.utils.ObjectResponse
import api.utils.ResourceRow
import com.kraftadmin.spi.KraftAdminColumn
import com.kraftadmin.ui_descriptors.LookupDescriptor
import events.SpringActionRegistry
import jakarta.persistence.*
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import persistence.jpa.conversion.ValueConverter
import persistence.jpa.fetch.RelatedResourceFetcher
import persistence.jpa.metadata.AssociationResolver
import persistence.jpa.metadata.PropertyResolver
import persistence.jpa.util.HibernateUtil
import kotlin.reflect.KClass

class ResourceRowMapper(
    private val entityClass: KClass<*>,
    private val applicationContext: ApplicationContext,
) {

    private val logger = LoggerFactory.getLogger(ResourceRowMapper::class.java)
    private val relatedFetcher = RelatedResourceFetcher(limit = 10)
    private val springActionRegistry = applicationContext.getBean<SpringActionRegistry>(SpringActionRegistry::class.java)

    /**
     * LIST VIEW — flat values only.
     * Relations → ObjectResponse. Collections → summarized list of ObjectResponse.
     * No related-resource expansion. Called for every row in FetchAll.
     */
    fun mapToRow(entity: Any, columns: List<KraftAdminColumn>): ResourceRow {

        val real = HibernateUtil.unproxy(entity) ?: entity
        val id = AssociationResolver.extractId(real)?.toString() ?: ""

        val timestampFields = setOf(
            "createdAt",
            "updatedAt"
        )

        val selectedColumns = columns
            .filter { it.name !in timestampFields }
            .take(8)
            .map { it.name }

        val allowedFields =
            (selectedColumns + timestampFields).toSet()

        val values = ValueConverter.mapEntityToValues(
            real,
            allowedFields
        )

        return ResourceRow(
            id = id,
            values = values,
            metadata = buildMetadata(real),
            relatedResources = null
        )
    }

    /**
     * DETAIL VIEW (single entity) — everything in mapToRow PLUS:
     *   - relatedResources: expanded OneToMany/ManyToMany collections (max 10 each)
     *   - mapEntityToData: edit-form-friendly shape (ObjectResponse with displayField)
     *
     * Only called from FetchById — never from FetchAll.
     */
    fun mapToDetailRow(entity: Any, columns: List<KraftAdminColumn>): ResourceRow {
        val real = HibernateUtil.unproxy(entity) ?: entity
        val id = AssociationResolver.extractId(real)?.toString() ?: ""
        val values = ValueConverter.mapEntityToValues(real)

        // ✅ Fetch related collections — limited to 10 per relation
        val related = try {
            relatedFetcher.fetch(real).mapValues { (_, collection) ->
                ResourceRow.RelatedCollection(

                    fieldName = collection.fieldName,
                    entityType = collection.entityType,
                    items = collection.items.map { item ->
                        ResourceRow.RelatedItem(
                            id = item.id,
                            entityType = collection.entityType,
                            displayLabel = item.displayLabel,
                            values = item.values
                        )
                    },
                    totalInMemory = collection.totalInMemory,
                    limited = collection.limited,
                    lookupDescriptor = LookupDescriptor(
                        targetEntity = collection.entityType,
                        lookupKey = collection.lookupKey,
                        displayField = collection.displayField,
                        searchableFields = collection.searchableFields
                    )
                )
            }
        } catch (e: Exception) {
            logger.warn("Could not fetch related resources for ${real::class.simpleName}#$id: ${e.message}")
            emptyMap()
        }

        return ResourceRow(
            id = id,
            values = values,
            metadata = buildMetadata(real),
            customActions = springActionRegistry.getResourceActions(real::class),
            relatedResources = related.ifEmpty { null }
        )
    }

    /**
     * EDIT FORM — resolves relation IDs + display labels,
     * unwraps EmbeddedResponse.data for form binding.
     * Called by JpaDataProvider.getEntityDataForEdit().
     */
    fun mapEntityToData(entity: Any?): Map<String, Any?> {
        if (entity == null) return emptyMap()
        val result = mutableMapOf<String, Any?>()

        PropertyResolver.getPersistableProperties(entity).forEach { (prop, field) ->
            try {
                val rawValue = PropertyResolver.getFieldValue(field, entity)
                val value = HibernateUtil.unproxy(rawValue)

                result[prop.name] = when {
                    field.isAnnotationPresent(Embedded::class.java) -> {
                        if (value == null) null
                        else {
                            val subMap = ValueConverter.mapEntityToValues(value)
                            val summary = subMap.values
                                .filterIsInstance<String>()
                                .filter { it.isNotBlank() }
                                .take(2)
                                .joinToString(", ")
                            ValueConverter.EmbeddedResponse(summary, subMap)
                        }
                    }

                    field.isAnnotationPresent(ManyToOne::class.java) ||
                            field.isAnnotationPresent(OneToOne::class.java) -> {
                        if (value == null) null
                        else {
                            val id = AssociationResolver.extractId(value)?.toString()
                                ?: return@forEach
                            val label = AssociationResolver.resolveDisplayLabel(value) ?: id
                            ObjectResponse(id = id, label= label)
                        }
                    }

                    field.isAnnotationPresent(ElementCollection::class.java) ->
                        if (value is Collection<*>) value.map { it?.toString() }
                        else emptyList<String>()

                    field.isAnnotationPresent(ManyToMany::class.java) ||
                            field.isAnnotationPresent(OneToMany::class.java) ->
                        if (value !is Collection<*>) emptyList<ObjectResponse>()
                        else value.mapNotNull { item ->
                            val real = HibernateUtil.unproxy(item) ?: return@mapNotNull null
                            val id = AssociationResolver.extractId(real)?.toString()
                                ?: return@mapNotNull null
                            val label = AssociationResolver.resolveDisplayLabel(real) ?: id
                            ObjectResponse(id = id, label = label)
                        }

                    value is Collection<*> ->
                        value.map { item ->
                            val real = HibernateUtil.unproxy(item)
                            when {
                                real == null -> null
                                PropertyResolver.isSimpleType(real::class) -> real
                                else -> AssociationResolver.extractId(real)?.toString()
                            }
                        }

                    else -> value
                }
            } catch (e: Exception) {
                logger.warn(
                    "mapEntityToData: field '${prop.name}' on ${entity::class.simpleName}: ${e.message}"
                )
                result[prop.name] = null
            }
        }

        return result
    }

    private fun buildMetadata(entity: Any): Map<String, Any?> = mapOf(
        "canEdit" to true,
        "canDelete" to true,
        "cssClass" to null
    )

}
