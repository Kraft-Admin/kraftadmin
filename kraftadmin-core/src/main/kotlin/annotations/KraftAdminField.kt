package com.kraftadmin.annotations

import annotations.RichTextConfig
import com.kraftadmin.enums.FormInputType

/**
 * Customizes the behavior, appearance, and validation of an entity property.
 * Apply this to fields within a @KraftAdminResource class to override the
 * engine's default inference logic.
 *
 * @property label The display provider for table headers and form labels.
 * @property inputType The UI component type used in forms. When [FormInputType.UNSET],
 * the engine infers the type from the property's JVM class.
 * @property showInTable Forces this field to be displayed in the resource list table.
 * By default, KraftAdmin automatically infers the most useful columns to display.
 * If one or more fields are explicitly marked with `showInTable = true`, only those
 * fields (plus any built-in system columns such as timestamps) will be shown.
 * @property group Logical grouping of fields within the Edit/Create form
 * (e.g. "Contact Info").
 * @property required Backend and frontend validation flag.
 * @property regex Regular expression used for validation.
 * @property validationMessage Custom validation message shown when regex validation fails.
 * @property sensitive If true, the value is masked in the UI and may be omitted
 * from bulk list fetches.
 * @property sortable Enables sorting for this field.
 * @property searchable Enables searching for this field.
 * @property placeholder Placeholder text displayed in form inputs.
 * @property readonly Makes the field visible but not editable.
 * @property displayField Indicates the primary display field for this resource.
 * Used by both JPA (target entity is inferred from the relation's declared type)
 * and MongoDB @DBRef fields (same — type inference works there too).
 * @property referenceTarget MongoDB-only. Declares the target collection name for
 * a manual/plain reference field (e.g. a String customerId with no @DBRef annotation).
 * Not used by JPA — JPA infers the target entity directly from a @ManyToOne/@OneToOne
 * field's declared type, and does not support unmapped foreign-key-style reference
 * fields via this mechanism. Ignored if set on a JPA entity field.
 * @property wysiwygConfig Configuration for WYSIWYG editors.
 * @property fileConfig Configuration for file upload fields.
 */
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY
)
@Retention(AnnotationRetention.RUNTIME)
annotation class KraftAdminField(
    val label: String = "",
    val inputType: FormInputType = FormInputType.UNSET,
    val showInTable: Boolean = false,
    val group: String = "General",
    val required: Boolean = false,
    val regex: String = "",
    val validationMessage: String = "",
    val sensitive: Boolean = false,
    val sortable: Boolean = true,
    val searchable: Boolean = true,
    val placeholder: String = "",
    val readonly: Boolean = false,
    val displayField: Boolean = false,
    val referenceTarget: String = "",
    val wysiwygConfig: RichTextConfig = RichTextConfig(),
    val fileConfig: FileConfig = FileConfig()
)