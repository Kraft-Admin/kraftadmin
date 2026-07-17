//package persistence.jpa.fetch
//
//import api.responses.PagedResponse
//import api.utils.ResourceRow
//import com.kraftadmin.annotations.KraftAdminResource
//import com.kraftadmin.context.KraftAdminContextHolder
//import com.kraftadmin.events.KraftAdminEvent1
//import com.kraftadmin.events.KraftLifecycleService
//import com.kraftadmin.spi.KraftAdminColumn
//import config.PaginationConfig
//import jakarta.persistence.EntityManager
//import org.slf4j.LoggerFactory
//import org.springframework.transaction.support.TransactionTemplate
//import persistence.error.PersistenceErrorResolver
//import persistence.error.PersistenceException
//import persistence.jpa.mapper.ResourceRowMapper
//import persistence.jpa.metadata.EntityMetadata
//import persistence.jpa.query.CriteriaQueryBuilder
//import persistence.jpa.query.PageableBuilder
//import persistence.jpa.query.PredicateBuilder
//import persistence.jpa.query.SortBuilder
//import kotlin.collections.emptyList
//import kotlin.reflect.KClass
//import kotlin.reflect.full.findAnnotation
//
///**
// * Handles paginated entity fetching.
// * Delegates all query construction to the query/ package —
// * this class only owns the fetch lifecycle.
// */
//class FetchAll<T : Any>(
//    private val entityClass: KClass<T>,
//    private val entityManager: EntityManager,
//    private val transactionTemplate: TransactionTemplate,
//    private val metadata: EntityMetadata<T>,
//    private val rowMapper: ResourceRowMapper,
//    private val pagination: PaginationConfig,
//    private val lifecycle: KraftLifecycleService,
//    val errorResolver: PersistenceErrorResolver
//) {
//    private val logger = LoggerFactory.getLogger(FetchAll::class.java)
//
//    fun execute(
//        page: Int,
//        size: Int,
//        columns: List<KraftAdminColumn>,
//        searchQuery: String? = null,
//        sortField: String? = null,
//        sortDirection: String? = null,
//        filters: List<PredicateBuilder.Filter> = emptyList()
//    ): PagedResponse<ResourceRow> {
//        logger.info("fetching all resources for ${entityClass.simpleName} and query $searchQuery")
//
//        val context = KraftAdminContextHolder.eventContext()
//
//        // Check if the resource has search globally disabled via annotation
//        val resourceAnno = entityClass.findAnnotation<KraftAdminResource>()
//        val globalSearchEnabled = resourceAnno?.searchable ?: true
//
//        val activeSearchQuery = if (globalSearchEnabled) searchQuery else null
//
//        val pageSpec = PageableBuilder.PageSpec(
//            page = page,
//            size = size,
//            maxSize = pagination.maxPageSize
//        )
//
//        val sortSpec = SortBuilder.from(
//            field = sortField ?: metadata.defaultSort,
//            direction = sortDirection
//        )
//
//        lifecycle.onBeforeFetchAll(
//            KraftAdminEvent1.BeforeFetchAll(
//                resourceName = entityClass.simpleName!!,
//                page = page,
//                size = size,
//                filters = PredicateBuilder.toEventFilters(filters),
//                sortField = sortField,
//                sortDirection = sortDirection,
//                context = context,
//                searchQuery =  activeSearchQuery,
//            )
//        )
//
//        val response = transactionTemplate.execute { status ->
//            try {
//                val queryBuilder = CriteriaQueryBuilder(entityManager, entityClass)
//                    .where(filters)
//                    .search(activeSearchQuery, if (globalSearchEnabled) metadata.searchableFields else emptyList())
//                    .sort(
//                        sortSpec,
//                        allowedFields = metadata.sortableFields
//                    )
//
//                // Count before applying pagination — total matching records
//                val total = queryBuilder.count()
//
//                // Data query with pagination applied
//                val entities = queryBuilder
//                    .page(pageSpec)
//                    .buildAndExecute()
//
//                val tableColumns = columns.filter { it.showInTable }
//
//                val rows = entities.map { entity ->
//                    metadata.ensureLobsInitialized(entity)
//                    rowMapper.mapToRow(entity, tableColumns)
//                }
//
//                val response = PagedResponse(
//                    items = rows,
//                    total = total,
//                    page = pageSpec.effectivePage,
//                    totalPages = PageableBuilder.totalPages(total, pageSpec.effectiveSize),
//                    pageSize = pageSpec.effectiveSize
//                )
//
//                lifecycle.onAfterFetchAll(
//                    KraftAdminEvent1.AfterFetchAll(
//                        resourceName = entityClass.simpleName!!,
//                        page = response.page,
//                        size = response.pageSize,
//                        total = response.total,
//                        returned = response.items.size,
//                        context = context
//                    )
//                )
//
//                response
//            } catch (e: Exception) {
//                logger.error("FetchAll failed for ${entityClass.simpleName}: ${e.message}", e)
//                status.setRollbackOnly()
//                lifecycle.onFetchAllFailed(
//                    KraftAdminEvent1.FetchAllFailed(
//                        resourceName = entityClass.simpleName!!,
//                        page = page,
//                        size = size,
//                        exception = e,
//                        context = context,
//                        searchQuery = activeSearchQuery,
//                    )
//                )
//
//                PagedResponse(
//                    items = emptyList(),
//                    total = 0,
//                    page = pageSpec.effectivePage,
//                    totalPages = 0,
//                    pageSize = pageSpec.effectiveSize
//                )
//
//                throw PersistenceException(
//                    errorResolver.resolve(entityClass.simpleName ?: "Resource", e),
//                    e
//                )
//
//            }
//        }
//
//        return response ?: PagedResponse(
//            items = emptyList(),
//            total = 0,
//            page = pageSpec.effectivePage,
//            pageSize = pageSpec.effectiveSize,
//            totalPages = 0
//        )
//    }
//
//}


package persistence.jpa.fetch

import api.responses.PagedResponse
import api.utils.ResourceRow
import com.kraftadmin.annotations.KraftAdminResource
import com.kraftadmin.context.KraftAdminContextHolder
import com.kraftadmin.events.KraftAdminEvent
import com.kraftadmin.events.KraftLifecycleService
import com.kraftadmin.spi.KraftAdminColumn
import config.PaginationConfig
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.transaction.support.TransactionTemplate
import persistence.error.PersistenceErrorResolver
import persistence.error.PersistenceException
import persistence.jpa.mapper.ResourceRowMapper
import persistence.jpa.metadata.EntityMetadata
import persistence.jpa.query.CriteriaQueryBuilder
import persistence.jpa.query.PageableBuilder
import persistence.jpa.query.PredicateBuilder
import persistence.jpa.query.SortBuilder
import kotlin.collections.emptyList
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

/**
 * Handles paginated entity fetching.
 * Delegates all query construction to the query/ package —
 * this class only owns the fetch lifecycle.
 *
 * Contract: this method either returns a valid PagedResponse or throws
 * PersistenceException. It never silently returns an empty page to mask
 * a failure — callers can rely on that distinction.
 */
class FetchAll<T : Any>(
    private val entityClass: KClass<T>,
    private val entityManager: EntityManager,
    private val transactionTemplate: TransactionTemplate,
    private val metadata: EntityMetadata<T>,
    private val rowMapper: ResourceRowMapper,
    private val pagination: PaginationConfig,
    private val lifecycle: KraftLifecycleService,
    val errorResolver: PersistenceErrorResolver,
) {
    private val logger = LoggerFactory.getLogger(FetchAll::class.java)

    fun execute(
        page: Int,
        size: Int,
        columns: List<KraftAdminColumn>,
        searchQuery: String? = null,
        sortField: String? = null,
        sortDirection: String? = null,
        filters: List<PredicateBuilder.Filter> = emptyList()
    ): PagedResponse<ResourceRow> {
        logger.info("fetching all resources for ${metadata.entityName} and query $searchQuery")

        val context = KraftAdminContextHolder.eventContext()

        // Check if the resource has search globally disabled via annotation
        val resourceAnno = entityClass.findAnnotation<KraftAdminResource>()
        val globalSearchEnabled = resourceAnno?.searchable ?: true

        val activeSearchQuery = if (globalSearchEnabled) searchQuery else null

        val pageSpec = PageableBuilder.PageSpec(
            page = page,
            size = size,
            maxSize = pagination.maxPageSize
        )

        val sortSpec = SortBuilder.from(
            field = sortField ?: metadata.defaultSort,
            direction = sortDirection
        )

        lifecycle.onBeforeFetchAll(
            KraftAdminEvent.BeforeFetchAll(
                resourceName = metadata.entityName,
                page = page,
                size = size,
                filters = PredicateBuilder.toEventFilters(filters),
                sortField = sortField,
                sortDirection = sortDirection,
                context = context,
                searchQuery = activeSearchQuery,
            )
        )

        val response = transactionTemplate.execute { status ->
            try {
                val queryBuilder = CriteriaQueryBuilder(entityManager, entityClass, metadata)
                    .where(filters)
                    .search(activeSearchQuery, if (globalSearchEnabled) metadata.searchableFields else emptyList())
                    .sort(
                        sortSpec,
                        allowedFields = metadata.sortableFields
                    )

                // Count before applying pagination — total matching records
                val total = queryBuilder.count()

                // Data query with pagination applied
                val entities = queryBuilder
                    .page(pageSpec)
                    .buildAndExecute()

                val tableColumns = columns.filter { it.showInTable }

                val rows = entities.map { entity ->
                    metadata.ensureLobsInitialized(entity)
                    rowMapper.mapToRow(entity, tableColumns)
                }

                val result = PagedResponse(
                    items = rows,
                    total = total,
                    page = pageSpec.effectivePage,
                    totalPages = PageableBuilder.totalPages(total, pageSpec.effectiveSize),
                    pageSize = pageSpec.effectiveSize
                )

                lifecycle.onAfterFetchAll(
                    KraftAdminEvent.AfterFetchAll(
                        resourceName = metadata.entityName,
                        page = result.page,
                        size = result.pageSize,
                        total = result.total,
                        returned = result.items.size,
                        context = context
                    )
                )

                result

            } catch (e: Exception) {
                logger.error("FetchAll failed for ${metadata.entityName}: ${e.message}", e)
                status.setRollbackOnly()
                lifecycle.onFetchAllFailed(
                    KraftAdminEvent.FetchAllFailed(
                        resourceName = metadata.entityName,
                        page = page,
                        size = size,
                        exception = e,
                        context = context,
                        searchQuery = activeSearchQuery,
                    )
                )

                throw PersistenceException(
                    errorResolver.resolve( metadata.entityName ?: "Resource", e),
                    e
                )
            }
        }

        // transactionTemplate.execute returns null only if the lambda's
        // TransactionStatus was marked rollback-only without an exception
        // ever propagating (rare, but possible depending on Spring internals).
        // Since the try/catch above always either returns a valid result or
        // throws, this is treated as a genuine failure too, not a silent
        // empty page — consistent with FetchById and EntityDeleter.
        return response ?: throw PersistenceException(
            errorResolver.resolve(
                entityClass.simpleName ?: "Resource",
                IllegalStateException("Transaction completed without a result for ${metadata.entityName}")
            )
        )
    }
}