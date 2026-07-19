package com.kraftadmin.events

interface KraftLifecycleService {
    // CRUD Lifecycle
    fun onBeforeCreate(event: KraftAdminEvent.BeforeCreate)
    fun onAfterCreate(event: KraftAdminEvent.AfterCreate)

    fun onBeforeUpdate(event: KraftAdminEvent.BeforeUpdate)
    fun onAfterUpdate(event: KraftAdminEvent.AfterUpdate)

    fun onBeforeDelete(event: KraftAdminEvent.BeforeDelete)
    fun onAfterDelete(event: KraftAdminEvent.AfterDelete)

    // Actions
    fun onBeforeAction(event: KraftAdminEvent.BeforeAction)
    fun onAfterAction(event: KraftAdminEvent.AfterAction)
    fun onActionFailed(event: KraftAdminEvent.ActionFailed)

    // Bulk Operations
    fun onBeforeBulkDelete(event: KraftAdminEvent.BeforeBulkDelete)
    fun onAfterBulkDelete(event: KraftAdminEvent.AfterBulkDelete)

    // Bulk
    fun onBeforeBulkInsert(event: KraftAdminEvent.BeforeBulkInsert)
    fun onAfterBulkInsert(event: KraftAdminEvent.AfterBulkInsert)

    // Export/Print
    fun onBeforeExport(event: KraftAdminEvent.BeforeExport)
    fun onAfterPrint(event: KraftAdminEvent.AfterPrint)
    fun onCreateFailed(event: KraftAdminEvent.CreateFailed) {}
    fun onDeleteFailed(deleteFailed: com.kraftadmin.events.KraftAdminEvent.DeleteFailed)
    fun onAfterFetchById(afterFetchById: KraftAdminEvent.AfterFetchById)
    fun onBeforeFetchById(beforeFetchById: KraftAdminEvent.BeforeFetchById)
    fun onFetchByIdFailed(fetchByIdFailed: KraftAdminEvent.FetchByIdFailed)
    fun onBeforeFetchAll(beforeFetchAll: com.kraftadmin.events.KraftAdminEvent.BeforeFetchAll)
    fun onAfterFetchAll(afterFetchAll: com.kraftadmin.events.KraftAdminEvent.AfterFetchAll)
    fun onFetchAllFailed(fetchAllFailed: com.kraftadmin.events.KraftAdminEvent.FetchAllFailed)
}