package persistence.jpa.fetch

import api.utils.ResourceRow
import com.kraftadmin.context.KraftAdminContextHolder
import com.kraftadmin.events.KraftAdminEvent
import com.kraftadmin.events.KraftLifecycleService
import com.kraftadmin.spi.KraftAdminColumn
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.transaction.support.TransactionTemplate
import persistence.error.PersistenceError
import persistence.error.PersistenceErrorResolver
import persistence.error.PersistenceException
import persistence.jpa.mapper.ResourceRowMapper
import persistence.jpa.metadata.EntityMetadata
import persistence.jpa.util.HibernateUtil
import kotlin.reflect.KClass

class FetchById<T : Any>(
    private val entityClass: KClass<T>,
    private val entityManager: EntityManager,
    private val transactionTemplate: TransactionTemplate,
    private val metadata: EntityMetadata<T>,
    private val rowMapper: ResourceRowMapper,
    private val lifecycle: KraftLifecycleService,
    val errorResolver: PersistenceErrorResolver
) {
    private val logger = LoggerFactory.getLogger(FetchById::class.java)

    fun execute(id: String, columns: List<KraftAdminColumn>): ResourceRow? {
        val context = KraftAdminContextHolder.eventContext()

        return transactionTemplate.execute { _ ->
            try {
                lifecycle.onBeforeFetchById(
                    KraftAdminEvent.BeforeFetchById(
                        resourceName = entityClass.simpleName!!,
                        id = id,
                        context = context
                    )
                )

                val convertedId = metadata.convertId(id)

                val entity = entityManager.find(entityClass.java, convertedId)
                    ?: throw EntityNotFoundException(
                        "${entityClass.simpleName} '$id' not found."
                    )

                // Initialize ALL lazy associations before the transaction closes
                // RelatedResourceFetcher needs collections to be in memory
                metadata.ensureLobsInitialized(entity)

//                metadata.ensureLobsInitialized(entity)

                metadata.ensureSingleRelationsInitialized(
                    entity
                )


                val real = HibernateUtil.unproxy(entity) ?: entity

                val row = rowMapper.mapToDetailRow(real, columns)

                logger.info(row.toString())

                lifecycle.onAfterFetchById(
                    KraftAdminEvent.AfterFetchById(
                        resourceName = entityClass.simpleName!!,
                        entity = real,
                        id = id,
                        context = context
                    )
                )

                row

            } catch (e: Exception) {

                logger.error(
                    "FetchById error for ${entityClass.simpleName}#$id: ${e.message}", e
                )
                lifecycle.onFetchByIdFailed(
                    KraftAdminEvent.FetchByIdFailed(
                        resourceName = entityClass.simpleName!!,
                        id = id,
                        exception = e,
                        context = context
                    )
                )

                throw PersistenceException(
                    errorResolver.resolve(entityClass.simpleName ?: "Resource", e),
                    e
                )

            }
        }
    }

    /**
     * Returns the actual JPA entity.
     * Used by custom actions, events and business logic.
     */
    fun fetchEntity(id: String): T? {
        return transactionTemplate.execute {
            try {
                val convertedId = metadata.convertId( id)

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