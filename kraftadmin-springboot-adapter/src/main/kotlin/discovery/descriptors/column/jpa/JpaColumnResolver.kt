package discovery.descriptors.column.jpa

import com.kraftadmin.annotations.KraftAdminField
import com.kraftadmin.enums.FormInputType
import com.kraftadmin.spi.KraftAdminColumn
import com.kraftadmin.spi.SelectOption
import com.kraftadmin.ui_descriptors.ElementCollectionDescriptor
import com.kraftadmin.ui_descriptors.WYSIWYGOptions
import discovery.descriptors.column.resolvers.EnumHelper
import jakarta.persistence.*
import org.slf4j.LoggerFactory
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.javaField

class JpaColumnResolver(
    private val annotationResolver: JpaAnnotationResolver,
    private val typeResolver: JpaTypeResolver,
    private val lookupResolver: JpaLookupResolver,
    private val validationResolver: JpaValidationResolver,
    private val visibilityResolver: JpaVisibilityResolver,
    private val fileResolver: JpaFileResolver,
    private val subColumnBuilder: JpaSubColumnBuilder,
    private val elementCollectionResolver: ElementCollectionResolver
) {

    private val log = LoggerFactory.getLogger(javaClass)

    init {
        log.info("jpa column resolver")
    }

    fun resolve(
        entityClass: KClass<*>,
        property: KProperty1<out Any, *>
    ): KraftAdminColumn? {

        try {


            val field = property.javaField ?: return null



            if (
                field.isAnnotationPresent(Transient::class.java) ||
                Modifier.isStatic(field.modifiers)
            ) {
                return null
            }

            val isOneToOne = hasRelation(field, property, OneToOne::class)
            val isManyToOne = hasRelation(field, property, ManyToOne::class)
            val isManyToMany = hasRelation(field, property, ManyToMany::class)
            val isOneToMany = hasRelation(field, property, OneToMany::class)

            val targetEntity = when {
                isOneToOne || isManyToOne ->
                    property.returnType.classifier as? KClass<*>

                isManyToMany || isOneToMany ->
                    property.returnType.arguments.firstOrNull()
                        ?.type
                        ?.classifier as? KClass<*>

                else -> null
            }

            val (type, defaultValue) =
                typeResolver.resolveTypeAndDefault(
                    property,
                    isOneToOne,
                    isManyToOne,
                    isManyToMany,
                    isOneToMany
                )

            val adminField =
                annotationResolver.resolveAnnotation(
                    field,
                    property,
                    KraftAdminField::class
                )

            val validation =
                validationResolver.resolve(field)

            val lookup =
                targetEntity?.let {
                    lookupResolver.buildLookup(property, field, it)
                }

            val fileOptions =
                fileResolver.resolve(type, adminField?.fileConfig)

            val subColumns =
                if (
                    type == FormInputType.OBJECT &&
                    (
                            field.isAnnotationPresent(Embedded::class.java) ||
                                    (property.returnType.classifier as? KClass<*>)
                                        ?.java
                                        ?.isAnnotationPresent(Embeddable::class.java) == true
                            )
                ) {
                    subColumnBuilder.build(
                        property.returnType.classifier as? KClass<*>
                    )
                } else {
                    null
                }

//            val selectOptions =
//                if (type == FormInputType.SELECT) {
//                    (property.returnType.classifier as? KClass<*>)
//                        ?.java
//                        ?.enumConstants
//                        ?.map {
//                            SelectOption(
//                                it.toString(),
//                                it.toString()
//                            )
//                        }
//                } else {
//                    null
//                }


            val selectOptions =
                if (type == FormInputType.SELECT){
                    val enumClass = (property.returnType.classifier as? KClass<*>)?.java
                    if (enumClass?.isEnum == true) {
                        @Suppress("UNCHECKED_CAST")
                        EnumHelper.getSelectOptions(enumClass as Class<out Enum<*>>)
                    } else {
                        null
                    }
                } else null

            val wysiwyg =
                if (type == FormInputType.WYSIWYG) {
                    adminField?.wysiwygConfig?.let {
                        WYSIWYGOptions(
                            toolbar = it.toolbarProfile.name,
                            placeholder = it.placeholder.ifBlank {
                                "Enter ${property.name}"
                            },
                            options = it.toolbarProfile.toolbarConfig
                        )
                    }
                } else {
                    null
                }

            val elementCollection = ElementCollectionResolver.resolve(field)

            val resolvedType =
                if (elementCollection != null) {
                    FormInputType.COLLECTION
                } else {
                    type
                }

            return KraftAdminColumn(
                name = property.name,

                label = property.name
                    .replace(
                        Regex("([a-z])([A-Z])"),
                        "$1 $2"
                    )
                    .replaceFirstChar { it.uppercase() },

//                type = FormInputType.valueOf(type.name),

                type = FormInputType.valueOf(resolvedType.name),

                searchable = true,

                sortable = true,

                visible = !property.name.equals("id", true) &&
                        !isOneToMany,

                showInTable =
                    visibilityResolver.shouldShowInTable(
                        property,
                        type,
                        true,
                        true,
                        adminField
                    ),

                required = validation.required,

                defaultValue = defaultValue,

                selectOptions = selectOptions,

                subColumns = subColumns,

                placeholder =
                    if (targetEntity != null)
                        "Search ${property.name}..."
                    else
                        "Enter ${property.name}",

                validationRules = validation.rules,

                validationMessages = validation.messages,

                lookup = lookup,
                wysiwygConfigValue = wysiwyg,

                fileOptions = fileOptions,
                elementCollection = elementCollection?.let{
                    ElementCollectionDescriptor(
                        shape = it.shape,
                        key = it.key,
                        value = it.value,
                        minItems = it.minItems,
                        maxItems = it.maxItems
                    )
                }
            )
        } catch (e : Exception) {
            log.info("error resolving column for {} {} {}", property, entityClass, e.message)
            return null
        }
    }

    private fun hasRelation(
        field: Field,
        property: KProperty1<out Any, *>,
        annotation: KClass<out Annotation>
    ): Boolean {

        return field.isAnnotationPresent(annotation.java) ||
                property.annotations.any {
                    it.annotationClass == annotation
                }
    }
}