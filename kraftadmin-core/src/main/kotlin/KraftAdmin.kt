package com.kraftadmin

import com.kraftadmin.config.KraftAdminConfig
import com.kraftadmin.logging.KraftAdminLogging

object KraftAdmin {
    private val logger = KraftAdminLogging.logger(javaClass)


    private var started = true

    fun start(config: KraftAdminConfig) {

        logger.info("Starting KraftAdmin with config: $config")

//        started = true
        logger.info("KraftAdmin started successfully")
    }

    fun isStarted(): Boolean = started

    fun stop() {
        if (!started) return
        logger.info("Stopping KraftAdmin")
        started = false
        logger.info("KraftAdmin stopped")
    }

}