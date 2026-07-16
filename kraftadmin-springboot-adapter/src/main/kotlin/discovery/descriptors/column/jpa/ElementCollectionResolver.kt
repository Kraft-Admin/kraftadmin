package discovery.descriptors.column.jpa

import com.kraftadmin.annotations.KraftAdminField
import com.kraftadmin.enums.FormInputType
import com.kraftadmin.spi.SelectOption
import com.kraftadmin.ui_descriptors.ElementCollectionDescriptor
import com.kraftadmin.ui_descriptors.ElementCollectionShape
import com.kraftadmin.ui_descriptors.EmbeddableFieldDescriptor
import com.kraftadmin.ui_descriptors.ValueDescriptor
import com.kraftadmin.ui_descriptors.ValueType
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Embeddable
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

/**
 * Resolves the complete structure of an @ElementCollection.
 *
 * The produced descriptor is recursive, meaning embeddables may themselves
 * contain collections, which in turn contain embeddables, etc.
 *
 * Every value in the tree carries an [ValueDescriptor.inputType] — either an
 * explicit override from @KraftAdminField(inputType = ...) on the owning
 * field, or a sensible default inferred from the Java type. This lets the
 * frontend render, e.g., a List<String> of image URLs as file uploaders
 * rather than plain text inputs, purely by reading the descriptor.
 */
object ElementCollectionResolver {

    val fileResolver = JpaFileResolver()

    fun resolve(field: Field): ElementCollectionDescriptor? {
        if (!field.isAnnotationPresent(ElementCollection::class.java)) {
            return null
        }

        val descriptor = describeCollection(field.genericType) ?: return null
        val admin = field.getAnnotation(KraftAdminField::class.java)

        // A whole-collection override applies to the VALUE side — e.g.
        // List<String> productImages annotated with inputType = IMAGE.
        // Map keys are never file-backed, so overrides never touch `key`.
        return descriptor.copy(value = applyOverride(descriptor.value, admin))
    }

    /**
     * Describes a collection type such as:
     *
     * List<String>
     * Set<UUID>
     * Map<String, Address>
     */
    private fun describeCollection(type: Type): ElementCollectionDescriptor? {

        val parameterized = type as? ParameterizedType ?: return null
        val raw = parameterized.rawType as? Class<*> ?: return null
        val args = parameterized.actualTypeArguments

        return when {

            Map::class.java.isAssignableFrom(raw) && args.size == 2 ->
                ElementCollectionDescriptor(
                    shape = ElementCollectionShape.MAP,
                    key = describeType(args[0]),
                    value = describeType(args[1])
                )

            List::class.java.isAssignableFrom(raw) && args.size == 1 ->
                ElementCollectionDescriptor(
                    shape = ElementCollectionShape.LIST,
                    value = describeType(args[0])
                )

            Set::class.java.isAssignableFrom(raw) && args.size == 1 ->
                ElementCollectionDescriptor(
                    shape = ElementCollectionShape.SET,
                    value = describeType(args[0])
                )

            else -> null
        }
    }

    /**
     * Describes any value. This may be:
     * - primitive
     * - enum
     * - embeddable
     * - another collection
     *
     * Always populates a default [ValueDescriptor.inputType] inferred from
     * the resolved [ValueType], so callers with no field-level override
     * still get a concrete rendering hint.
     */
    private fun describeType(type: Type): ValueDescriptor {

        // Nested collections
        if (type is ParameterizedType) {
            val collection = describeCollection(type)
            if (collection != null) {
                return ValueDescriptor(
                    type = ValueType.COLLECTION,
                    className = (type.rawType as Class<*>).simpleName,
                    collection = collection,
                    inputType = FormInputType.ARRAY
                )
            }
        }

        val cls = (type as? Class<*>) ?: String::class.java

        // Embeddable
        if (cls.isAnnotationPresent(Embeddable::class.java)) {
            return ValueDescriptor(
                type = ValueType.EMBEDDABLE,
                className = cls.simpleName,
                fields = cls.declaredFields
                    .filter { !Modifier.isStatic(it.modifiers) }
                    .map { field -> describeEmbeddableField(field) },
                inputType = FormInputType.OBJECT
            )
        }

        // Enum
//        if (cls.isEnum) {
//            return ValueDescriptor(
//                type = ValueType.ENUM,
//                className = cls.simpleName,
//                enumValues = cls.enumConstants.map {
//                    SelectOption(label = it.toString(), value = it.toString())
//                },
//                inputType = FormInputType.SELECT
//            )
//        }

        if (cls.isEnum) {
            val kClass = cls.kotlin

            // SMART DISCOVERY:
            // 1. Look for a property that is a String and is NOT the name/ordinal/declaringClass.
            // 2. We prefer properties that appear early in the constructor (usually the label).
            val labelProperty = kClass.memberProperties
                .filter { it.returnType.classifier == String::class }
                .filter { it.name != "name" && it.name != "declaringClass" }
                // Pick the property that is most likely the "display" label.
                // If there are multiple, the first one declared in the class is usually the label.
                .minByOrNull { member ->
                    // This favors properties that were likely defined first in the class body
                    member.javaField?.modifiers ?: 0
                }

            val options = cls.enumConstants.map { enumConstant ->
                val label = try {
                    labelProperty?.let { it.getter.call(enumConstant) as? String }
                        ?: enumConstant.toString()
                } catch (e: Exception) {
                    enumConstant.toString()
                }

                SelectOption(
                    label = label,
                    value = enumConstant.toString()
                )
            }

            return ValueDescriptor(
                type = ValueType.ENUM,
                className = cls.simpleName,
                enumValues = options,
                inputType = FormInputType.SELECT
            )
        }

        // Primitive / scalar
        val valueType = mapToValueType(cls)
        return ValueDescriptor(
            type = valueType,
            className = cls.simpleName,
            inputType = defaultInputTypeFor(valueType)
        )
    }

    private fun describeEmbeddableField(field: Field): EmbeddableFieldDescriptor {

        val column = field.getAnnotation(Column::class.java)
        val admin = field.getAnnotation(KraftAdminField::class.java)

        val baseValue = describeType(field.genericType)
        val resolvedValue = applyOverride(baseValue, admin)

        return EmbeddableFieldDescriptor(
            name = field.name,
            label = field.name
                .replace(Regex("([a-z])([A-Z])"), "$1 $2")
                .replaceFirstChar { it.uppercase() },
            required = admin?.required ?: !(column?.nullable ?: true),
            placeholder = "Enter ${field.name}",
            value = resolvedValue
        )
    }

    /**
     * Applies an explicit @KraftAdminField(inputType = ..., fileConfig = ...)
     * override onto an inferred ValueDescriptor. UNSET (or no annotation)
     * leaves the inferred default untouched.
     */
    private fun applyOverride(base: ValueDescriptor, admin: KraftAdminField?): ValueDescriptor {
        if (admin == null || admin.inputType == FormInputType.UNSET) return base

        return base.copy(
            inputType = admin.inputType,
            fileOptions = fileResolver.resolve(admin.inputType, admin.fileConfig)

        )
    }

    /**
     * Default rendering widget for a value type when no explicit
     * @KraftAdminField(inputType = ...) override is present. Kept in sync
     * with FormInputType's intent — file-backed types (IMAGE, FILE, VIDEO,
     * AUDIO, DOCUMENT) are NEVER inferred here, since nothing about a bare
     * String/UUID Java type implies "this is a file" — that always requires
     * an explicit override, same as it does for top-level columns.
     */
    private fun defaultInputTypeFor(valueType: ValueType): FormInputType = when (valueType) {
        ValueType.STRING -> FormInputType.TEXT
        ValueType.INT, ValueType.LONG, ValueType.DOUBLE -> FormInputType.NUMBER
        ValueType.BOOLEAN -> FormInputType.CHECKBOX
        ValueType.UUID -> FormInputType.TEXT
        ValueType.ENUM -> FormInputType.SELECT
        ValueType.EMBEDDABLE -> FormInputType.OBJECT
        ValueType.DATE -> FormInputType.DATE
        ValueType.DATETIME -> FormInputType.DATETIME
        ValueType.COLLECTION -> FormInputType.ARRAY
    }

    private fun mapToValueType(cls: Class<*>): ValueType {
        return when (cls) {
            String::class.java -> ValueType.STRING
            Int::class.java, Integer::class.java -> ValueType.INT
            Long::class.java, java.lang.Long::class.java -> ValueType.LONG
            Double::class.java, java.lang.Double::class.java -> ValueType.DOUBLE
            Boolean::class.java, java.lang.Boolean::class.java -> ValueType.BOOLEAN
            UUID::class.java -> ValueType.UUID
            LocalDate::class.java -> ValueType.DATE
            LocalDateTime::class.java -> ValueType.DATETIME
            else -> ValueType.STRING
        }
    }
}