package com.kraftadmin.actions

import com.kraftadmin.context.KraftActionContext

/**
 * Implement this to define a custom action on a KraftAdmin resource.
 * Pure Kotlin interface — no framework imports required.
 *
 * Usage:
 * ```kotlin
 * class SendInvoiceHandler(private val emailService: EmailService) : KraftActionHandler {
 *     override val actionName = "send-invoice"
 *     override fun execute(context: KraftActionContext): KraftActionResponse {
 *         val order = context.entity as Order
 *         emailService.sendInvoice(order)
 *         return KraftActionResponse.ok("Invoice sent")
 *     }
 * }
 * ```
 */
interface KraftActionHandler {
    /** Must match @KraftAdminCustomAction.provider exactly */
    val actionName: String

    fun execute(context: KraftActionContext): KraftActionResponse
}