package persistence.jpa.metadata

import com.kraftadmin.annotations.KraftAdminField
import jakarta.persistence.EntityManager
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Lob
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import org.hibernate.Hibernate
import org.hibernate.annotations.CreationTimestamp
import org.slf4j.LoggerFactory
import org.springframework.data.annotation.CreatedDate
import java.lang.reflect.Modifier
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

/**
 * Optimized metadata inspector for JPA entities.
 * Features memoized sort resolution and hierarchical hierarchy scanning.
 */
class EntityMetadata<T : Any>(private val entityClass: KClass<T>) {

    private val logger = LoggerFactory.getLogger(EntityMetadata::class.java)

    // Memoize the sort field to prevent repeated reflection overhead
    val sortableFields: List<String> by lazy { resolveSortableFields() }

    // Cache the searchable fields
    val searchableFields: List<String> by lazy { resolveSearchableFields() }

//    fun findBestSortField(): String? = bestSortField

    // In ResourceDescriptor.from
    val defaultSort : String by lazy { resolveDefaultSortField() }

    // Resolved once and cached — no repeated reflection on every request
//    val sortableFields: Set<String> by lazy {
//        SortableFieldResolver.resolveSortableFields(entityClass)
//    }

    private val PREFERRED_SORT_FIELDS = listOf(
        "createdAt", "updatedAt", "created_at", "updated_at",
        "createdDate", "updatedDate", "timestamp", "date", "id"
    )

    /**
     * Returns the best default sort field — prefers timestamps, falls back
     * to any sortable field, then null (no ordering).
     */
    fun findBestSortField(): String? {
        // Prefer known timestamp fields that exist AND are sortable
        val preferred = PREFERRED_SORT_FIELDS.firstOrNull { it in sortableFields }
        if (preferred != null) return preferred

        // Fall back to the first sortable non-ID field
        return sortableFields.firstOrNull { it != "id" } ?: sortableFields.firstOrNull()
    }

    /**
     * Validates that a caller-supplied sort field is actually sortable.
     * Prevents SQL injection via ORDER BY and avoids sorting on unsortable columns.
     */
    fun validateSortField(fieldName: String): String? {
        return if (SortableFieldResolver.isSortableField(entityClass, fieldName)) {
            fieldName
        } else {
            logger.warn(
                "Sort requested on non-sortable field '{}' for {} — falling back to default.",
                fieldName, entityClass.simpleName
            )
            findBestSortField()
        }
    }

    private fun resolveSearchableFields(): List<String> {
        return entityClass.memberProperties.filter { prop ->
            val field = prop.javaField ?: return@filter false

            // 1. Hard Exclusions (Nothing should pass these)
            if (PropertyResolver.shouldSkip(field)) return@filter false
            if (field.isAnnotationPresent(Transient::class.java) || Modifier.isTransient(field.modifiers)) return@filter false

            val isRelation = field.isAnnotationPresent(OneToOne::class.java) ||
                    field.isAnnotationPresent(OneToMany::class.java) ||
                    field.isAnnotationPresent(ManyToOne::class.java) ||
                    field.isAnnotationPresent(ManyToMany::class.java)
            if (isRelation) return@filter false

            // 2. Logic for Searchability
            val adminField = field.getAnnotation(KraftAdminField::class.java)

            if (adminField != null) {
                // Even if explicitly set to true, we block it if it's not a String or Enum
                // This prevents a developer from accidentally enabling search on a numeric field
                val isSearchableType = field.type == String::class.java || field.type.isEnum
                adminField.searchable && isSearchableType
            } else {
                // Default rule: ONLY Strings and Enums
                field.type == String::class.java || field.type.isEnum
            }
        }
            .map { it.name }
            .take(5)
    }

    /**
     * Helper to determine if a field is valid for sorting based on your strict rules.
     */
    private fun isSortableField(field: java.lang.reflect.Field): Boolean {
        // 1. Gatekeeper Exclusions
        if (PropertyResolver.shouldSkip(field)) return false
        if (field.isAnnotationPresent(Transient::class.java) || Modifier.isTransient(field.modifiers)) return false

        // 2. Relation Exclusions
        val isRelation = field.isAnnotationPresent(OneToOne::class.java) ||
                field.isAnnotationPresent(OneToMany::class.java) ||
                field.isAnnotationPresent(ManyToOne::class.java) ||
                field.isAnnotationPresent(ManyToMany::class.java)
        if (isRelation) return false

        // 3. Type Whitelisting
        val type = field.type
        val isNumber = Number::class.java.isAssignableFrom(type) || (type.isPrimitive && type != Boolean::class.java)
        val isDate = java.time.temporal.Temporal::class.java.isAssignableFrom(type) || java.util.Date::class.java.isAssignableFrom(type)
        val isString = type == String::class.java
        val isStringEnum = type.isEnum && field.getAnnotation(Enumerated::class.java)?.value == EnumType.STRING

        return isNumber || isDate || isString || isStringEnum
    }

    private fun resolveDefaultSortField(): String {
        // 1. Priority: Creation timestamps
        val createAnnos = listOf(CreatedDate::class, CreationTimestamp::class)
        val createNames = listOf("createdAt", "createdDate")

        // Traverse hierarchy: includes current class and all superclasses
        val hierarchy = generateSequence<KClass<*>>(entityClass) { kClass ->
            kClass.supertypes
                .firstOrNull()
                ?.classifier as? KClass<*>
        }.filter { it != Any::class }.toList()

        for (clazz in hierarchy) {
            // Check annotations
            for (anno in createAnnos) {
                clazz.memberProperties.find {
                    val field = it.javaField
                    field != null && field.isAnnotationPresent(anno.java) && isSortableField(field)
                }?.let { return it.name }
            }
            // Check names
            clazz.memberProperties.find {
                val field = it.javaField
                field != null && it.name in createNames && isSortableField(field)
            }?.let { return it.name }
        }

        // 2. Fallback: Numeric fields (excluding 'id')
        for (clazz in hierarchy) {
            clazz.memberProperties.find { prop ->
                val field = prop.javaField ?: return@find false
                val type = field.type
                val isNumber = Number::class.java.isAssignableFrom(type) || (type.isPrimitive && type != Boolean::class.java)
                prop.name != "id" && isNumber && isSortableField(field)
            }?.let { return it.name }
        }

        return "id"
    }

    /**
     * Scans all properties in the hierarchy to identify valid sortable fields.
     */
    private fun resolveSortableFields(): List<String> {
        val hierarchy = generateSequence<KClass<*>>(entityClass) { kClass ->
            kClass.supertypes
                .firstOrNull()
                ?.classifier as? KClass<*>
        }.filter { it != Any::class }.toList()

        val sortableFields = mutableSetOf<String>()

        for (clazz in hierarchy) {
            clazz.memberProperties.forEach { prop ->
                val field = prop.javaField ?: return@forEach

                // Check if field is valid using our helper
                if (isSortableField(field)) {
                    val adminField = field.getAnnotation(KraftAdminField::class.java)

                    // If annotated, respect the 'sortable' flag; otherwise, default to true
                    val isAllowed = adminField?.sortable ?: true

                    if (isAllowed) {
                        sortableFields.add(prop.name)
                    }
                }
            }
        }
        return sortableFields.toList()
    }

    fun ensureLobsInitialized(entity: Any) {
        entity::class.memberProperties.forEach { prop ->
            val field = prop.javaField ?: return@forEach
            val shouldInitialize = field.isAnnotationPresent(Lob::class.java) ||
                    field.isAnnotationPresent(OneToOne::class.java) ||
                    field.isAnnotationPresent(ManyToOne::class.java) ||
                    field.isAnnotationPresent(ManyToMany::class.java) ||
                    field.isAnnotationPresent(OneToMany::class.java)
            if (shouldInitialize) {
                try {
                    field.isAccessible = true
                    val value = field.get(entity)
                    if (value != null) Hibernate.initialize(value)
                } catch (e: Exception) {
                    logger.debug("Could not initialize field ${field.name}: ${e.message}")
                }
            }
        }
    }

    /**
     * Converts a raw value (e.g., from URL) to the ID type required by the JPA Metamodel.
     */
    fun convertId(entityManager: EntityManager, idValue: Any?): Any? {
        if (idValue == null) return null
        val idString = idValue.toString()

        return try {
            val metamodel = entityManager.metamodel
            val entityType = metamodel.entity(entityClass.java)
            val idType = entityType.idType.javaType

            when (idType) {
                UUID::class.java -> UUID.fromString(idString)
                Long::class.java, java.lang.Long::class.java -> idString.toLong()
                Int::class.java, java.lang.Integer::class.java -> idString.toInt()
                else -> idString
            }
        } catch (e: Exception) {
            logger.warn("ID conversion failed for ${entityClass.simpleName}, returning as String: ${e.message}")
            idString
        }
    }
}