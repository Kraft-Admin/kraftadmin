//package persistence.jpa.conversion
//
//import jakarta.persistence.ElementCollection
//import jakarta.persistence.Embeddable
//import jakarta.persistence.Embedded
//import org.slf4j.LoggerFactory
//import java.lang.reflect.Field
//import java.math.BigDecimal
//import java.time.LocalDate
//import java.time.LocalDateTime
//import java.time.format.DateTimeFormatter
//import java.util.UUID
//import kotlin.reflect.KClass
//
///**
// * Converts raw form values into the exact Java type required by an entity field.
// */
//object TypeConverter {
//
//    private val logger = LoggerFactory.getLogger(TypeConverter::class.java)
//
//    private val DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE
//    private val DATETIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME
//
//    @Suppress("UNCHECKED_CAST")
//    fun convert(
//        value: Any?,
//        targetType: Class<*>,
//        field: Field? = null
//    ): Any? {
//
//        if (value == null) {
//            return null
//        }
//
//        // Handle @ElementCollection first
//        if (field?.isAnnotationPresent(ElementCollection::class.java) == true) {
//            return convertElementCollection(value, field)
//        }
//
//        val isEmbedded =
//            field?.isAnnotationPresent(Embedded::class.java) == true ||
//                    targetType.isAnnotationPresent(Embeddable::class.java)
//
//        if (isEmbedded) {
//            return convertEmbedded(value, targetType)
//        }
//
//        if (targetType.isInstance(value)) {
//            return value
//        }
//
//        return try {
//            val str = value.toString()
//
//            when {
//
//                targetType == String::class.java ->
//                    str
//
//                targetType == Boolean::class.java ||
//                        targetType == java.lang.Boolean::class.java ->
//                    str.equals("true", true) || str == "1"
//
//                targetType == Int::class.java ||
//                        targetType == java.lang.Integer::class.java ->
//                    str.toInt()
//
//                targetType == Long::class.java ||
//                        targetType == java.lang.Long::class.java ->
//                    str.toLong()
//
//                targetType == Double::class.java ||
//                        targetType == java.lang.Double::class.java ->
//                    str.toDouble()
//
//                targetType == Float::class.java ||
//                        targetType == java.lang.Float::class.java ->
//                    str.toFloat()
//
//                targetType == BigDecimal::class.java ->
//                    BigDecimal(str)
//
//                targetType == UUID::class.java ->
//                    UUID.fromString(str)
//
//                targetType == LocalDate::class.java ->
//                    LocalDate.parse(str, DATE_FORMATTER)
//
//                targetType == LocalDateTime::class.java ->
//                    LocalDateTime.parse(
//                        str.replace(" ", "T"),
//                        DATETIME_FORMATTER
//                    )
//
//                targetType.isEnum -> {
//                    val enumClass = targetType as Class<Enum<*>>
//                    enumClass.enumConstants
//                        ?.firstOrNull { it.name.equals(str, ignoreCase = true) }
//                }
//
//                else -> value
//            }
//
//        } catch (e: Exception) {
//            logger.warn(
//                "TypeConverter: could not convert '$value' to ${targetType.simpleName}: ${e.message}"
//            )
//            null
//        }
//    }
//
//    private fun convertElementCollection(
//        value: Any?,
//        field: Field
//    ): Any? {
//
//        return when {
//
//            Map::class.java.isAssignableFrom(field.type) -> {
//                when (value) {
//                    is Map<*, *> -> value.toMutableMap()
//                    else -> mutableMapOf<String, Any?>()
//                }
//            }
//
//            List::class.java.isAssignableFrom(field.type) -> {
//                when (value) {
//                    is Collection<*> -> value.toMutableList()
//                    else -> mutableListOf(value)
//                }
//            }
//
//            Set::class.java.isAssignableFrom(field.type) -> {
//                when (value) {
//                    is Collection<*> -> value.toMutableSet()
//                    else -> mutableSetOf(value)
//                }
//            }
//
//            else -> value
//        }
//    }
//
//    // ─── Embedded value objects ─────────────────────────────────────────────
//
//    private fun convertEmbedded1(entityClass: KClass<*>, entity: Any, field: java.lang.reflect.Field, value: Any?) {
//        // FormDataCoercer already unwrapped EmbeddedResponse.data → plain map
//        val dataMap = when (value) {
//            null -> { field.set(entity, null); return }
//            is Map<*, *> -> @Suppress("UNCHECKED_CAST") (value as Map<String, Any?>)
//            else -> { logger.warn("writeEmbedded: unexpected value type ${value::class} for ${field.name}"); return }
//        }
//
//        // Get or create the embedded instance
//        val embeddedInstance = field.get(entity)
//            ?: try { field.type.getDeclaredConstructor().also { it.isAccessible = true }.newInstance() }
//            catch (e: Exception) { logger.error("Could not instantiate embedded ${field.type.simpleName}: ${e.message}"); return }
//
//        // ✅ Recursively write all sub-fields using PropertyWriter logic inline
//        embeddedInstance::class.java.declaredFields.forEach { embField ->
//            if (!dataMap.containsKey(embField.name)) return@forEach
//            val embValue = dataMap[embField.name]
//            try {
//                embField.isAccessible = true
//                if (embValue == null) {
//                    if (!embField.type.isPrimitive) embField.set(embeddedInstance, null)
//                } else {
//                    // Re-use TypeConverter for primitive coercion inside embedded objects
//                    val converted = persistence.jpa.conversion.TypeConverter.convert(embValue, embField.type)
//                    if (converted != null) embField.set(embeddedInstance, converted)
//                }
//            } catch (e: Exception) {
//                logger.warn("writeEmbedded: could not write sub-field ${embField.name}: ${e.message}")
//            }
//        }
//
//        field.set(entity, embeddedInstance)
//    }
//
//    private fun convertEmbedded(
//        value: Any?,
//        targetType: Class<*>
//    ): Any? {
//
//        if (value == null) {
//            return null
//        }
//
//        if (value !is Map<*, *>) {
//            logger.warn(
//                "Expected Map for embedded ${targetType.simpleName}, got ${value::class.simpleName}"
//            )
//            return null
//        }
//
//        @Suppress("UNCHECKED_CAST")
//        val data = value as Map<String, Any?>
//
//        val instance = try {
//            targetType.getDeclaredConstructor()
//                .also { it.isAccessible = true }
//                .newInstance()
//        } catch (e: Exception) {
//            logger.error(
//                "Could not instantiate embedded ${targetType.simpleName}: ${e.message}"
//            )
//            return null
//        }
//
//        targetType.declaredFields.forEach { embField ->
//
//            if (!data.containsKey(embField.name))
//                return@forEach
//
//            embField.isAccessible = true
//
//            try {
//
//                val converted = convert(
//                    data[embField.name],
//                    embField.type,
//                    embField
//                )
//
//                if (converted != null || !embField.type.isPrimitive) {
//                    embField.set(instance, converted)
//                }
//
//            } catch (e: Exception) {
//                logger.warn(
//                    "Could not write embedded field '${embField.name}': ${e.message}"
//                )
//            }
//        }
//
//        return instance
//    }
//
//
//}


package persistence.jpa.conversion

import org.slf4j.LoggerFactory
import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

object TypeConverter {

    private val logger = LoggerFactory.getLogger(TypeConverter::class.java)
    private val DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE
    private val DATETIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun convert(value: Any?, targetType: Class<*>, field: Field? = null): Any? {
        logger.info(
            "convertMap: type={}, value={}",
            value?.javaClass?.name,
            value
        )

        if (value == null) return null

        return try {
            when {
                // ── Collections (List, Set, @ElementCollection) ──────────────
                List::class.java.isAssignableFrom(targetType) ||
                        Set::class.java.isAssignableFrom(targetType) ||
                        MutableList::class.java.isAssignableFrom(targetType) ||
                        MutableSet::class.java.isAssignableFrom(targetType) ->
                    convertCollection(value, targetType, field)

                // ── Maps ──────────────────────────────────────────────────────
                Map::class.java.isAssignableFrom(targetType) ->
                    convertMap(value)

                // ── Scalar ────────────────────────────────────────────────────
                else -> convertScalar(value, targetType)
            }
        } catch (e: Exception) {
            logger.warn(
                "TypeConverter: failed converting '{}' to {}: {}",
                value, targetType.simpleName, e.message
            )
            null
        }
    }

    // ─── Scalar ───────────────────────────────────────────────────────────────

    @Suppress("UNCHECKED_CAST")
    fun convertScalar(value: Any?, targetType: Class<*>): Any? {
        if (value == null) return null
        if (targetType.isInstance(value)) return value

        val str = value.toString().trim()

        return when {
            targetType == String::class.java -> str

            targetType == Boolean::class.java ||
                    targetType == java.lang.Boolean::class.java ->
                str.equals("true", ignoreCase = true) || str == "1"

            targetType == Int::class.java ||
                    targetType == java.lang.Integer::class.java -> str.toInt()

            targetType == Long::class.java ||
                    targetType == java.lang.Long::class.java -> str.toLong()

            targetType == Double::class.java ||
                    targetType == java.lang.Double::class.java -> str.toDouble()

            targetType == Float::class.java ||
                    targetType == java.lang.Float::class.java -> str.toFloat()

            targetType == Short::class.java ||
                    targetType == java.lang.Short::class.java -> str.toShort()

            targetType == BigDecimal::class.java -> BigDecimal(str)

            targetType == UUID::class.java -> UUID.fromString(str)

            targetType == LocalDate::class.java ->
                LocalDate.parse(str, DATE_FORMATTER)

            targetType == LocalDateTime::class.java ->
                LocalDateTime.parse(str.replace(" ", "T"), DATETIME_FORMATTER)

            targetType.isEnum -> {
                (targetType as Class<Enum<*>>).enumConstants
                    ?.firstOrNull { it.name.equals(str, ignoreCase = true) }
            }

            else -> value
        }
    }

    // ─── Collection ───────────────────────────────────────────────────────────

    @Suppress("UNCHECKED_CAST")
    fun convertCollection(
        value: Any?,
        targetType: Class<*>,
        field: Field?
    ): Any? {
        if (value == null) return null

        // Resolve the declared element type from generic signature
        // e.g. List<String> → String, Set<Long> → Long
        val elementType: Class<*> = field
            ?.let { (it.genericType as? ParameterizedType)?.actualTypeArguments?.firstOrNull() as? Class<*> }
            ?: String::class.java

        // Flatten the incoming value into a list of raw items
        val rawItems: List<Any?> = when (value) {
            is List<*> -> value
            is Set<*> -> value.toList()
            is Collection<*> -> value.toList()
            is Array<*> -> value.toList()
            is String -> {
                val trimmed = value.trim()
                when {
                    trimmed.startsWith("[") -> parseJsonArray(trimmed)
                    trimmed.isNotBlank() -> trimmed.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    else -> emptyList()
                }
            }
            else -> listOf(value)
        }

        // Coerce each raw item to the declared element type
        val coerced: List<Any> = rawItems.mapNotNull { item ->
            if (item == null) return@mapNotNull null
            if (elementType.isInstance(item)) item
            else convertScalar(item, elementType)
        }

        logger.debug(
            "convertCollection: elementType={} rawCount={} coercedCount={}",
            elementType.simpleName, rawItems.size, coerced.size
        )

        return when {
            Set::class.java.isAssignableFrom(targetType) ||
                    MutableSet::class.java.isAssignableFrom(targetType) -> LinkedHashSet(coerced)
            else -> ArrayList(coerced)
        }
    }

    // ─── Map ──────────────────────────────────────────────────────────────────

    @Suppress("UNCHECKED_CAST")
    fun convertMap(value: Any?): Map<*, *>? {
        return when (value) {
            null -> null
            is Map<*, *> -> value
            is String -> {
                val trimmed = value.trim()
                if (trimmed.startsWith("{")) parseJsonObject(trimmed) else null
            }
            else -> null
        }
    }

    // ─── JSON helpers (no Jackson — keeps core dependency-free) ──────────────

    private fun parseJsonArray(json: String): List<String> {
        return try {
            json.trim()
                .removePrefix("[").removeSuffix("]")
                .split(",")
                .map { it.trim().removeSurrounding("\"").removeSurrounding("'") }
                .filter { it.isNotBlank() }
        } catch (e: Exception) {
            logger.warn("TypeConverter: parseJsonArray failed for '{}': {}", json, e.message)
            emptyList()
        }
    }

//    private fun parseJsonObject(json: String): Map<String, String> {
//        return try {
//            json.trim()
//                .removePrefix("{").removeSuffix("}")
//                .split(",")
//                .mapNotNull { pair ->
//                    val parts = pair.split(":", limit = 2)
//                    if (parts.size != 2) return@mapNotNull null
//                    val k = parts[0].trim().removeSurrounding("\"")
//                    val v = parts[1].trim().removeSurrounding("\"")
//                    k to v
//                }
//                .toMap()
//        } catch (e: Exception) {
//            logger.warn("TypeConverter: parseJsonObject failed for '{}': {}", json, e.message)
//            emptyMap()
//        }
//    }

    private fun parseJsonObject(json: String): MutableMap<String, String> {
        return try {
            json.trim()
                .removePrefix("{")
                .removeSuffix("}")
                .split(",")
                .mapNotNull {
                    val parts = it.split(":", limit = 2)
                    if (parts.size != 2) return@mapNotNull null

                    val key = parts[0]
                        .trim()
                        .removeSurrounding("\"")

                    val value = parts[1]
                        .trim()
                        .removeSurrounding("\"")

                    key to value
                }
                .toMap()
                .toMutableMap()
        } catch (e: Exception) {
            mutableMapOf()
        }
    }

}