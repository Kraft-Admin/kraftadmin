//package persistence.jpa.save
//
//import jakarta.persistence.*
//import jakarta.persistence.metamodel.EntityType
//import org.slf4j.LoggerFactory
//import persistence.jpa.conversion.FormDataCoercer
//import persistence.jpa.metadata.EntityMetadata
//import persistence.jpa.metadata.PropertyResolver
//import java.util.UUID
//import kotlin.reflect.KClass
//import kotlin.reflect.full.memberProperties
//import kotlin.reflect.jvm.javaField
//
///**
// * Writes JPA relation and embedded fields onto an entity instance.
// *
// * Handles: ManyToOne, OneToOne, ManyToMany, OneToMany, Embedded,
// * and ElementCollection.
// *
// * FormDataCoercer has already extracted ids from ObjectResponse maps
// * and unwrapped EmbeddedResponse.data — so this class only needs to
// * resolve entity references and set fields, not parse UI shapes.
// */
//class RelationshipWriter(
//    private val entityManager: EntityManager,
//) {
//
//    private val logger = LoggerFactory.getLogger(RelationshipWriter::class.java)
//
//
//    fun write(entityClass:KClass<*>, entity: Any, data: Map<String, Any?>) {
//        // ✅ Pre-coerce so all values are in their canonical backend shape
//        val coerced = FormDataCoercer.coerce(data)
//
//        entity::class.memberProperties.forEach { prop ->
//            val field = prop.javaField ?: return@forEach
//            if (PropertyResolver.shouldSkip(field)) return@forEach
//            if (!coerced.containsKey(prop.name)) return@forEach
//
//            val value = coerced[prop.name]
//
//            try {
//                field.isAccessible = true
//
//                when {
//                    field.isAnnotationPresent(ManyToOne::class.java) ||
//                            field.isAnnotationPresent(OneToOne::class.java) ->
//                        writeSingleRelation(entityClass, entity, field, value)
//
//                    field.isAnnotationPresent(ManyToMany::class.java) ||
//                            field.isAnnotationPresent(OneToMany::class.java) ->
//                        writeCollectionRelation(entityClass, entity, field, value)
//
//                }
//            } catch (e: Exception) {
//                logger.warn(
//                    "Could not write relation '${prop.name}' on ${entity::class.simpleName}: ${e.message}"
//                )
//            }
//        }
//    }
//
//    // ─── Single relation ────────────────────────────────────────────────────
//
//    private fun writeSingleRelation(entityClass: KClass<*>, entity: Any, field: java.lang.reflect.Field, value: Any?) {
//        if (value == null) {
//            field.set(entity, null)
//            return
//        }
//        // After FormDataCoercer, value is a bare id string
//        val id = value.toString()
//        val related = entityManager.find(field.type, convertId(entityManager, entityClass, id))
//        if (related == null) {
//            logger.warn("writeSingleRelation: ${field.type.simpleName}#$id not found — setting null")
//        }
//        field.set(entity, related)
//    }
//
//    // ─── Collection relation ────────────────────────────────────────────────
//
//    @Suppress("UNCHECKED_CAST")
//    private fun writeCollectionRelation(entityClass: KClass<*>, entity: Any, field: java.lang.reflect.Field, value: Any?) {
//        val ids = when (value) {
//            null -> emptyList()
//            is List<*> -> value.mapNotNull { it?.toString() }
//            is String -> value.split(",").map { it.trim() }.filter { it.isNotBlank() }
//            else -> listOf(value.toString())
//        }
//
//        val elementType = resolveElementType(field) ?: run {
//            logger.warn("writeCollectionRelation: could not resolve element type for ${field.name}")
//            return
//        }
//
//        val related = ids.mapNotNull { id ->
//            entityManager.find(elementType, convertId(entityManager, entityClass, id)).also {
//                if (it == null) logger.warn("writeCollectionRelation: $elementType#$id not found")
//            }
//        }
//
//        val existing = field.get(entity)
//        when (existing) {
//            is MutableList<*> -> {
//                (existing as MutableList<Any>).clear()
//                existing.addAll(related)
//            }
//            is MutableSet<*> -> {
//                (existing as MutableSet<Any>).clear()
//                existing.addAll(related)
//            }
//            else -> field.set(entity, related.toMutableList())
//        }
//    }
//
//    // ─── Helpers ────────────────────────────────────────────────────────────
//
//    private fun resolveElementType(field: java.lang.reflect.Field): Class<*>? {
//        val generic = field.genericType as? java.lang.reflect.ParameterizedType ?: return null
//        return generic.actualTypeArguments.firstOrNull() as? Class<*>
//    }
//
//    /**
//     * Converts a string from the URL into the actual type required by the JPA Entity
//     */
//    fun convertId(entityManager: EntityManager, entityClass: KClass<*>, id: String): Any {
//        val metamodel1 = entityManager.metamodel
//
//        // Defensive check: Verify if the class is actually a managed JPA entity
//        val entityType: EntityType<*> = try {
//            metamodel1.entity(entityClass.java)
//        } catch (e: IllegalArgumentException) {
//            // Fallback: If it's not a managed entity, we have no choice but
//            // to return the string and hope the caller handles it,
//            // or throw a more meaningful custom exception.
//            return id
//        }
//
//        val idType = entityType.idType.javaType
//
//        return try {
//            when (idType) {
//                UUID::class.java -> UUID.fromString(id)
//                Long::class.java, Long::class.javaObjectType -> id.toLong()
//                Int::class.java, Int::class.javaObjectType -> id.toInt()
//                else -> id
//            }
//        } catch (e: Exception) {
//            throw IllegalArgumentException("Cannot convert ID '$id' to type ${idType.simpleName} for entity ${entityClass.simpleName}", e)
//        }
//    }
//
//
//}

package persistence.jpa.save

import jakarta.persistence.*
import jakarta.persistence.metamodel.EntityType
import org.slf4j.LoggerFactory
import persistence.jpa.conversion.TypeConverter
import persistence.jpa.metadata.PropertyResolver
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

/**
 * Writes ONLY JPA relation fields:
 *   ManyToOne, OneToOne → single entity reference
 *   ManyToMany, OneToMany → collection of entity references
 *   Embedded → nested value object
 *
 * ElementCollection is NOT handled here — PropertyWriter + TypeConverter own it.
 */
class RelationshipWriter(private val entityManager: jakarta.persistence.EntityManager) {

    private val logger = LoggerFactory.getLogger(RelationshipWriter::class.java)

    fun write(entityClass:KClass<*>, entity: Any, coerced: Map<String, Any?>) {
        writeCoerced(entityClass, entity, coerced) // data already coerced by EntitySaver
    }

//    fun writeCoerced(entity: KClass, coerced: Map<String, Any?>) {
        fun writeCoerced(entityClass:KClass<*>, entity: Any, coerced: Map<String, Any?>) {
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
                        writeCollectionRelation(entityClass,entity, field, value)

//                    field.isAnnotationPresent(Embedded::class.java) ->
//                        writeEmbedded(entity, field, value)

                    field.isAnnotationPresent(Embedded::class.java) || field.isAnnotationPresent(Embeddable::class.java) ->
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

    // ─── Single relation ──────────────────────────────────────────────────────

    private fun writeSingleRelation(
        entityClass:KClass<*>,
        entity: Any,
        field: java.lang.reflect.Field,
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

//        val related = entityManager.find(field.type, coerceId(id, field.type))
        if (related == null) {
            logger.warn(
                "writeSingleRelation: {}#{} not found — setting null for '{}'",
                field.type.simpleName, id, field.name
            )
        }
        field.set(entity, related)
        logger.debug("wrote single relation '{}' → {}#{}", field.name, field.type.simpleName, id)
    }

    // ─── Collection relation ──────────────────────────────────────────────────

    @Suppress("UNCHECKED_CAST")
    private fun writeCollectionRelation(
        entityClass:KClass<*>,
        entity: Any,
        field: java.lang.reflect.Field,
        value: Any?
    ) {
        val ids: List<String> = when (value) {
            null -> emptyList()
            is List<*> -> value.mapNotNull { it?.toString()?.trim() }.filter { it.isNotBlank() }
            is String -> value.split(",").map { it.trim() }.filter { it.isNotBlank() }
            else -> listOf(value.toString())
        }

        val elementType = resolveElementType(field) ?: run {
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

        logger.debug("wrote collection relation '{}' → {} items", field.name, related.size)
    }

    // ─── Embedded value object ────────────────────────────────────────────────

    private fun writeEmbedded(
        entityClass:KClass<*>,
        entity: Any,
        field: java.lang.reflect.Field,
        value: Any?
    ) {
        val dataMap: Map<String, Any?> = when (value) {
            null -> { field.set(entity, null); return }
            is Map<*, *> -> @Suppress("UNCHECKED_CAST") (value as Map<String, Any?>)
            else -> {
                logger.warn("writeEmbedded: unexpected type {} for '{}'", value::class.simpleName, field.name)
                return
            }
        }

        val embeddedInstance = field.get(entity)
            ?: try {
                field.type.getDeclaredConstructor()
                    .also { it.isAccessible = true }
                    .newInstance()
            } catch (e: Exception) {
                logger.error("writeEmbedded: cannot instantiate {}: {}", field.type.simpleName, e.message)
                return
            }

        embeddedInstance.javaClass.declaredFields.forEach { embField ->
            if (!dataMap.containsKey(embField.name)) return@forEach
            val embValue = dataMap[embField.name] ?: return@forEach
            try {
                embField.isAccessible = true
                val converted = TypeConverter.convertScalar(embValue, embField.type)
                if (converted != null) embField.set(embeddedInstance, converted)
            } catch (e: Exception) {
                logger.debug("writeEmbedded: field '{}': {}", embField.name, e.message)
            }
        }

        field.set(entity, embeddedInstance)
        logger.debug("wrote embedded '{}' with keys={}", field.name, dataMap.keys)
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun resolveElementType(field: java.lang.reflect.Field): Class<*>? {
        val generic = field.genericType as? java.lang.reflect.ParameterizedType ?: return null
        return generic.actualTypeArguments.firstOrNull() as? Class<*>
    }

    private fun coerceId1(id: String, targetType: Class<*>): Any {
        return when {
            targetType == Long::class.java ||
                    targetType == java.lang.Long::class.java -> id.toLong()

            targetType == Int::class.java ||
                    targetType == java.lang.Integer::class.java -> id.toInt()

            targetType == java.util.UUID::class.java ->
                java.util.UUID.fromString(id)

            else -> id
        }
    }

        /**
     * Converts a string from the URL into the actual type required by the JPA Entity
     */
    fun coerceId(entityManager: EntityManager, entityClass: KClass<*>, id: String): Any {
        val metamodel1 = entityManager.metamodel

        // Defensive check: Verify if the class is actually a managed JPA entity
        val entityType: EntityType<*> = try {
            metamodel1.entity(entityClass.java)
        } catch (e: IllegalArgumentException) {
            // Fallback: If it's not a managed entity, we have no choice but
            // to return the string and hope the caller handles it,
            // or throw a more meaningful custom exception.
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
            throw IllegalArgumentException("Cannot convert ID '$id' to type ${idType.simpleName} for entity ${entityClass.simpleName}", e)
        }
    }
}