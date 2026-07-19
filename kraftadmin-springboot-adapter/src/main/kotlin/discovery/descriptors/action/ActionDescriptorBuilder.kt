package discovery.descriptors.action

import com.kraftadmin.ui_descriptors.KraftActionDescriptor
import events.SpringActionRegistry
import kotlin.reflect.KClass

class ActionDescriptorBuilder(
    private val actionRegistry: SpringActionRegistry
) {

    fun build(resourceClass: KClass<*>): List<KraftActionDescriptor> {
        return actionRegistry.getResourceActions(resourceClass)
    }

}