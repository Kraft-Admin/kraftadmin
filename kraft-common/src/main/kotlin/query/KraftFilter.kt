package com.kraftadmin.query

sealed class KraftFilter {

    data class Equals(
        val field: String,
        val value: Any?
    ) : KraftFilter()

    data class Like(
        val field: String,
        val value: String
    ) : KraftFilter()

    data class GreaterThan(
        val field: String,
        val value: Any
    ) : KraftFilter()

    data class LessThan(
        val field: String,
        val value: Any
    ) : KraftFilter()

    data class Between(
        val field: String,
        val from: Any,
        val to: Any
    ) : KraftFilter()

    data class In(
        val field: String,
        val values: List<Any?>
    ) : KraftFilter()

    data class IsNull(
        val field: String
    ) : KraftFilter()

    data class IsNotNull(
        val field: String
    ) : KraftFilter()

    data class Search(
        val fields: List<String>,
        val value: String
    ) : KraftFilter()
}