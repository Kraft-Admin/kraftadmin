package com.kraftadmin.logging

import org.slf4j.Logger

class KraftAdminLogger(
    private val logger: Logger,
    private val enabled: () -> Boolean
) {

    fun debug(message: String) {
        if (enabled() && logger.isDebugEnabled) {
            logger.debug(message)
        }
    }

    fun debug(message: String, vararg args: Any?) {
        if (enabled() && logger.isDebugEnabled) {
            logger.debug(message, *args)
        }
    }

    fun info(message: String) {
        if (enabled() && logger.isInfoEnabled) {
            logger.info(message)
        }
    }

    fun info(message: String, vararg args: Any?) {
        if (enabled() && logger.isInfoEnabled) {
            logger.info(message, *args)
        }
    }

    fun warn(message: String) {
        if (enabled() && logger.isWarnEnabled) {
            logger.warn(message)
        }
    }

    fun warn(message: String, vararg args: Any?) {
        if (enabled() && logger.isWarnEnabled) {
            logger.warn(message, *args)
        }
    }

    fun error(message: String, throwable: Throwable? = null) {
        if (!enabled()) return

        if (throwable != null) {
            logger.error(message, throwable)
        } else {
            logger.error(message)
        }
    }


}