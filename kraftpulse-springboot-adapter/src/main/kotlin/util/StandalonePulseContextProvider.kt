package util

import interceptors.PulseContextProvider
import model.PulseContext
import model.QueryPattern
import org.springframework.stereotype.Component
import java.util.UUID
import kotlin.collections.getOrPut
import kotlin.collections.mutableMapOf


/**
 * Holds the current [PulseContext] and per-request query tracking state
 * in a ThreadLocal so it flows naturally through synchronous Spring/JPA code.
 *
 * For async/coroutine code, propagate manually or use a coroutine context element.
 */
object PulseContextHolder {
    private val currentTrace = InheritableThreadLocal<String>()
    private val contextHolder = InheritableThreadLocal<PulseContext?>()
    private val queryPatternHolder = InheritableThreadLocal<MutableMap<String, QueryPattern>>()

    fun start(id: String = UUID.randomUUID().toString()) {
        currentTrace.set(id)
    }

    fun getTraceId(): String = currentTrace.get()

    // ---------------------------------------------------------------------------
    // Context lifecycle
    // ---------------------------------------------------------------------------

    fun set(context: PulseContext) {
        contextHolder.set(context)
        queryPatternHolder.set(mutableMapOf())
    }

    fun get(): PulseContext? = contextHolder.get()

    fun getOrThrow(): PulseContext =
        contextHolder.get() ?: error("No PulseContext bound to current thread. Did you call PulseContextHolder.set()?")

    fun clear() {
        contextHolder.remove()
        queryPatternHolder.remove()
        currentTrace.remove()
    }

    // ---------------------------------------------------------------------------
    // N+1 detection helpers
    // ---------------------------------------------------------------------------

    /**
     * Records a query pattern occurrence.
     * Returns the updated count for this pattern in the current request.
     */
    fun recordQueryPattern(normalizedSql: String, entityName: String?): Int {
        val patterns = queryPatternHolder.get() ?: return 1
        val pattern = patterns.getOrPut(normalizedSql) {
            QueryPattern(normalizedSql = normalizedSql, entityName = entityName, count = 0)
        }
        pattern.count++
        return pattern.count
    }

    fun getQueryPatterns(): Map<String, QueryPattern> =
        queryPatternHolder.get()?.toMap() ?: emptyMap()
}