package events

import actions.ActionHandlerEntry
import actions.KraftActionResponse
import com.kraftadmin.context.KraftAdminContextHolder
import com.kraftadmin.events.KraftAdminEvent
import com.kraftadmin.ui_descriptors.KraftAdminDescriptorFactory
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(prefix = "kraftadmin", name = ["enabled"], havingValue = "true")
class SpringKraftCustomActionService(
    private val descriptorFactory: KraftAdminDescriptorFactory,
    private val actionRegistry: SpringActionRegistry,
    private val publisher: SpringKraftEventPublisher,
) {

    private val logger = LoggerFactory.getLogger(SpringKraftCustomActionService::class.java)

    fun execute(resourceName: String, id: String, actionName: String, input: Any?): KraftActionResponse? {
        val handler = actionRegistry.getAction(actionName)
            ?: throw IllegalArgumentException("Action '$actionName' not found.")

        val convertedInput = handler.bindInput(input)

        // 1. Fetch the real Entity using the repository associated with this resource
        val dataProvider = descriptorFactory.getDataProviderForResource(resourceName)
            ?: throw IllegalArgumentException("No data provider for $resourceName")

        val entity = dataProvider.findById(id) // This returns the actual entity
            ?: throw IllegalArgumentException("Entity $id not found")

        logger.info("entity: $entity")

        val context = KraftAdminContextHolder.actionContext(
            resourceName,
            entity,
            id,
            convertedInput
        )


        logger.info("context $context")

        publisher.publish(
            KraftAdminEvent.BeforeAction(
                resourceName = resourceName,
                entity = entity,
                actionName = actionName,
                input = input,
                context = context.requestContext
            )
        )

        val response = handler.execute(context)

        publisher.publish(
            KraftAdminEvent.AfterAction(
                resourceName = resourceName,
                entity = entity,
                actionName = actionName,
                input = input,
                context = context.requestContext
            )
        )

        return response
    }


    private fun loadEntity(
        resource: String,
        id: String
    ): Any {

        return descriptorFactory
            .getResourceDetailsData(resource, id)
            ?: throw IllegalArgumentException(
                "Resource '$resource' with id '$id' not found."
            )
    }

    private fun findAction(
        resource: String,
        actionName: String
    ): ActionHandlerEntry {

        val entityClass =
            descriptorFactory.getEntityClassForResource(resource)
                ?: throw IllegalArgumentException(
                    "Unknown resource '$resource'"
                )

        return actionRegistry.getAction(actionName)
            ?: throw IllegalArgumentException(
                "Action '$actionName' is not registered for ${entityClass.simpleName}"
            )
    }


}