package com.kraftadmin.context

data class KraftAdminContext(
    val traceId: String,
    val actorUsername: String?,
    val actorRoles: Set<String> = emptySet(),
    val tenantId: String?,
    val ipAddress: String?,
    val userAgent: String?,
    val attributes: MutableMap<String, Any?> = mutableMapOf()
)