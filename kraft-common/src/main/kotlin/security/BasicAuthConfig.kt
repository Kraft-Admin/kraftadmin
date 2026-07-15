package security

class BasicAuthConfig {
    var username: String = "admin@example.com"
    var password: String? = null
    var roles: Set<String> = mutableSetOf()

    override fun toString(): String {
        return "BasicAuthConfig(username='$username', password='$password', roles=$roles)"
    }
}