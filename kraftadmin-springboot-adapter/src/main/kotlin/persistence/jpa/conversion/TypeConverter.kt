package persistence.jpa.conversion

import com.kraftadmin.logging.KraftAdminLogging
import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

object TypeConverter {

    private val logger = KraftAdminLogging.logger(javaClass)

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
                // Collections (List, Set, @ElementCollection)
                List::class.java.isAssignableFrom(targetType) ||
                        Set::class.java.isAssignableFrom(targetType) ||
                        MutableList::class.java.isAssignableFrom(targetType) ||
                        MutableSet::class.java.isAssignableFrom(targetType) ->
                    convertCollection(value, targetType, field)

                // Maps
                Map::class.java.isAssignableFrom(targetType) ->
                    convertMap(value)

                // Scalar
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

    // Collection

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

    // Map
    @Suppress("UNCHECKED_CAST")
    fun convertMap(value: Any?): Map<*, *>? {
        return when (value) {
            null -> null

            is Map<*, *> -> value

            // Row-shape used by the FE for element-collection maps:
            // [{"key": "...", "value": "..."}]
            is List<*> -> value.mapNotNull { item ->
                val row = item as? Map<*, *> ?: return@mapNotNull null
                if (!row.containsKey("key")) return@mapNotNull null
                row["key"] to row["value"]
            }.toMap()

            is String -> {
                val trimmed = value.trim()
                if (trimmed.startsWith("{")) parseJsonObject(trimmed) else null
            }

            else -> null
        }
    }

    // JSON helpers (no Jackson — keeps core dependency-free)
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