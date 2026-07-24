package com.kraftadmin.annotations

import com.kraftadmin.enums.ActionVariant
import com.kraftadmin.enums.KraftIcon

import com.kraftadmin.ui_descriptors.ActionTarget
import kotlin.reflect.KClass

/**
 * Declares a custom action that can be rendered by KraftAdmin.
 *
 * May be placed on:
 *  - an entity/resource class
 *  - an action method
 *
 * The framework converts this annotation into a
 * [KraftActionDescriptor] during startup.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class KraftAdminCustomAction(

    /** The class of the entity this action applies to. */
    val entityClass: KClass<*> = Nothing::class,

    /** Unique action identifier (e.g. "approve-order"). */
    val name: String,

    /** Text displayed to the user. */
    val label: String,

    /** Optional icon. */
    val icon: String = KraftIcon.ICON_PLAY,

    /** primary | secondary | success | warning | danger */
    val variant: ActionVariant = ActionVariant.PRIMARY,

    /** Where the action should appear. */
    val target: ActionTarget = ActionTarget.ROW,

    /** Can operate on multiple selected rows. */
    val bulk: Boolean = false,

    /** Requires a selected entity. */
    val requiresSelection: Boolean = true,

    /** Optional confirmation dialog. */
    val confirmMessage: String = "",

    /** Name of the form/schema used by the dialog. */
    val input: KClass<*> = Nothing::class,

    val inputTitle: String = "",
    val inputDescription: String = "",
    val submitLabel: String = "Submit",
    val cancelLabel: String = "Cancel",

    /** Hide the action after successful execution. */
    val hideAfterExecution: Boolean = false,

    /** Refresh the current page after execution. */
    val refresh: Boolean = true,

    /** Display order. Lower numbers appear first. */
    val order: Int = 0,

    /** Optional permission required to execute. */
    val permission: String = "",

    /** Optional grouping in the UI. */
    val group: String = ""
)