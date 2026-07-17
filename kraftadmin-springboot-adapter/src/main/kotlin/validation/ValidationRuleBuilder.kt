package validation

import annotations.KraftAnnotationUtils
import com.kraftadmin.annotations.KraftAdminField
import com.kraftadmin.enums.FormInputType
import org.slf4j.LoggerFactory
import java.lang.reflect.Field

object ValidationRuleBuilder {

    fun getBaseRules(field: Field): MutableSet<String> {
        val rules = linkedSetOf<String>()

        val admin = KraftAnnotationUtils.getAnnotation(field, KraftAdminField::class)
            ?: return rules

        if (admin.regex.isNotBlank()) {
            rules += "regex:${admin.regex}"
        }

        when (admin.inputType) {
            FormInputType.TEXT,
            FormInputType.TEXTAREA,
            FormInputType.WYSIWYG ->
                rules += "string"

            FormInputType.NUMBER,
            FormInputType.RANGE ->
                rules += "numeric"

            FormInputType.COLOR ->
                rules += "hexColor"

            FormInputType.EMAIL ->
                rules += "email"

            FormInputType.TEL ->
                rules += "tel"

            FormInputType.URL ->
                rules += "url"

            FormInputType.PASSWORD -> {
                rules += "minLength:8"
                rules += "mustContainUppercase"
                rules += "mustContainSpecialChar"
            }

            FormInputType.DATE,
            FormInputType.DATETIME,
            FormInputType.TIME ->
                rules += "date"

            else -> {}
        }

        return rules
    }

    fun getBaseMessages(field: Field): MutableMap<String, String> {
        val messages = linkedMapOf<String, String>()

        val admin = KraftAnnotationUtils.getAnnotation(field, KraftAdminField::class)
            ?: return messages

        val label = admin.label.ifBlank { field.javaClass.simpleName }

        if (admin.validationMessage.isNotBlank()) {
            if (admin.required) {
                messages["required"] = admin.validationMessage
            }

            messages["regex"] = admin.validationMessage
        }

        if (admin.required) {
            messages.putIfAbsent(
                "required",
                "$label is required"
            )
        }

        when (admin.inputType) {

            FormInputType.EMAIL ->
                messages.putIfAbsent(
                    "email",
                    "Please enter a valid email address"
                )

            FormInputType.TEL ->
                messages.putIfAbsent(
                    "tel",
                    "Please enter a valid phone number"
                )

            FormInputType.URL ->
                messages.putIfAbsent(
                    "url",
                    "Please enter a valid URL"
                )

            FormInputType.COLOR ->
                messages.putIfAbsent(
                    "hexColor",
                    "Please select a valid hex color"
                )

            FormInputType.PASSWORD -> {
                messages.putIfAbsent(
                    "minLength",
                    "$label must be at least 8 characters"
                )

                messages.putIfAbsent(
                    "mustContainUppercase",
                    "$label must contain an uppercase letter"
                )

                messages.putIfAbsent(
                    "mustContainSpecialChar",
                    "$label must contain a special character"
                )
            }

            else -> {}
        }

        return messages
    }

}