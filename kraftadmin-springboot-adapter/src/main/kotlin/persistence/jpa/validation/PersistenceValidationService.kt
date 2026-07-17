package persistence.jpa.validation

import org.springframework.stereotype.Component

/**
 * Coordinates execution of all registered persistence validators.
 *
 * Validators are executed in registration order.
 * Validation stops immediately when the first validator throws an exception.
 */
class PersistenceValidationService(
    private val validators: List<PersistenceValidator> = emptyList()
) {

    /**
     * Executes all validators supporting the current operation.
     *
     * @throws PersistenceException if any validator fails.
     */
    fun validate(context: ValidationContext<*>) {
        validators
            .filter { it.supports(context.operation) }
            .forEach { it.validate(context) }
    }



}