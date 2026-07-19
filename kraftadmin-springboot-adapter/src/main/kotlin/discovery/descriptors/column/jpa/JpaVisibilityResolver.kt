package discovery.descriptors.column.jpa

import com.kraftadmin.annotations.KraftAdminField
import com.kraftadmin.enums.FormInputType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubclassOf

/**
 * Determines whether a column should be shown in the table by default.
 *
 * Explicit @KraftAdminField(showInTable = true) always wins.
 */
class JpaVisibilityResolver {

    fun shouldShowInTable(
        property: KProperty1<out Any, *>,
        type: FormInputType,
        searchable: Boolean,
        sortable: Boolean,
        annotation: KraftAdminField?
    ): Boolean {

        // Explicit override
        if (annotation?.showInTable == true) {
            return true
        }

        // Never show id by default
        if (property.name.equals("id", ignoreCase = true)) {
            return false
        }

        val SENSITIVE_FIELD_NAMES = setOf("password", "secret", "token", "auth")

// Inside shouldShowInTable:


        val classifier = property.returnType.classifier as? KClass<*>

        return when {

            // Never display these automatically
            type in setOf(
                FormInputType.WYSIWYG,
                FormInputType.OBJECT,
                FormInputType.ARRAY,
                FormInputType.MULTI_RELATION,
                FormInputType.FILE,
                FormInputType.IMAGE,
                FormInputType.VIDEO,
                FormInputType.AUDIO,
                FormInputType.DOCUMENT,
                FormInputType.JSON
            ) -> false

            // Collections
            classifier != null &&
                    Collection::class.java.isAssignableFrom(classifier.java) ->
                false

            // Maps
            classifier != null &&
                    Map::class.java.isAssignableFrom(classifier.java) ->
                false

            // OneToOne / ManyToOne
            type == FormInputType.RELATION ->
                true

            // Dates
            classifier in setOf(
                LocalDate::class,
                LocalDateTime::class,
                LocalTime::class,
                Instant::class,
                OffsetDateTime::class,
                ZonedDateTime::class
            ) ->
                true

            // Numbers
            classifier?.isSubclassOf(Number::class) == true ->
                true

            // Boolean
            classifier == Boolean::class ->
                true

            // Enum
            classifier?.isSubclassOf(Enum::class) == true ->
                true

            // String
            classifier == String::class ->
                true

            // Fallback
            else ->
                searchable || sortable
        }
    }
}