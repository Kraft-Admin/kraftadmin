package com.kraftadmin.binder

import kotlin.reflect.KClass

interface KraftInputBinder {

    fun bind(
        value: Any?,
        target: KClass<*>
    ): Any?

}