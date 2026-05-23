package interceptors

import model.PulseContext
import model.QueryEvent

//interface QueryPulseInterceptor {
//    /**
//     * Intercepts a database operation.
//     * @param context metadata like tenantId, traceId, or calling method
//     * @param event the actual query details (SQL/Command, parameters, timing)
//     */
//    fun onQuery(context: PulseContext, event: QueryEvent)
//}

interface QueryPulseInterceptor {
    /**
     * Called after every database query completes (success or failure).
     *
     * @param context  Request-scoped metadata: traceId, actor, tenant, etc.
     * @param event    The full query record including SQL, timing, and outcome.
     */
    fun onQuery(context: PulseContext, event: QueryEvent)

    /**
     * Called when a batch of statements completes via executeBatch().
     * Default implementation delegates each statement to [onQuery].
     */
    fun onBatch(context: PulseContext, events: List<QueryEvent>) {
        events.forEach { onQuery(context, it) }
    }

    /**
     * Called when an N+1 pattern is detected within the current request.
     * Override to add alerting / sampling logic.
     *
     * @param context      The current request context.
     * @param pattern      The repeated query pattern that triggered detection.
     * @param occurrences  How many times this pattern fired in the request.
     */
    fun onNPlusOneDetected(context: PulseContext, pattern: String, occurrences: Int) {}

    /**
     * Called when a query exceeds the configured slow-query threshold.
     * Override to add alerting. By default, [onQuery] is still called too.
     */
    fun onSlowQuery(context: PulseContext, event: QueryEvent) {}
}