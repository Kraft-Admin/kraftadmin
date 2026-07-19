package com.kraftadmin.events

interface KraftEventLogger {
    fun log(event: KraftAdminEvent)
}