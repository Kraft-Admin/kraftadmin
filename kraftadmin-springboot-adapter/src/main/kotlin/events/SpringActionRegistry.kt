package events

import actions.ActionHandlerEntry
import com.kraftadmin.annotations.KraftAdminCustomAction
import com.kraftadmin.annotations.toDescriptor
import com.kraftadmin.ui_descriptors.KraftActionDescriptor
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import kotlin.collections.forEach
import kotlin.reflect.KClass

@Component
class SpringActionRegistry(
    private val applicationContext: ApplicationContext
) {
    private val logger = LoggerFactory.getLogger(SpringActionRegistry::class.java)

    // This map stores the association between an action name and its executable method
    private val actionHandlers = mutableMapOf<String, ActionHandlerEntry>()

    private val actionsByName = mutableMapOf<String, ActionHandlerEntry>()

    private val actionsByEntity =
        mutableMapOf<KClass<*>, MutableList<ActionHandlerEntry>>()

    @PostConstruct
    fun discover() {

        applicationContext.beanDefinitionNames.forEach { beanName ->

            val bean = runCatching {
                applicationContext.getBean(beanName)
            }.getOrNull() ?: return@forEach

            bean::class.java.declaredMethods.forEach { method ->

                val annotation =
                    method.getAnnotation(KraftAdminCustomAction::class.java)
                        ?: return@forEach

                val descriptor = annotation.toDescriptor()

                val entry = ActionHandlerEntry(
                    bean = bean,
                    method = method,
                    descriptor = descriptor
                )

                actionsByName[annotation.name] = entry

                actionsByEntity
                    .computeIfAbsent(annotation.entityClass) { mutableListOf() }
                    .add(entry)
            }
        }
    }

    fun getAction(name: String): ActionHandlerEntry? =
        actionsByName[name]

//    fun getAction(name: String) = actionHandlers[name]
    fun getAllActions() = actionHandlers.values.map { it.descriptor }
//    open fun getResourceActions(resource: String) = actionHandlers.values.map { it.descriptor }.filter { it ->
//        it.entityClass == resource
//    }.toList()

    fun getResourceActions(entityClass: KClass<*>): List<KraftActionDescriptor> =
        actionsByEntity[entityClass]
            ?.map { it.descriptor }
            ?.sortedBy { it.order }
            ?: emptyList()

    fun getResourceActionEntries(entityClass: KClass<*>): List<ActionHandlerEntry> =
        actionsByEntity[entityClass] ?: emptyList()

}
