package com.kraftadmin.ui_descriptors

import com.kraftadmin.spi.SelectOption

/**
 * Describes the container type of a JPA {@code @ElementCollection}.
 *
 * This tells the frontend how the collection itself behaves, while the
 * {@link ValueDescriptor} defines the type of the contained elements.
 *
 * Examples:
 * - LIST -> List<String>, List<Address>
 * - SET  -> Set<UUID>, Set<ProductStatus>
 * - MAP  -> Map<String, Integer>, Map<String, Address>
 */
enum class ElementCollectionShape {

    /** An ordered collection that may contain duplicate elements. */
    LIST,

    /** An unordered collection containing unique elements. */
    SET,

    /** A key-value collection. */
    MAP
}

/**
 * Describes the complete structure of an {@code @ElementCollection}.
 *
 * This metadata allows the frontend to render collection editors without
 * requiring any knowledge of the underlying Java or Kotlin types.
 *
 * Examples:
 *
 * List<String>
 * shape = LIST
 * value.type = STRING
 *
 * Set<ProductStatus>
 * shape = SET
 * value.type = ENUM
 *
 * Map<String, Integer>
 * shape = MAP
 * key.type = STRING
 * value.type = INT
 *
 * Map<String, Address>
 * shape = MAP
 * key.type = STRING
 * value.type = EMBEDDABLE
 */
data class ElementCollectionDescriptor(

    /**
     * The overall collection shape.
     */
    val shape: ElementCollectionShape,

    /**
     * Descriptor for the map key.
     *
     * Only populated when [shape] is [ElementCollectionShape.MAP].
     */
    val key: ValueDescriptor? = null,

    /**
     * Descriptor for the collection element or map value.
     */
    val value: ValueDescriptor,

    /**
     * Optional minimum number of items allowed.
     *
     * May be derived from validation annotations such as
     * {@code @Size(min = ...)}.
     */
    val minItems: Int? = null,

    /**
     * Optional maximum number of items allowed.
     *
     * May be derived from validation annotations such as
     * {@code @Size(max = ...)}.
     */
    val maxItems: Int? = null
)

/**
 * Describes a single value type used throughout KraftAdmin.
 *
 * A ValueDescriptor is reused everywhere:
 *
 * - List elements
 * - Set elements
 * - Map keys
 * - Map values
 * - Fields inside embeddables
 *
 * This provides a single, recursive description model that allows the
 * frontend to render complex object graphs without hardcoded knowledge of
 * backend types.
 */
data class ValueDescriptor1(

    /**
     * High-level value category.
     */
    val type: ValueType,

    /**
     * Fully qualified or simple class name of the underlying Java/Kotlin type.
     *
     * Examples:
     * - String
     * - UUID
     * - ProductStatus
     * - Address
     */
    val className: String? = null,

    /**
     * Whether this value may be null.
     */
    val nullable: Boolean = true,

    /**
     * Optional default value.
     */
    val defaultValue: Any? = null,

    /**
     * Enumeration options.
     *
     * Only populated when [type] is [ValueType.ENUM].
     */
    val enumValues: List<SelectOption> = emptyList(),

    /**
     * Nested fields for embeddable objects.
     *
     * Only populated when [type] is [ValueType.EMBEDDABLE].
     *
     * Since every field itself contains a [ValueDescriptor], embeddables
     * may be nested recursively.
     */
    val fields: List<EmbeddableFieldDescriptor> = emptyList()
)

/**
 * Describes a single field inside an embeddable object.
 *
 * The field's own type is represented by [value], allowing embeddables
 * to contain primitive values, enums, UUIDs, dates, or even other
 * embeddable objects.
 */
data class EmbeddableFieldDescriptor(

    /**
     * Property name in the embeddable class.
     */
    val name: String,

    /**
     * Human-readable label displayed by the UI.
     */
    val label: String,

    /**
     * Whether this field is required.
     */
    val required: Boolean,

    /**
     * Optional placeholder shown by text-based inputs.
     */
    val placeholder: String?,

    /**
     * Descriptor of the field's value type.
     */
    val value: ValueDescriptor
)
enum class ValueType {
    STRING,
    INT,
    LONG,
    DOUBLE,
    BOOLEAN,
    UUID,
    ENUM,
    EMBEDDABLE,
    DATE,
    DATETIME,
    COLLECTION
}

data class ValueDescriptor(

    val type: ValueType,

    val className: String? = null,

    val nullable: Boolean = true,

    val defaultValue: Any? = null,

    val enumValues: List<SelectOption> = emptyList(),

    val fields: List<EmbeddableFieldDescriptor> = emptyList(),

    val collection: ElementCollectionDescriptor? = null,

    /**
     * How the frontend should render this value. Always populated — either
     * from an explicit @KraftAdminField(inputType = ...) override on the
     * owning field, or inferred from [type] via a sensible default. The
     * frontend should never need to guess a widget from [type] alone.
     */
    val inputType: com.kraftadmin.enums.FormInputType? = null,

    /**
     * Upload constraints, populated only when [inputType] is one of the
     * file-backed types (IMAGE, FILE, VIDEO, AUDIO, DOCUMENT).
     */
    val fileOptions: FileConfigDescriptor? = null,

)