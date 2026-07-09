package persistence.jpa.query

import jakarta.persistence.TypedQuery
import kotlin.math.ceil

/**
 * Applies pagination (offset + limit) to any TypedQuery.
 * Extracted so FetchAll doesn't need to know how pagination math works.
 */
object PageableBuilder {

    data class PageSpec(
        val page: Int,      // 1-based
        val size: Int,
        val maxSize: Int = 100
    ) {
        val effectivePage: Int = page.coerceAtLeast(1)
        val effectiveSize: Int = size.coerceAtLeast(1).coerceAtMost(maxSize)
        val offset: Int = (effectivePage - 1) * effectiveSize
    }

    fun <T> apply(query: TypedQuery<T>, spec: PageSpec): TypedQuery<T> {
        query.firstResult = spec.offset
        query.maxResults = spec.effectiveSize
        return query
    }

    fun totalPages(total: Long, size: Int): Int {
        if (total == 0L) return 0
        return ceil(total.toDouble() / size).toInt()
    }
}