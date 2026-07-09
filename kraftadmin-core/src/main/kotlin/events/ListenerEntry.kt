package events

import com.kraftadmin.events.KraftAdminEvent
import org.slf4j.LoggerFactory
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import kotlin.reflect.KClass

/**
 * Discovered listener method entry.
 * Populated by the adapter's scanner — Spring/Ktor/Quarkus all produce this same shape.
 */
//data class ListenerEntry(
//
//    val bean: Any,
//
//    val method: Method,
//
//    val entityClass: KClass<*>?,
//
//    val eventTypes: Set<Class<out KraftAdminEvent>>,
//
//    val order: Int,
//
//    val async: Boolean
//
//) {
//
//    private val logger =
//        LoggerFactory.getLogger(ListenerEntry::class.java)
//
//    fun supports(event: KraftAdminEvent): Boolean {
//
//        if (!eventTypes.contains(event::class.java))
//            return false
//
//        if (entityClass == null)
//            return true
//
//        val entity = event.entity ?: return false
//
//        return entityClass == entity::class
//
//    }
//
//    fun execute(event: KraftAdminEvent) {
//
//        logger.info(
//            "Executing {}.{} for {}",
//            bean::class.simpleName,
//            method.name,
//            event::class.simpleName
//        )
//
//        try {
//
//            method.invoke(bean, event)
//
//            logger.info(
//                "Completed {}.{}",
//                bean::class.simpleName,
//                method.name
//            )
//
//        }
//
//        catch (e: InvocationTargetException) {
//
//            val cause = e.targetException ?: e
//
//            logger.error(
//                "Listener failed: {}",
//                cause.message,
//                cause
//            )
//
//            throw cause
//
//        }
//
//        catch (e: Exception) {
//
//            logger.error(
//                "Listener failed",
//                e
//            )
//
//            throw e
//
//        }
//
//    }
//
//}



data class ListenerEntry(
    val bean: Any,
    val method: Method,
    val entityClass: KClass<*>?,   // null = no entity filter
    val resource: String,           // blank = no resource name filter
    val eventTypes: Set<Class<out KraftAdminEvent>>,
    val order: Int,
    val async: Boolean
) {
    private val logger = LoggerFactory.getLogger(ListenerEntry::class.java)

    /**
     * Returns true if this listener should fire for the given event.
     * Both filters must pass if set — they are AND conditions.
     */
    fun supports(event: KraftAdminEvent): Boolean {
        // Must match event type
        if (event::class.java !in eventTypes) return false

        // Resource name filter — blank means all
        if (resource.isNotBlank() &&
            !resource.equals(event.resourceName, ignoreCase = true)
        ) return false

        // Entity class filter — Any::class / null means all
        if (entityClass != null && entityClass != Any::class) {
            val entity = event.entity ?: return false
            if (!entityClass.java.isInstance(entity)) return false
        }

        return true
    }

    fun execute(event: KraftAdminEvent) {
        logger.debug(
            "Executing {}.{}() for {}",
            bean::class.simpleName, method.name, event::class.simpleName
        )
        try {
            method.invoke(bean, event)
            logger.debug("Completed {}.{}()", bean::class.simpleName, method.name)
        } catch (e: InvocationTargetException) {
            val cause = e.targetException ?: e
            logger.error(
                "Listener {}.{}() failed: {}",
                bean::class.simpleName, method.name, cause.message, cause
            )
            throw cause // re-throw so BEFORE_ events can veto
        } catch (e: Exception) {
            logger.error(
                "Listener {}.{}() failed: {}",
                bean::class.simpleName, method.name, e.message, e
            )
            throw e
        }
    }
}