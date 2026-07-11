package discovery.descriptors.validation

import validation.JakartaValidationExtractor
import java.lang.reflect.Field

//class ValidationDescriptorBuilder {
//}

object ValidationDescriptorBuilder {
    private val extractor = JakartaValidationExtractor()

    fun buildRules(javaField: Field): String = extractor.extractRules(javaField)

    fun buildMessages(javaField: Field): Map<String, String> = extractor.extractMessages(javaField)

    fun isRequired(javaField: Field): Boolean = buildRules(javaField).contains("required")
}