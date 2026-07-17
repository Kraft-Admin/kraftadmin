//package persistence.jpa.metadata
//
//import com.kraftadmin.annotations.KraftAdminField
//import jakarta.persistence.Entity
//import jakarta.persistence.EntityManager
//import jakarta.persistence.EnumType
//import jakarta.persistence.Enumerated
//import jakarta.persistence.Lob
//import jakarta.persistence.ManyToMany
//import jakarta.persistence.ManyToOne
//import jakarta.persistence.OneToMany
//import jakarta.persistence.OneToOne
//import org.hibernate.Hibernate
//import org.hibernate.annotations.CreationTimestamp
//import org.slf4j.LoggerFactory
//import org.springframework.data.annotation.CreatedDate
//import java.lang.reflect.Field
//import java.lang.reflect.Modifier
//import java.util.UUID
//import kotlin.reflect.KClass
//import kotlin.reflect.full.memberProperties
//import kotlin.reflect.jvm.javaField
//
///**
// * Optimized metadata inspector for JPA entities.
// * Features memoized sort resolution and hierarchical hierarchy scanning.
// */
//class EntityMetadata<T : Any>(private val entityClass: KClass<T>) {
//
//    private val logger = LoggerFactory.getLogger(EntityMetadata::class.java)
//
////    private val hierarchy by lazy(LazyThreadSafetyMode.PUBLICATION) {
////        generateSequence(entityClass.java) { it.superclass }
////            .takeWhile { it != Any::class.java }
////            .toList()
////    }
//
//    private val hierarchy: List<Class<*>> by lazy(LazyThreadSafetyMode.PUBLICATION) {
//        generateSequence<Class<*>>(entityClass.java) { clazz ->
//            clazz.superclass
//        }
//            .takeWhile { it != Any::class.java }
//            .toList()
//    }
//
//    private val fields by lazy(LazyThreadSafetyMode.PUBLICATION) {
//        hierarchy.flatMap { it.declaredFields.asList() }
//            .onEach { it.isAccessible = true }
//    }
//
//    private val properties by lazy(LazyThreadSafetyMode.PUBLICATION) {
//        hierarchy
//            .flatMap { it.kotlin.memberProperties }
//            .associateBy { it.name }
//    }
//
//    private val relationFields by lazy(LazyThreadSafetyMode.PUBLICATION) {
//        fields.filter {
//            it.isAnnotationPresent(OneToOne::class.java) ||
//                    it.isAnnotationPresent(OneToMany::class.java) ||
//                    it.isAnnotationPresent(ManyToOne::class.java) ||
//                    it.isAnnotationPresent(ManyToMany::class.java)
//        }
//    }
//
//    private val lobFields by lazy(LazyThreadSafetyMode.PUBLICATION) {
//        fields.filter {
//            it.isAnnotationPresent(Lob::class.java)
//        }
//    }
//
//    private val initializationFields by lazy(LazyThreadSafetyMode.PUBLICATION) {
//        (lobFields + relationFields).distinct()
//    }
//    val idType by lazy(LazyThreadSafetyMode.PUBLICATION) {
//        fields.firstOrNull {
//            it.isAnnotationPresent(jakarta.persistence.Id::class.java) ||
//                    it.isAnnotationPresent(org.springframework.data.annotation.Id::class.java)
//        }?.type ?: String::class.java
//    }
//
////    private val fields by lazy(LazyThreadSafetyMode.PUBLICATION) {
////        hierarchy.flatMap { it.declaredFields.asList() }
////    }
//
////    init {
////        logger.info("received entityClass ${entityClass}")
////    }
//
//    val idField: String by lazy { resolveIdField() }
//
//    val resourceName: String by lazy(LazyThreadSafetyMode.PUBLICATION) {
//        entityClass.java.getAnnotation(Entity::class.java)
//            ?.name
//            ?.takeIf(String::isNotBlank)
//            ?: entityClass.simpleName
//            ?: "UnknownResource"
//    }
//
//    val displayField: String by lazy { resolveDisplayField() }
//
//    private fun resolveIdField1(): String {
//        val hierarchy = generateSequence<KClass<*>>(entityClass) { cls ->
//            cls.supertypes.firstOrNull()?.classifier as? KClass<*>
//        }.filter { it != Any::class }.toList()
//
//        for (clazz in hierarchy) {
//            clazz.memberProperties.firstOrNull {
//                val field = it.javaField
//                field?.isAnnotationPresent(jakarta.persistence.Id::class.java) == true ||
//                        field?.isAnnotationPresent(org.springframework.data.annotation.Id::class.java) == true
//            }?.let { return it.name }
//        }
//
//        // Throw if no @Id found, as requested
//        throw IllegalStateException("Entity ${entityClass.simpleName} must have a field annotated with @Id")
//    }
//
//    private fun resolveIdField(): String =
//        properties.values.firstOrNull {
//            val field = it.javaField ?: return@firstOrNull false
//            field.isAnnotationPresent(jakarta.persistence.Id::class.java) ||
//                    field.isAnnotationPresent(org.springframework.data.annotation.Id::class.java)
//        }?.name
//            ?: error("Entity ${entityClass.simpleName} must declare an @Id")
//
//    // resolves display field by looking at displayField bool value
//    private fun resolveDisplayField(): String {
//
//        val hierarchy = generateSequence<Class<*>>(entityClass.java) { it.superclass }
//            .takeWhile { it != Any::class.java }
//
//        val displayFields = mutableListOf<String>()
//
//        hierarchy.forEach { clazz ->
//            clazz.declaredFields.forEach { field ->
//                val admin = field.getAnnotation(KraftAdminField::class.java)
//
//                if (admin?.displayField == true) {
//                    displayFields += field.name
//                }
//            }
//        }
//
//        return when {
//            displayFields.isEmpty() -> idField
//
//            displayFields.size == 1 -> displayFields.first()
//
//            else -> throw IllegalStateException(
//                buildString {
//                    append("Entity '${entityClass.simpleName}' has multiple @KraftAdminField(displayField = true) fields: ")
//                    append(displayFields.joinToString(", "))
//                    append(". Only one display field is allowed.")
//                }
//            )
//        }
//    }
//
//    // Memoize the sort field to prevent repeated reflection overhead
//    val sortableFields: List<String> by lazy { resolveSortableFields() }
//
//    // Cache the searchable fields
//    val searchableFields: List<String> by lazy { resolveSearchableFields() }
//
//    // In ResourceDescriptor.from
//    val defaultSort : String by lazy { resolveDefaultSortField() }
//
//
//    private val PREFERRED_SORT_FIELDS = listOf(
//        "createdAt", "updatedAt", "created_at", "updated_at",
//        "createdDate", "updatedDate", "timestamp", "date", "id"
//    )
//
//    /**
//     * Returns the best default sort field — prefers timestamps, falls back
//     * to any sortable field, then null (no ordering).
//     */
//    fun findBestSortField(): String? {
//        // Prefer known timestamp fields that exist AND are sortable
//        val preferred = PREFERRED_SORT_FIELDS.firstOrNull { it in sortableFields }
//        if (preferred != null) return preferred
//
//        // Fall back to the first sortable non-ID field
//        return sortableFields.firstOrNull { it != "id" } ?: sortableFields.firstOrNull()
//    }
//
//    /**
//     * Validates that a caller-supplied sort field is actually sortable.
//     * Prevents SQL injection via ORDER BY and avoids sorting on unsortable columns.
//     */
//    fun validateSortField(fieldName: String): String? {
//        return if (SortableFieldResolver.isSortableField(entityClass, fieldName)) {
//            fieldName
//        } else {
//            logger.warn(
//                "Sort requested on non-sortable field '{}' for {} — falling back to default.",
//                fieldName, entityClass.simpleName
//            )
//            findBestSortField()
//        }
//    }
//
//    private fun resolveSearchableFields(): List<String> {
//        return entityClass.memberProperties.filter { prop ->
//            val field = prop.javaField ?: return@filter false
//
//            // 1. Hard Exclusions (Nothing should pass these)
//            if (PropertyResolver.shouldSkip(field)) return@filter false
//            if (field.isAnnotationPresent(Transient::class.java) || Modifier.isTransient(field.modifiers)) return@filter false
//
//            val isRelation = field.isAnnotationPresent(OneToOne::class.java) ||
//                    field.isAnnotationPresent(OneToMany::class.java) ||
//                    field.isAnnotationPresent(ManyToOne::class.java) ||
//                    field.isAnnotationPresent(ManyToMany::class.java)
//            if (isRelation) return@filter false
//
//            // 2. Logic for Searchability
//            val adminField = field.getAnnotation(KraftAdminField::class.java)
//
//            if (adminField != null) {
//                // Even if explicitly set to true, we block it if it's not a String or Enum
//                // This prevents a developer from accidentally enabling search on a numeric field
//                val isSearchableType = field.type == String::class.java || field.type.isEnum
//                adminField.searchable && isSearchableType
//            } else {
//                // Default rule: ONLY Strings and Enums
//                field.type == String::class.java || field.type.isEnum
//            }
//        }
//            .map { it.name }
//            .take(5)
//    }
//
//    /**
//     * Helper to determine if a field is valid for sorting based on your strict rules.
//     */
//    private fun isSortableField(field: Field): Boolean {
//        // 1. Gatekeeper Exclusions
//        if (PropertyResolver.shouldSkip(field)) return false
//        if (field.isAnnotationPresent(Transient::class.java) || Modifier.isTransient(field.modifiers)) return false
//
//        // 2. Relation Exclusions
//        val isRelation = field.isAnnotationPresent(OneToOne::class.java) ||
//                field.isAnnotationPresent(OneToMany::class.java) ||
//                field.isAnnotationPresent(ManyToOne::class.java) ||
//                field.isAnnotationPresent(ManyToMany::class.java)
//        if (isRelation) return false
//
//        // 3. Type Whitelisting
//        val type = field.type
//        val isNumber = Number::class.java.isAssignableFrom(type) || (type.isPrimitive && type != Boolean::class.java)
//        val isDate = java.time.temporal.Temporal::class.java.isAssignableFrom(type) || java.util.Date::class.java.isAssignableFrom(type)
//        val isString = type == String::class.java
//        val isStringEnum = type.isEnum && field.getAnnotation(Enumerated::class.java)?.value == EnumType.STRING
//
//        return isNumber || isDate || isString || isStringEnum
//    }
//
//    private fun resolveDefaultSortField(): String {
//        // 1. Priority: Creation timestamps
//        val createAnnos = listOf(CreatedDate::class, CreationTimestamp::class)
//        val createNames = listOf("createdAt", "createdDate")
//
//        // Traverse hierarchy: includes current class and all superclasses
//        val hierarchy = generateSequence<KClass<*>>(entityClass) { kClass ->
//            kClass.supertypes
//                .firstOrNull()
//                ?.classifier as? KClass<*>
//        }.filter { it != Any::class }.toList()
//
//        for (clazz in hierarchy) {
//            // Check annotations
//            for (anno in createAnnos) {
//                clazz.memberProperties.find {
//                    val field = it.javaField
//                    field != null && field.isAnnotationPresent(anno.java) && isSortableField(field)
//                }?.let { return it.name }
//            }
//            // Check names
//            clazz.memberProperties.find {
//                val field = it.javaField
//                field != null && it.name in createNames && isSortableField(field)
//            }?.let { return it.name }
//        }
//
//        // 2. Fallback: Numeric fields (excluding 'id')
//        for (clazz in hierarchy) {
//            clazz.memberProperties.find { prop ->
//                val field = prop.javaField ?: return@find false
//                val type = field.type
//                val isNumber = Number::class.java.isAssignableFrom(type) || (type.isPrimitive && type != Boolean::class.java)
//                prop.name != "id" && isNumber && isSortableField(field)
//            }?.let { return it.name }
//        }
//
//        return idField
//    }
//
//    /**
//     * Scans all properties in the hierarchy to identify valid sortable fields.
//     */
//    private fun resolveSortableFields(): List<String> {
//        val hierarchy = generateSequence<KClass<*>>(entityClass) { kClass ->
//            kClass.supertypes
//                .firstOrNull()
//                ?.classifier as? KClass<*>
//        }.filter { it != Any::class }.toList()
//
//        val sortableFields = mutableSetOf<String>()
//
//        for (clazz in hierarchy) {
//            clazz.memberProperties.forEach { prop ->
//                val field = prop.javaField ?: return@forEach
//
//                // Check if field is valid using our helper
//                if (isSortableField(field)) {
//                    val adminField = field.getAnnotation(KraftAdminField::class.java)
//
//                    // If annotated, respect the 'sortable' flag; otherwise, default to true
//                    val isAllowed = adminField?.sortable ?: true
//
//                    if (isAllowed) {
//                        sortableFields.add(prop.name)
//                    }
//                }
//            }
//        }
//        return sortableFields.toList()
//    }
//
//    fun ensureLobsInitialized1(entity: Any) {
//        entity::class.memberProperties.forEach { prop ->
//            val field = prop.javaField ?: return@forEach
//            val shouldInitialize = field.isAnnotationPresent(Lob::class.java) ||
//                    field.isAnnotationPresent(OneToOne::class.java) ||
//                    field.isAnnotationPresent(ManyToOne::class.java) ||
//                    field.isAnnotationPresent(ManyToMany::class.java) ||
//                    field.isAnnotationPresent(OneToMany::class.java)
//            if (shouldInitialize) {
//                try {
//                    field.isAccessible = true
//                    val value = field.get(entity)
//                    if (value != null) Hibernate.initialize(value)
//                } catch (e: Exception) {
//                    logger.debug("Could not initialize field ${field.name}: ${e.message}")
//                }
//            }
//        }
//    }
//
//    fun ensureLobsInitialized(entity: Any) {
//        initializationFields.forEach { field ->
//            try {
//                Hibernate.initialize(field.get(entity))
//            } catch (_: Exception) {
//            }
//        }
//    }
//
//    /**
//     * Converts a raw value (e.g., from URL) to the ID type required by the JPA Metamodel.
//     */
//    fun convertId1(entityManager: EntityManager, idValue: Any?): Any? {
//        if (idValue == null) return null
//        val idString = idValue.toString()
//
//        return try {
//            val metamodel = entityManager.metamodel
//            val entityType = metamodel.entity(entityClass.java)
//            val idType = entityType.idType.javaType
//
//            when (idType) {
//                UUID::class.java -> UUID.fromString(idString)
//                Long::class.java, java.lang.Long::class.java -> idString.toLong()
//                Int::class.java, java.lang.Integer::class.java -> idString.toInt()
//                else -> idString
//            }
//        } catch (e: Exception) {
//            logger.warn("ID conversion failed for ${entityClass.simpleName}, returning as String: ${e.message}")
//            idString
//        }
//    }
//
//    fun convertId(idValue: Any?): Any? {
//        if (idValue == null) return null
//
//        val value = idValue.toString()
//
//        return when (idType) {
//            UUID::class.java -> UUID.fromString(value)
//            Long::class.java, java.lang.Long::class.java -> value.toLong()
//            Int::class.java, java.lang.Integer::class.java -> value.toInt()
//            Short::class.java, java.lang.Short::class.java -> value.toShort()
//            Byte::class.java, java.lang.Byte::class.java -> value.toByte()
//            else -> value
//        }
//    }
//
//}


package persistence.jpa.metadata

import com.kraftadmin.annotations.KraftAdminField
import jakarta.persistence.*
import org.hibernate.Hibernate
import org.hibernate.annotations.CreationTimestamp
import org.slf4j.LoggerFactory
import org.springframework.data.annotation.CreatedDate
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

/**
 * Optimized metadata inspector for JPA entities.
 * All heavy reflection is performed once during initialization.
 */
class EntityMetadata<T : Any>(private val entityClass: KClass<T>) {

    private val logger = LoggerFactory.getLogger(EntityMetadata::class.java)

    //  Eagerly cache the class hierarchy and fields
    private val allFields: List<Field> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        // Use Class<*> to explicitly handle the hierarchy traversal
        val startClass: Class<*> = entityClass.java

        generateSequence(startClass) { it.superclass }
            .takeWhile { it != Any::class.java }
            .flatMap { it.declaredFields.toList() }
            .onEach { it.isAccessible = true }
            .toList()
    }

    val versionField: Field? by lazy {
        allFields.firstOrNull {
            it.isAnnotationPresent(Version::class.java)
        }
    }

    val versioningEnabled: Boolean
        get() = versionField != null

    //  Pre-filter fields by category for O(1) retrieval during runtime
    private val idFieldInternal: Field by lazy {
        allFields.find { it.isAnnotationPresent(Id::class.java) || it.isAnnotationPresent(org.springframework.data.annotation.Id::class.java) }
            ?: error("Entity ${entityClass.simpleName} must have a field annotated with @Id")
    }

    //  Cached public properties
    val entityName : String by lazy {
        entityClass.java.getAnnotation(Entity::class.java)?.name?.takeIf { it.isNotBlank() }
            ?: entityClass.simpleName ?: "UnknownResource"
    }

    val idField: String = idFieldInternal.name
    val idType: Class<*> = idFieldInternal.type

    val displayField: String by lazy {
        val display = allFields.filter { it.getAnnotation(KraftAdminField::class.java)?.displayField == true }
        when {
            display.isEmpty() -> idField
            display.size == 1 -> display.first().name
            else -> error("Multiple display fields in ${entityClass.simpleName}: ${display.map { it.name }}")
        }
    }

    val sortableFields: List<String> by lazy {
        allFields.filter { isSortable(it) && (it.getAnnotation(KraftAdminField::class.java)?.sortable ?: true) }
            .map { it.name }.distinct()
    }

    val searchableFields: List<String> by lazy {
        allFields.filter { isSearchable(it) }
            .map { it.name }.take(5)
    }

    val defaultSort: String by lazy { resolveDefaultSort() }

    // --- High-Performance Logic ---
    fun convertId(idValue: Any?): Any? {
        val value = idValue?.toString() ?: return null
        return try {
            when (idType) {
                UUID::class.java -> UUID.fromString(value)
                Long::class.java, java.lang.Long::class.java -> value.toLong()
                Int::class.java, java.lang.Integer::class.java -> value.toInt()
                Short::class.java, java.lang.Short::class.java -> value.toShort()
                Byte::class.java, java.lang.Byte::class.java -> value.toByte()
                else -> value
            }
        } catch (e: Exception) { value }
    }

    // --- Private Helpers ---

    private fun isRelation(f: Field) = f.isAnnotationPresent(OneToOne::class.java) ||
            f.isAnnotationPresent(OneToMany::class.java) || f.isAnnotationPresent(ManyToOne::class.java) ||
            f.isAnnotationPresent(ManyToMany::class.java)

    private fun isSearchable(f: Field): Boolean {
        if (PropertyResolver.shouldSkip(f) || Modifier.isTransient(f.modifiers) || isRelation(f)) return false
        val admin = f.getAnnotation(KraftAdminField::class.java)
        val isTypeValid = f.type == String::class.java || f.type.isEnum
        return if (admin != null) admin.searchable && isTypeValid else isTypeValid
    }

    private fun isSortable(f: Field): Boolean {
        if (PropertyResolver.shouldSkip(f) || Modifier.isTransient(f.modifiers) || isRelation(f)) return false
        val t = f.type
        val isNumber = Number::class.java.isAssignableFrom(t) || (t.isPrimitive && t != Boolean::class.java)
        val isDate = java.time.temporal.Temporal::class.java.isAssignableFrom(t) || java.util.Date::class.java.isAssignableFrom(t)
        return isNumber || isDate || t == String::class.java || (t.isEnum && f.getAnnotation(Enumerated::class.java)?.value == EnumType.STRING)
    }

    private fun resolveDefaultSort(): String {
        val createAnnos = listOf(CreatedDate::class.java, CreationTimestamp::class.java)
        val createNames = setOf("createdAt", "createdDate", "created_at")

        allFields.find { f -> createAnnos.any { f.isAnnotationPresent(it) } && isSortable(f) }?.let { return it.name }
        allFields.find { f -> f.name in createNames && isSortable(f) }?.let { return it.name }
        allFields.find { f -> f.name != idField && Number::class.java.isAssignableFrom(f.type) && isSortable(f) }?.let { return it.name }

        return idField
    }

    /**
     * Forces initialization of ONLY @Lob fields. This exists to prevent
     * LazyInitializationException when a LOB is streamed/serialized AFTER
     * the transaction/session closes. It must never touch relation fields —
     * that was the source of a severe N+1 (one query per relation per row,
     * regardless of whether the relation is shown).
     */
    fun ensureLobsInitialized(entity: Any) {
        entity::class.memberProperties.forEach { prop ->
            val field = prop.javaField ?: return@forEach
            if (!field.isAnnotationPresent(Lob::class.java)) return@forEach

            try {
                field.isAccessible = true
                val value = field.get(entity)
                if (value != null) Hibernate.initialize(value)
            } catch (e: Exception) {
                logger.debug("Could not initialize LOB field ${field.name}: ${e.message}")
            }
        }
    }

    /**
     * Forces initialization of specific relation fields, by name, while the
     * session is still open. Used by FetchById (detail view) to resolve the
     * ManyToOne/OneToOne relations actually needed for that entity — never
     * called in list-view row mapping, and never touches @OneToMany/@ManyToMany
     * (those go through RelatedResourceFetcher's own bounded query instead).
     */
    fun ensureSingleRelationsInitialized(entity: Any) {
        entity::class.memberProperties.forEach { prop ->
            val field = prop.javaField ?: return@forEach

            val isSingleRelation = field.isAnnotationPresent(ManyToOne::class.java) ||
                    field.isAnnotationPresent(OneToOne::class.java)
            if (!isSingleRelation) return@forEach

            try {
                field.isAccessible = true
                val value = field.get(entity)
                if (value != null) Hibernate.initialize(value)
            } catch (e: Exception) {
                logger.debug("Could not initialize relation ${field.name}: ${e.message}")
            }
        }
    }

}