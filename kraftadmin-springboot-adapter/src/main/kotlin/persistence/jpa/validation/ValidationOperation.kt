package persistence.jpa.validation

/**
 * Describes the persistence operation currently being validated.
 */
enum class ValidationOperation {

    /**
     * A brand new entity is being created.
     */
    CREATE,

    /**
     * An existing entity is being modified.
     */
    UPDATE
}