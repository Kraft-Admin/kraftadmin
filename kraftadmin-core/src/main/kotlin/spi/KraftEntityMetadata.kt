package com.kraftadmin.spi

interface KraftEntityMetadata<T : Any> {
    val entityName: String
    val idField: String
    val idType: Class<*>
    val displayField: String
    val sortableFields: List<String>
    val searchableFields: List<String>
    val defaultSort: String
    val versioningEnabled: Boolean

    fun convertId(idValue: Any?): Any?
}