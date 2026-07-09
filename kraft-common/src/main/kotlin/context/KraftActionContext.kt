package com.kraftadmin.context


/**
 * Rich, typed context passed to every KraftActionHandler.
 * Pure Kotlin — no framework imports.
 */
data class KraftActionContext(
    val resourceName: String,
    val entity: Any?,
    val entityId: String?,
    val params: Map<String, Any?>,
    val requestContext: KraftAdminEventContext
) {

    @Suppress("UNCHECKED_CAST")
    fun <T> entity(): T {
        return entity as? T
            ?: throw IllegalStateException(
                "Expected entity of requested type but got ${entity?.javaClass?.simpleName ?: "null"}"
            )
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> entityOrNull(): T? =
        entity as? T

    @Suppress("UNCHECKED_CAST")
    fun <T> param(key: String): T? =
        params[key] as? T

    fun <T> param(key: String, default: T): T =
        param<T>(key) ?: default

    fun hasParam(key: String): Boolean =
        params.containsKey(key)

    fun requireParam(key: String): Any =
        params[key]
            ?: throw IllegalArgumentException("Missing required parameter '$key'")

    fun id(): String =
        entityId ?: throw IllegalStateException("Entity id is missing")

    fun idOrNull(): String? = entityId

//    inline fun <reified T> requireParam(name: String): T =
//        param<T>(name)
//            ?: throw IllegalArgumentException("Missing parameter '$name'")
}