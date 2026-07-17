package persistence.jpa.query

import jakarta.persistence.EntityManager
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.TypedQuery
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.slf4j.LoggerFactory
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
    private val logger = LoggerFactory.getLogger(CriteriaQueryBuilder::class.java)

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
        if (logger.isDebugEnabled) {
            logger.info("Search for ${metadata.entityName} with fields $fields and query $query")
        }
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
            if(logger.isErrorEnabled) {
                logger.error("Count query failed for ${metadata.entityName}: ${e.message}", e)
            }
            0L
        }
    }

    // ─── Data query — fetches the actual entities ─────────────────────────

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
            if(logger.isErrorEnabled) {
                logger.error("Query failed: ${e.message}", e)
            }
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


//class CriteriaQueryBuilder1<T : Any>(
//    private val entityManager: EntityManager,
//    private val entityClass: KClass<T>,
//    private val metadata: EntityMetadata<T>
//) {
//    private val logger = LoggerFactory.getLogger(CriteriaQueryBuilder::class.java)
//
//    private val cb = entityManager.criteriaBuilder
//
//    private var filters: List<PredicateBuilder.Filter> = emptyList()
//    private var searchQuery: String? = null
//    private var searchFields: List<String> = emptyList()
//    private var allowedSortFields: List<String> = emptyList()
//    private var sortSpec: SortBuilder.SortSpec? = null
//    private var pageSpec: PageableBuilder.PageSpec? = null
//    private var relationFieldsToFetch: Set<String> = emptySet()
//
//    fun where(filters: List<PredicateBuilder.Filter>): CriteriaQueryBuilder<T> {
//        this.filters = filters
//        return this
//    }
//
//    fun search(query: String?, fields: List<String>): CriteriaQueryBuilder<T> {
//        this.searchQuery = query
//        this.searchFields = fields
//        return this
//    }
//
//    fun sort(spec: SortBuilder.SortSpec?, allowedFields: List<String>): CriteriaQueryBuilder<T> {
//        this.sortSpec = spec
//        this.allowedSortFields = allowedFields
//        return this
//    }
//
//    fun page(spec: PageableBuilder.PageSpec?): CriteriaQueryBuilder<T> {
//        this.pageSpec = spec
//        return this
//    }
//
//    /**
//     * Marks *-to-one relation fields (@ManyToOne/@OneToOne) to be LEFT JOIN
//     * FETCHed in the same query, instead of lazily initialized per-row later
//     * (which causes exactly one extra SELECT per row — the N+1 this method
//     * exists to eliminate). Only applies to buildAndExecute(); count() never
//     * needs a fetch join since it doesn't touch entity state.
//     *
//     * Deliberately ignores *-to-many field names silently — fetch-joining a
//     * collection here would multiply base rows (cartesian product) and
//     * corrupt pagination (LIMIT/OFFSET would apply to the joined row count,
//     * not the entity count). *-to-many relations are handled separately by
//     * RelatedResourceFetcher's own bounded query.
//     */
//    fun fetchSingleRelations(fieldNames: Collection<String>): CriteriaQueryBuilder<T> {
//        this.relationFieldsToFetch = fieldNames.toSet()
//        return this
//    }
//
//    fun count(): Long {
//        return try {
//            val cq = cb.createQuery(Long::class.java)
//            val root = cq.from(entityClass.java)
//            cq.select(cb.count(root))
//            applyPredicates(cq, root)
//            entityManager.createQuery(cq).singleResult
//        } catch (e: Exception) {
//            logger.error("Count query failed for ${metadata.entityName}: ${e.message}", e)
//            0L
//        }
//    }
//
//    fun buildAndExecute(): List<T> {
//        return try {
//            val cq = cb.createQuery(entityClass.java)
//            val root = cq.from(entityClass.java)
//
//            applyFetchJoins(root)
//            applyPredicates(cq, root)
//
//            if (sortSpec != null) {
//                SortBuilder.apply(cb, cq, root, sortSpec, allowedSortFields)
//            }
//
//            // DISTINCT prevents duplicate parent rows when a fetch join is
//            // present — required for correctness even for *-to-one joins
//            // once combined with certain predicate/sort shapes.
//            if (relationFieldsToFetch.isNotEmpty()) {
//                cq.distinct(true)
//            }
//
//            val typedQuery: TypedQuery<T> = entityManager.createQuery(cq)
//            pageSpec?.let { PageableBuilder.apply(typedQuery, it) }
//
//            typedQuery.resultList
//        } catch (e: Exception) {
//            logger.error("Query failed: ${e.message}", e)
//            emptyList()
//        }
//    }
//
//    private fun applyFetchJoins(root: Root<T>) {
//        if (relationFieldsToFetch.isEmpty()) return
//
//        relationFieldsToFetch.forEach { fieldName ->
//            try {
//                val field = findField(entityClass.java, fieldName) ?: return@forEach
//                val isSingular = field.isAnnotationPresent(ManyToOne::class.java) ||
//                        field.isAnnotationPresent(OneToOne::class.java)
//                if (isSingular) {
//                    root.fetch<T, Any>(fieldName, JoinType.LEFT)
//                }
//            } catch (e: Exception) {
//                logger.debug("Could not fetch-join field '{}': {}", fieldName, e.message)
//            }
//        }
//    }
//
//    private fun findField(cls: Class<*>, name: String): java.lang.reflect.Field? {
//        var current: Class<*>? = cls
//        while (current != null && current != Any::class.java) {
//            try {
//                return current.getDeclaredField(name)
//            } catch (e: NoSuchFieldException) {
//                current = current.superclass
//            }
//        }
//        return null
//    }
//
//    private fun <R> applyPredicates(cq: CriteriaQuery<R>, root: Root<T>) {
//        val predicates = mutableListOf<Predicate>()
//
//        if (filters.isNotEmpty()) {
//            predicates.addAll(PredicateBuilder.build(cb, root, filters))
//        }
//
//        if (!searchQuery.isNullOrBlank() && searchFields.isNotEmpty()) {
//            PredicateBuilder.searchPredicates(cb, root, searchQuery!!, searchFields)?.let {
//                predicates.add(it)
//            }
//        }
//
//        if (predicates.isNotEmpty()) {
//            cq.where(*predicates.toTypedArray())
//        }
//    }
//}