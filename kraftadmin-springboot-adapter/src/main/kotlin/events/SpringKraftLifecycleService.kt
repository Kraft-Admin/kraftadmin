package events

import com.kraftadmin.events.KraftAdminEvent
import com.kraftadmin.events.KraftEventPublisher
import com.kraftadmin.events.KraftLifecycleService

class SpringKraftLifecycleService(
    private val publisher: KraftEventPublisher
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
    override fun onCreateFailed(event: KraftAdminEvent.CreateFailed) = dispatch(event)
    override fun onDeleteFailed(deleteFailed: KraftAdminEvent.DeleteFailed) {
        dispatch(deleteFailed)
    }

    override fun onAfterFetchById(afterFetchById: KraftAdminEvent.AfterFetchById) {
        dispatch(afterFetchById)
    }

    override fun onBeforeFetchById(beforeFetchById: KraftAdminEvent.BeforeFetchById) {
        dispatch(beforeFetchById)
    }

    override fun onFetchByIdFailed(fetchByIdFailed: KraftAdminEvent.FetchByIdFailed) {
        dispatch(fetchByIdFailed)
    }

    override fun onBeforeFetchAll(beforeFetchAll: KraftAdminEvent.BeforeFetchAll) {
        dispatch(beforeFetchAll)
    }

    override fun onAfterFetchAll(afterFetchAll: KraftAdminEvent.AfterFetchAll) {
        dispatch(afterFetchAll)
    }

    override fun onFetchAllFailed(fetchAllFailed: KraftAdminEvent.FetchAllFailed) {
       dispatch(fetchAllFailed)
    }


}