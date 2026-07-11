package discovery.descriptors.column.jpa

import com.kraftadmin.annotations.KraftAdminField
import com.kraftadmin.enums.FormInputType
import java.time.*

/**
 * Determines whether a column should be shown in the table by default.
 *
 * Explicit @KraftAdminField(showInTable = true) always wins.
 */
class JpaVisibilityResolver {

    private val sensitiveNames = setOf(
        "password",
        "passwd",
        "secret",
        "token",
        "accessToken",
        "refreshToken",
        "apiKey",
        "apikey",
        "privateKey",
        "publicKey",
        "clientSecret",
        "jwt",
        "otp",
        "pin",
        "cvv",
        "salt",
        "hash",
        "signature",
        "credential",
        "credentials"
    )

    fun shouldShowInTable(
        field: java.lang.reflect.Field,
        type: FormInputType,
        searchable: Boolean,
        sortable: Boolean,
        annotation: KraftAdminField?
    ): Boolean {

        // Explicit override
        if (annotation?.showInTable == true) {
            return true
        }

        // Sensitive fields should never be shown automatically
        if (annotation?.sensitive == true) {
            return false
        }

        if (field.name.lowercase() in sensitiveNames) {
            return false
        }

        // Hide primary key by default
        if (field.name.equals("id", ignoreCase = true)) {
            return false
        }

        val clazz = field.type

        return when {

            type == FormInputType.WYSIWYG ||
                    type == FormInputType.OBJECT ||
                    type == FormInputType.ARRAY ||
                    type == FormInputType.MULTI_RELATION ||
                    type == FormInputType.FILE ||
                    type == FormInputType.IMAGE ||
                    type == FormInputType.VIDEO ||
                    type == FormInputType.AUDIO ||
                    type == FormInputType.DOCUMENT ||
                    type == FormInputType.JSON ->
                false

            Collection::class.java.isAssignableFrom(clazz) ->
                false

            Map::class.java.isAssignableFrom(clazz) ->
                false

            type == FormInputType.RELATION ->
                true

            clazz == LocalDate::class.java ||
                    clazz == LocalDateTime::class.java ||
                    clazz == LocalTime::class.java ||
                    clazz == Instant::class.java ||
                    clazz == OffsetDateTime::class.java ||
                    clazz == ZonedDateTime::class.java ->
                true

            Number::class.java.isAssignableFrom(clazz) ->
                true

            clazz == Boolean::class.java ||
                    clazz == java.lang.Boolean::class.java ->
                true

            clazz.isEnum ->
                true

            clazz == String::class.java ->
                true

            else ->
                searchable || sortable
        }
    }
}