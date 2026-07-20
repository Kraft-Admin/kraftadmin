package com.kraftadmin.events

import com.kraftadmin.events.KraftAdminEvent
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import kotlin.reflect.KClass

/**
 * Represents a discovered listener method.
 *
 * ListenerEntry is framework-agnostic and contains only the metadata
 * required to determine whether a listener supports an event and to
 * invoke the underlying method.
 */
data class ListenerEntry(

    val bean: Any,

    val method: Method,

    /**
     * Optional entity filter.
     *
     * Null means every entity.
     */
    val entityClass: KClass<*>?,

    /**
     * Optional resource filter.
     *
     * Blank means every resource.
     */
    val resource: String,

    /**
     * Event types this listener subscribes to.
     */
    val eventTypes: Set<Class<out KraftAdminEvent>>,

    /**
     * Listener execution order.
     */
    val order: Int
) {

    /**
     * Returns true if this listener supports the supplied event.
     */
    fun supports(event: KraftAdminEvent): Boolean {

        if (event::class.java !in eventTypes) {
            return false
        }

        if (
            resource.isNotBlank() &&
            !resource.equals(event.resourceName, ignoreCase = true)
        ) {
            return false
        }

        if (entityClass != null && entityClass != Any::class) {

            val entity = event.entity ?: return false

            if (!entityClass.java.isInstance(entity)) {
                return false
            }
        }

        return true
    }

    /**
     * Invokes the listener.
     *
     * InvocationTargetException is unwrapped so callers receive the
     * original exception thrown by the listener.
     */
    fun execute(event: KraftAdminEvent) {

        try {

            method.invoke(bean, event)

        } catch (e: InvocationTargetException) {

            throw (e.targetException ?: e)

        }
    }
}