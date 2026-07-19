package com.kraftadmin.context

import java.util.UUID

object KraftAdminContextHolder {

    private val holder = ThreadLocal<KraftAdminContext>()

    fun adminContext(): KraftAdminContext =
        holder.get() ?: emptyContext()

    fun set(context: KraftAdminContext) {
        holder.set(context)
    }

    fun clear() {
        holder.remove()
    }

    /**
     * Context used by CRUD operations, exports, imports,
     * print actions and custom admin actions.
     */
    fun actionContext(
        resourceName: String,
        entity: Any? = null,
        entityId: String? = null,
        input: Any? = null
    ): KraftActionContext =
        KraftActionContext(
            resourceName = resourceName,
            entity = entity,
            entityId = entityId,
            input = input,
            requestContext = adminContext()
        )

    private fun emptyContext() =
        KraftAdminContext(
            traceId = UUID.randomUUID().toString(),
            actorUsername = null,
            actorRoles = emptySet(),
            tenantId = null,
            ipAddress = null,
            userAgent = null
        )
}