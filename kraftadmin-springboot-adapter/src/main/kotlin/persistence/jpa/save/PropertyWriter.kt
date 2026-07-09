//package persistence.jpa.save
//
//import jakarta.persistence.*
//import org.slf4j.LoggerFactory
//import persistence.jpa.conversion.FormDataCoercer
//import persistence.jpa.conversion.TypeConverter
//import persistence.jpa.metadata.PropertyResolver
//import kotlin.reflect.KClass
//import kotlin.reflect.full.memberProperties
//import kotlin.reflect.jvm.javaField
//
///**
// * Writes simple (non-relation, non-embedded) field values onto an entity.
// *
// * Contract:
// *   - Receives PRE-COERCED data from FormDataCoercer — relation ids are
// *     already bare strings, embedded objects already unwrapped.
// *   - Skips relation annotations (handled by RelationshipWriter).
// *   - Skips embedded annotations (handled by RelationshipWriter.writeEmbedded).
// *   - Writes null explicitly so callers can clear a field — the old
// *     `?: return@forEach` pattern is gone.
// *   - Uses TypeConverter for the final primitive coercion.
// */
//class PropertyWriter(private val typeConverter: TypeConverter) {
//
//    private val logger = LoggerFactory.getLogger(PropertyWriter::class.java)
//
//    private val SKIP_FIELDS = setOf(
//        "id", "createdAt", "updatedAt", "created_at", "updated_at",
//        "createdDate", "updatedDate", "isDeleted", "deletedAt"
//    )
//
//    private val RELATION_ANNOTATIONS = setOf(
//        ManyToOne::class.java,
//        OneToOne::class.java,
//        ManyToMany::class.java,
//        OneToMany::class.java
////        Embedded::class.java,
////        ElementCollection::class.java  // handled by RelationshipWriter.writeElementCollection
//    )
//
//    fun write(entityClass:KClass<*>, entity: Any, data: Map<String, Any?>) {
//        // ✅ Pre-coerce the whole map once before touching any field
//        val coerced = FormDataCoercer.coerce(data)
//
//        logger.debug("writing data {}, coerced = {}", data, coerced)
//
//        entity::class.memberProperties.forEach { prop ->
//            val field = prop.javaField ?: return@forEach
//            if (PropertyResolver.shouldSkip(field)) return@forEach
//            if (prop.name in SKIP_FIELDS) return@forEach
//            if (RELATION_ANNOTATIONS.any { field.isAnnotationPresent(it) }) return@forEach
//
//            // ✅ Use containsKey, not ?: — so explicit null clears the field
//            if (!coerced.containsKey(prop.name)) return@forEach
//
//            val rawValue = coerced[prop.name]
//
//            try {
//                field.isAccessible = true
//
//                if (rawValue == null) {
//                    // Only write null if the field type allows it (not primitive)
//                    if (!field.type.isPrimitive) {
//                        field.set(entity, null)
//                    }
//                } else {
////                    val converted = typeConverter.convert(rawValue, field.type)
//                    val converted = typeConverter.convert(
//                        rawValue,
//                        field.type,
//                        field
//                    )
//                    if (converted != null) {
//                        field.set(entity, converted)
//                    } else {
//                        logger.warn(
//                            "TypeConverter returned null for field '${prop.name}' " +
//                                    "(${field.type.simpleName}) with value '$rawValue' — skipping"
//                        )
//                    }
//                }
//            } catch (e: Exception) {
//                logger.warn(
//                    "Could not write field '${prop.name}' " +
//                            "(${field.type.simpleName}) on ${entity::class.simpleName}: ${e.message}"
//                )
//            }
//        }
//    }
//}

package persistence.jpa.save

import jakarta.persistence.*
import org.slf4j.LoggerFactory
import persistence.jpa.conversion.FormDataCoercer
import persistence.jpa.conversion.TypeConverter
import persistence.jpa.metadata.PropertyResolver
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

class PropertyWriter(private val typeConverter: TypeConverter) {

    private val logger = LoggerFactory.getLogger(PropertyWriter::class.java)

    private val SKIP_FIELDS = setOf(
        "id", "createdAt", "updatedAt", "created_at", "updated_at",
        "createdDate", "updatedDate", "isDeleted", "deletedAt"
    )

    // ✅ Only true JPA relations — RelationshipWriter handles these
    // ElementCollection is NOT here — PropertyWriter + TypeConverter handle it
    private val RELATION_ANNOTATIONS = setOf(
        ManyToOne::class.java,
        OneToOne::class.java,
        ManyToMany::class.java,
        OneToMany::class.java,
        Embedded::class.java    // RelationshipWriter.writeEmbedded handles this
    )

    fun write(entityClass: KClass<*>, entity: Any, data: Map<String, Any?>) {
        writeCoerced(entity, FormDataCoercer.coerce(data))
    }

    fun writeCoerced(entity: Any, coerced: Map<String, Any?>) {
        entity::class.memberProperties.forEach { prop ->
            val field = prop.javaField ?: return@forEach
            if (PropertyResolver.shouldSkip(field)) return@forEach
            if (prop.name in SKIP_FIELDS) return@forEach
            if (RELATION_ANNOTATIONS.any { field.isAnnotationPresent(it) }) return@forEach
            if (!coerced.containsKey(prop.name)) return@forEach

            val rawValue = coerced[prop.name]

            try {
                field.isAccessible = true

                if (rawValue == null) {
                    if (!field.type.isPrimitive) {
                        field.set(entity, null)
                        logger.debug("cleared '{}'", prop.name)
                    }
                    return@forEach
                }

                val converted = typeConverter.convert(rawValue, field.type, field)

                if (converted == null) {
                    logger.warn(
                        "TypeConverter returned null for '{}' ({}) value='{}' — skipping",
                        prop.name, field.type.simpleName, rawValue
                    )
                    return@forEach
                }

                // ✅ ElementCollection special handling:
                // Preserve Hibernate's existing collection proxy by mutating in-place
                // rather than replacing the collection instance.
                // Replacing the instance breaks Hibernate's dirty-checking.
                if (field.isAnnotationPresent(ElementCollection::class.java)) {
                    val existing = field.get(entity)
                    if (existing is MutableCollection<*>) {
                        @Suppress("UNCHECKED_CAST")
                        val mutable = existing as MutableCollection<Any?>
                        mutable.clear()
                        mutable.addAll(
                            (converted as? Collection<*>) ?: listOf(converted)
                        )
                        logger.debug(
                            "wrote ElementCollection '{}' → {} items",
                            prop.name, mutable.size
                        )
                        return@forEach // ✅ don't fall through to field.set()
                    }
                }

                if (field.isAnnotationPresent(ElementCollection::class.java)
                    && Map::class.java.isAssignableFrom(field.type)) {

                    val existing = field.get(entity)

                    if (existing is MutableMap<*, *>) {

                        @Suppress("UNCHECKED_CAST")
                        val map = existing as MutableMap<Any?, Any?>

                        map.clear()

                        @Suppress("UNCHECKED_CAST")
                        map.putAll(converted as Map<Any?, Any?>)

                        logger.debug(
                            "updated ElementCollection Map '{}' with {} entries",
                            field.name,
                            map.size
                        )

                        return@forEach
                    }
                }

                // Standard field write
                field.set(entity, converted)
                logger.debug("wrote '{}' ({}) = {}", prop.name, field.type.simpleName, converted)

            } catch (e: Exception) {
                logger.warn(
                    "Could not write '{}' ({}) on {}: {}",
                    prop.name, field.type.simpleName, entity::class.simpleName, e.message
                )
            }
        }
    }

}