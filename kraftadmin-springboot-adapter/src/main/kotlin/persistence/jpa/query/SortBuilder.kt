package persistence.jpa.query

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Root

/**
 * Utility object for constructing type-safe ORDER BY clauses in JPA Criteria queries.
 *
 * This implementation uses a 'CASE' expression to force NULL values to the end of
 * the result set, ensuring consistent behavior across different SQL dialects
 * (PostgreSQL, MySQL, H2) regardless of their default NULL sorting rules.
 */
object SortBuilder {

    enum class Direction { ASC, DESC }

    /**
     * Defines a sorting specification for a resource field.
     * * @property field The name of the entity property to sort by.
     * @property direction The sort order (ASC or DESC). Defaults to DESC.
     */
    data class SortSpec(
        val field: String,
        val direction: Direction = Direction.DESC
    )

    /**
     * Applies an ordering strategy to the provided CriteriaQuery.
     * * The sorting logic works in two stages:
     * 1. A synthetic priority column (CASE) is generated to place NULLs last.
     * 2. The primary value-based sort is applied.
     *
     * @param cb The CriteriaBuilder used to construct query elements.
     * @param query The CriteriaQuery to which the order will be applied.
     * @param root The query root (source entity).
     * @param spec The sorting instructions; does nothing if null.
     * @param allowedFields A whitelist of field names allowed for sorting to prevent SQL injection.
     */
    fun <T> apply(
        cb: CriteriaBuilder,
        query: CriteriaQuery<T>,
        root: Root<T>,
        spec: SortSpec?,
        allowedFields: List<String>
    ) {
        if (spec == null || spec.field !in allowedFields) return

        val path = root.get<Any>(spec.field)
        val javaType = path.javaType

        // Build the null/empty condition dynamically
        val isNullOrEmpty = if (String::class.java.isAssignableFrom(javaType)) {
            // For Strings: check for null OR empty string
            cb.or(cb.isNull(path), cb.equal(path, ""))
        } else {
            // For DateTimes, Numbers, etc.: only check for null
            cb.isNull(path)
        }

        val nullsLast = cb.selectCase<Int>()
            .`when`(isNullOrEmpty, 1)
            .otherwise(0)

        val valueOrder = when (spec.direction) {
            Direction.ASC -> cb.asc(path)
            Direction.DESC -> cb.desc(path)
        }

        query.orderBy(cb.asc(nullsLast), valueOrder)
    }

    /**
     * Parses raw request parameters into a type-safe SortSpec.
     * * @param field The field name provided by the API request.
     * @param direction The direction string (e.g., "ASC", "DESC").
     * @return A valid SortSpec, or null if the field is empty.
     */
    fun from(field: String?, direction: String?): SortSpec? {
        field ?: return null

        val dir = when (direction?.uppercase()) {
            "ASC" -> Direction.ASC
            else -> Direction.DESC
        }

        return SortSpec(field, dir)
    }
}