package persistence.jpa.validation

import jakarta.persistence.Version
import persistence.error.PersistenceErrorDetails
import persistence.error.PersistenceException
import java.lang.reflect.Field
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

/**
 * Guards @Version (optimistic locking) fields against client tampering.
 *
 * PropertyWriter writes every key present in the submitted payload onto the
 * managed entity — including `version`, if the client sends one. Without
 * this check, a client could always resubmit the entity's ORIGINAL version
 * number to silently bypass optimistic locking and overwrite a concurrent
 * edit that Hibernate would otherwise have caught at flush().
 *
 * This validator runs BEFORE PropertyWriter has a chance to overwrite the
 * version field, comparing the value still on the freshly-loaded managed
 * entity (context.entity) against whatever the client submitted
 * (context.formData). Any mismatch means the client's copy is stale.
 */
class VersionValidator : PersistenceValidator {

    override fun supports(operation: ValidationOperation): Boolean =
        operation == ValidationOperation.UPDATE

    override fun validate(context: ValidationContext<*>) {
        if (!context.isUpdate) return

        // 1. Get the field from cache. Explicitly treat the result as nullable.
        val versionField =
            resolveVersionField(context.entityClass) ?: return

        if (versionField == null) return

        // 2. If null, no @Version exists; skip validation.

        // 3. Proceed with validation
        if (!context.formData.containsKey(versionField.name)) return

        val submitted = context.formData[versionField.name] ?: return

        versionField.isAccessible = true
        val current = versionField.get(context.entity)

        if (current != null && !valuesMatch(current, submitted)) {
            throw PersistenceException(
                PersistenceErrorDetails(
                    "locked",
                    "This record was modified by someone else. Please reload and try again."
                )
            )
        }
    }

    private fun valuesMatch(current: Any, submitted: Any): Boolean =
        current.toString() == submitted.toString()

    private fun resolveVersionField(entityClass: KClass<*>): Field? =
        entityClass.memberProperties
            .mapNotNull { it.javaField }
            .firstOrNull {
                it.isAnnotationPresent(Version::class.java)
            }

}