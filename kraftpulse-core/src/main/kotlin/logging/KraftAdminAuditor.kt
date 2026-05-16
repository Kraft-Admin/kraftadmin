package logging

import security.AdminUserDTO


interface KraftAdminAuditor {
    fun record(action: KraftLogAction, resource: String, id: String, actor: AdminUserDTO)
}