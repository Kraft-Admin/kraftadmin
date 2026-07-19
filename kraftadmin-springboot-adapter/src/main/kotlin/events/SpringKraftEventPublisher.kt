package events

import com.kraftadmin.events.AsynchronousEvent
import com.kraftadmin.events.KraftAdminEvent
import com.kraftadmin.events.KraftEventConsumer
import com.kraftadmin.events.KraftEventPublisher
import com.kraftadmin.events.SynchronousEvent
import com.kraftadmin.logging.KraftAdminLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

/**
 * Spring implementation of the Kraft event publisher.
 *
 * Event execution semantics are determined by the event type:
 *
 *  • SynchronousEvent
 *      Executed immediately. Any exception propagates back to the caller
 *      and may veto the current operation.
 *
 *  • AsynchronousEvent
 *      Executed in the background. Exceptions are logged only.
 */
@Component
@ConditionalOnProperty(
    prefix = "kraftadmin",
    name = ["enabled"],
    havingValue = "true"
)
open class SpringKraftEventPublisher(
    private val registry: SpringListenerRegistry,
    private val consumers: List<KraftEventConsumer>
) : KraftEventPublisher {

    private val logger = KraftAdminLogging.logger(javaClass)


    init {
        logger.info("KraftAdmin registered ${consumers.size} consumers: ${consumers.map { it::class.simpleName }}")
    }

    override fun publish(event: KraftAdminEvent) {

        logger.info("Publishing {}", event::class.simpleName)

        logger.info("Publishing {}", event::class.simpleName)

        registry
            .getListeners(event::class.java)
            .filter { it.supports(event) }
            .forEach { listener ->

                when (event) {

                    is SynchronousEvent ->
                        listener.execute(event)

                    is AsynchronousEvent ->
                        invokeAsync(listener, event)

                    else ->
                        error("Unknown event type: ${event::class.qualifiedName}")
                }
            }

        // Framework consumers
        consumers
            .filter { it.supports(event) }
            .forEach { consumer ->
                when (event) {
                    is SynchronousEvent -> consumer.consume(event)
                    is AsynchronousEvent -> invokeConsumerAsync(consumer, event)
                    else -> {}
                }
            }


    }
//
//    private fun invokeConsumerAsync(
//        consumer: KraftEventConsumer,
//        event: KraftAdminEvent
//    ) {
//    }

    private fun invokeConsumerAsync(
        consumer: KraftEventConsumer,
        event: KraftAdminEvent // Pass the event here
    ) {
        // Re-use your existing @Async infrastructure
        invokeConsumerInternal(consumer, event)
    }

    @Async("kraftEventExecutor")
    protected fun invokeConsumerInternal(
        consumer: KraftEventConsumer,
        event: KraftAdminEvent
    ) {
        runCatching {
            consumer.consume(event)
        }.onFailure { ex ->
            logger.error("Async consumer ${consumer::class.simpleName} failed", ex)
        }
    }

    @Async("kraftEventExecutor")
    protected fun invokeAsync(
        listener: ListenerEntry,
        event: KraftAdminEvent
    ) {

        runCatching {

            listener.execute(event)

        }.onFailure { ex ->

            logger.error(
                "Async listener failed",
                ex
            )
        }
    }
}
