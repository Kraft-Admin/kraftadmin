package persistence.jpa.save

import com.kraftadmin.logging.KraftAdminLogging
import jakarta.persistence.*
import jakarta.persistence.metamodel.EntityType
import persistence.jpa.conversion.TypeConverter
import persistence.jpa.metadata.PropertyResolver
import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

/**
 * Writes JPA association fields:
 *
 *  • ManyToOne / OneToOne      -> single entity reference
 *  • OneToMany / ManyToMany    -> collection of entity references
 *  • ElementCollection         -> collection of scalar or embeddable values
 *  • Embedded                  -> nested value object
 *
 * PropertyWriter remains responsible only for scalar properties.
 */
class RelationshipWriter(private val entityManager: jakarta.persistence.EntityManager) {


    private val logger = KraftAdminLogging.logger(javaClass)


    fun writeCoerced(entityClass: KClass<*>, entity: Any, coerced: Map<String, Any?>) {
        entity::class.memberProperties.forEach { prop ->
            val field = prop.javaField ?: return@forEach
            if (PropertyResolver.shouldSkip(field)) return@forEach
            if (!coerced.containsKey(prop.name)) return@forEach

            val value = coerced[prop.name]

            try {
                field.isAccessible = true

                when {
                    field.isAnnotationPresent(ManyToOne::class.java) ||
                            field.isAnnotationPresent(OneToOne::class.java) ->
                        writeSingleRelation(entityClass, entity, field, value)

                    field.isAnnotationPresent(ManyToMany::class.java) ||
                            field.isAnnotationPresent(OneToMany::class.java) ->
                        writeCollectionRelation(entityClass, entity, field, value)

                    field.isAnnotationPresent(ElementCollection::class.java) ->
                        writeElementCollection(entity, field, value)

                    field.isAnnotationPresent(Embedded::class.java) ->
                        writeEmbedded(entityClass, entity, field, value)
                }
            } catch (e: Exception) {
                logger.warn(
                    "RelationshipWriter: could not write '{}' on {}: {}",
                    prop.name, entity::class.simpleName, e.message
                )
            }
        }
    }

    // ElementCollection dispatch

    private fun writeElementCollection(
        entity: Any,
        field: Field,
        value: Any?
    ) {
        when {
            Map::class.java.isAssignableFrom(field.type) ->
                writeElementCollectionMap(entity, field, value)

            Collection::class.java.isAssignableFrom(field.type) ->
                writeElementCollectionCollection(entity, field, value)

            else ->
                logger.warn(
                    "Unsupported @ElementCollection type {}",
                    field.type.name
                )
        }
    }

    //  List / Set element collections

    @Suppress("UNCHECKED_CAST")
    private fun writeElementCollectionCollection(
        entity: Any,
        field: Field,
        value: Any?
    ) {
        val elementType = resolveCollectionElementType(field) ?: run {
            logger.warn("writeElementCollectionCollection: cannot resolve element type for '{}'", field.name)
            return
        }

        val converted = when (value) {
            null -> emptyList()

            is Collection<*> ->
                value.mapNotNull { convertElement(elementType, it) }

            else ->
                listOfNotNull(convertElement(elementType, value))
        }

        when (val existing = field.get(entity)) {

            is MutableList<*> -> {
                val list = existing as MutableList<Any?>
                list.clear()
                list.addAll(converted)
            }

            is MutableSet<*> -> {
                val set = existing as MutableSet<Any?>
                set.clear()
                set.addAll(converted)
            }

            else ->
                // Fall back to a mutable list if no existing collection proxy is present
                // (e.g. transient/new entity where Hibernate hasn't wired the proxy yet).
                field.set(entity, converted.toMutableList())
        }

        logger.debug("wrote element collection '{}' → {} items", field.name, converted.size)
    }

    // Map element collections

    @Suppress("UNCHECKED_CAST")
    private fun writeElementCollectionMap(
        entity: Any,
        field: Field,
        value: Any?
    ) {
        val (keyType, valueType) = resolveMapTypes(field) ?: run {
            logger.warn("writeElementCollectionMap: cannot resolve key/value types for '{}'", field.name)
            return
        }

        val rawEntries: List<Pair<Any?, Any?>> = when (value) {
            null -> emptyList()
            is Map<*, *> -> value.entries.map { it.key to it.value }
            is List<*> -> value.mapNotNull { item ->
                val row = item as? Map<*, *> ?: return@mapNotNull null
                if (!row.containsKey("key")) return@mapNotNull null
                row["key"] to row["value"]
            }
            else -> {
                logger.warn(
                    "writeElementCollectionMap: expected Map or List<{{key,value}}> for '{}', got {}",
                    field.name, value::class.simpleName
                )
                return
            }
        }

        val converted: Map<Any, Any?> = rawEntries
            .mapNotNull { (rawKey, rawValue) ->
                val k = convertElement(keyType, rawKey) ?: return@mapNotNull null
                k to convertElement(valueType, rawValue)
            }
            .toMap()

        when (val existing = field.get(entity)) {
            is MutableMap<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                val map = existing as MutableMap<Any, Any?>
                map.clear()
                map.putAll(converted)
            }
            else -> field.set(entity, LinkedHashMap(converted))
        }

        logger.debug("wrote element collection map '{}' → {} entries", field.name, converted.size)
    }

    // Shared scalar/embeddable conversion

    private fun convertElement(
        targetType: Class<*>,
        value: Any?
    ): Any? {

        if (value == null)
            return null

        if (targetType.isAnnotationPresent(Embeddable::class.java)) {
            @Suppress("UNCHECKED_CAST")
            val data = value as? Map<String, Any?> ?: run {
                logger.warn(
                    "convertElement: expected object for embeddable {}, got {}",
                    targetType.simpleName, value::class.simpleName
                )
                return null
            }
            return createEmbeddedInstance(targetType, data)
        }

        return TypeConverter.convertScalar(value, targetType)
    }

    private fun resolveCollectionElementType(field: Field): Class<*>? {
        val generic = field.genericType as? ParameterizedType ?: return null
        return generic.actualTypeArguments.getOrNull(0) as? Class<*>
    }

    private fun resolveMapTypes(field: Field): Pair<Class<*>, Class<*>>? {
        val generic = field.genericType as? ParameterizedType ?: return null
        val key = generic.actualTypeArguments.getOrNull(0) as? Class<*> ?: return null
        val value = generic.actualTypeArguments.getOrNull(1) as? Class<*> ?: return null
        return key to value
    }

    private fun createEmbeddedInstance(
        type: Class<*>,
        data: Map<String, Any?>
    ): Any {
        val instance = type.getDeclaredConstructor()
            .also { it.isAccessible = true }
            .newInstance()

        type.declaredFields.forEach { embField ->
            if (!data.containsKey(embField.name)) return@forEach

            embField.isAccessible = true

            val raw = data[embField.name]

            val converted = if (
                embField.type.isAnnotationPresent(
                    Embeddable::class.java
                )
            ) {
                @Suppress("UNCHECKED_CAST")
                (raw as? Map<String, Any?>)?.let {
                    createEmbeddedInstance(
                        embField.type,
                        it
                    )
                }
            } else {
                TypeConverter.convertScalar(
                    raw,
                    embField.type
                )
            }

            if (converted != null || raw == null) {
                embField.set(
                    instance,
                    converted
                )
            }
        }

        return instance
    }


    // Single relation

    private fun writeSingleRelation(
        entityClass: KClass<*>,
        entity: Any,
        field: Field,
        value: Any?
    ) {
        if (value == null) {
            field.set(entity, null)
            logger.debug("cleared single relation '{}'", field.name)
            return
        }

        val id = value.toString().trim()
        if (id.isBlank()) {
            field.set(entity, null)
            return
        }

        val coercedId = coerceId(entityManager, entityClass, id)
        val related = entityManager.find(field.type, coercedId)

        if (related == null) {
            logger.warn(
                "writeSingleRelation: {}#{} not found — setting null for '{}'",
                field.type.simpleName, id, field.name
            )
        }
        field.set(entity, related)
    }

    // Collection relation

    @Suppress("UNCHECKED_CAST")
    private fun writeCollectionRelation(
        entityClass: KClass<*>,
        entity: Any,
        field: Field,
        value: Any?
    ) {
        val ids: List<String> = when (value) {
            null -> emptyList()
            is List<*> -> value.mapNotNull { it?.toString()?.trim() }.filter { it.isNotBlank() }
            is String -> value.split(",").map { it.trim() }.filter { it.isNotBlank() }
            else -> listOf(value.toString())
        }

        val elementType = resolveCollectionElementType(field) ?: run {
            logger.warn("writeCollectionRelation: cannot resolve element type for '{}'", field.name)
            return
        }

        val related = ids.mapNotNull { id ->
            entityManager.find(elementType, coerceId(entityManager, entityClass, id)).also {
                if (it == null) logger.warn(
                    "writeCollectionRelation: {}#{} not found",
                    elementType.simpleName, id
                )
            }
        }

        // ✅ Mutate existing collection proxy — never replace the instance
        val existing = field.get(entity)
        when (existing) {
            is MutableList<*> -> {
                (existing as MutableList<Any>).clear()
                existing.addAll(related)
            }
            is MutableSet<*> -> {
                (existing as MutableSet<Any>).clear()
                existing.addAll(related)
            }
            else -> field.set(entity, ArrayList(related))
        }

    }

    //  Embedded value object
    private fun writeEmbedded(
        entityClass: KClass<*>,
        entity: Any,
        field: Field,
        value: Any?
    ) {
        if (value == null) {
            field.set(entity, null)
            return
        }

        val dataMap = value as? Map<String, Any?>
            ?: run {
                logger.warn(
                    "writeEmbedded: expected object for '{}', got {}",
                    field.name,
                    value::class.simpleName
                )
                return
            }

        val embeddedInstance = createEmbeddedInstance(
            field.type,
            dataMap
        )

        field.set(entity, embeddedInstance)

        logger.debug(
            "wrote embedded '{}' with keys={}",
            field.name,
            dataMap.keys
        )
    }

    // Helpers

    /**
     * Converts a string from the URL/payload into the actual type required by the JPA entity's ID.
     */
    fun coerceId(entityManager: EntityManager, entityClass: KClass<*>, id: String): Any {
        val metamodel = entityManager.metamodel

        // Defensive check: verify the class is actually a managed JPA entity
        val entityType: EntityType<*> = try {
            metamodel.entity(entityClass.java)
        } catch (e: IllegalArgumentException) {
            // Fallback: if it's not a managed entity, return the raw string
            return id
        }

        val idType = entityType.idType.javaType

        return try {
            when (idType) {
                UUID::class.java -> UUID.fromString(id)
                Long::class.java, Long::class.javaObjectType -> id.toLong()
                Int::class.java, Int::class.javaObjectType -> id.toInt()
                else -> id
            }
        } catch (e: Exception) {
            throw IllegalArgumentException(
                "Cannot convert ID '$id' to type ${idType.simpleName} for entity ${entityClass.simpleName}", e
            )
        }
    }
}