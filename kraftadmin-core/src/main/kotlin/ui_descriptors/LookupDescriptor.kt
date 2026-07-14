package com.kraftadmin.ui_descriptors

//data class LookupDescriptor(
//    val targetEntity: String?, // e.g., "Institution"
//    val searchField: String?,   // e.g., "provider" or "code"
//    val displayField: String?,
//    val lookupKey: String? = "id",
//)

data class LookupDescriptor(
    val targetEntity: String?,
    var lookupKey: String = "id",
    // populated internally
    var displayField: String="id",
    val searchableFields: List<String> = emptyList()
)

data class LookupDescriptor1(
    val targetEntity: String,
    val displayField: String?,
    val lookupKey: String,
    val searchableFields: List<String>
)