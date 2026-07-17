package persistence.jpa.validation

import jakarta.persistence.EntityManager
import persistence.jpa.metadata.EntityMetadata
import kotlin.reflect.KClass

/**
 * Immutable context shared by every persistence validator.
 *
 * Validators should never need constructor injection for common state.
 * Everything required to inspect metadata, relationships and the database
 * is available through this object.
 */
data class ValidationContext<T : Any>(

    /**
     * Entity type being validated.
     */
    val entityClass: KClass<T>,

    /**
     * Entity instance after property mapping but before persistence.
     */
    val entity: T,

    /**
     * CREATE or UPDATE.
     */
    val operation: ValidationOperation,

    /**
     * Existing entity id when updating.
     */
    val entityId: Any? = null,

    /**
     * Form values submitted by the client.
     *
     * Useful for validators that need to know whether a field
     * was actually supplied.
     */
    val formData: Map<String, Any?> = emptyMap(),

    /**
     * Entity metadata discovered during startup.
     */
    val metadata: EntityMetadata<T>,

    /**
     * Active EntityManager.
     */
    val entityManager: EntityManager
) {

    /**
     * Convenience helpers.
     */
    val isCreate: Boolean
        get() = operation == ValidationOperation.CREATE

    val isUpdate: Boolean
        get() = operation == ValidationOperation.UPDATE
}