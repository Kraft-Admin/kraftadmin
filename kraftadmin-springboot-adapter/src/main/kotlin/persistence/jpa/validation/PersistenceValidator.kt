package persistence.jpa.validation


/**
 * Performs framework-level validation before an entity is persisted.
 *
 * Validators should throw a [PersistenceValidationException] whenever
 * validation fails.
 */
interface PersistenceValidator {

    /**
     * Whether this validator should execute for the current operation.
     */
    fun supports(operation: ValidationOperation): Boolean = true

    /**
     * Performs validation.
     *
     * Implementations should throw an exception when validation fails.
     */
    fun validate(context: ValidationContext<*>)
}