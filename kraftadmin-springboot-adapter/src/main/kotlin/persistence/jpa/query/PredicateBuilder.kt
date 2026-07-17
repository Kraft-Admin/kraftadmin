package persistence.jpa.query

import com.kraftadmin.query.KraftFilter
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.slf4j.LoggerFactory

/**
 * Builds JPA criteria Predicates from filter descriptors.
 * All predicate construction lives here — nothing else needs
 * to know about CriteriaBuilder predicate APIs.
 */
object PredicateBuilder {

    private val logger = LoggerFactory.getLogger(PredicateBuilder::class.java)

    sealed class Filter {
        data class Equals(val field: String, val value: Any) : Filter()
        data class Like(val field: String, val value: String) : Filter()
        data class GreaterThan(val field: String, val value: Comparable<Any>) : Filter()
        data class LessThan(val field: String, val value: Comparable<Any>) : Filter()
        data class Between(
            val field: String,
            val from: Comparable<Any>,
            val to: Comparable<Any>
        ) : Filter()

        data class In(val field: String, val values: List<Any>) : Filter()
        data class IsNull(val field: String) : Filter()
        data class IsNotNull(val field: String) : Filter()
        data class Search(val fields: List<String>, val value: String) : Filter()
    }

    /**
     * Converts adapter-specific filters into framework-wide filters
     * suitable for lifecycle events.
     */
    fun toEventFilters(filters: List<Filter>): List<KraftFilter> =
        filters.map(::toEventFilter)

    private fun toEventFilter(filter: Filter): KraftFilter =
        when (filter) {
            is Filter.Equals ->
                KraftFilter.Equals(filter.field, filter.value)

            is Filter.Like ->
                KraftFilter.Like(filter.field, filter.value)

            is Filter.GreaterThan ->
                KraftFilter.GreaterThan(filter.field, filter.value)

            is Filter.LessThan ->
                KraftFilter.LessThan(filter.field, filter.value)

            is Filter.Between ->
                KraftFilter.Between(filter.field, filter.from, filter.to)

            is Filter.In ->
                KraftFilter.In(filter.field, filter.values)

            is Filter.IsNull ->
                KraftFilter.IsNull(filter.field)

            is Filter.IsNotNull ->
                KraftFilter.IsNotNull(filter.field)

            is Filter.Search ->
                KraftFilter.Search(filter.fields, filter.value)
        }

    fun <T> build(
        cb: CriteriaBuilder,
        root: Root<T>,
        filters: List<Filter>
    ): List<Predicate> {
        return filters.mapNotNull { filter ->
            try {
                buildPredicate(cb, root, filter)
            } catch (e: Exception) {
                logger.warn("Could not build predicate for filter $filter: ${e.message}")
                null
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> buildPredicate(
        cb: CriteriaBuilder,
        root: Root<T>,
        filter: Filter
    ): Predicate? {
        return when (filter) {
            is Filter.Equals ->
                cb.equal(root.get<Any>(filter.field), filter.value)

            is Filter.Like ->
                cb.like(
                    cb.lower(root.get(filter.field)),
                    "%${filter.value.lowercase()}%"
                )

            is Filter.GreaterThan ->
                cb.greaterThan(
                    root.get<Comparable<Any>>(filter.field),
                    filter.value
                )

            is Filter.LessThan ->
                cb.lessThan(
                    root.get<Comparable<Any>>(filter.field),
                    filter.value
                )

            is Filter.Between ->
                cb.between(
                    root.get<Comparable<Any>>(filter.field),
                    filter.from,
                    filter.to
                )

            is Filter.In ->
                root.get<Any>(filter.field).`in`(filter.values)

            is Filter.IsNull ->
                cb.isNull(root.get<Any>(filter.field))

            is Filter.IsNotNull ->
                cb.isNotNull(root.get<Any>(filter.field))

            is Filter.Search -> {
                if (filter.fields.isEmpty()) {
                    return null
                }

                val predicates = filter.fields.map { field ->
                    cb.like(
                        cb.lower(
                            cb.function(
                                "CAST",
                                String::class.java,
                                root.get<Any>(field)
                            )
                        ),
                        "%${filter.value.lowercase()}%"
                    )
                }

                cb.or(*predicates.toTypedArray())
            }
        }
    }

    /**
     * Converts a raw search string into LIKE predicates across the
     * configured searchable fields.
     */
    fun <T> searchPredicates(
        cb: CriteriaBuilder,
        root: Root<T>,
        searchQuery: String,
        searchableFields: List<String>
    ): Predicate? {

        if (searchQuery.isBlank() || searchableFields.isEmpty())
            return null

        val predicates = searchableFields.mapNotNull { field ->
            try {
                cb.like(
                    cb.lower(root.get<Any>(field).`as`(String::class.java)),
                    "%${searchQuery.lowercase()}%"
                )
            } catch (e: Exception) {
                logger.error(
                    "Failed to build search predicate for field '$field': ${e.message}"
                )
                null
            }
        }

        return if (predicates.isEmpty())
            null
        else
            cb.or(*predicates.toTypedArray())
    }
}