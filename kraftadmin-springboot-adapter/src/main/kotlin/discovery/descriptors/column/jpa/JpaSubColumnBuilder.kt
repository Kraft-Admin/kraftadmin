package discovery.descriptors.column.jpa

import com.kraftadmin.annotations.KraftAdminField
import com.kraftadmin.enums.FormInputType
import com.kraftadmin.spi.SelectOption
import com.kraftadmin.ui_descriptors.ColumnDescriptor
import com.kraftadmin.ui_descriptors.WYSIWYGOptions
import jakarta.persistence.Transient
import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

class JpaSubColumnBuilder(
    private val annotationResolver: JpaAnnotationResolver,
    private val typeResolver: JpaTypeResolver,
    private val validationResolver: JpaValidationResolver,
    private val fileResolver: JpaFileResolver
) {

    fun build(
        kClass: KClass<*>?
    ): List<ColumnDescriptor> {

        if (kClass == null) {
            return emptyList()
        }

        return kClass.memberProperties.mapNotNull { prop ->

            val javaField = prop.javaField ?: return@mapNotNull null

            if (
                javaField.isAnnotationPresent(Transient::class.java) ||
                Modifier.isStatic(javaField.modifiers)
            ) {
                return@mapNotNull null
            }

            val (type, defaultValue) =
                typeResolver.resolveTypeAndDefault(
                    prop,
                    false,
                    false,
                    false,
                    false
                )

            val adminField =
                annotationResolver.resolveAnnotation(
                    javaField,
                    prop,
                    KraftAdminField::class
                )

            val validation =
                validationResolver.resolve(javaField)

            val fileOptions =
                fileResolver.resolve(
                    type,
                    adminField?.fileConfig
                )

            val wysiwyg = if (type == FormInputType.WYSIWYG) {

                adminField?.wysiwygConfig?.let {

                    WYSIWYGOptions(
                        toolbar = it.toolbarProfile.name,
                        placeholder = it.placeholder.ifBlank {
                            "Enter ${prop.name}"
                        },
                        options = it.toolbarProfile.toolbarConfig
                    )
                }

            } else {
                null
            }

            val enumOptions =
                if (type == FormInputType.SELECT) {

                    (prop.returnType.classifier as? KClass<*>)
                        ?.java
                        ?.enumConstants
                        ?.map {
                            SelectOption(
                                label = it.toString(),
                                value = it.toString()
                            )
                        }

                } else {
                    null
                }

            ColumnDescriptor(

                name = prop.name,

                label = prop.name
                    .replace(
                        Regex("([a-z])([A-Z])"),
                        "$1 $2"
                    )
                    .replaceFirstChar { it.uppercase() },

                type = type.name,

                defaultValue = defaultValue,

                subColumns =
                    if (type == FormInputType.OBJECT)
                        build(prop.returnType.classifier as? KClass<*>)
                    else
                        null,

                selectOptions = enumOptions,

                searchable = true,

                sortable = true,

                visible = true,

                showInTable = false,

                required = validation.required,

                validationRules = validation.rules,

                validationMessages = validation.messages,

                placeholder = "Enter ${prop.name}",

                wysiwygConfig = wysiwyg,

                fileOptions = fileOptions
            )
        }
    }
}