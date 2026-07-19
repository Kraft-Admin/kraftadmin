package actions

import com.fasterxml.jackson.databind.ObjectMapper
import com.kraftadmin.context.KraftActionContext
import com.kraftadmin.logging.KraftAdminLogging
import com.kraftadmin.ui_descriptors.KraftActionDescriptor
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

/**
 * Holds the mapping between a UI-defined action and the
 * underlying Spring bean method that implements it.
 */
data class ActionHandlerEntry(
    val bean: Any,
    val method: Method,
    val descriptor: KraftActionDescriptor
) {
    private val mapper = ObjectMapper()
    private val logger = KraftAdminLogging.logger(javaClass)


    fun bindInput(rawInput: Any?): Any? {

        val inputType = descriptor.input
            ?: return null

        return mapper.convertValue(
            rawInput,
            Class.forName(inputType.className)
        )
    }

    /**
     * Executes the action logic.
     * Uses reflection to invoke the method on the Spring bean.
     */
    fun execute(context: KraftActionContext): KraftActionResponse {

        return try {

            method.invoke(bean, context) as KraftActionResponse

        } catch (e: InvocationTargetException) {

            val cause = e.targetException ?: e.cause ?: e

            when (cause) {

                is IllegalStateException ->
                    KraftActionResponse
                        .fail(cause.message ?: "Action not allowed.")
                        .build()

                is IllegalArgumentException ->
                    KraftActionResponse
                        .fail(cause.message ?: "Invalid request.")
                        .build()

                else -> {
                    logger.error("Action failed", cause)

                    KraftActionResponse
                        .fail("Unexpected error.")
                        .build()
                }
            }

        } catch (e: Exception) {

            logger.error("Action failed", e)

            KraftActionResponse
                .fail("Unexpected error. ${e.message}")
                .build()
        }
    }


}