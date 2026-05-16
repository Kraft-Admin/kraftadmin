package json

/**
 * Framework-agnostic JSON abstraction.
 * Each adapter provides its own implementation.
 */
interface KraftJsonSerializer1 {
    fun toJson(value: Any?): String
    fun <T> fromJson(json: String, type: Class<T>): T
    fun toMap(value: Any?): Map<String, Any?>
}

interface KraftJsonSerializer {
    fun toJson(value: Any?): String
    fun toPrettyJson(value: Any?): String = toJson(value) // default fallback
    fun <T> fromJson(json: String, type: Class<T>): T
    fun toMap(value: Any?): Map<String, Any?>
}