package events

import com.kraftadmin.events.KraftAdminEvent
import com.kraftadmin.events.KraftLifecycleService

class SpringKraftLifecycleService(
    private val publisher: SpringKraftEventPublisher
) : KraftLifecycleService {

    // Simple delegation helper
    private fun dispatch(event: KraftAdminEvent) {
        publisher.publish(event)
    }

    override fun onBeforeCreate(event: KraftAdminEvent.BeforeCreate) = dispatch(event)
    override fun onAfterCreate(event: KraftAdminEvent.AfterCreate) = dispatch(event)
    override fun onBeforeUpdate(event: KraftAdminEvent.BeforeUpdate) = dispatch(event)
    override fun onAfterUpdate(event: KraftAdminEvent.AfterUpdate) = dispatch(event)
    override fun onBeforeDelete(event: KraftAdminEvent.BeforeDelete) = dispatch(event)
    override fun onAfterDelete(event: KraftAdminEvent.AfterDelete) = dispatch(event)
    override fun onBeforeAction(event: KraftAdminEvent.BeforeAction) = dispatch(event)
    override fun onAfterAction(event: KraftAdminEvent.AfterAction) = dispatch(event)
    override fun onActionFailed(event: KraftAdminEvent.ActionFailed) = dispatch(event)
    override fun onBeforeBulkDelete(event: KraftAdminEvent.BeforeBulkDelete) = dispatch(event)
    override fun onAfterBulkDelete(event: KraftAdminEvent.AfterBulkDelete) = dispatch(event)
    override fun onBeforeBulkInsert(event: KraftAdminEvent.BeforeBulkInsert) = dispatch(event)
    override fun onAfterBulkInsert(event: KraftAdminEvent.AfterBulkInsert) = dispatch(event)
    override fun onBeforeExport(event: KraftAdminEvent.BeforeExport) = dispatch(event)
    override fun onAfterPrint(event: KraftAdminEvent.AfterPrint) = dispatch(event)
}