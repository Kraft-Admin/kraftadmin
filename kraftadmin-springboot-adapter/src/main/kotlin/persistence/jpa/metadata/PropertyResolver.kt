package persistence.jpa.metadata

import com.kraftadmin.logging.KraftAdminLogging
import jakarta.persistence.Transient
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

/**
 * Resolves entity properties, skipping non-persistent and ignored fields.
 */
object PropertyResolver {

    private val logger = KraftAdminLogging.logger(javaClass)


    private val SIMPLE_TYPES: Set<KClass<*>> = setOf(
        String::class, Boolean::class, Byte::class, Short::class,
        Int::class, Long::class, Float::class, Double::class,
        Char::class, java.math.BigDecimal::class, java.math.BigInteger::class,
        java.time.LocalDate::class, java.time.LocalDateTime::class,
        java.time.ZonedDateTime::class, java.time.OffsetDateTime::class,
        java.time.Instant::class, java.util.Date::class, java.util.UUID::class,
        java.sql.Timestamp::class, java.sql.Date::class
    )

    fun isSimpleType(kClass: KClass<*>?): Boolean {
        if (kClass == null) return false
        return kClass in SIMPLE_TYPES || kClass.java.isEnum
    }

    fun shouldSkip(field: Field): Boolean {
        return field.isAnnotationPresent(Transient::class.java) ||
                field.isAnnotationPresent(jakarta.persistence.Transient::class.java) ||
                field.isAnnotationPresent(com.fasterxml.jackson.annotation.JsonIgnore::class.java) ||
                Modifier.isStatic(field.modifiers)
    }

    fun getFieldValue(field: Field, entity: Any): Any? {
        return try {
            field.isAccessible = true
            field.get(entity)
        } catch (e: Exception) {
            logger.warn("Could not read field ${field.name}: ${e.message}")
            null
        }
    }

    fun getPersistableProperties(entity: Any): List<Pair<KProperty1<out Any, *>, Field>> {
        return entity::class.memberProperties.mapNotNull { prop ->
            val field = prop.javaField ?: return@mapNotNull null
            if (shouldSkip(field)) null else prop to field
        }
    }
}