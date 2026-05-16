package logging

import security.AdminUserDTO


data class KraftLogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: KraftLogLevel,
    val action: KraftLogAction? = null,
    val resource: String? = null,
    val resourceId: String? = null,
    val actor: AdminUserDTO,
    val message: String,
    val trace: String? = null   // For Exception stack traces
) {

}