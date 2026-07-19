package persistence.jpa.save

import com.kraftadmin.logging.KraftAdminLogging
import jakarta.persistence.*
import persistence.jpa.conversion.FormDataCoercer
import persistence.jpa.conversion.TypeConverter
import persistence.jpa.metadata.PropertyResolver
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

/**
 * Writes only scalar properties.
 *
 * The following are handled by RelationshipWriter:
 *  • ManyToOne
 *  • OneToOne
 *  • OneToMany
 *  • ManyToMany
 *  • ElementCollection
 *  • Embedded
 */
class PropertyWriter(
    private val typeConverter: TypeConverter
) {

    private val logger = KraftAdminLogging.logger(javaClass)


    private val SKIP_FIELDS = setOf(
        "id",
        "createdAt",
        "updatedAt",
        "created_at",
        "updated_at",
        "createdDate",
        "updatedDate",
        "deletedAt",
        "isDeleted"
    )

    private val RELATION_ANNOTATIONS = setOf(
        ManyToOne::class.java,
        OneToOne::class.java,
        OneToMany::class.java,
        ManyToMany::class.java,
        ElementCollection::class.java,
        Embedded::class.java
    )

    fun write(
        entityClass: KClass<*>,
        entity: Any,
        data: Map<String, Any?>
    ) {
        writeCoerced(entity, FormDataCoercer.coerce(data))
    }

    fun writeCoerced(
        entity: Any,
        coerced: Map<String, Any?>
    ) {
        entity::class.memberProperties.forEach { property ->

            val field = property.javaField ?: return@forEach

            if (PropertyResolver.shouldSkip(field)) return@forEach
            if (property.name in SKIP_FIELDS) return@forEach
            if (!coerced.containsKey(property.name)) return@forEach

            // Skip everything RelationshipWriter owns.
            if (RELATION_ANNOTATIONS.any(field::isAnnotationPresent)) {
                return@forEach
            }

            val raw = coerced[property.name]

            try {
                field.isAccessible = true

                if (raw == null) {
                    if (!field.type.isPrimitive) {
                        field.set(entity, null)
                        logger.debug("cleared '{}'", property.name)
                    }
                    return@forEach
                }

                val converted = typeConverter.convert(
                    raw,
                    field.type,
                    field
                )

                if (converted == null) {
                    logger.warn(
                        "TypeConverter returned null for '{}' ({}) value={}",
                        property.name,
                        field.type.simpleName,
                        raw
                    )
                    return@forEach
                }

                field.set(entity, converted)

                logger.debug(
                    "wrote '{}' ({}) = {}",
                    property.name,
                    field.type.simpleName,
                    converted
                )

            } catch (e: Exception) {
                logger.warn(
                    "Could not write '{}' ({}) on {}: {}",
                    property.name,
                    field.type.simpleName,
                    entity::class.simpleName,
                    e.message,
                    e
                )
            }
        }
    }
}