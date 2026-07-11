package com.kraftadmin.spi

import com.kraftadmin.enums.ProviderType
import org.slf4j.LoggerFactory
import kotlin.collections.emptySet

//class EntityDiscoveryService1(
//    private val discoverers: List<EntityDiscoverer>
//) {
//    private val logger = LoggerFactory.getLogger(javaClass)
//
////    fun discoverAll(): Set<Class<*>> {
////        val discoveredEntities = mutableSetOf<Class<*>>()
////        discoverers.forEach { discoverer ->
////            try {
////                val discovered = discoverer.discover()
////                discoveredEntities.addAll(discovered)
////            } catch (e: Exception) {
////                logger.error("Error in discoverer ${discoverer.provider}", e)
////            }
////        }
////        return discoveredEntities
////    }
//
////    override fun discover(): Set<Class<*>> {
////        val allEntities = mutableSetOf<Class<*>>()
////        discoverers.forEach { discoverer ->
////            try {
////                val discovered = discoverer.discover()
////                allEntities.addAll(discovered)
////            } catch (e: Exception) {
////                logger.error("Error in discoverer ${discoverer.provider}", e)
////            }
////        }
////        return allEntities
////    }
////
//////    override val provider
//
//}


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