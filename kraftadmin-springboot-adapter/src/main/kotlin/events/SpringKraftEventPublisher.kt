package events

import com.kraftadmin.events.KraftAdminEvent
import com.kraftadmin.events.KraftEventPublisher
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

/**
 * Spring implementation of the Kraft event publisher.
 *
 * The publisher is responsible only for delivering events to registered
 * listeners. It does not decide which events should be synchronous or
 * asynchronous—that is defined by each listener.
 */
@Component
open class SpringKraftEventPublisher(
    private val registry: SpringListenerRegistry
) : KraftEventPublisher {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun publish(event: KraftAdminEvent) {
        logger.debug("Publishing {}", event::class.simpleName)

        registry.getListeners(event::class.java)
            .filter { it.supports(event) }
            .forEach { listener ->
                if (listener.async) {
                    invokeAsync(listener, event)
                } else {
                    listener.execute(event)
                }
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
                "Async listener {}.{}() failed: {}",
                listener.bean::class.simpleName,
                listener.method.name,
                ex.message,
                ex
            )
        }
    }
}