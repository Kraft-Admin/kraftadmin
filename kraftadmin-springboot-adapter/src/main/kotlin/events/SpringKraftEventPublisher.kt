package events

import com.kraftadmin.events.AsynchronousEvent
import com.kraftadmin.events.KraftAdminEvent
import com.kraftadmin.events.KraftEventConsumer
import com.kraftadmin.events.KraftEventPublisher
import com.kraftadmin.events.SynchronousEvent
import org.slf4j.LoggerFactory
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
    prefix = "kraftpulse",
    name = ["enabled"],
    havingValue = "true"
)
open class SpringKraftEventPublisher(
    private val registry: SpringListenerRegistry,
    private val consumers: List<KraftEventConsumer> = emptyList()
) : KraftEventPublisher {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun publish(event: KraftAdminEvent) {

        logger.debug("Publishing {}", event::class.simpleName)

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
        consumers.forEach { consumer ->

            when (event) {
                is SynchronousEvent ->
                    consumer.consume(event)

                is AsynchronousEvent ->
                    invokeConsumerAsync(
                        consumer,
//                        event
                    )

                else -> {}
            }
        }


    }

    private fun invokeConsumerAsync(
        consumer: KraftEventConsumer,
//        event: AsynchronousEvent & KraftAdminEvent
    ) {
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
                "Async listener {}.{}() failed",
                listener.bean::class.simpleName,
                listener.method.name,
                ex
            )
        }
    }
}