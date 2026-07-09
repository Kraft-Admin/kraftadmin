package actions

import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Framework-agnostic action handler registry.
 * Each adapter populates this at startup:
 *   Spring  → getBeansOfType(KraftActionHandler::class.java)
 *   Ktor    → manual registration in plugin DSL
 *   Quarkus → Arc CDI Instance<KraftActionHandler>
 */
class KraftActionRegistry {

    private val logger = LoggerFactory.getLogger(KraftActionRegistry::class.java)
    private val handlers = ConcurrentHashMap<String, KraftActionHandler>()

    fun register(handler: KraftActionHandler) {
        val name = handler.actionName.trim()
        if (name.isBlank()) {
            logger.warn("KraftActionHandler {} has blank actionName — skipped", handler::class.simpleName)
            return
        }
        if (handlers.containsKey(name)) {
            logger.warn(
                "Duplicate handler for '{}': {} overrides {}",
                name, handler::class.simpleName, handlers[name]!!::class.simpleName
            )
        }
        handlers[name] = handler
        logger.info("Registered action handler '{}' → {}", name, handler::class.simpleName)
    }

    fun find(actionName: String): KraftActionHandler? = handlers[actionName]

    fun all(): Map<String, KraftActionHandler> = handlers.toMap()

    fun registeredNames(): Set<String> = handlers.keys.toSet()
}