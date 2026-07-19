package com.kraftadmin.context

/**
 * Rich, typed context passed to every KraftActionHandler.
 *
 */
data class KraftActionContext(
    val resourceName: String,
    val entity: Any?,
    val entityId: String?,
    val input: Any?,
    val requestContext: KraftAdminContext
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
    fun <T> input(): T =
        input as T

    fun id(): String =
        entityId ?: throw IllegalStateException("Entity id is missing")

    fun idOrNull(): String? = entityId


}