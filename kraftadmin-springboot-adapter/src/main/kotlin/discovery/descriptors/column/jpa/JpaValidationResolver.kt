package discovery.descriptors.column.jpa

import util.JakartaValidationExtractor
import java.lang.reflect.Field

class JpaValidationResolver {

    private val extractor = JakartaValidationExtractor()

    fun resolve(field: Field): ValidationMetadata {

        val rules = extractor.extractRules(field)
        val messages = extractor.extractMessages(field)

        return ValidationMetadata(
            required = rules.contains("required"),
            rules = rules.ifBlank { null },
            messages = messages.ifEmpty { null }
        )
    }
}

/**
 * Simple DTO returned by the validation resolver.
 */
data class ValidationMetadata(
    val required: Boolean,
    val rules: String?,
    val messages: Map<String, String>?
)