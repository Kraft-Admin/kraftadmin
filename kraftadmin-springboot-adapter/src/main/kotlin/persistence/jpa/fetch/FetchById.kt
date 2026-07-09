//package persistence.jpa.fetch
//
//import api.utils.ResourceRow
//import com.kraftadmin.spi.KraftAdminColumn
//import jakarta.persistence.EntityManager
//import org.slf4j.LoggerFactory
//import org.springframework.transaction.support.TransactionTemplate
//import persistence.jpa.mapper.ResourceRowMapper
//import persistence.jpa.metadata.EntityMetadata
//import persistence.jpa.util.HibernateUtil
//import kotlin.reflect.KClass
//
///**
// * Handles single-entity fetch by ID.
// */
//class FetchById<T : Any>(
//    private val entityClass: KClass<T>,
//    private val entityManager: EntityManager,
//    private val transactionTemplate: TransactionTemplate,
//    private val metadata: EntityMetadata<T>,
//    private val rowMapper: ResourceRowMapper
//) {
//    private val logger = LoggerFactory.getLogger(FetchById::class.java)
//
//    fun execute(id: String, columns: List<KraftAdminColumn>): ResourceRow? {
//        return transactionTemplate.execute { _ ->
//            try {
//                val convertedId = metadata.convertId(entityManager, id)
//                val entity = entityManager.find(entityClass.java, convertedId)
//                if (entity != null) {
//                    metadata.ensureLobsInitialized(entity)
//                    // map also relations for display in ui
//                    rowMapper.mapToRow(HibernateUtil.unproxy(entity) ?: entity, columns)
//                } else null
//            } catch (e: Exception) {
//                logger.error("FetchById error for ${entityClass.simpleName}#$id: ${e.message}", e)
//                null
//            }
//        }
//    }
//}

package persistence.jpa.fetch

import api.utils.ResourceRow
import com.kraftadmin.spi.KraftAdminColumn
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.transaction.support.TransactionTemplate
import persistence.jpa.mapper.ResourceRowMapper
import persistence.jpa.metadata.EntityMetadata
import persistence.jpa.util.HibernateUtil
import kotlin.reflect.KClass

class FetchById<T : Any>(
    private val entityClass: KClass<T>,
    private val entityManager: EntityManager,
    private val transactionTemplate: TransactionTemplate,
    private val metadata: EntityMetadata<T>,
    private val rowMapper: ResourceRowMapper
) {
    private val logger = LoggerFactory.getLogger(FetchById::class.java)

    fun execute(id: String, columns: List<KraftAdminColumn>): ResourceRow? {
        return transactionTemplate.execute { _ ->
            try {
                val convertedId = metadata.convertId(entityManager, id)
                val entity = entityManager.find(entityClass.java, convertedId)
                    ?: return@execute null

                // ✅ Initialize ALL lazy associations before the transaction closes
                // RelatedResourceFetcher needs collections to be in memory
                metadata.ensureLobsInitialized(entity)

                val real = HibernateUtil.unproxy(entity) ?: entity

                // ✅ mapToDetailRow — includes related resources (max 10 each)
                // This is the ONLY place this method is called
                rowMapper.mapToDetailRow(real, columns)

            } catch (e: Exception) {
                logger.error(
                    "FetchById error for ${entityClass.simpleName}#$id: ${e.message}", e
                )
                null
            }
        }
    }

    /**
     * Returns the raw JPA entity without mapping it to a ResourceRow.
     */
    fun fetchRaw(id: String): T? {
        return transactionTemplate.execute { _ ->
            try {
                val convertedId = metadata.convertId(entityManager, id)
                val entity = entityManager.find(entityClass.java, convertedId)

                if (entity != null) {
                    // Ensure the object is not a proxy and LOBs are loaded
                    metadata.ensureLobsInitialized(entity)
                    HibernateUtil.unproxy(entity) ?: entity
                } else null
            } catch (e: Exception) {
                logger.error("FetchRaw error for ${entityClass.simpleName}#$id: ${e.message}", e)
                null
            } as T?
        }
    }

    /**
     * Returns the actual JPA entity.
     * Used by custom actions, events and business logic.
     */
    fun fetchEntity(id: String): T? {
        return transactionTemplate.execute {
            try {
                val convertedId = metadata.convertId(entityManager, id)

                val entity = entityManager.find(entityClass.java, convertedId)
                    ?: return@execute null

                metadata.ensureLobsInitialized(entity)

                HibernateUtil.unproxy(entity) as T
            } catch (e: Exception) {
                logger.error(
                    "Failed to fetch ${entityClass.simpleName}#$id",
                    e
                )
                null
            }
        }
    }

}