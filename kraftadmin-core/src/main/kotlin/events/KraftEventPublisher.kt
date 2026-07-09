package com.kraftadmin.events

interface KraftEventPublisher {
    fun publish(event: KraftAdminEvent)
}