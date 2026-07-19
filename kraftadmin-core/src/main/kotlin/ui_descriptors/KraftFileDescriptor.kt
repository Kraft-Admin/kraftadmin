package com.kraftadmin.ui_descriptors

data class KraftFileDescriptor(

    val multiple: Boolean = false,

    val maxFiles: Int = 1,

    val maxSizeBytes: Long? = null,

    val allowedExtensions: List<String> = emptyList(),

    val allowedMimeTypes: List<String> = emptyList()
)