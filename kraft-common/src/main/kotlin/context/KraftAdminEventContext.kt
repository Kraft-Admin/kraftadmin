package com.kraftadmin.context

/**
 * Request-scoped context propagated with every event.
 * Listeners use this for audit logs, notifications, tenant routing, etc.
 */
data class KraftAdminEventContext(
    val adminContext: KraftAdminContext
) {

    val traceId get() = adminContext.traceId
    val actorUsername get() = adminContext.actorUsername
    val actorRoles get() = adminContext.actorRoles
    val tenantId get() = adminContext.tenantId
    val ipAddress get() = adminContext.ipAddress
    val userAgent get() = adminContext.userAgent
    val extra get() = adminContext.extra

    companion object {
        val SYSTEM = KraftAdminEventContext(
            KraftAdminContext(
                traceId = "system",
                actorUsername = "system",
                actorRoles = setOf("ROLE_SYSTEM"),
                tenantId = null,
                ipAddress = null,
                userAgent = null
            )
        )
    }
}