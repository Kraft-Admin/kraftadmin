package com.kraftadmin.events

import com.kraftadmin.context.KraftAdminEventContext
import java.time.Instant

/**
 * Sealed hierarchy of all KraftAdmin lifecycle events.
 * Sealed classes give type safety — listeners can exhaustively
 * handle exactly the events they care about with no stringly-typed matching.
 *
 * Every event carries:
 *   - resourceName: which resource triggered it
 *   - entity: the JPA entity (null for BEFORE_CREATE)
 *   - context: request/actor metadata
 *   - occurredAt: monotonic timestamp
 */
sealed class KraftAdminEvent {

    abstract val resourceName: String
    abstract val entity: Any?
    abstract val context: KraftAdminEventContext
    abstract val occurredAt: Instant

    // ----------------------------------------------------
    // CREATE
    // ----------------------------------------------------

    data class BeforeCreate(
        override val resourceName: String,
        val data: Map<String, Any?>,
        override val context: KraftAdminEventContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent() {
        override val entity: Any? = null
    }

    data class AfterCreate(
        override val resourceName: String,
        override val entity: Any,
        val data: Map<String, Any?>,
        override val context: KraftAdminEventContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent()

    // ----------------------------------------------------
    // UPDATE
    // ----------------------------------------------------

    data class BeforeUpdate(
        override val resourceName: String,
        override val entity: Any,
        val id: String,
        val data: Map<String, Any?>,
        override val context: KraftAdminEventContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent()

    data class AfterUpdate(
        override val resourceName: String,
        override val entity: Any,
        val id: String,
        val data: Map<String, Any?>,
        override val context: KraftAdminEventContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent()

    // ----------------------------------------------------
    // DELETE
    // ----------------------------------------------------

    data class BeforeDelete(
        override val resourceName: String,
        override val entity: Any,
        val id: String,
        override val context: KraftAdminEventContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent()

    data class AfterDelete(
        override val resourceName: String,
        override val entity: Any,
        val id: String,
        override val context: KraftAdminEventContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent()

    // ----------------------------------------------------
    // CUSTOM ACTIONS
    // ----------------------------------------------------

    data class BeforeAction(
        override val resourceName: String,
        override val entity: Any?,
        val actionName: String,
        val params: Map<String, Any?>,
        override val context: KraftAdminEventContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent()

    data class AfterAction(
        override val resourceName: String,
        override val entity: Any?,
        val actionName: String,
        val params: Map<String, Any?>,
        override val context: KraftAdminEventContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent()

    data class ActionFailed(
        override val resourceName: String,
        override val entity: Any?,
        val actionName: String,
        val params: Map<String, Any?>,
        val exception: Throwable,
        override val context: KraftAdminEventContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent()

    // ----------------------------------------------------
    // BULK
    // ----------------------------------------------------

    data class BeforeBulkDelete(
        override val resourceName: String,
        val ids: List<String>,
        override val context: KraftAdminEventContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent() {
        override val entity: Any? = null
    }

    data class AfterBulkDelete(
        override val resourceName: String,
        val ids: List<String>,
        val deletedCount: Int,
        override val context: KraftAdminEventContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent() {
        override val entity: Any? = null
    }

    // ----------------------------------------------------
    // BULK OPERATIONS
    // ----------------------------------------------------

    data class BeforeBulkInsert(
        override val resourceName: String,
        val dataList: List<Map<String, Any?>>,
        override val context: KraftAdminEventContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent() { override val entity: Any? = null }

    data class AfterBulkInsert(
        override val resourceName: String,
        val entities: List<Any>,
        val count: Int,
        override val context: KraftAdminEventContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent() { override val entity: Any? = null }

    // ----------------------------------------------------
    // DATA EXPORT / PRINT
    // ----------------------------------------------------

    data class BeforeExport(
        override val resourceName: String,
        val format: String, // "CSV", "PDF", "XLSX"
        val filterCriteria: Map<String, Any?>,
        override val context: KraftAdminEventContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent() { override val entity: Any? = null }

    data class AfterPrint(
        override val resourceName: String,
        val entityId: String,
        val templateName: String,
        override val context: KraftAdminEventContext,
        override val occurredAt: Instant = Instant.now()
    ) : KraftAdminEvent() { override val entity: Any? = null }


}