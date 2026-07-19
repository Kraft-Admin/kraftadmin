package events

import actions.ActionHandlerEntry
import com.kraftadmin.annotations.KraftAdminCustomAction
import com.kraftadmin.ui_descriptors.KraftActionDescriptor
import com.kraftadmin.ui_descriptors.KraftActionInputDescriptor
import com.kraftadmin.ui_descriptors.KraftFieldDescriptorFactory
import com.kraftadmin.logging.KraftAdminLogging
import com.kraftadmin.utils.toDescriptor
import jakarta.annotation.PostConstruct
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
@ConditionalOnProperty(prefix = "kraftadmin", name = ["enabled"], havingValue = "true")
class SpringActionRegistry(
    private val applicationContext: ApplicationContext
) {
    private val logger = KraftAdminLogging.logger(javaClass)


    // This map stores the association between an action provider and its executable method
    private val actionHandlers = mutableMapOf<String, ActionHandlerEntry>()

    private val actionsByName = mutableMapOf<String, ActionHandlerEntry>()

    private val kraftFieldDescriptorFactory = KraftFieldDescriptorFactory()

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

//                val descriptor = annotation.toDescriptor()

                val descriptor = annotation.toDescriptor().copy(
                    input = annotation.input
                        .takeIf { it != Nothing::class }
                        ?.let { dto ->

                            KraftActionInputDescriptor(
                                title = annotation.inputTitle.ifBlank { annotation.label },
                                description = annotation.inputDescription.ifBlank { null },
                                submitLabel = annotation.submitLabel,
                                cancelLabel = annotation.cancelLabel,
                                className = dto.qualifiedName!!,
                                fields = kraftFieldDescriptorFactory.create(dto)
                            )
                        }
                )

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

    fun getAllActions() = actionHandlers.values.map { it.descriptor }

    fun getResourceActions(entityClass: KClass<*>): List<KraftActionDescriptor> =
        actionsByEntity[entityClass]
            ?.map { it.descriptor }
            ?.sortedBy { it.order }
            ?: emptyList()

    fun getResourceActionEntries(entityClass: KClass<*>): List<ActionHandlerEntry> =
        actionsByEntity[entityClass] ?: emptyList()

}
