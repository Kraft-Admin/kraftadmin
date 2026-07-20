package com.kraftadmin.spi

import com.kraftadmin.enums.ProviderType
import org.slf4j.LoggerFactory
import kotlin.collections.emptySet

class EntityDiscoveryService(
    private val discoverers: List<EntityDiscoverer>
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun discoverAll(): Set<DiscoveredEntity<*>> =
        discoverers
            .flatMap(::safeDiscover)
            .toSet()

    fun discover(provider: ProviderType): Set<DiscoveredEntity<*>> =
        discoverers
            .asSequence()
            .filter { it.provider == provider }
            .flatMap { safeDiscover(it).asSequence() }
            .toSet()

    private fun safeDiscover(
        discoverer: EntityDiscoverer
    ): Set<DiscoveredEntity<*>> =
        try {
            discoverer.discover()
        } catch (ex: Exception) {
            logger.error(
                "Failed to discover entities using {}",
                discoverer.provider,
                ex
            )
            emptySet()
        }
}