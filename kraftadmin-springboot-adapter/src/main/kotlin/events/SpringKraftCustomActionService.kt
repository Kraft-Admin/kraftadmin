package events

import actions.ActionHandlerEntry
import com.kraftadmin.context.KraftActionContext
import actions.KraftActionResponse
import com.kraftadmin.context.KraftAdminContextHolder
import com.kraftadmin.events.KraftAdminEvent
import com.kraftadmin.ui_descriptors.KraftAdminDescriptorFactory
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SpringKraftCustomActionService(
    private val descriptorFactory: KraftAdminDescriptorFactory,
    private val actionRegistry: SpringActionRegistry,
    private val publisher: SpringKraftEventPublisher
) {

    private val logger = LoggerFactory.getLogger(SpringKraftCustomActionService::class.java)

    fun execute(resourceName: String, id: String, actionName: String, params: Map<String, Any?>): KraftActionResponse? {
        val handler = actionRegistry.getAction(actionName)
            ?: throw IllegalArgumentException("Action '$actionName' not found.")

        // 1. Fetch the real Entity using the repository associated with this resource
        val dataProvider = descriptorFactory.getDataProviderForResource(resourceName)
            ?: throw IllegalArgumentException("No data provider for $resourceName")

        val entity = dataProvider.findById(id) // This returns the actual Order entity
            ?: throw IllegalArgumentException("Entity $id not found")

        logger.info("entity: $entity")

        val context = KraftAdminContextHolder.actionContext(
            resourceName,
            entity,
            id,
            params
        )


        logger.info("context $context")

        publisher.publish(
            KraftAdminEvent.BeforeAction(
                resourceName = resourceName,
                entity = entity,
                actionName = actionName,
                params = params,
                context = context.requestContext
            )
        )

        val response = handler.execute(context)

        publisher.publish(
            KraftAdminEvent.AfterAction(
                resourceName = resourceName,
                entity = entity,
                actionName = actionName,
                params = params,
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