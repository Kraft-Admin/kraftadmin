package persistence.jpa.query

import com.kraftadmin.logging.KraftAdminLogging
import jakarta.persistence.EntityManager
import jakarta.persistence.TypedQuery
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import persistence.jpa.metadata.EntityMetadata
import kotlin.reflect.KClass

/**
 * Fluent builder that assembles a complete JPA criteria query
 * from its constituent parts (predicates, sort, pagination).
 *
 * Separating this from FetchAll means any part of the system
 * can build a typed query without knowing how criteria API works.
 *
 * Usage:
 * ```
 * val result = CriteriaQueryBuilder(entityManager)
 *     .from(User::class)
 *     .where(listOf(Filter.Equals("active", true)))
 *     .search("john", listOf("name", "email"))
 *     .sort(SortSpec("createdAt", Direction.DESC))
 *     .page(PageSpec(page = 1, size = 20))
 *     .buildAndExecute()
 * ```
 */
class CriteriaQueryBuilder<T : Any>(
    private val entityManager: EntityManager,
    private val entityClass: KClass<T>,
    private val metadata: EntityMetadata<T>
) {
    private val logger = KraftAdminLogging.logger(javaClass)


    private val cb = entityManager.criteriaBuilder

    private var filters: List<PredicateBuilder.Filter> = emptyList()
    private var searchQuery: String? = null
    private var searchFields: List<String> = emptyList()
    private var allowedSortFields: List<String> = emptyList()
    private var sortSpec: SortBuilder.SortSpec? = null
    private var pageSpec: PageableBuilder.PageSpec? = null

    fun where(filters: List<PredicateBuilder.Filter>): CriteriaQueryBuilder<T> {
        this.filters = filters
        return this
    }

    fun search(query: String?, fields: List<String>): CriteriaQueryBuilder<T> {
            logger.info("Search for ${metadata.entityName} with fields $fields and query $query")

        this.searchQuery = query
        this.searchFields = fields
        return this
    }

    fun sort(spec: SortBuilder.SortSpec?, allowedFields: List<String>): CriteriaQueryBuilder<T> {
        this.sortSpec = spec
        this.allowedSortFields = allowedFields
        return this
    }

    fun page(spec: PageableBuilder.PageSpec?): CriteriaQueryBuilder<T> {
        this.pageSpec = spec
        return this
    }

    //  Count query — total matching records (ignores pagination)

    fun count(): Long {
        return try {
            val cq = cb.createQuery(Long::class.java)
            val root = cq.from(entityClass.java)
            cq.select(cb.count(root))
            applyPredicates(cq, root)
            entityManager.createQuery(cq).singleResult
        } catch (e: Exception) {
                logger.error("Count query failed for ${metadata.entityName}: ${e.message}", e)

            0L
        }
    }

    //  Data query — fetches the actual entities

    fun buildAndExecute(): List<T> {
        return try {
            val cq = cb.createQuery(entityClass.java)
            val root = cq.from(entityClass.java)

            // 1. Predicates MUST be applied to the same Root
            applyPredicates(cq, root)

            // 2. Sort MUST be applied to the same Root
            if (sortSpec != null) {
                SortBuilder.apply(cb, cq, root, sortSpec, allowedSortFields)
            }

            val typedQuery: TypedQuery<T> = entityManager.createQuery(cq)
            pageSpec?.let { PageableBuilder.apply(typedQuery, it) }

            typedQuery.resultList
        } catch (e: Exception) {
                logger.error("Query failed: ${e.message}", e)
            emptyList()
        }
    }

    private fun <R> applyPredicates(cq: CriteriaQuery<R>, root: Root<T>) {
        val predicates = mutableListOf<Predicate>()

        //  Explicit filters
        if (filters.isNotEmpty()) {
            val filterPredicates = PredicateBuilder.build(cb, root, filters)
            logger.info("Applying ${filterPredicates.size} explicit filters")
            predicates.addAll(filterPredicates)
        }

        //  Global search
        if (!searchQuery.isNullOrBlank() && searchFields.isNotEmpty()) {
            val searchPredicate = PredicateBuilder.searchPredicates(cb, root, searchQuery!!, searchFields)
            if (searchPredicate != null) {
                logger.info("Adding search predicate for query: $searchQuery")
                predicates.add(searchPredicate)
            } else {
                logger.warn("Search predicate was null for query: $searchQuery")
            }
        }

        //  Apply to query
        if (predicates.isNotEmpty()) {
            logger.info("Total predicates applied to WHERE clause: ${predicates.size}")
            cq.where(*predicates.toTypedArray())
        } else {
            logger.info("No predicates to apply to WHERE clause")
        }
    }


}
