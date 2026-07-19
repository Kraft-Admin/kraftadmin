package binder

import com.fasterxml.jackson.databind.ObjectMapper
import com.kraftadmin.binder.KraftInputBinder
import kotlin.reflect.KClass

class JacksonInputBinder(
    private val mapper: ObjectMapper
) : KraftInputBinder {

    override fun bind(
        value: Any?,
        target: KClass<*>
    ): Any? {

        if (value == null) return null

        return mapper.convertValue(
            value,
            target.java
        )
    }
}