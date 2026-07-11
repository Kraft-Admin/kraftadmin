package persistence.jpa.metadata

import com.kraftadmin.annotations.KraftAdminField
import com.kraftadmin.enums.FormInputType
import jakarta.persistence.*
import org.slf4j.LoggerFactory
import java.lang.reflect.Field
import java.math.BigDecimal
import java.time.*
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

/**
 * Determines which entity fields are sortable.
 *
 * A field is sortable if ANY of these conditions are true:
 *   1. Annotated with @KraftAdminField(sortable = true) — explicit opt-in
 *   2. Is a number type (Int, Long, Double, Float, BigDecimal, Short)
 *   3. Is a date/time type (LocalDate, LocalDateTime, ZonedDateTime, etc.)
 *   4. Is a String with a @Column(length) less than or equal to SORTABLE_STRING_MAX_LENGTH
 *   5. Is an enum stored as a String (@Enumerated(STRING))
 *   6. Has a FormInputType of TEXT, EMAIL, NUMBER, DATE, DATETIME, or TIME
 *      (derived from @KraftAdminField.inputType)
 *
 * A field is NEVER sortable if:
 *   - It is a collection (OneToMany, ManyToMany, ElementCollection)
 *   - It is an embedded object
 *   - It is a relation (ManyToOne, OneToOne) — sort by the FK ID instead
 *   - It is a LOB (Lob annotation) — too large for DB sorting
 *   - It is static or transient
 *   - Its FormInputType is in the excluded set
 */
object SortableFieldResolver {

    private val logger = LoggerFactory.getLogger(SortableFieldResolver::class.java)

    // Strings up to this length are considered sortable
    private const val SORTABLE_STRING_MAX_LENGTH = 255

    // FormInputTypes that are sortable
    private val SORTABLE_INPUT_TYPES = setOf(
        FormInputType.TEXT,
        FormInputType.EMAIL,
        FormInputType.NUMBER,
        FormInputType.DATE,
        FormInputType.DATETIME,
        FormInputType.TIME
    )

    // FormInputTypes that are explicitly NOT sortable regardless of field type
    private val UNSORTABLE_INPUT_TYPES = setOf(
        FormInputType.TEXTAREA,
        FormInputType.WYSIWYG,
        FormInputType.JSON,
        FormInputType.IMAGE,
        FormInputType.VIDEO,
        FormInputType.AUDIO,
        FormInputType.FILE,
        FormInputType.DOCUMENT,
        FormInputType.ARRAY,
        FormInputType.OBJECT,
        FormInputType.RELATION,
        FormInputType.MULTI_RELATION,
        FormInputType.MULTI_SELECT,
        FormInputType.COLOR,
        FormInputType.RANGE,
        FormInputType.PASSWORD,
        FormInputType.HIDDEN,
        FormInputType.CHECKBOX,
        FormInputType.RADIO,
        FormInputType.SEARCH
    )

    // Java/Kotlin types that are always sortable
    private val NUMERIC_TYPES = setOf(
        Int::class.java, java.lang.Integer::class.java,
        Long::class.java, java.lang.Long::class.java,
        Double::class.java, java.lang.Double::class.java,
        Float::class.java, java.lang.Float::class.java,
        Short::class.java, java.lang.Short::class.java,
        BigDecimal::class.java,
        Number::class.java
    )

    private val DATE_TIME_TYPES = setOf(
        LocalDate::class.java,
        LocalDateTime::class.java,
        ZonedDateTime::class.java,
        OffsetDateTime::class.java,
        Instant::class.java,
        Date::class.java,
        java.sql.Date::class.java,
        java.sql.Timestamp::class.java,
        Calendar::class.java
    )

    /**
     * Returns the set of field names that are safe to use in ORDER BY clauses
     * for the given entity class.
     */
    fun resolveSortableFields(entityClass: KClass<*>): Set<String> {
        return entityClass.memberProperties
            .mapNotNull { prop ->
                val field = prop.javaField ?: return@mapNotNull null
                if (isSortable(field)) prop.name else null
            }
            .toSet()
    }

    /**
     * Returns true if this specific field provider is sortable for the entity.
     * Used by FetchAll to validate incoming sort parameters.
     */
    fun isSortableField(entityClass: KClass<*>, fieldName: String): Boolean {
        val field = entityClass.memberProperties
            .firstOrNull { it.name == fieldName }
            ?.javaField ?: return false
        return isSortable(field)
    }

    fun isSortable(field: Field): Boolean {
        // ─── Always exclude ──────────────────────────────────────────────

        // Static, transient — not a persistent field
        if (java.lang.reflect.Modifier.isStatic(field.modifiers)) return false
        if (field.isAnnotationPresent(Transient::class.java)) return false
        if (field.isAnnotationPresent(jakarta.persistence.Transient::class.java)) return false

        // Collections — unsortable at DB level
        if (field.isAnnotationPresent(OneToMany::class.java)) return false
        if (field.isAnnotationPresent(ManyToMany::class.java)) return false
        if (field.isAnnotationPresent(ElementCollection::class.java)) return false

        // Embedded objects — composite, not sortable as a single column
        if (field.isAnnotationPresent(Embedded::class.java)) return false

        // Relations — sort by the FK column value, not the object reference
        if (field.isAnnotationPresent(ManyToOne::class.java)) return false
        if (field.isAnnotationPresent(OneToOne::class.java)) return false

        // LOBs — typically too large for DB sorting, and many DBs reject it
        if (field.isAnnotationPresent(Lob::class.java)) return false

        // ─── @KraftAdminField evaluation ─────────────────────────────────

        val adminField = field.getAnnotation(KraftAdminField::class.java)

        if (adminField != null) {
            // Explicit sortable = true always wins
            if (adminField.sortable) return true

            // FormInputType takes priority when set and not UNSET
            if (adminField.inputType != FormInputType.UNSET) {
                if (adminField.inputType in UNSORTABLE_INPUT_TYPES) return false
                if (adminField.inputType in SORTABLE_INPUT_TYPES) return true
                // For anything else (SELECT, TEL, URL...) fall through to type-based check
            }
        }

        // ─── Type-based evaluation ────────────────────────────────────────

        val fieldType = field.type

        // Numbers — always sortable
        if (fieldType in NUMERIC_TYPES) return true
        if (Number::class.java.isAssignableFrom(fieldType)) return true

        // Dates and timestamps — always sortable
        if (fieldType in DATE_TIME_TYPES) return true

        // Enums stored as String — sortable
        if (fieldType.isEnum) {
            val enumerated = field.getAnnotation(Enumerated::class.java)
            // Default JPA behavior without @Enumerated stores as ordinal (int) — sortable
            // @Enumerated(STRING) stores as string — sortable
            // Both are fine for sorting
            return true
        }

        // Strings — sortable only if bounded
        if (fieldType == String::class.java || fieldType == java.lang.String::class.java) {
            val column = field.getAnnotation(Column::class.java)
            return if (column != null) {
                // @Column(length = N) — sortable if within threshold
                column.length <= SORTABLE_STRING_MAX_LENGTH
            } else {
                // No @Column annotation — JPA default length is 255, treat as sortable
                true
            }
        }

        // Boolean — not particularly useful to sort, but technically valid
        if (fieldType == Boolean::class.java || fieldType == java.lang.Boolean::class.java) return false

        // UUID — sortable (useful for cursor-based pagination)
        if (fieldType == UUID::class.java) return true

        return false
    }
}