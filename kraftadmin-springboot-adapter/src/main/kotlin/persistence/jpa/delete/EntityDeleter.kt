package persistence.jpa.delete

import com.kraftadmin.annotations.KraftAdminField
import com.kraftadmin.enums.FormInputType
import com.kraftadmin.utils.files.AdminStorageProvider
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.transaction.support.TransactionTemplate
import persistence.jpa.metadata.EntityMetadata
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

/**
 * Handles soft and hard delete operations.
 */
class EntityDeleter<T : Any>(
    private val entityClass: KClass<T>,
    private val entityManager: EntityManager,
    private val transactionTemplate: TransactionTemplate,
    private val metadata: EntityMetadata<T>,
    adminStorageProvider: AdminStorageProvider
) {
    private val logger = LoggerFactory.getLogger(EntityDeleter::class.java)
    private val fileCleanupService = FileCleanupService(adminStorageProvider)

    fun delete(id: String): Boolean {
        return transactionTemplate.execute { status ->
            try {
                val convertedId = metadata.convertId(entityManager, id)
                val entity = entityManager.find(entityClass.java, convertedId)?: return@execute false
                // Perform cleanup before removing
                fileCleanupService.cleanupFiles(entity)
                entityManager.remove(entity)
                entityManager.flush()
                true
            } catch (e: Exception) {
                logger.error("Delete failed for ${entityClass.simpleName}#$id: ${e.message}", e)
                status.setRollbackOnly()
                false
            }
        } ?: false
    }

    fun bulkDelete(ids: List<String>): Int {
        return transactionTemplate.execute { status ->
            try {
                var count = 0
                ids.forEach { id ->
                    val convertedId = metadata.convertId(entityManager, id)
                    val entity = entityManager.find(entityClass.java, convertedId)
                    if (entity != null) { entityManager.remove(entity); count++ }
                }
                entityManager.flush()
                count
            } catch (e: Exception) {
                logger.error("Bulk delete failed for ${entityClass.simpleName}: ${e.message}", e)
                status.setRollbackOnly()
                0
            }
        } ?: 0
    }



}