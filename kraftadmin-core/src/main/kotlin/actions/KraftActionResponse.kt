package actions

/**
 * Standardised response from any KraftActionHandler.
 *
 */
class KraftActionResponse private constructor(
    val success: Boolean,
    val message: String,
    val payload: Any?,
    val refresh: Boolean,
    val redirect: String?,
    val errors: Map<String, List<String>>
) {

    class Builder {
        private var success = true
        private var message = ""
        private var payload: Any? = null
        private var refresh = true
        private var redirect: String? = null
        private var errors = emptyMap<String, List<String>>()

        fun success(value: Boolean) = apply { success = value }
        fun message(value: String) = apply { message = value }
        fun payload(value: Any?) = apply { payload = value }
        fun refresh(value: Boolean) = apply { refresh = value }
        fun redirect(value: String?) = apply { redirect = value }
        fun errors(value: Map<String, List<String>>) = apply { errors = value }

        fun build() = KraftActionResponse(
            success,
            message,
            payload,
            refresh,
            redirect,
            errors
        )
    }

    companion object {

        @JvmStatic
        fun ok(message: String) =
            Builder()
                .message(message)

        @JvmStatic
        fun fail(message: String) =
            Builder()
                .success(false)
                .message(message)
    }

    override fun toString(): String {
        return "KraftActionResponse(" +
                "success=$success, " +
                "message='$message', " +
                "payload=$payload, " +
                "refresh=$refresh, " +
                "redirect=$redirect, " +
                "errors=$errors" +
                ")"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KraftActionResponse) return false

        return success == other.success &&
                message == other.message &&
                payload == other.payload &&
                refresh == other.refresh &&
                redirect == other.redirect &&
                errors == other.errors
    }

    override fun hashCode(): Int {
        var result = success.hashCode()
        result = 31 * result + message.hashCode()
        result = 31 * result + (payload?.hashCode() ?: 0)
        result = 31 * result + refresh.hashCode()
        result = 31 * result + (redirect?.hashCode() ?: 0)
        result = 31 * result + errors.hashCode()
        return result
    }
}