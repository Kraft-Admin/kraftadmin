package com.kraftadmin.events


/**
 * Framework-agnostic listener registry.
 * Holds discovered listener entries indexed by event type.
 * Each adapter populates this at startup in whatever way fits its DI model.
 */
interface KraftListenerRegistry {
    fun register(entry: ListenerEntry)

    fun getListeners(
        eventType: Class<out KraftAdminEvent>
    ): List<ListenerEntry>
}
