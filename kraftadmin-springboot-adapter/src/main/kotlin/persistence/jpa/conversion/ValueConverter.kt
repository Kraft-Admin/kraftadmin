package persistence.jpa.conversion

import api.utils.ObjectResponse
import jakarta.persistence.*
import org.slf4j.LoggerFactory
import persistence.jpa.metadata.AssociationResolver
import persistence.jpa.metadata.PropertyResolver
import persistence.jpa.util.HibernateUtil
import java.lang.reflect.Field

/**
 * Converts raw JPA field values into admin-UI-friendly representations.
 * Uses the canonical ObjectResponse from api.utils — no duplicate model.
 *
 * Entity → UI direction (read path).
 * Symmetric inverse is FormDataCoercer (write path).
 */
object ValueConverter {

    private val logger = LoggerFactory.getLogger(ValueConverter::class.java)

    /**
     * Wrapper for embedded value objects.
     * summary: human-readable preview for table cells
     * data:    full field map for the edit form
     */
    data class EmbeddedResponse(val summary: String, val data: Map<String, Any?>)

    // ✅ No local ObjectResponse — use api.utils.ObjectResponse everywhere

    fun convert(field: Field, rawValue: Any?): Any? {
        val value = HibernateUtil.unproxy(rawValue)

        return when {
            field.isAnnotationPresent(Embedded::class.java) ->
                convertEmbedded(value)

            field.isAnnotationPresent(ManyToOne::class.java) ||
                    field.isAnnotationPresent(OneToOne::class.java) ->
                convertSingleRelation(field, value)

            field.isAnnotationPresent(ElementCollection::class.java) ->
                convertElementCollection(value)

            field.isAnnotationPresent(ManyToMany::class.java) ||
                    field.isAnnotationPresent(OneToMany::class.java) ->
                convertCollectionRelation(value)

            else -> value
        }
    }

    private fun convertEmbedded(value: Any?): EmbeddedResponse? {
        if (value == null) return null
        val fullMap = mapEntityToValues(value)
        val summary = fullMap.values
            .filterIsInstance<String>()
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString(", ")
        return EmbeddedResponse(summary, fullMap)
    }

    private fun convertSingleRelation(field: Field, value: Any?): ObjectResponse? {
        if (value == null) return null
        return try {
            val id = AssociationResolver.extractId(value)?.toString() ?: return null
            val label = AssociationResolver.resolveDisplayLabel(value) ?: id
            ObjectResponse(id = id, displayField = label)
        } catch (e: Exception) {
            logger.warn("Could not convert relation ${field.name}: ${e.message}")
            null
        }
    }

    private fun convertElementCollection(value: Any?): List<String?> {
        return if (value is Collection<*>) value.map { it?.toString() } else emptyList()
    }

    private fun convertCollectionRelation(value: Any?): List<ObjectResponse?> {
        if (value !is Collection<*>) return emptyList()
        return value.map { item ->
            val real = HibernateUtil.unproxy(item) ?: return@map null
            val id = AssociationResolver.extractId(real)?.toString() ?: return@map null
            val label = AssociationResolver.resolveDisplayLabel(real) ?: id
            ObjectResponse(id = id, displayField = label)
        }
    }


    /**
     * Full entity → flat values map.
     * traverses the class hierarchy to include inherited fields.
     */
    fun mapEntityToValues(entity: Any?): Map<String, Any?> {
        if (entity == null) return emptyMap()

        val result = mutableMapOf<String, Any?>()
        var clazz: Class<*>? = entity.javaClass

        // Traverse the class hierarchy upwards
        while (clazz != null && clazz != Any::class.java) {
            // Assume PropertyResolver has a method that takes a Class type or
            // you use reflection directly here to get all declared fields
            for (field in clazz.declaredFields) {
                // Skip already mapped fields (shadowing) or transient fields
                if (result.containsKey(field.name) || java.lang.reflect.Modifier.isStatic(field.modifiers)) continue

                try {
                    field.isAccessible = true
                    val rawValue = field.get(entity)
                    // Use your existing ValueConverter.convert logic
                    result[field.name] = convert(field, rawValue)
                } catch (e: Exception) {
                    logger.debug("Skipping field ${field.name}: ${e.message}")
                }
            }
            clazz = clazz.superclass
        }
        return result
    }
}