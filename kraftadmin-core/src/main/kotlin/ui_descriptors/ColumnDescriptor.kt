package com.kraftadmin.ui_descriptors

import com.kraftadmin.annotations.RichTextConfig
import com.kraftadmin.spi.SelectOption
import kotlin.reflect.KClass

data class ColumnDescriptor(
    val name: String,
    val label: String,
    val type: String,
    val searchable: Boolean,
    val sortable: Boolean,
    val visible: Boolean,
    val required: Boolean,
    val defaultValue: Any? = null,
    val subColumns: List<ColumnDescriptor>? = null,
    val selectOptions: List<SelectOption>? = null,
    val placeholder: String? = null,
    val validationRules: String? = null,
    val validationMessages: Map<String, String>? = null,
    // Server-side error message (e.g., "This email is already taken")
    val error: String? = null,
    val lookup: LookupDescriptor? = null,
    val wysiwygConfig: WYSIWYGOptions? = null,
    val fileOptions: FileConfigDescriptor? = null
)

/**
 * Clean POJO representation of the UI configuration parameters
 */
data class WYSIWYGOptions(
    val toolbar: String,
    val placeholder: String? = null,
    val options: List<List<Any>> = listOf(),
)

/**
 * File options UI descriptor with sensible default parameters
 */
data class FileConfigDescriptor(
    val multiple: Boolean = false,
    val maxFiles: Int = 1,
    val allowedExtensions: List<String> = emptyList(),
    val minSizeBytes: Long = 0L,
    val maxSizeBytes: Long = 10 * 1024 * 1024, // 10MB default
    val allowedMimeTypes: List<String> = emptyList()
)