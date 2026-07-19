package com.kraftadmin.ui_descriptors

import com.kraftadmin.enums.FormInputType

data class KraftFieldDescriptor(

    /** Java property provider */
    val name: String,

    /** Label shown in the UI */
    val label: String,

    /** Input component to render */
    val type: FormInputType,

    /** Placeholder */
    val placeholder: String? = null,

    /** Help text shown below the field */
    val helperText: String? = null,

    /** Whether the field is required */
    val required: Boolean = false,

    /** Whether the field is readonly */
    val readOnly: Boolean = false,

    /** Whether the field is hidden */
    val hidden: Boolean = false,

    /** Display order */
    val order: Int = 0,

    /** Default value */
    val defaultValue: Any? = null,

    /** Validation */

    val min: Double? = null,
    val max: Double? = null,
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val pattern: String? = null,

    /** Select / Relation options */

    val options: List<KraftOptionDescriptor> = emptyList(),

    /** File configuration */

    val file: KraftFileDescriptor? = null
)