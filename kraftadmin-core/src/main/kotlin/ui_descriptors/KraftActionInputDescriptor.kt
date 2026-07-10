package com.kraftadmin.ui_descriptors

data class KraftActionInputDescriptor(

    /** Dialog title */
    val title: String,

    /** Optional description shown below the title */
    val description: String? = null,

    /** Text for the submit button */
    val submitLabel: String = "Submit",

    /** Text for the cancel button */
    val cancelLabel: String = "Cancel",

    /** DTO class (mainly for binding/debugging) */
    val className: String,

    /** Form fields */
    val fields: List<KraftFieldDescriptor>
)