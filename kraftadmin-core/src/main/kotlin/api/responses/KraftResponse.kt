package com.kraftadmin.api.responses

data class KraftOperationResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null,
    val errors: Map<String, List<String>> = emptyMap()
)