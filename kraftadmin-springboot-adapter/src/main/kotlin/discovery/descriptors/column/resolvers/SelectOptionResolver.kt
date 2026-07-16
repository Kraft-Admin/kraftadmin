package discovery.descriptors.column.resolvers

import com.kraftadmin.spi.SelectOption
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

object EnumHelper {
    fun getSelectOptions(enumClass: Class<out Enum<*>>): List<SelectOption> {
        val kClass = enumClass.kotlin

        // Find property that is a String and is NOT the enum 'name'
        val labelProperty = kClass.memberProperties
            .firstOrNull { prop ->
                prop.name != "name" && prop.returnType.classifier == String::class
            }

        return enumClass.enumConstants.map { constant ->
            val label = if (labelProperty != null) {
                try {
                    // Force the property to be accessible if needed
                    labelProperty.isAccessible = true
                    labelProperty.getter.call(constant) as? String
                } catch (e: Exception) {
                    null
                }
            } else null

            SelectOption(
                label = label ?: constant.toString(),
                value = constant.name
            )
        }
    }
}