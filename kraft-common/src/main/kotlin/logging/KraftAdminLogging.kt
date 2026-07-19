package com.kraftadmin.logging

import org.slf4j.LoggerFactory

object KraftAdminLogging {

    @Volatile
    var enabled: Boolean = true

    fun logger(clazz: Class<*>): KraftAdminLogger {
        return KraftAdminLogger(
            LoggerFactory.getLogger(clazz),
            { enabled }
        )
    }

}