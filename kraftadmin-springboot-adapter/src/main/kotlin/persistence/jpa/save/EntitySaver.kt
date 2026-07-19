package persistence.jpa.save

import com.kraftadmin.context.KraftAdminContextHolder
import com.kraftadmin.events.KraftAdminEvent
import com.kraftadmin.events.KraftLifecycleService
import com.kraftadmin.logging.KraftAdminLogging
import jakarta.persistence.EntityManager
import org.springframework.transaction.support.TransactionTemplate
import persistence.error.PersistenceErrorResolver
import persistence.error.PersistenceException
import persistence.jpa.conversion.FormDataCoercer
import persistence.jpa.metadata.EntityMetadata
import persistence.jpa.validation.PersistenceValidationService
import persistence.jpa.validation.ValidationContext
import persistence.jpa.validation.ValidationOperation
import kotlin.reflect.KClass

class EntitySaver<T : Any>(
    private val entityClass: KClass<T>,
    private val entityManager: EntityManager,
    private val transactionTemplate: TransactionTemplate,
    private val metadata: EntityMetadata<T>,
    private val instantiator: EntityInstantiator<T>,
    private val propertyWriter: PropertyWriter,
    private val relationshipWriter: RelationshipWriter,
    private val lifecycle: KraftLifecycleService,
    val errorResolver: PersistenceErrorResolver,
    private val validationService: PersistenceValidationService,
) {
    private val logger = KraftAdminLogging.logger(javaClass)


    fun create(data: Map<String, Any?>): Any? {
        val coerced = FormDataCoercer.coerce(data)
//        logger.debug("EntitySaver.create: coerced keys={}", coerced.keys)

        val entity = instantiator.newInstance()
        val context = KraftAdminContextHolder.adminContext()


        return transactionTemplate.execute { status ->
            try {
                propertyWriter.writeCoerced(entity, coerced)
                relationshipWriter.writeCoerced(entityClass, entity, coerced)

                validationService.validate(
                    ValidationContext(
                        entityClass = entityClass,
                        entity = entity,
                        operation = ValidationOperation.CREATE,
                        metadata = metadata,
                        entityManager = entityManager,
                        formData = coerced
                    )
                )

                // Allow listeners to modify the entity before persistence
                lifecycle.onBeforeCreate(KraftAdminEvent.BeforeCreate(
                    resourceName = entityClass.simpleName ?: entity.javaClass.simpleName,
                    data = coerced,
                    entity = entity,
                    context = context,
                ))

                entityManager.persist(entity)
                entityManager.flush()

                // Notify listeners after successful persistence
                lifecycle.onAfterCreate(KraftAdminEvent.AfterCreate(
                    resourceName = entityClass.simpleName ?: entity.javaClass.simpleName,
                    entity = entity,
                    data = coerced,
                    context = context,
                    )
                )

                entity
            }catch (e: Exception) {

                logger.error(
                    "Create failed for {}",
                    e
                )

                status.setRollbackOnly()

                lifecycle.onCreateFailed(
                    KraftAdminEvent.CreateFailed(
                        resourceName = entityClass.simpleName
                            ?: entity.javaClass.simpleName,
                        entity = entity,
                        data = coerced,
                        context = context,
                        exception = e
                    )
                )

                throw PersistenceException(
                    errorResolver.resolve(
                        entityClass.simpleName ?: "Resource",
                        e
                    ),
                    e
                )
            }


        }
    }


    fun update(
        id: String,
        data: Map<String, Any?>,
    ): Any? {
        val coerced = FormDataCoercer.coerce(data)
        var entity: Any? = null
        val context = KraftAdminContextHolder.adminContext()


        return transactionTemplate.execute { status ->
            try {
                val convertedId = metadata.convertId(id)

                val found = entityManager.find(entityClass.java, convertedId)
                    ?: throw IllegalArgumentException("${entityClass.simpleName}#$id not found")

                entity = found
                logger.info("context $context")

                lifecycle.onBeforeUpdate(
                    KraftAdminEvent.BeforeUpdate(
                        resourceName = found.javaClass.simpleName,
                        entity = found,
                        id = id,
                        data = coerced,
                        context = context
                    )
                )

                propertyWriter.writeCoerced(found, coerced)
                relationshipWriter.writeCoerced(entityClass, found, coerced)

                validationService.validate(
                            ValidationContext(
                            entityClass = entityClass,
                    entity = found,
                    operation = ValidationOperation.UPDATE,
                    entityId = convertedId,
                    metadata = metadata,
                    entityManager = entityManager,
                    formData = coerced
                    )
                )


                entityManager.merge(found)
                entityManager.flush()

                lifecycle.onAfterUpdate(
                    KraftAdminEvent.AfterUpdate(
                        resourceName = found.javaClass.simpleName,
                        entity = found,
                        id = id,
                        data = coerced,
                        context = context
                    )
                )

                found

            } catch (e: Exception) {
                status.setRollbackOnly()
                lifecycle.onCreateFailed(
                    KraftAdminEvent.CreateFailed(
                        resourceName = entityClass.simpleName
                            ?: entity?.javaClass?.simpleName!!,
                        entity = entity,
                        data = coerced,
                        context = context,
                        exception = e
                    )
                )
                throw PersistenceException(
                    errorResolver.resolve(
                        entityClass.simpleName ?: "Resource",
                        e
                    ),
                    e
                )
            }
        }
    }


}