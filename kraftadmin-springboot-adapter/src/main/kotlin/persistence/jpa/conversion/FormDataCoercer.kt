package persistence.jpa.conversion

import api.utils.ObjectResponse
import org.slf4j.LoggerFactory
import persistence.jpa.conversion.ValueConverter.EmbeddedResponse

/**
 * Normalises UI form data into shapes the writers can consume directly.
 * Symmetric inverse of ValueConverter (entity → UI).
 *
 * UI → writers direction (write path):
 *   ObjectResponse / { id, displayField } map  →  bare id string
 *   EmbeddedResponse / { summary, data } map   →  plain sub-map
 *   List of ObjectResponse / id strings        →  List of id strings
 *   List of primitives (ElementCollection)     →  pass through
 *   null                                       →  null  (clears the field)
 *   primitives                                 →  pass through
 */
object FormDataCoercer {

    private val logger = LoggerFactory.getLogger(FormDataCoercer::class.java)

    fun coerce(data: Map<String, Any?>): Map<String, Any?> {
        return data.mapValues { (key, value) ->
            try {
                coerceValue(value)
            } catch (e: Exception) {
                logger.warn("FormDataCoercer: could not coerce field '$key': ${e.message}")
                value
            }
        }
    }

    fun coerceValue1(value: Any?): Any? {
        return when {
            value == null -> null

            // ── Kotlin data class: api.utils.ObjectResponse ───────────────
            value is ObjectResponse -> value.id

            // ── Kotlin data class: ValueConverter.EmbeddedResponse ────────
            value is EmbeddedResponse -> coerceEmbeddedData(value.data)

            // ── Raw map shaped like ObjectResponse ────────────────────────
            // { id: "...", displayField: "..." }
            isObjectResponseMap(value) -> extractIdFromMap(value as Map<*, *>)

            // ── Raw map shaped like EmbeddedResponse ──────────────────────
            // { summary: "...", data: { ... } }
            isEmbeddedResponseMap(value) -> {
                @Suppress("UNCHECKED_CAST")
                val data = (value as Map<*, *>)["data"] as? Map<String, Any?> ?: emptyMap()
                coerceEmbeddedData(data)
            }

            // ── Plain map (already-unwrapped embedded or sub-object) ───────
            value is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                coerceEmbeddedData(value as Map<String, Any?>)
            }

            // ── List / Collection ─────────────────────────────────────────
            value is List<*> -> coerceList(value)

            // ── Primitives, strings, enums ────────────────────────────────
            else -> value
        }
    }

    fun coerceValue(value: Any?): Any? {
        logger.info(
            "coerceValue: type={}, value={}",
            value?.javaClass?.name,
            value
        )
        return when {
            value == null -> null
            value is ObjectResponse -> value.id
            value is EmbeddedResponse -> coerceEmbeddedData(value.data)

            // ONLY coerce if it looks like a wrapped response, NOT just any map
            isObjectResponseMap(value) -> extractIdFromMap(value as Map<*, *>)
            isEmbeddedResponseMap(value) -> {
                @Suppress("UNCHECKED_CAST")
                val data = (value as Map<*, *>)["data"] as? Map<String, Any?> ?: emptyMap()
                coerceEmbeddedData(data)
            }

            // If it's a plain map and NOT one of the above, leave it alone!
            value is Map<*, *> -> value

            value is List<*> -> coerceList(value)
            else -> value
        }

    }

    // ─── Shape detectors ─────────────────────────────────────────────────────

    private fun isObjectResponseMap(value: Any?): Boolean {
        if (value !is Map<*, *>) return false
        return value.containsKey("id") && value.containsKey("displayField")
    }

    private fun isEmbeddedResponseMap(value: Any?): Boolean {
        if (value !is Map<*, *>) return false
        return value.containsKey("summary") && value.containsKey("data")
    }

    // ─── Extractors ──────────────────────────────────────────────────────────

    private fun extractIdFromMap(map: Map<*, *>): String? {
        return map["id"]?.toString()?.takeIf { it.isNotBlank() }
    }

    private fun coerceEmbeddedData(data: Map<String, Any?>): Map<String, Any?> {
        // Recursively coerce every sub-field — embedded objects can themselves
        // contain relations that arrived as ObjectResponse maps
        return data.mapValues { (key, v) ->
            try {
                coerceValue(v)
            } catch (e: Exception) {
                logger.warn("FormDataCoercer: embedded sub-field '$key': ${e.message}")
                v
            }
        }
    }

    private fun coerceList(list: List<*>): List<Any?> {
        return list.map { item ->
            when {
                item == null -> null
                item is ObjectResponse -> item.id
                item is EmbeddedResponse -> coerceEmbeddedData(item.data)
                isObjectResponseMap(item) -> extractIdFromMap(item as Map<*, *>)
                isEmbeddedResponseMap(item) -> {
                    @Suppress("UNCHECKED_CAST")
                    val data = (item as Map<*, *>)["data"] as? Map<String, Any?> ?: emptyMap()
                    coerceEmbeddedData(data)
                }
                item is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    coerceEmbeddedData(item as Map<String, Any?>)
                }
                else -> item
            }
        }
    }
}