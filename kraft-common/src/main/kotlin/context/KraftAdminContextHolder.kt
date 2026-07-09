package com.kraftadmin.context

import com.kraftadmin.com.kraftadmin.context.KraftAnalyticsContext
import com.kraftadmin.com.kraftadmin.context.KraftAuditContext
import java.util.UUID

object KraftAdminContextHolder {

    private val holder = ThreadLocal<KraftAdminContext>()

    /**
     * Returns the current request context.
     * If none exists (background thread, tests, scheduled jobs),
     * a lightweight anonymous context is created.
     */
    fun adminContext(): KraftAdminContext =
        holder.get() ?: emptyContext()

    /**
     * Registers the current request context.
     */
    fun set(context: KraftAdminContext) {
        holder.set(context)
    }

    /**
     * Clears the current request context.
     * Must be called at the end of every request.
     */
    fun clear() {
        holder.remove()
    }

    /**
     * Event context used by the event system.
     */
    fun eventContext(): KraftAdminEventContext =
        KraftAdminEventContext(adminContext())

    /**
     * Action context used by custom action handlers.
     */
    fun actionContext(
        resourceName: String,
        entity: Any?,
        entityId: String?,
        params: Map<String, Any?> = emptyMap()
    ): KraftActionContext =
        KraftActionContext(
            resourceName = resourceName,
            entity = entity,
            entityId = entityId,
            params = params,
            requestContext = eventContext()
        )

    /**
     * Audit context used by audit providers.
     */
    fun auditContext(
        operation: String,
        resourceName: String,
        entityId: String?,
        before: Any?,
        after: Any?
    ): KraftAuditContext? = null
//        KraftAuditContext(
//            requestContext = eventContext(),
//            operation = operation,
//            resourceName = resourceName,
//            entityId = entityId,
//            before = before,
//            after = after
//        )

    /**
     * Analytics context used by telemetry/observability.
     */
    fun analyticsContext(
        operation: String,
        resourceName: String,
        durationMs: Long = 0
    ): KraftAnalyticsContext? = null
//        KraftAnalyticsContext(
//            requestContext = eventContext(),
//            operation = operation,
//            resourceName = resourceName,
//            durationMs = durationMs
//        )

    /**
     * Anonymous context used outside an HTTP request.
     */
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