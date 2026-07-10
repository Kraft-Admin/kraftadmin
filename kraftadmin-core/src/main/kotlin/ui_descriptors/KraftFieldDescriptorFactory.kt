package com.kraftadmin.ui_descriptors

import com.kraftadmin.annotations.KraftAdminField
import com.kraftadmin.enums.FormInputType
import com.kraftadmin.ui_descriptors.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation

class KraftFieldDescriptorFactory {

    fun create(type: KClass<*>): List<KraftFieldDescriptor> {

        return type.declaredMemberProperties.map { property ->

            val annotation = property.findAnnotation<KraftAdminField>()

            val inputType = annotation?.inputType ?: inferType(property.returnType.classifier as? KClass<*>)

            KraftFieldDescriptor(

                name = property.name,

                label = annotation?.label?.takeIf { it.isNotBlank() }
                    ?: property.name.replaceFirstChar(Char::uppercase),

                type = inputType,

                required = annotation?.required ?: false,

                readOnly = annotation?.readonly ?: false,

                hidden = annotation?.displayField ?: false,

                placeholder = annotation?.placeholder?.takeIf { it.isNotBlank() },

                helperText = annotation?.placeholder?.takeIf { it.isNotBlank() },

                order = 0,

                defaultValue = null,

                options = emptyList(),

                file = annotation?.fileConfig?.let {

                    KraftFileDescriptor(
                        multiple = it.multiple,
                        maxFiles = it.maxFiles,
                        maxSizeBytes = it.maxSizeBytes,
                        allowedExtensions = it.allowedExtensions.map { ext -> ext.name },
                        allowedMimeTypes = it.allowedMimeTypes.map { mime -> mime.value }
                    )
                }
            )
        }.sortedBy { it.order }
    }

    private fun inferType(type: KClass<*>?): FormInputType {

        return when (type) {

            String::class -> FormInputType.TEXT

            Int::class,
            Long::class,
            Double::class,
            Float::class,
            Short::class,
            Byte::class,
            BigDecimal::class -> FormInputType.NUMBER

            Boolean::class -> FormInputType.CHECKBOX

            LocalDate::class -> FormInputType.DATE

            LocalDateTime::class -> FormInputType.DATETIME

            LocalTime::class -> FormInputType.TIME

            else -> {
                if (type?.java?.isEnum == true)
                    FormInputType.SELECT
                else
                    FormInputType.TEXT
            }
        }
    }
}