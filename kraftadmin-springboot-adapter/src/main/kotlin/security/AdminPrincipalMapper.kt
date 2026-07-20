package security

import com.kraftadmin.security.AdminPrincipal
import com.kraftadmin.security.AdminUserDTO
import org.springframework.security.core.Authentication


class AdminPrincipalMapper {
    companion object {
        fun toDTO(adminPrincipal: AdminPrincipal): AdminUserDTO {
            // 1. Extract initials from username as fallback
            val fallbackInitials = adminPrincipal.username.take(2).uppercase()

            // 2. Try to reach into the 'raw' Spring Authentication
            // Based on your logs, 'raw' is a UsernamePasswordAuthenticationToken
            // and its principal is your domain User object.
            val springAuth = adminPrincipal.raw as? Authentication
            val domainUser = springAuth?.principal

            // We use reflection or 'as?' to get fields if we don't want a hard dependency on the User class in Core
            // Or, if this code is in the Spring-specific module, just cast it:
            // val user = domainUser as? io.enthuzze.authentication.models.User

            val fullName = try {
                domainUser?.javaClass?.getMethod("getProvider")?.invoke(domainUser) as? String
            } catch (e: Exception) {
                null
            } ?: adminPrincipal.username

            val avatarUrl = try {
                domainUser?.javaClass?.getMethod("getAvatar")?.invoke(domainUser) as? String
            } catch (e: Exception) {
                null
            }

            return AdminUserDTO(
                name = fullName,
                username = adminPrincipal.username,
                roles = adminPrincipal.roles,
                initials = fullName.split(" ").filter { it.isNotBlank() }
                    .mapNotNull { it.firstOrNull()?.uppercase() }
                    .joinToString("").take(2).ifEmpty { fallbackInitials },
                avatar = avatarUrl,
                isBridgeMode = adminPrincipal.raw != null
            )
        }
    }
}