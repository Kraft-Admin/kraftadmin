package com.kraftadmin.events

interface KraftEventConsumer {

    fun supports(event: KraftAdminEvent): Boolean = true

    fun consume(event: KraftAdminEvent)
}