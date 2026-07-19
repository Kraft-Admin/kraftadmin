package com.kraftadmin.model

import java.time.Instant

data class KraftEventRecord(
    val timestamp: Instant,
    val type: String,
    val resourceName: String?,
    val traceId: String?,
    val actorUsername: String?,
    val actorRoles: Set<String>?,
    val tenantId: String?,
    val ipAddress: String?,
    val userAgent: String?,
    val payload: Map<String, Any?>
)

data class KraftEventPage(
    val content: List<KraftEventRecord>,
    val page: Int,
    val size: Int,
    val total: Int
)