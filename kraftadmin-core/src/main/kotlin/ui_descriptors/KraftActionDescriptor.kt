package com.kraftadmin.ui_descriptors

import com.kraftadmin.enums.ActionVariant
import kotlin.reflect.KClass


data class KraftActionDescriptor(
    val entityClass: String? = null,

    /** Unique action identifier (e.g. approve-order) */
    val name: String,

    /** Text shown to the user */
    val label: String,

    /** Optional icon */
    val icon: String? = null,

    /** primary | success | warning | danger | secondary */
    val variant: ActionVariant = ActionVariant.PRIMARY,

    /** Row action or toolbar action */
    val target: ActionTarget = ActionTarget.ROW,

    /** Whether multiple rows can be selected */
    val bulk: Boolean = false,

    /** Whether a row/entity must exist */
    val requiresSelection: Boolean = true,

    /** Confirmation dialog */
    val confirmMessage: String? = null,

    /** Optional form schema name */
    val input: KraftActionInputDescriptor? = null,

    /** Hide after execution */
    val hideAfterExecution: Boolean = false,

    /** Refresh current page after success */
    val refresh: Boolean = true,

    /** Display order */
    val order: Int = 0,

    /** Permission or authority */
    val permission: String? = null,

    /** Optional grouping */
    val group: String? = null
){

}


enum class ActionTarget {
    /** Action appears inside the row (e.g., "Delete") */
    ROW,
    /** Action appears in the top table toolbar (e.g., "Bulk Export") */
    TOOLBAR
}