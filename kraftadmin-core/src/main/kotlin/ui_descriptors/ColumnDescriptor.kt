package com.kraftadmin.ui_descriptors

import com.kraftadmin.spi.SelectOption
import kotlin.reflect.KClass

data class ColumnDescriptor(
    val name: String,
    val label: String,
    val type: String,
    val searchable: Boolean,
    val sortable: Boolean,
    val visible: Boolean,
    val showInTable: Boolean = false,
    val required: Boolean,
    val defaultValue: Any? = null,
    val subColumns: List<ColumnDescriptor>? = null,
    val selectOptions: List<SelectOption>? = null,
    val placeholder: String? = null,
    val validationRules: String? = null,
    val validationMessages: Map<String, String>? = null,
    val error: String? = null,
    val lookup: LookupDescriptor? = null,
    val wysiwygConfig: WYSIWYGOptions? = null,
    val fileOptions: FileConfigDescriptor? = null,
    val elementCollection: ElementCollectionDescriptor? = null
)

/**
 * Clean POJO representation of the UI configuration parameters
 */
class WYSIWYGOptions {
    var toolbar: String = "MINIMAL"
    var placeholder: String? = null
    var options: List<List<Any>> = mutableListOf()

    constructor() // No-arg

    // The precise constructor the compiler expects
    constructor(toolbar: String, placeholder: String?, options: List<List<Any>>) {
        this.toolbar = toolbar
        this.placeholder = placeholder
        this.options = options
    }
}

/**
 * File options UI descriptor with sensible default parameters
 */
class FileConfigDescriptor {
    var multiple: Boolean = false
    var maxFiles: Int = 1
    var allowedExtensions: List<String> = mutableListOf()
    var minSizeBytes: Long = 0L
    var maxSizeBytes: Long = 10 * 1024 * 1024
    var allowedMimeTypes: List<String> = mutableListOf()

    constructor()

    constructor(allowedExtensions: List<String>, allowedMimeTypes: List<String>) {
        this.allowedExtensions = allowedExtensions
        this.allowedMimeTypes = allowedMimeTypes
    }

    constructor(multiple: Boolean, maxFiles: Int, allowedExtensions: List<String>,
                minSizeBytes: Long, maxSizeBytes: Long, allowedMimeTypes: List<String>) {
        this.multiple = multiple
        this.maxFiles = maxFiles
        this.allowedExtensions = allowedExtensions
        this.minSizeBytes = minSizeBytes
        this.maxSizeBytes = maxSizeBytes
        this.allowedMimeTypes = allowedMimeTypes
    }
}