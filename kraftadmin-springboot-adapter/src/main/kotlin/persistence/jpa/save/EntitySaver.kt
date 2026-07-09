//package persistence.jpa.save
//
//import jakarta.persistence.EntityManager
//import org.slf4j.LoggerFactory
//import org.springframework.transaction.support.TransactionTemplate
//import persistence.jpa.conversion.FormDataCoercer
//import persistence.jpa.metadata.EntityMetadata
//import kotlin.reflect.KClass
//import kotlin.reflect.full.memberProperties
//import kotlin.reflect.jvm.javaField
//
///**
// * Orchestrates CREATE and UPDATE operations.
// * Delegates field writing to PropertyWriter + RelationshipWriter.
// */
//class EntitySaver<T : Any>(
//    private val entityClass: KClass<T>,
//    private val entityManager: EntityManager,
//    private val transactionTemplate: TransactionTemplate,
//    private val metadata: EntityMetadata<T>,
//    private val instantiator: EntityInstantiator<T>,
//    private val propertyWriter: PropertyWriter,
//    private val relationshipWriter: RelationshipWriter
//) {
//    private val logger = LoggerFactory.getLogger(EntitySaver::class.java)
//
//    fun create(data: Map<String, Any?>): Any? {
//        // ✅ Coerce once at the entry point — both writers receive the same coerced map
//        val coerced = FormDataCoercer.coerce(data)
//
//        logger.warn("coerced {}", coerced)
//
//        return transactionTemplate.execute { status ->
//            try {
//                val entity = instantiator.newInstance()
//                propertyWriter.write(entityClass,entity, coerced)
//                relationshipWriter.write(entityClass, entity, coerced)
//                entityManager.persist(entity)
//                entityManager.flush()
//                entity
//            } catch (e: Exception) {
//                logger.error("Create failed for ${entityClass.simpleName}: ${e.message}", e)
//                status.setRollbackOnly()
//                null
//            }
//        }
//    }
//
////    fun update(id: String, data: Map<String, Any?>): Any? {
////        val coerced = FormDataCoercer.coerce(data)
////
////        return transactionTemplate.execute { status ->
////            try {
////                val convertedId = metadata.convertId(entityManager, id)
////                val entity = entityManager.find(entityClass.java, convertedId)
////                    ?: throw IllegalArgumentException("${entityClass.simpleName}#$id not found")
////
////                propertyWriter.write(entityClass, entity, coerced)
////                relationshipWriter.write(entityClass, entity, coerced)
////                entityManager.merge(entity)
////                entityManager.flush()
////                entity
////            } catch (e: Exception) {
////                logger.error("Update failed for ${entityClass.simpleName}#$id: ${e.message}", e)
////                status.setRollbackOnly()
////                null
////            }
////        }
////    }
//
//    fun update(id: String, data: Map<String, Any?>): Any? {
//        val coerced = FormDataCoercer.coerce(data)
//
//        return transactionTemplate.execute { status ->
//            try {
//                val convertedId = metadata.convertId(entityManager, id)
//                val entity = entityManager.find(entityClass.java, convertedId)
//                    ?: throw IllegalArgumentException("${entityClass.simpleName}#$id not found")
//
//                // 1. Perform writes
//                propertyWriter.write(entityClass, entity, coerced)
//                relationshipWriter.write(entityClass, entity, coerced)
//
//                // 2. DYNAMIC AUDIT DETECTION
//                // Instead of casting to a hardcoded BaseEntity, find the field
//                // via reflection or your Metadata object
//                updateTimestampDynamically(entity)
//
//                entityManager.flush()
//                entity
//            } catch (e: Exception) {
//                status.setRollbackOnly()
//                null
//            }
//        }
//    }
//
//    private fun updateTimestampDynamically(entity: Any) {
//        // 1. Define supported names and annotations
//        val supportedNames = listOf("updatedAt", "lastModifiedAt")
//
//        val timestampField = entity::class.memberProperties.find { prop ->
//            // Check by property name
//            prop.name in supportedNames ||
//
//                    // Check by Hibernate annotation
//                    prop.javaField?.isAnnotationPresent(org.hibernate.annotations.UpdateTimestamp::class.java) == true ||
//
//                    // Check by Spring Data JPA annotation
//                    prop.javaField?.isAnnotationPresent(org.springframework.data.annotation.LastModifiedDate::class.java) == true ||
//
//                    // Check by naming convention
//                    prop.name.lowercase().let { it.contains("updatedat") || it.contains("lastmodified") }
//        }
//
//        timestampField?.let { prop ->
//            val field = prop.javaField
//            field?.isAccessible = true
//            // Only set if the field is actually a LocalDateTime or similar
//            try {
//                field?.set(entity, java.time.LocalDateTime.now())
//            } catch (e: Exception) {
//                logger.warn("Could not set timestamp on ${prop.name}: ${e.message}")
//            }
//        }
//    }
//
//
//}

package persistence.jpa.save

import com.kraftadmin.context.KraftAdminContextHolder
import com.kraftadmin.events.KraftAdminEvent
import com.kraftadmin.events.KraftLifecycleService
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.transaction.support.TransactionTemplate
import persistence.jpa.conversion.FormDataCoercer
import persistence.jpa.metadata.EntityMetadata
import kotlin.reflect.KClass

class EntitySaver<T : Any>(
    private val entityClass: KClass<T>,
    private val entityManager: EntityManager,
    private val transactionTemplate: TransactionTemplate,
    private val metadata: EntityMetadata<T>,
    private val instantiator: EntityInstantiator<T>,
    private val propertyWriter: PropertyWriter,
    private val relationshipWriter: RelationshipWriter,
    private val lifecycle: KraftLifecycleService
) {
    private val logger = LoggerFactory.getLogger(EntitySaver::class.java)

    fun create(data: Map<String, Any?>): Any? {
        val coerced = FormDataCoercer.coerce(data)
        logger.debug("EntitySaver.create: coerced keys={}", coerced.keys)

        return transactionTemplate.execute { status ->
            try {
                val entity = instantiator.newInstance()
                // ✅ PropertyWriter handles: simple fields + ElementCollection
                // RelationshipWriter handles: ManyToOne, OneToOne, ManyToMany, OneToMany, Embedded
                propertyWriter.writeCoerced(entity, coerced)
                relationshipWriter.writeCoerced(entityClass, entity, coerced)
                entityManager.persist(entity)
                entityManager.flush()
                entity
            } catch (e: Exception) {
                logger.error("Create failed for ${entityClass.simpleName}: ${e.message}", e)
                status.setRollbackOnly()
                null
            }
        }
    }

//    fun update(id: String, data: Map<String, Any?>): Any? {
//        val coerced = FormDataCoercer.coerce(data)
//        logger.debug("EntitySaver.update #{}: coerced keys={}", id, coerced.keys)
//
//        return transactionTemplate.execute { status ->
//            try {
//                val convertedId = metadata.convertId(entityManager, id)
//                val entity = entityManager.find(entityClass.java, convertedId)
//                    ?: throw IllegalArgumentException("${entityClass.simpleName}#$id not found")
//
//                propertyWriter.writeCoerced(entity, coerced)
//                relationshipWriter.writeCoerced(entityClass, entity, coerced)
//                entityManager.merge(entity)
//                entityManager.flush()
//                entity
//            } catch (e: Exception) {
//                logger.error("Update failed for ${entityClass.simpleName}#$id: ${e.message}", e)
//                status.setRollbackOnly()
//                null
//            }
//        }
//    }

    fun update(
        id: String,
        data: Map<String, Any?>,
    ): Any? {

        val coerced = FormDataCoercer.coerce(data)

        return transactionTemplate.execute { status ->
            try {

                val convertedId = metadata.convertId(entityManager, id)

                val entity = entityManager.find(entityClass.java, convertedId)
                    ?: throw IllegalArgumentException("${entityClass.simpleName}#$id not found")

//                val context = KraftAdminEventContext(
//                    traceId = "",
//                    actorUsername = "",
//                    actorRoles = emptySet(),
//                    tenantId = "",
//                    ipAddress = "",
//                    userAgent = "",
//                    extra = emptyMap()
//                )

                val context = KraftAdminContextHolder.eventContext()

                logger.info("context $context")

                lifecycle.onBeforeUpdate(
                    KraftAdminEvent.BeforeUpdate(
                        resourceName = entity.javaClass.simpleName,
                        entity = entity,
                        id = id,
                        data = coerced,
                        context = context
                    )
                )

                propertyWriter.writeCoerced(entity, coerced)
                relationshipWriter.writeCoerced(entityClass, entity, coerced)

                entityManager.merge(entity)
                entityManager.flush()

                lifecycle.onAfterUpdate(
                    KraftAdminEvent.AfterUpdate(
                        resourceName = entity.javaClass.simpleName,
                        entity = entity,
                        id = id,
                        data = coerced,
                        context = context
                    )
                )

                entity

            } catch (e: Exception) {
                status.setRollbackOnly()
                throw e
            }
        }
    }


}