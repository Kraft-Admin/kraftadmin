package discovery.descriptors.column.jpa

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

/**
 * Resolves the complete structure of an @ElementCollection.
 *
 * The produced descriptor is recursive, meaning embeddables may themselves
 * contain collections, which in turn contain embeddables, etc.
 */
object ElementCollectionResolver {

    fun resolve(field: Field): ElementCollectionDescriptor? {
        if (!field.isAnnotationPresent(ElementCollection::class.java)) {
            return null
        }

        return describeCollection(field.genericType)
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
     * Describes any value.
     *
     * This may be:
     *
     * - primitive
     * - enum
     * - embeddable
     * - another collection
     */
    private fun describeType(type: Type): ValueDescriptor {

        //
        // Nested collections
        //
        if (type is ParameterizedType) {

            val collection = describeCollection(type)

            if (collection != null) {
                return ValueDescriptor(
                    type = ValueType.COLLECTION,
                    className = (type.rawType as Class<*>).simpleName,
                    collection = collection
                )
            }
        }

        val cls = (type as? Class<*>) ?: String::class.java

        //
        // Embeddable
        //
        if (cls.isAnnotationPresent(Embeddable::class.java)) {

            return ValueDescriptor(
                type = ValueType.EMBEDDABLE,
                className = cls.simpleName,
                fields = cls.declaredFields
                    .filter { !Modifier.isStatic(it.modifiers) }
                    .map { field -> describeEmbeddableField(field) }
            )
        }

        //
        // Enum
        //
        if (cls.isEnum) {

            return ValueDescriptor(
                type = ValueType.ENUM,
                className = cls.simpleName,
                enumValues = cls.enumConstants
                    .map {
                        SelectOption(
                            label = it.toString(),
                            value = it.toString()
                        )
                    }
            )
        }

        //
        // Primitive / scalar
        //
        return ValueDescriptor(
            type = mapToValueType(cls),
            className = cls.simpleName
        )
    }

    private fun describeEmbeddableField(field: Field): EmbeddableFieldDescriptor {

        val column = field.getAnnotation(Column::class.java)

        return EmbeddableFieldDescriptor(

            name = field.name,

            label = field.name
                .replace(Regex("([a-z])([A-Z])"), "$1 $2")
                .replaceFirstChar { it.uppercase() },

            required = !(column?.nullable ?: true),

            placeholder = "Enter ${field.name}",

            value = describeType(field.genericType)
        )
    }

    private fun mapToValueType(cls: Class<*>): ValueType {

        return when (cls) {

            String::class.java ->
                ValueType.STRING

            Int::class.java,
            Integer::class.java ->
                ValueType.INT

            Long::class.java,
            java.lang.Long::class.java ->
                ValueType.LONG

            Double::class.java,
            java.lang.Double::class.java ->
                ValueType.DOUBLE

            Boolean::class.java,
            java.lang.Boolean::class.java ->
                ValueType.BOOLEAN

            UUID::class.java ->
                ValueType.UUID

            LocalDate::class.java ->
                ValueType.DATE

            LocalDateTime::class.java ->
                ValueType.DATETIME

            else ->
                ValueType.STRING
        }
    }
}