package com.kraftadmin.context

data class KraftAdminContext(
    val traceId: String,
    val actorUsername: String?,
    val actorRoles: Set<String>,
    val tenantId: String?,
    val ipAddress: String?,
    val userAgent: String?,
    val extra: MutableMap<String, Any?> = mutableMapOf()
)