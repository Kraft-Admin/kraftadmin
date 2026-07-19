//package com.kraftadmin.events
//
//import com.kraftadmin.context.KraftAdminContext
//import com.kraftadmin.query.KraftFilter
//import java.time.Instant
//
///**
// * Base type for every lifecycle event emitted by KraftAdmin.
// *
// * Events are immutable snapshots describing something that is about to happen
// * or has already happened during a resource operation.
// *
// * Every event contains:
// * - the resource provider
// * - the affected entity (when applicable)
// * - request/user context
// * - the timestamp when the event occurred
// *
// * Lifecycle semantics are defined by marker interfaces:
// *
// * - [SynchronousEvent]
// *      Executed before an operation. Listener exceptions are propagated back
// *      to the caller and may veto the operation.
// *
// * - [AsynchronousEvent]
// *      Executed after an operation completes successfully. Listener failures
// *      are logged but never affect the completed request.
// */
//sealed class KraftAdminEvent {
//
//    /**
//     * Name of the affected resource.
//     */
//    abstract val resourceName: String
//
//    /**
//     * The entity involved in the operation.
//     *
//     * May be null for events that occur before an entity exists or for
//     * bulk operations.
//     */
//    abstract val entity: Any?
//
//    /**
//     * Request metadata such as authenticated user,
//     * HTTP request information and adapter-specific context.
//     */
//    abstract val context: KraftAdminContext
//
//    /**
//     * Timestamp when the event instance was created.
//     */
//    abstract val occurredAt: Instant
//
//    open fun toMap(): MutableMap<String, Any?> {
//        return linkedMapOf(
//            "resourceName" to resourceName,
//            "occurredAt" to occurredAt,
//
//            "traceId" to context.traceId,
//            "actorUsername" to context.actorUsername,
//            "actorRoles" to context.actorRoles,
//            "tenantId" to context.tenantId,
//            "ipAddress" to context.ipAddress,
//            "userAgent" to context.userAgent
//        )
//    }
//
//    // CREATE
//
//    /**
//     * Fired before a new entity is created.
//     *
//     * Throwing an exception from a listener aborts creation.
//     */
//    data class BeforeCreate(
//        override val resourceName: String,
//        override val entity: Any,
//        val data: Map<String, Any?>,
//        override val context: KraftAdminContext,
//        override val occurredAt: Instant = Instant.now()
//    ) : KraftAdminEvent(), SynchronousEvent{
//
//    }
//
//    /**
//     * Fired after an entity has been successfully created.
//     */
//    data class AfterCreate(
//        override val resourceName: String,
//        override val entity: Any,
//        val data: Map<String, Any?>,
//        override val context: KraftAdminContext,
//        override val occurredAt: Instant = Instant.now()
//    ) : KraftAdminEvent(), AsynchronousEvent
//
//    /**
//     * Fired whenever entity creation fails.
//     *
//     * This event is published after the transaction has been rolled back.
//     * Intended for audit logging, monitoring and notifications.
//     */
//    data class CreateFailed(
//        override val resourceName: String,
//        override val entity: Any?,
//        val data: Map<String, Any?>,
//        val exception: Throwable,
//        override val context: KraftAdminContext,
//        override val occurredAt: Instant = Instant.now()
//    ) : KraftAdminEvent(), AsynchronousEvent
//
//    // UPDATE
//    /**
//     * Fired before an entity is updated.
//     *
//     * Throwing an exception prevents the update.
//     */
//    data class BeforeUpdate(
//        override val resourceName: String,
//        override val entity: Any,
//        val id: String,
//        val data: Map<String, Any?>,
//        override val context: KraftAdminContext,
//        override val occurredAt: Instant = Instant.now()
//    ) : KraftAdminEvent(), SynchronousEvent
//
//    /**
//     * Fired after an entity has been updated.
//     */
//    data class AfterUpdate(
//        override val resourceName: String,
//        override val entity: Any,
//        val id: String,
//        val data: Map<String, Any?>,
//        override val context: KraftAdminContext,
//        override val occurredAt: Instant = Instant.now()
//    ) : KraftAdminEvent(), AsynchronousEvent
//
//
//    /**
//     * Fired whenever an update operation fails.
//     */
//    data class UpdateFailed(
//        override val resourceName: String,
//        override val entity: Any?,
//        val id: String,
//        val data: Map<String, Any?>,
//        val exception: Throwable,
//        override val context: KraftAdminContext,
//        override val occurredAt: Instant = Instant.now()
//    ) : KraftAdminEvent(), AsynchronousEvent
//
////    FETCH
//    /**
//     * Fired before an entity is fetched by its identifier.
//     *
//     * Throwing an exception prevents the fetch.
//     */
//    data class BeforeFetchById(
//        override val resourceName: String,
//        val id: String,
//        override val context: KraftAdminContext,
//        override val occurredAt: Instant = Instant.now()
//    ) : KraftAdminEvent(), SynchronousEvent {
//        override val entity: Any? = null
//    }
//
//    /**
//     * Fired after an entity has been successfully fetched.
//     */
//    data class AfterFetchById(
//        override val resourceName: String,
//        override val entity: Any,
//        val id: String,
//        override val context: KraftAdminContext,
//        override val occurredAt: Instant = Instant.now()
//    ) : KraftAdminEvent(), AsynchronousEvent
//
//    /**
//     * Fired whenever fetching an entity by id fails.
//     */
//    data class FetchByIdFailed(
//        override val resourceName: String,
//        val id: String,
//        val exception: Throwable,
//        override val context: KraftAdminContext,
//        override val occurredAt: Instant = Instant.now()
//    ) : KraftAdminEvent(), AsynchronousEvent {
//        override val entity: Any? = null
//    }
//
//    /**
//     * Fired before a paginated fetch begins.
//     *
//     * Throwing an exception prevents the query.
//     */
//    data class BeforeFetchAll(
//        override val resourceName: String,
//        val page: Int,
//        val size: Int,
//        val searchQuery: String?,
//        val filters: List<KraftFilter>,
//        val sortField: String?,
//        val sortDirection: String?,
//        override val context: KraftAdminContext,
//        override val occurredAt: Instant = Instant.now()
//    ) : KraftAdminEvent(), SynchronousEvent {
//
//        override val entity: Any? = null
//
//        override fun toMap() =
//            super.toMap().apply {
//                put("page", page)
//                put("size", size)
//                put("searchQuery", searchQuery)
//                put("filters", filters)
//                put("sortField", sortField)
//                put("sortDirection", sortDirection)
//            }
//    }
//
//    /**
//     * Fired after a successful paginated fetch.
//     */
//    data class AfterFetchAll(
//        override val resourceName: String,
//        val page: Int,
//        val size: Int,
//        val total: Long,
//        val returned: Int,
//        override val context: KraftAdminContext,
//        override val occurredAt: Instant = Instant.now()
//    ) : KraftAdminEvent(), AsynchronousEvent {
//        override val entity: Any? = null
//    }
//
//    /**
//     * Fired whenever a paginated fetch fails.
//     */
//    data class FetchAllFailed(
//        override val resourceName: String,
//        val page: Int,
//        val size: Int,
//        val searchQuery: String?,
//        val exception: Throwable,
//        override val context: KraftAdminContext,
//        override val occurredAt: Instant = Instant.now()
//    ) : KraftAdminEvent(), AsynchronousEvent {
//        override val entity: Any? = null
//    }
//
//    // DELETE
//
//    /**
//     * Fired before an entity is deleted.
//     *
//     * Throwing an exception prevents deletion.
//     */
//    data class BeforeDelete(
//        override val resourceName: String,
//        override val entity: Any,
//        val id: String,
//        override val context: KraftAdminContext,
//        override val occurredAt: Instant = Instant.now()
//    ) : KraftAdminEvent(), SynchronousEvent
//
//    /**
//     * Fired after an entity has been deleted.
//     */
//    data class AfterDelete(
//        override val resourceName: String,
//        override val entity: Any,
//        val id: String,
//        override val context: KraftAdminContext,
//        override val occurredAt: Instant = Instant.now()
//    ) : KraftAdminEvent(), AsynchronousEvent
//
//    /**
//     * Fired whenever a delete operation fails.
//     */
//    data class DeleteFailed(
//        override val resourceName: String,
//        override val entity: Any?,
//        val id: String,
//        val exception: Throwable,
//        override val context: KraftAdminContext,
//        override val occurredAt: Instant = Instant.now()
//    ) : KraftAdminEvent(), AsynchronousEvent
//
//    // CUSTOM ACTIONS
//
//    /**
//     * Fired before a custom action executes.
//     *
//     * Throwing an exception cancels the action and the exception may be
//     * returned to the UI.
//     */
//    data class BeforeAction(
//        override val resourceName: String,
//        override val entity: Any?,
//        val actionName: String,
//        val input: Any?,
//        override val context: KraftAdminContext,
//        override val occurredAt: Instant = Instant.now()
//    ) : KraftAdminEvent(), SynchronousEvent
//
//    /**
//     * Fired after a custom action completes successfully.
//     */
//    data class AfterAction(
//        override val resourceName: String,
//        override val entity: Any?,
//        val actionName: String,
//        val input: Any?,
//        override val context: KraftAdminContext,
//        override val occurredAt: Instant = Instant.now()
//    ) : KraftAdminEvent(), AsynchronousEvent
//
//    /**
//     * Fired whenever a custom action fails.
//     *
//     * Intended for audit logging, monitoring and notifications.
//     */
//    data class ActionFailed(
//        override val resourceName: String,
//        override val entity: Any?,
//        val actionName: String,
//        val input: Any?,
//        val exception: Throwable,
//        override val context: KraftAdminContext,
//        override val occurredAt: Instant = Instant.now()
//    ) : KraftAdminEvent(), AsynchronousEvent
//
//    // BULK DELETE
//
//    /**
//     * Fired before a bulk delete operation begins.
//     */
//    data class BeforeBulkDelete(
//        override val resourceName: String,
//        val ids: List<String>,
//        override val context: KraftAdminContext,
//        override val occurredAt: Instant = Instant.now()
//    ) : KraftAdminEvent(), SynchronousEvent {
//        override val entity: Any? = null
//    }
//
//    /**
//     * Fired after a bulk delete operation completes.
//     */
//    data class AfterBulkDelete(
//        override val resourceName: String,
//        val ids: List<String>,
//        val deletedCount: Int,
//        override val context: KraftAdminContext,
//        override val occurredAt: Instant = Instant.now()
//    ) : KraftAdminEvent(), AsynchronousEvent {
//        override val entity: Any? = null
//    }
//
//    /**
//     * Fired whenever a bulk delete operation fails.
//     */
//    data class BulkDeleteFailed(
//        override val resourceName: String,
//        val ids: List<String>,
//        val exception: Throwable,
//        override val context: KraftAdminContext,
//        override val occurredAt: Instant = Instant.now()
//    ) : KraftAdminEvent(), AsynchronousEvent {
//        override val entity: Any? = null
//    }
//
//    // BULK INSERT
//
//    /**
//     * Fired before a bulk insert operation.
//     */
//    data class BeforeBulkInsert(
//        override val resourceName: String,
//        val dataList: List<Map<String, Any?>>,
//        override val context: KraftAdminContext,
//        override val occurredAt: Instant = Instant.now()
//    ) : KraftAdminEvent(), SynchronousEvent {
//        override val entity: Any? = null
//    }
//
//    /**
//     * Fired after a successful bulk insert.
//     */
//    data class AfterBulkInsert(
//        override val resourceName: String,
//        val entities: List<Any>,
//        val count: Int,
//        override val context: KraftAdminContext,
//        override val occurredAt: Instant = Instant.now()
//    ) : KraftAdminEvent(), AsynchronousEvent {
//        override val entity: Any? = null
//    }
//
//    /**
//     * Fired whenever a bulk insert operation fails.
//     */
//    data class BulkInsertFailed(
//        override val resourceName: String,
//        val dataList: List<Map<String, Any?>>,
//        val exception: Throwable,
//        override val context: KraftAdminContext,
//        override val occurredAt: Instant = Instant.now()
//    ) : KraftAdminEvent(), AsynchronousEvent {
//        override val entity: Any? = null
//    }
//
//    // EXPORT / PRINT
//
//    /**
//     * Fired before data export begins.
//     *
//     * Listeners may cancel the export.
//     */
//    data class BeforeExport(
//        override val resourceName: String,
//        val format: String,
//        val filterCriteria: Map<String, Any?>,
//        override val context: KraftAdminContext,
//        override val occurredAt: Instant = Instant.now()
//    ) : KraftAdminEvent(), SynchronousEvent {
//        override val entity: Any? = null
//    }
//
//    /**
//     * Fired after data has been successfully exported.
//     */
//    data class AfterExport(
//        override val resourceName: String,
//        val format: String,
//        val exportedRows: Long,
//        override val context: KraftAdminContext,
//        override val occurredAt: Instant = Instant.now()
//    ) : KraftAdminEvent(), AsynchronousEvent {
//        override val entity: Any? = null
//    }
//
//    /**
//     * Fired whenever an export operation fails.
//     */
//    data class ExportFailed(
//        override val resourceName: String,
//        val format: String,
//        val filterCriteria: Map<String, Any?>,
//        val exception: Throwable,
//        override val context: KraftAdminContext,
//        override val occurredAt: Instant = Instant.now()
//    ) : KraftAdminEvent(), AsynchronousEvent {
//        override val entity: Any? = null
//    }
//
//    /**
//     * Fired before a print operation.
//     */
//    data class BeforePrint(
//        override val resourceName: String,
//        val entityId: String,
//        val templateName: String,
//        override val context: KraftAdminContext,
//        override val occurredAt: Instant = Instant.now()
//    ) : KraftAdminEvent(), SynchronousEvent {
//        override val entity: Any? = null
//    }
//
//    /**
//     * Fired whenever printing fails.
//     */
//    data class PrintFailed(
//        override val resourceName: String,
//        val entityId: String,
//        val templateName: String,
//        val exception: Throwable,
//        override val context: KraftAdminContext,
//        override val occurredAt: Instant = Instant.now()
//    ) : KraftAdminEvent(), AsynchronousEvent {
//        override val entity: Any? = null
//    }
//
//    /**
//     * Fired after a print operation completes.
//     */
//    data class AfterPrint(
//        override val resourceName: String,
//        val entityId: String,
//        val templateName: String,
//        override val context: KraftAdminContext,
//        override val occurredAt: Instant = Instant.now()
//    ) : KraftAdminEvent(), AsynchronousEvent {
//        override val entity: Any? = null
//    }
//}
//
///**
// * Marker interface for events that execute synchronously.
// *
// * Listener exceptions are propagated to the caller and may cancel
// * the operation currently being performed.
// */
//interface SynchronousEvent
//
///**
// * Marker interface for events that execute asynchronously.
// *
// * Listener failures are logged but never propagated back to the caller.
// */
//interface AsynchronousEvent

package com.kraftadmin.events

import com.kraftadmin.context.KraftAdminContext
import com.kraftadmin.query.KraftFilter
import java.time.Instant

/**
 * Base type for every lifecycle event emitted by KraftAdmin.
 *
 * Events are immutable snapshots describing something that is about to happen
 * or has already happened during a resource operation.
 */
sealed class KraftAdminEvent {

    abstract val resourceName: String
    abstract val entity: Any?
    abstract val context: KraftAdminContext
    abstract val occurredAt: Instant

    /**
     * Converts the event into a flat map structure suitable for JSONL logging.
     * Overridden in child classes to include event-specific payloads.
     */
    open fun toMap(): MutableMap<String, Any?> {
        return linkedMapOf(
            "timestamp" to Instant.now().toString(),
            "type" to this::class.simpleName,
            "resourceName" to resourceName,
            "occurredAt" to occurredAt.toString(),
            "traceId" to context.traceId,
            "actorUsername" to context.actorUsername,
            "actorRoles" to context.actorRoles,
            "tenantId" to context.tenantId,
            "ipAddress" to context.ipAddress,
            "userAgent" to context.userAgent
        )
    }

    // CREATE
    data class BeforeCreate(
        override val resourceName: String,
        override val entity: Any,
        val data: Map<String, Any?>,
        override val context: KraftAdminContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent(), SynchronousEvent {
        override fun toMap() = super.toMap().apply { put("dataKeys", data.keys) }
    }

    data class AfterCreate(
        override val resourceName: String,
        override val entity: Any,
        val data: Map<String, Any?>,
        override val context: KraftAdminContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent(), AsynchronousEvent {
        override fun toMap() = super.toMap().apply { put("dataKeys", data.keys) }
    }

    data class CreateFailed(
        override val resourceName: String,
        override val entity: Any?,
        val data: Map<String, Any?>,
        val exception: Throwable,
        override val context: KraftAdminContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent(), AsynchronousEvent {
        override fun toMap() = super.toMap().apply { put("dataKeys", data.keys); put("error", exception.message) }
    }

    // UPDATE
    data class BeforeUpdate(
        override val resourceName: String,
        override val entity: Any,
        val id: String,
        val data: Map<String, Any?>,
        override val context: KraftAdminContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent(), SynchronousEvent {
        override fun toMap() = super.toMap().apply { put("id", id); put("dataKeys", data.keys) }
    }

    data class AfterUpdate(
        override val resourceName: String,
        override val entity: Any,
        val id: String,
        val data: Map<String, Any?>,
        override val context: KraftAdminContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent(), AsynchronousEvent {
        override fun toMap() = super.toMap().apply { put("id", id); put("dataKeys", data.keys) }
    }

    data class UpdateFailed(
        override val resourceName: String,
        override val entity: Any?,
        val id: String,
        val data: Map<String, Any?>,
        val exception: Throwable,
        override val context: KraftAdminContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent(), AsynchronousEvent {
        override fun toMap() = super.toMap().apply { put("id", id); put("error", exception.message) }
    }

    // FETCH
    data class BeforeFetchById(
        override val resourceName: String,
        val id: String,
        override val context: KraftAdminContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent(), SynchronousEvent {
        override val entity: Any? = null
        override fun toMap() = super.toMap().apply { put("id", id) }
    }

    data class AfterFetchById(
        override val resourceName: String,
        override val entity: Any,
        val id: String,
        override val context: KraftAdminContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent(), AsynchronousEvent {
        override fun toMap() = super.toMap().apply { put("id", id) }
    }

    data class FetchByIdFailed(
        override val resourceName: String,
        val id: String,
        val exception: Throwable,
        override val context: KraftAdminContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent(), AsynchronousEvent {
        override val entity: Any? = null
        override fun toMap() = super.toMap().apply { put("id", id); put("error", exception.message) }
    }

    data class BeforeFetchAll(
        override val resourceName: String,
        val page: Int,
        val size: Int,
        val searchQuery: String?,
        val filters: List<KraftFilter>,
        val sortField: String?,
        val sortDirection: String?,
        override val context: KraftAdminContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent(), SynchronousEvent {
        override val entity: Any? = null
        override fun toMap() = super.toMap().apply {
            put("page", page); put("size", size); put("searchQuery", searchQuery)
            put("filters", filters.map { it.toString() }); put("sortField", sortField)
            put("sortDirection", sortDirection)
        }
    }

    data class AfterFetchAll(
        override val resourceName: String,
        val page: Int,
        val size: Int,
        val total: Long,
        val returned: Int,
        override val context: KraftAdminContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent(), AsynchronousEvent {
        override val entity: Any? = null
        override fun toMap() = super.toMap().apply {
            put("page", page); put("size", size); put("total", total); put("returned", returned)
        }
    }

    data class FetchAllFailed(
        override val resourceName: String,
        val page: Int,
        val size: Int,
        val searchQuery: String?,
        val exception: Throwable,
        override val context: KraftAdminContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent(), AsynchronousEvent {
        override val entity: Any? = null
        override fun toMap() = super.toMap().apply { put("error", exception.message) }
    }

    // DELETE
    data class BeforeDelete(
        override val resourceName: String,
        override val entity: Any,
        val id: String,
        override val context: KraftAdminContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent(), SynchronousEvent {
        override fun toMap() = super.toMap().apply { put("id", id) }
    }

    data class AfterDelete(
        override val resourceName: String,
        override val entity: Any,
        val id: String,
        override val context: KraftAdminContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent(), AsynchronousEvent {
        override fun toMap() = super.toMap().apply { put("id", id) }
    }

    data class DeleteFailed(
        override val resourceName: String,
        override val entity: Any?,
        val id: String,
        val exception: Throwable,
        override val context: KraftAdminContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent(), AsynchronousEvent {
        override fun toMap() = super.toMap().apply { put("id", id); put("error", exception.message) }
    }

    // CUSTOM ACTIONS
    data class BeforeAction(
        override val resourceName: String,
        override val entity: Any?,
        val actionName: String,
        val input: Any?,
        override val context: KraftAdminContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent(), SynchronousEvent {
        override fun toMap() = super.toMap().apply { put("actionName", actionName) }
    }

    data class AfterAction(
        override val resourceName: String,
        override val entity: Any?,
        val actionName: String,
        val input: Any?,
        override val context: KraftAdminContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent(), AsynchronousEvent {
        override fun toMap() = super.toMap().apply { put("actionName", actionName) }
    }

    data class ActionFailed(
        override val resourceName: String,
        override val entity: Any?,
        val actionName: String,
        val input: Any?,
        val exception: Throwable,
        override val context: KraftAdminContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent(), AsynchronousEvent {
        override fun toMap() = super.toMap().apply { put("actionName", actionName); put("error", exception.message) }
    }

    // BULK DELETE
    data class BeforeBulkDelete(
        override val resourceName: String,
        val ids: List<String>,
        override val context: KraftAdminContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent(), SynchronousEvent {
        override val entity: Any? = null
        override fun toMap() = super.toMap().apply { put("ids", ids) }
    }

    data class AfterBulkDelete(
        override val resourceName: String,
        val ids: List<String>,
        val deletedCount: Int,
        override val context: KraftAdminContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent(), AsynchronousEvent {
        override val entity: Any? = null
        override fun toMap() = super.toMap().apply { put("ids", ids); put("deletedCount", deletedCount) }
    }

    data class BulkDeleteFailed(
        override val resourceName: String,
        val ids: List<String>,
        val exception: Throwable,
        override val context: KraftAdminContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent(), AsynchronousEvent {
        override val entity: Any? = null
        override fun toMap() = super.toMap().apply { put("ids", ids); put("error", exception.message) }
    }

    // BULK INSERT
    data class BeforeBulkInsert(
        override val resourceName: String,
        val dataList: List<Map<String, Any?>>,
        override val context: KraftAdminContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent(), SynchronousEvent {
        override val entity: Any? = null
        override fun toMap() = super.toMap().apply { put("count", dataList.size) }
    }

    data class AfterBulkInsert(
        override val resourceName: String,
        val entities: List<Any>,
        val count: Int,
        override val context: KraftAdminContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent(), AsynchronousEvent {
        override val entity: Any? = null
        override fun toMap() = super.toMap().apply { put("count", count) }
    }

    data class BulkInsertFailed(
        override val resourceName: String,
        val dataList: List<Map<String, Any?>>,
        val exception: Throwable,
        override val context: KraftAdminContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent(), AsynchronousEvent {
        override val entity: Any? = null
        override fun toMap() = super.toMap().apply { put("error", exception.message) }
    }

    // EXPORT / PRINT
    data class BeforeExport(
        override val resourceName: String,
        val format: String,
        val filterCriteria: Map<String, Any?>,
        override val context: KraftAdminContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent(), SynchronousEvent {
        override val entity: Any? = null
        override fun toMap() = super.toMap().apply { put("format", format) }
    }

    data class AfterExport(
        override val resourceName: String,
        val format: String,
        val exportedRows: Long,
        override val context: KraftAdminContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent(), AsynchronousEvent {
        override val entity: Any? = null
        override fun toMap() = super.toMap().apply { put("format", format); put("rows", exportedRows) }
    }

    data class ExportFailed(
        override val resourceName: String,
        val format: String,
        val filterCriteria: Map<String, Any?>,
        val exception: Throwable,
        override val context: KraftAdminContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent(), AsynchronousEvent {
        override val entity: Any? = null
        override fun toMap() = super.toMap().apply { put("format", format); put("error", exception.message) }
    }

    data class BeforePrint(
        override val resourceName: String,
        val entityId: String,
        val templateName: String,
        override val context: KraftAdminContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent(), SynchronousEvent {
        override val entity: Any? = null
        override fun toMap() = super.toMap().apply { put("entityId", entityId); put("template", templateName) }
    }

    data class PrintFailed(
        override val resourceName: String,
        val entityId: String,
        val templateName: String,
        val exception: Throwable,
        override val context: KraftAdminContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent(), AsynchronousEvent {
        override val entity: Any? = null
        override fun toMap() = super.toMap().apply { put("entityId", entityId); put("error", exception.message) }
    }

    data class AfterPrint(
        override val resourceName: String,
        val entityId: String,
        val templateName: String,
        override val context: KraftAdminContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent(), AsynchronousEvent {
        override val entity: Any? = null
        override fun toMap() = super.toMap().apply { put("entityId", entityId); put("template", templateName) }
    }
}

interface SynchronousEvent
interface AsynchronousEvent
