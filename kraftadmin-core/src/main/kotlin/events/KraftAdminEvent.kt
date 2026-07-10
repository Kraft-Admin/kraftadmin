package com.kraftadmin.events

import com.kraftadmin.context.KraftAdminEventContext
import java.time.Instant

/**
 * Base type for every lifecycle event emitted by KraftAdmin.
 *
 * Events are immutable snapshots describing something that is about to happen
 * or has already happened during a resource operation.
 *
 * Every event contains:
 * - the resource name
 * - the affected entity (when applicable)
 * - request/user context
 * - the timestamp when the event occurred
 *
 * Lifecycle semantics are defined by marker interfaces:
 *
 * - [SynchronousEvent]
 *      Executed before an operation. Listener exceptions are propagated back
 *      to the caller and may veto the operation.
 *
 * - [AsynchronousEvent]
 *      Executed after an operation completes successfully. Listener failures
 *      are logged but never affect the completed request.
 */
sealed class KraftAdminEvent {

    /**
     * Name of the affected resource.
     */
    abstract val resourceName: String

    /**
     * The entity involved in the operation.
     *
     * May be null for events that occur before an entity exists or for
     * bulk operations.
     */
    abstract val entity: Any?

    /**
     * Request metadata such as authenticated user,
     * HTTP request information and adapter-specific context.
     */
    abstract val context: KraftAdminEventContext

    /**
     * Timestamp when the event instance was created.
     */
    abstract val occurredAt: Instant

    // ---------------------------------------------------------------------
    // CREATE
    // ---------------------------------------------------------------------

    /**
     * Fired before a new entity is created.
     *
     * Throwing an exception from a listener aborts creation.
     */
    data class BeforeCreate(
        override val resourceName: String,
        val data: Map<String, Any?>,
        override val context: KraftAdminEventContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent(), SynchronousEvent {
        override val entity: Any? = null
    }

    /**
     * Fired after an entity has been successfully created.
     */
    data class AfterCreate(
        override val resourceName: String,
        override val entity: Any,
        val data: Map<String, Any?>,
        override val context: KraftAdminEventContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent(), AsynchronousEvent

    // ---------------------------------------------------------------------
    // UPDATE
    // ---------------------------------------------------------------------

    /**
     * Fired before an entity is updated.
     *
     * Throwing an exception prevents the update.
     */
    data class BeforeUpdate(
        override val resourceName: String,
        override val entity: Any,
        val id: String,
        val data: Map<String, Any?>,
        override val context: KraftAdminEventContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent(), SynchronousEvent

    /**
     * Fired after an entity has been updated.
     */
    data class AfterUpdate(
        override val resourceName: String,
        override val entity: Any,
        val id: String,
        val data: Map<String, Any?>,
        override val context: KraftAdminEventContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent(), AsynchronousEvent

    // ---------------------------------------------------------------------
    // DELETE
    // ---------------------------------------------------------------------

    /**
     * Fired before an entity is deleted.
     *
     * Throwing an exception prevents deletion.
     */
    data class BeforeDelete(
        override val resourceName: String,
        override val entity: Any,
        val id: String,
        override val context: KraftAdminEventContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent(), SynchronousEvent

    /**
     * Fired after an entity has been deleted.
     */
    data class AfterDelete(
        override val resourceName: String,
        override val entity: Any,
        val id: String,
        override val context: KraftAdminEventContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent(), AsynchronousEvent

    // ---------------------------------------------------------------------
    // CUSTOM ACTIONS
    // ---------------------------------------------------------------------

    /**
     * Fired before a custom action executes.
     *
     * Throwing an exception cancels the action and the exception may be
     * returned to the UI.
     */
    data class BeforeAction(
        override val resourceName: String,
        override val entity: Any?,
        val actionName: String,
        val input: Any?,
        override val context: KraftAdminEventContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent(), SynchronousEvent

    /**
     * Fired after a custom action completes successfully.
     */
    data class AfterAction(
        override val resourceName: String,
        override val entity: Any?,
        val actionName: String,
        val input: Any?,
        override val context: KraftAdminEventContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent(), AsynchronousEvent

    /**
     * Fired whenever a custom action fails.
     *
     * Intended for audit logging, monitoring and notifications.
     */
    data class ActionFailed(
        override val resourceName: String,
        override val entity: Any?,
        val actionName: String,
        val input: Any?,
        val exception: Throwable,
        override val context: KraftAdminEventContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent(), AsynchronousEvent

    // ---------------------------------------------------------------------
    // BULK DELETE
    // ---------------------------------------------------------------------

    /**
     * Fired before a bulk delete operation begins.
     */
    data class BeforeBulkDelete(
        override val resourceName: String,
        val ids: List<String>,
        override val context: KraftAdminEventContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent(), SynchronousEvent {
        override val entity: Any? = null
    }

    /**
     * Fired after a bulk delete operation completes.
     */
    data class AfterBulkDelete(
        override val resourceName: String,
        val ids: List<String>,
        val deletedCount: Int,
        override val context: KraftAdminEventContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent(), AsynchronousEvent {
        override val entity: Any? = null
    }

    // ---------------------------------------------------------------------
    // BULK INSERT
    // ---------------------------------------------------------------------

    /**
     * Fired before a bulk insert operation.
     */
    data class BeforeBulkInsert(
        override val resourceName: String,
        val dataList: List<Map<String, Any?>>,
        override val context: KraftAdminEventContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent(), SynchronousEvent {
        override val entity: Any? = null
    }

    /**
     * Fired after a successful bulk insert.
     */
    data class AfterBulkInsert(
        override val resourceName: String,
        val entities: List<Any>,
        val count: Int,
        override val context: KraftAdminEventContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent(), AsynchronousEvent {
        override val entity: Any? = null
    }

    // ---------------------------------------------------------------------
    // EXPORT / PRINT
    // ---------------------------------------------------------------------

    /**
     * Fired before data export begins.
     *
     * Listeners may cancel the export.
     */
    data class BeforeExport(
        override val resourceName: String,
        val format: String,
        val filterCriteria: Map<String, Any?>,
        override val context: KraftAdminEventContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent(), SynchronousEvent {
        override val entity: Any? = null
    }

    /**
     * Fired after a print operation completes.
     */
    data class AfterPrint(
        override val resourceName: String,
        val entityId: String,
        val templateName: String,
        override val context: KraftAdminEventContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent(), AsynchronousEvent {
        override val entity: Any? = null
    }
}

/**
 * Marker interface for events that execute synchronously.
 *
 * Listener exceptions are propagated to the caller and may cancel
 * the operation currently being performed.
 */
interface SynchronousEvent

/**
 * Marker interface for events that execute asynchronously.
 *
 * Listener failures are logged but never propagated back to the caller.
 */
interface AsynchronousEvent