package events

import annotations.KraftAdminOn
import com.kraftadmin.events.KraftAdminEvent
import com.kraftadmin.logging.KraftAdminLogging
import jakarta.annotation.PostConstruct
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component


/**
 * Spring adapter for KraftListenerRegistry.
 * Scans all Spring beans for @KraftAdminOn methods at startup.
 * Results are cached in InMemoryListenerRegistry — zero reflection at dispatch time.
 */
@Component
@ConditionalOnProperty(prefix = "kraftadmin", name = ["enabled"], havingValue = "true")
class SpringListenerRegistry(
    private val applicationContext: ApplicationContext
) : KraftListenerRegistry {

    private val logger = KraftAdminLogging.logger(javaClass)


    private val listeners =
        mutableMapOf<Class<out KraftAdminEvent>, MutableList<ListenerEntry>>()

    @PostConstruct
    fun discover() {
        var discovered = 0

        // ✅ Scan @KraftAdminEventListener marked classes first for cleaner logging
        val allBeans = applicationContext.beanDefinitionNames.mapNotNull { beanName ->
            runCatching { applicationContext.getBean(beanName) }.getOrNull()
        }

        val (marked, unmarked) = allBeans.partition { bean ->
            bean::class.java.isAnnotationPresent(KraftAdminOn::class.java)
        }

        (marked + unmarked).forEach { bean ->
            // Skip KraftPulse internals
            val pkg = bean::class.java.packageName
            if (pkg.startsWith("events.") ||
                pkg.startsWith("config.events") ||
                pkg.startsWith("actions.")
            ) return@forEach

            bean::class.java.declaredMethods.forEach { method ->
                val annotation = method.getAnnotation(KraftAdminOn::class.java)
                    ?: return@forEach

                if (annotation.events.isEmpty()) {
                    logger.warn(
                        "@KraftAdminOn on {}.{}() has no event types — skipping",
                        bean::class.simpleName, method.name
                    )
                    return@forEach
                }

                // ✅ Validate method signature
                if (method.parameterCount != 1 ||
                    !KraftAdminEvent::class.java.isAssignableFrom(method.parameterTypes[0])
                ) {
                    logger.warn(
                        "@KraftAdminOn on {}.{}() must accept exactly one KraftAdminEvent parameter — skipping",
                        bean::class.simpleName, method.name
                    )
                    return@forEach
                }

                method.isAccessible = true

                val entry = ListenerEntry(

                    bean = bean,

                    method = method,

                    entityClass =
                        if (annotation.entityClass == Any::class)
                            null
                        else
                            annotation.entityClass,

                    resource = annotation.resource,

                    eventTypes = annotation.events
                        .map { it.java as Class<out KraftAdminEvent> }
                        .toSet(),

                    order = annotation.order
                )

                register(entry)
                discovered++

                logger.info(
                    "Registered {}.{}() → events={} resource='{}' entityClass={} order={}",
                    bean::class.simpleName,
                    method.name,
                    entry.eventTypes.map { it.simpleName },
                    entry.resource.ifBlank { "*" },
                    entry.entityClass?.simpleName ?: "*",
                    entry.order
                )


            }
        }

        logger.info("KraftAdmin: {} listener(s) discovered", discovered)
    }

    override fun register(entry: ListenerEntry) {

        entry.eventTypes.forEach { type ->

            logger.info(
                "Registering {} for {}",
                entry.method.name,
                type.simpleName
            )

            listeners
                .computeIfAbsent(type) {
                    mutableListOf()
                }
                .add(entry)

            logger.info(
                "{} now has {} listeners",
                type.simpleName,
                listeners[type]!!.size
            )
        }
    }


    override fun getListeners(
        eventType: Class<out KraftAdminEvent>
    ): List<ListenerEntry> {

        logger.info(
            "Current keys = {}",
            listeners.keys.map { it.simpleName }
        )

        val list = listeners[eventType] ?: emptyList()

        logger.info(
            "Lookup {} -> {}",
            eventType.simpleName,
            list.size
        )

        return list
    }


}