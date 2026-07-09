//package persistence.jpa.provider
//
//import api.responses.PagedResponse
//import api.utils.EmbeddedResponse
//import api.utils.ObjectResponse
//import api.utils.ResourceRow
//import api.utils.RowMetadata
//import com.fasterxml.jackson.annotation.JsonIgnore
//import com.kraftadmin.annotations.KraftAdminField
//import security.AdminUserDTO
//import logging.KraftLogAction
//import security.SecurityProviderChain
//import com.kraftadmin.spi.KraftAdminColumn
//import com.kraftadmin.spi.KraftDataProvider
//import com.kraftadmin.ui_descriptors.LookupDescriptor
//import com.kraftadmin.utils.files.AdminStorageProvider
//import config.KraftPulseSpringKraftAdminProperties
//import jakarta.persistence.ElementCollection
//import jakarta.persistence.Embedded
//import jakarta.persistence.EntityManager
//import jakarta.persistence.Id
//import jakarta.persistence.Lob
//import jakarta.persistence.ManyToMany
//import jakarta.persistence.ManyToOne
//import jakarta.persistence.OneToMany
//import jakarta.persistence.OneToOne
//import jakarta.persistence.metamodel.EntityType
//import logging.KraftAdminAuditor
//import telemetry.KraftTelemetryService
//import org.hibernate.Hibernate
//import org.hibernate.annotations.CreationTimestamp
//import org.hibernate.proxy.HibernateProxy
//import org.slf4j.LoggerFactory
//import org.springframework.data.annotation.CreatedDate
//import org.springframework.transaction.support.TransactionTemplate
//import persistence.jpa.mapper.EntityMapper
//import telemetry.KraftPulse
//import java.lang.reflect.Modifier
//import java.time.LocalDate
//import java.time.LocalDateTime
//import java.time.LocalTime
//import java.time.ZonedDateTime
//import java.util.*
//import kotlin.collections.get
//import kotlin.jvm.Transient
//import kotlin.jvm.java
//import kotlin.jvm.javaClass
//import kotlin.reflect.KClass
//import kotlin.reflect.KMutableProperty
//import kotlin.reflect.full.isSubclassOf
//import kotlin.reflect.full.memberProperties
//import kotlin.reflect.jvm.isAccessible
//import kotlin.reflect.jvm.javaField
//
//
//class JpaDataProvider<T : Any>(
//    private val entityManager: EntityManager,
//    private val entityClass: KClass<T>,
//    private val transactionTemplate: TransactionTemplate,
//    private val adminStorageProvider: AdminStorageProvider? = null,
//    private val kraftAdminAuditor: KraftAdminAuditor,
//    private val securityChain: SecurityProviderChain,
//    private val properties: KraftPulseSpringKraftAdminProperties,
//    private val telemetryService: KraftTelemetryService
//) : KraftDataProvider<T> {
//
//    val logger = LoggerFactory.getLogger(KraftDataProvider::class.java)
//    private val mapper = EntityMapper(entityManager)
////    private val queryBuilder = JpaQueryBuilder(entityManager, entityClass)
//
//    private fun ensureLobsInitialized(entity: Any) {
//        entity::class.memberProperties.forEach { prop ->
//            val field = prop.javaField ?: return@forEach
//            if (field.isAnnotationPresent(OneToOne::class.java) ||
//                field.isAnnotationPresent(ManyToOne::class.java) ||
//                field.isAnnotationPresent(Lob::class.java)) {
//                field.isAccessible = true
//                val v = field.get(entity)
//                if (v != null) Hibernate.initialize(v)
//            }
//        }
//    }
//
//
//    private fun ensureLobsInitialized1(entity: Any?) {
//        if (entity == null) return
//
//        // Scan for @Lob fields and "touch" them to trigger the stream load
//        entity.javaClass.declaredFields.forEach { field ->
//            // Using standard reflection check or your KraftAnnotationUtils
//            if (field.isAnnotationPresent(Lob::class.java)) {
//                try {
//                    field.isAccessible = true
//                    val value = field.get(entity)
//                    // Just accessing the value (calling toString or length)
//                    // triggers the OID streaming while the transaction is open.
//                    value?.toString()
//                } catch (e: Exception) {
//                    logger.warn("Could not pre-initialize LOB field: ${field.name}")
//                }
//            }
//        }
//    }
//
//    override fun fetchAll(
//        page: Int,
//        size: Int,
//        columns: List<KraftAdminColumn>
//    ): PagedResponse<ResourceRow> {
//        val config = properties.pagination
//        val effectivePage = page.coerceAtLeast(1)
//        val limit = size.coerceAtLeast(1).coerceAtMost(config.maxPageSize)
//        val offset = (effectivePage - 1) * limit
//
//        val response = transactionTemplate.execute { status ->
//            try {
//                val cb = entityManager.criteriaBuilder
//
//                // --- 1. Total Count ---
//                val countQuery = cb.createQuery(Long::class.java)
//                countQuery.select(cb.count(countQuery.from(entityClass.java)))
//                val total = entityManager.createQuery(countQuery).singleResult
//
//                // --- 2. Build the Data Query ---
//                val selectQuery = cb.createQuery(entityClass.java)
//                val root = selectQuery.from(entityClass.java)
//
//                // DYNAMIC SORTING
//                val sortField = findBestSortField()
//                if (sortField != null) {
//                    // CRITICAL: This MUST happen before createQuery()
//                    selectQuery.orderBy(cb.desc(root.get<Any>(sortField)))
//                }
//
//                // --- 3. Create the Executable TypedQuery ---
//                val typedQuery = entityManager.createQuery(selectQuery)
//
//                typedQuery.firstResult = offset
//                typedQuery.maxResults = limit
//
//                val rows = typedQuery.resultList.map { entity ->
//                    ensureLobsInitialized(entity)
//                    // Unproxy the root entity
//                    val realEntity = unproxy(entity) ?: entity
//                    mapToRow(realEntity)
////                    mapper.toRow(realEntity)
//                }
//
////                typedQuery.resultList.map { mapper.toRow(it) }
//
//                val totalPages = if (total == 0L) 0 else Math.ceil(total.toDouble() / limit).toInt()
//
//                PagedResponse(rows, total, effectivePage, limit, totalPages)
//            } catch (e: Exception) {
//                logger.error("DB Pagination Error: ${e.message}", e)
//                status.setRollbackOnly()
//                PagedResponse(emptyList(), 0, effectivePage, limit, 0)
//            }
//        }
////            ?: PagedResponse(emptyList(), 0, effectivePage, limit, 0)
//
//        return response!!
//    }
//
//    /**
//     * Finds the field name annotated with common creation timestamp annotations.
//     */
//    private val cachedSortField: String? by lazy {
//        findFieldInHierarchy(entityClass.java)
//    }
//
//    private fun findFieldInHierarchy(clazz: Class<*>): String? {
//        logger.info("finding best sort field for ${entityClass.java.simpleName}")
//
//        val fieldName = clazz.declaredFields.find { field ->
//            field.isAnnotationPresent(CreationTimestamp::class.java) ||
//                    field.isAnnotationPresent(CreatedDate::class.java) ||
//                    field.name == "createdAt" ||
//                    field.name == "createdDate"
//        }?.name
//
//        if (fieldName != null) return fieldName
//
//        //  If not found and there's a superclass, recurse
//        val superclass = clazz.superclass
//        if (superclass != null && superclass != Any::class.java) {
//            return findFieldInHierarchy(superclass)
//        }
//
//        return null
//    }
//
//    private fun findBestSortField(): String? = cachedSortField
//
//    // fetch resource using its id
//    override fun fetchById(id: String, columns: List<KraftAdminColumn>): ResourceRow? {
//        return transactionTemplate.execute { status ->
//            try {
//                val entity = entityManager.find(entityClass.java, convertId(id))
//
//                if (entity != null) {
//                    ensureLobsInitialized(entity)
//                    mapToRow(unproxy(entity) ?: entity)
//                } else null
//            } catch (e: Exception) {
//                // SILENTLY RECORD: Capture the exception, trigger, and context
////                KraftPulse.recordException(e, mapOf(
////                    "entity" to entityClass.simpleName,
////                    "id" to id,
////                    "source" to "fetchById"
////                ))
//
//                KraftPulse.recordException(e, mapOf(
//                    "entity" to entityClass.simpleName,
//                    "resource_id" to id,
//                    "operation" to "fetchById"
//                ))
//
//                //  RETHROW: Allow the Parent App or our Global Handler to see it
//                throw e
//            }
//        }
//    }
//
//    override fun delete(id: String) {
//        transactionTemplate.execute { status ->
//            try {
//                val convertedId = convertId(id)
//                val entity = entityManager.find(entityClass.java, convertedId)
//
//                if (entity != null) {
//                    cleanupEntityFiles(entity)
//
//                    //  Record the audit BEFORE the delete (while we still have the entity)
//                    kraftAdminAuditor.record(
//                        action = KraftLogAction.DELETE,
//                        resource = entityClass.simpleName ?: "Unknown",
//                        id = id,
//                        actor = getCurrentUser()!!
//                    )
//
//                    entityManager.remove(entity)
//                    // Explicitly flush if you want to catch constraint violations
//                    // (like foreign key errors) within this block
//                    entityManager.flush()
//                    logger.info("Deleted ${entityClass.simpleName} with id: $id")
//                } else {
//                    logger.warn("Delete skipped: ${entityClass.simpleName} with id $id not found")
//                }
//            } catch (e: Exception) {
//                logger.error("❌ Delete failed for $id: ${e.message}")
//                status.setRollbackOnly() // Ensure the transaction rolls back on failure
//                throw e
//            }
//        }
//    }
//
//    /**
//     * Scans the entity for String fields containing KraftAdmin file paths
//     * and tells the storage provider to wipe them.
//     */
//    private fun cleanupEntityFiles(entity: Any) {
//        // Only proceed if a storage provider was actually injected
//        adminStorageProvider?.let { provider ->
//            entity::class.java.declaredFields.forEach { field ->
//                if (field.type == String::class.java) {
//                    field.isAccessible = true
//                    val value = field.get(entity) as? String
//
//                    if (value != null && (value.startsWith("/admin/files/") || value.contains("cloudinary.com"))) {
//                        provider.delete(value)
//                    }
//                }
//            }
//        }
//    }
//
///*
//* Save item to db
//*
// */
//    override fun save(name: String, data: Map<String, Any?>): Map<String, Any?> {
//        return transactionTemplate.execute<Map<String, Any?>> { status ->
//            val data = (data["data"] as? Map<String, Any?>) ?: data
//            val rawId = data["id"] ?: data["ID"]
//
//            logger.info("data $data rawId $rawId")
//
//            // Identify if we are creating or updating
//            val isNew = (rawId == null || rawId.toString().isBlank())
//
//            val entity: T = if (!isNew) {
////                val idType = entityManager.entityManagerFactory.metamodel
////                    .entity(entityClass.java)
////                    .idType.javaType
//
//                val convertedId = try {
//                   convertId(rawId.toString())
//                } catch (e: Exception) {
//                    logger.warn("ID conversion failed for $rawId, using raw.")
//                    rawId
//                }
//                entityManager.find(entityClass.java, convertedId) ?: createNewInstance()
//            } else {
//                createNewInstance()
//            }
//
//            // Sanitize payload (don't manually overwrite audit timestamps)
//            val updateableData = data.filterKeys {
//                it.lowercase() !in listOf("id", "createdat", "updatedat", "created_at", "updated_at")
//            }
//
//            applyDataToEntity(entity, updateableData)
//            val entity1 = mapper.toEntity(data)
//
//            // Persist to DB
//            val managedEntity = entityManager.merge(entity)
//            entityManager.flush()
//
//            // Get the final ID (crucial for NEW entities where ID was generated on flush)
//            val finalId = getEntityId(managedEntity).toString()
//
//            // Record the Audit
//            kraftAdminAuditor.record(
//                action = if (isNew) KraftLogAction.CREATE else KraftLogAction.UPDATE,
//                resource = entityClass.simpleName ?: "Unknown",
//                id = finalId,
//                actor = getCurrentUser()!!
//            )
//
//            mapEntityToData(managedEntity)
//
//        } ?: emptyMap()
//    }
//
//    /**
//     * Helper to extract ID from a managed entity using JPA metadata
//     */
//    private fun getEntityId(entity: T): Any? {
//        return entityManager.entityManagerFactory.persistenceUnitUtil.getIdentifier(entity)
//    }
//
//    /**
//     * Extracted helper to keep the save logic clean
//     */
//    private fun createNewInstance(): T {
//        return try {
//            val constructor = entityClass.java.getDeclaredConstructor()
//            constructor.isAccessible = true
//            constructor.newInstance() as T
//        } catch (e: Exception) {
//            val constructor = entityClass.java.constructors.first()
//            val args: Array<Any?> = Array(constructor.parameterCount) { null }
//            constructor.newInstance(*args) as T
//        }
//    }
//
//    /**
//     * Recursively applies map data to an entity or an @Embedded object.
//     */
//    private fun applyDataToEntity(target: Any, data: Map<String, Any?>) {
//        val targetClass = target::class
//
//        data.forEach { (key, value) ->
//            if (key == "id" || value == null) return@forEach
//
//            val prop = targetClass.memberProperties.find { it.name == key }
//            if (prop is KMutableProperty<*>) {
//                prop.isAccessible = true
//                val field = prop.javaField ?: return@forEach
//                val classifier = prop.returnType.classifier as? KClass<*>
//
//                try {
//                    when {
//                        // 1. COLLECTIONS (List, Set, etc.)
//                        field.isAnnotationPresent(ElementCollection::class.java) ||
//                                field.isAnnotationPresent(ManyToMany::class.java) ||
//                                field.isAnnotationPresent(OneToMany::class.java) -> {
//
//                            var currentCollection = prop.getter.call(target) as? MutableCollection<Any>
//                            if (currentCollection == null) {
//                                currentCollection = if (classifier?.isSubclassOf(Set::class) == true) mutableSetOf() else mutableListOf()
//                                prop.setter.call(target, currentCollection)
//                            }
//                            currentCollection.clear()
//
//                            val items = when (value) {
//                                is Collection<*> -> value
//                                is String -> value.split(",").map { it.trim() }.filter { it.isNotBlank() }
//                                else -> listOf(value)
//                            }
//
//                            val genericType = prop.returnType.arguments.firstOrNull()?.type?.classifier as? KClass<*>
//                            items.forEach { item ->
//                                if (item == null) return@forEach
//
//                                val valueToAdd = when {
//                                    // Entities
//                                    field.isAnnotationPresent(ManyToMany::class.java) || field.isAnnotationPresent(OneToMany::class.java) -> {
//                                        val id = if (item is Map<*, *>) item["id"] ?: item["lookupKey"] else item
//                                        val ref = entityManager.getReference(genericType!!.java, convertIdForClass(genericType, id))
//                                        if (field.isAnnotationPresent(OneToMany::class.java)) setBackReference(ref, target)
//                                        ref
//                                    }
//                                    // Embeddables
////                                    genericType?.isAnnotationPresent(Embeddable::class.java) == true && item is Map<*, *> -> {
////                                        val inst = genericType.java.getDeclaredConstructor().apply { isAccessible = true }.newInstance()
////                                        applyDataToEntity(inst, item as Map<String, Any?>)
////                                        inst
////                                    }
//                                    // NEW: Enums inside a collection
//                                    genericType != null && genericType.isSubclassOf(Enum::class) -> {
//                                        genericType.java.enumConstants.filterIsInstance<Enum<*>>().find { it.name.equals(item.toString(), true) }
//                                    }
//                                    // Simple Types
//                                    else -> coerceValue(item, genericType)
//                                }
//                                if (valueToAdd != null) currentCollection.add(valueToAdd)
//                            }
//                        }
//
//                        // 2. MAPS (e.g. @ElementCollection Map<String, String>)
//                        classifier != null && classifier.isSubclassOf(Map::class) && value is Map<*, *> -> {
//                            val currentMap = prop.getter.call(target) as? MutableMap<Any, Any>
//                            if (currentMap != null) {
//                                currentMap.clear()
//                                currentMap.putAll(value as Map<out Any, Any>)
//                            }
//                        }
//
//                        // 3. EMBEDDED SINGLE OBJECT
//                        field.isAnnotationPresent(Embedded::class.java) && value is Map<*, *> -> {
//                            var inst = prop.getter.call(target)
//                            if (inst == null) {
//                                inst = (classifier as KClass<*>).java.getDeclaredConstructor().apply { isAccessible = true }.newInstance()
//                                prop.setter.call(target, inst)
//                            }
//                            applyDataToEntity(inst!!, value as Map<String, Any?>)
//                        }
//
//                        // 4. SCALAR FIELDS
//                        else -> {
//                            val convertedValue = when {
//                                // Relational ID
//                                field.isAnnotationPresent(ManyToOne::class.java) || field.isAnnotationPresent(OneToOne::class.java) -> {
//                                    val id = if (value is Map<*, *>) value["id"] ?: value["lookupKey"] else value
//                                    if (id?.toString()?.isNotBlank() == true) {
//                                        val relatedClass = classifier as KClass<*>
//                                        entityManager.getReference(relatedClass.java, convertIdForClass(relatedClass, id))
//                                    } else null
//                                }
//                                // Dates (JSR-310)
//                                classifier == LocalDateTime::class -> parseDateTime(value)
//                                classifier == LocalDate::class -> LocalDate.parse(value.toString().substringBefore("T"))
//                                classifier == ZonedDateTime::class -> ZonedDateTime.parse(value.toString())
//
//                                // Time
//                                // Handle LocalTime from <input type="time">
//                                classifier == LocalTime::class -> {
//                                    val raw = value.toString()
//                                    when {
//                                        // If it's a full ISO string (from JS Date.toISOString())
//                                        raw.contains("T") -> LocalTime.parse(raw.substringAfter("T").substringBefore("Z"))
//                                        // If it's just "HH:mm" (Standard HTML5 time input)
//                                        raw.length == 5 -> LocalTime.parse("$raw:00")
//                                        // Already "HH:mm:ss"
//                                        else -> LocalTime.parse(raw)
//                                    }
//                                }
//                                // Enums
//                                classifier != null && classifier.isSubclassOf(Enum::class) -> {
//                                    classifier.java.enumConstants.filterIsInstance<Enum<*>>().find { it.name.equals(value.toString(), true) }
//                                }
//                                // UUID
//                                classifier == UUID::class && value is String -> UUID.fromString(value)
//                                // Booleans (Handle "true", "on", 1)
//                                classifier == Boolean::class -> value.toString().lowercase().let { it == "true" || it == "on" || it == "1" }
//                                // Numbers
//                                classifier != null && classifier.isSubclassOf(Number::class) -> coerceValue(value, classifier)
//                                else -> value
//                            }
//                            prop.setter.call(target, convertedValue)
//                        }
//                    }
//                } catch (e: Exception) {
//                    logger.warn("Mapping failed for '$key' in ${targetClass.simpleName}: ${e.message}")
//                }
//            }
//        }
//    }
//
//    private fun parseDateTime(value: Any?): LocalDateTime? {
//        val str = value?.toString() ?: return null
//        if (str.isBlank()) return null
//        return try {
//            // Handle ISO with T or space
//            LocalDateTime.parse(str.replace(" ", "T"))
//        } catch (e: Exception) {
//            // Fallback for date-only strings
//            LocalDate.parse(str).atStartOfDay()
//        }
//    }
//
//    /**
//     * For OneToMany, we need to find the field in the child that points back to the parent
//     * and set it so JPA knows the relationship is owned.
//     */
//    private fun setBackReference(child: Any, parent: Any) {
//        val childClass = child::class
//        val parentClass = parent::class
//
//        val backRefProp = childClass.memberProperties.find { prop ->
//            val classifier = prop.returnType.classifier as? KClass<*>
//            classifier != null && parentClass.isSubclassOf(classifier) &&
//                    prop.javaField?.isAnnotationPresent(ManyToOne::class.java) == true
//        }
//
//        if (backRefProp is KMutableProperty<*>) {
//            backRefProp.isAccessible = true
//            backRefProp.setter.call(child, parent)
//        }
//    }
//
//    /**
//     * Coerces simple types for ElementCollections (e.g. String to Double)
//     */
//    private fun coerceValue(value: Any, targetType: KClass<*>?): Any {
//        return when (targetType) {
//            Double::class -> value.toString().toDouble()
//            Int::class -> value.toString().toInt()
//            Long::class -> value.toString().toLong()
//            else -> value
//        }
//    }
//
//    override fun getLookupData(
//        lookup: LookupDescriptor,
//        limit: Int,
//        searchQuery: String?
//    ): List<ObjectResponse> {
//        logger.info("lookup $lookup")
//        val cb = entityManager.criteriaBuilder
//        val javaClass = entityClass.java as Class<Any>
//        val query = cb.createQuery(javaClass)
//        val root = query.from(javaClass)
//
//        if (!searchQuery.isNullOrBlank()) {
//            query.where(
//                cb.like(
//                    cb.lower(root.get<String>(lookup.searchField)), // ← use lookup.searchField directly
//                    "%${searchQuery.lowercase()}%"
//                )
//            )
//        }
//
//        return entityManager.createQuery(query.select(root))
//            .setMaxResults(limit)
//            .resultList
//            .map { entity ->
//                ObjectResponse(
//                    id = extractId(entity).toString(),
//                    displayField = resolveDisplayLabel(entity) ?: "Unknown"
//                )
//            }
//    }
//
//    override fun countAll(name: String): Long? {
//            return transactionTemplate.execute {
//                val cb = entityManager.criteriaBuilder
//                val query = cb.createQuery(Long::class.java)
//                query.select(cb.count(query.from(entityClass.java)))
//                entityManager.createQuery(query).singleResult
//            } ?: 0L
//    }
//
//    private fun mapEntityToData(entity: Any?): Map<String, Any?> {
//        if (entity == null) return emptyMap()
//        val result = mutableMapOf<String, Any?>()
//        val kClass = entity::class
//
//        kClass.memberProperties.forEach { prop ->
//            val field = prop.javaField ?: return@forEach
//
//            //  Also check @Transient on the getter method (covers @Transient on methods like getAuthor())
//            val getterMethod = try {
//                entity::class.java.getMethod("get${prop.name.replaceFirstChar { it.uppercase() }}")
//            } catch (e: NoSuchMethodException) { null }
//
//            val isTransientField = field.isAnnotationPresent(Transient::class.java) || field.isAnnotationPresent(jakarta.persistence.Transient::class.java)
//
//            val isTransientMethod = getterMethod?.isAnnotationPresent(Transient::class.java) == true ||
//                    getterMethod?.isAnnotationPresent(jakarta.persistence.Transient::class.java) == true
//
//            // ✅ Also skip if there's no backing field at all (computed/derived properties)
//            val hasNoBackingField = field == null
//
//            if (isTransientField || isTransientMethod || hasNoBackingField ||
//                Modifier.isStatic(field.modifiers ?: 0) || field.isAnnotationPresent(JsonIgnore::class.java)
//            ) {
//                return@forEach
//            }
//
////            // 1. Skip non-persistent or ignored fields
////            if (field.isAnnotationPresent(Transient::class.java) ||
////                Modifier.isStatic(field.modifiers) ||
////                field.isAnnotationPresent(jakarta.persistence.Transient::class.java) ||
////                field.isAnnotationPresent(JsonIgnore::class.java)
////            ) {
////                return@forEach
////            }
//
//            val rawValue = try {
//                field.isAccessible = true
//                field.get(entity)
//            } catch (e: Exception) {
//                logger.warn("Could not read field ${field.name}: ${e.message}")
//                null
//            }
//
//            // ✅ Unproxy before inspection — handles ByteBuddy/CGLIB Hibernate proxies
//            val value = unproxy(rawValue)
//
//            when {
//                // 2. Handle Embedded (Value Objects)
//                field.isAnnotationPresent(Embedded::class.java) -> {
//                    if (value == null) {
//                        result[prop.name] = null
//                    } else {
//                        val fullMap = mapEntityToData(value)
//                        val summary = fullMap.values
//                            .filterIsInstance<String>()
//                            .filter { it.isNotBlank() }
//                            .take(2)
//                            .joinToString(", ")
//
//                        result[prop.name] = EmbeddedResponse(summary, fullMap)
//                    }
//                }
//
//                // 3. Handle Relationships (ManyToOne / OneToOne)
//                // 3. Handle Relationships (ManyToOne / OneToOne)
//                field.isAnnotationPresent(ManyToOne::class.java) ||
//                        field.isAnnotationPresent(OneToOne::class.java) -> {
//                    if (value == null) {
//                        result[prop.name] = null
//                    } else {
//                        val id = try { extractId(value).toString() } catch (e: Exception) { null }
//
//                        if (id == null) {
//                            result[prop.name] = null
//                        } else {
//                            val label = try {
//                                value::class.memberProperties
//                                    .filter { p ->
//                                        val javaField = p.javaField ?: return@filter false
//                                        // Skip transient, static, ignored fields on the related entity
//                                        if (javaField.isAnnotationPresent(Transient::class.java) ||
//                                            javaField.isAnnotationPresent(jakarta.persistence.Transient::class.java) ||
//                                            Modifier.isStatic(javaField.modifiers)) return@filter false
//
//                                        val name = p.name.lowercase()
//                                        if (name == "id" || name.endsWith("id") || name.contains("password")) return@filter false
//
//                                        // ✅ Safe classifier check — skip if not a plain KClass
//                                        val classifier = p.returnType.classifier
//                                        if (classifier !is KClass<*>) return@filter false
//
//                                        isSimpleType(classifier)
//                                    }
//                                    .let { candidates ->
//                                        candidates.find { it.name == "name" || it.name == "title" || it.name == "label" }
//                                            ?: candidates.firstOrNull()
//                                    }
//                                    ?.let { best ->
//                                        best.isAccessible = true
//                                        best.getter.call(value)?.toString()
//                                    }
//                            } catch (e: Exception) {
//                                logger.warn("Could not resolve label for ${value::class.simpleName}.${prop.name}: ${e.message}")
//                                null
//                            } ?: id
//
//                            result[prop.name] = ObjectResponse(id, label)
//                        }
//                    }
//                }
//
//                // 4. Handle Collections
//                value is Collection<*> -> {
//                    result[prop.name] = value.map { item ->
//                        val realItem = unproxy(item) // ✅ Unproxy each collection element
//                        if (realItem == null) null
//                        else if (isSimpleType(realItem::class)) realItem
//                        else extractId(realItem).toString()
//                    }
//                }
//
//                // 5. Simple Types
//                else -> result[prop.name] = value
//            }
//        }
//        return result
//    }
//
//    /**
//     * Helper to check if we can safely pass the value to JSON without recursion
//     */
//    private fun isSimpleType(kClass: KClass<*>): Boolean {
//        return kClass.java.isPrimitive ||
//                kClass == String::class ||
//                kClass == Boolean::class ||
//                kClass == Number::class ||
//                kClass.java.isEnum ||
//                kClass.simpleName?.contains("LocalDate") == true
//    }
//
//    private fun isSimpleType1(kClass: KClass<*>): Boolean {
//        val typeName = kClass.java.name
//
//        // If the class name contains ByteBuddy or Hibernate, it is NOT simple.
//        if (typeName.contains("Hibernate") ||
//            typeName.contains("ByteBuddy") ||
//            typeName.contains("_$$")) return false
//
//        return kClass.java.isPrimitive ||
//                typeName.startsWith("java.lang") ||
//                typeName.startsWith("java.time") ||
//                typeName.startsWith("java.math") ||
//                kClass.java.isEnum ||
//                typeName == "java.util.UUID"
//    }
//
//    private fun extractId(entity: Any): Any? {
//        val clean = unproxy(entity) ?: return null
//        var currentClass: Class<*>? = clean.javaClass
//
//        while (currentClass != null && currentClass != Any::class.java) {
//            // Find by annotation or name
//            val idField = currentClass.declaredFields.find {
//                it.isAnnotationPresent(Id::class.java) || it.name == "id"
//            }
//
//            if (idField != null) {
//                return try {
//                    idField.isAccessible = true
//                    idField.get(clean)
//                } catch (e: Exception) {
//                    null
//                }
//            }
//            // Move up the hierarchy to find ID in BaseEntity
//            currentClass = currentClass.superclass
//        }
//
//        return null
//    }
//
//    /**
//     * Converts a string from the URL into the actual type required by the JPA Entity
//     */
//    fun convertId(id: String): Any {
//        val metamodel = entityManager.metamodel
//
//        // Defensive check: Verify if the class is actually a managed JPA entity
//        val entityType: EntityType<*> = try {
//            metamodel.entity(entityClass.java)
//        } catch (e: IllegalArgumentException) {
//            // Fallback: If it's not a managed entity, we have no choice but
//            // to return the string and hope the caller handles it,
//            // or throw a more meaningful custom exception.
//            return id
//        }
//
//        val idType = entityType.idType.javaType
//
//        return try {
//            when (idType) {
//                UUID::class.java -> UUID.fromString(id)
//                Long::class.java, Long::class.javaObjectType -> id.toLong()
//                Int::class.java, Int::class.javaObjectType -> id.toInt()
//                else -> id
//            }
//        } catch (e: Exception) {
//            throw IllegalArgumentException("Cannot convert ID '$id' to type ${idType.simpleName} for entity ${entityClass.simpleName}", e)
//        }
//    }
//
//
//    /**
//     * Entry point: Converts a JPA Entity into a structured ResourceRow.
//     */
//    fun mapToRow(entity: Any): ResourceRow {
//        val id = extractId(entity).toString()
//        val values = mapEntityToValues(entity)
//
//        return ResourceRow(
//            id = id,
//            values = values,
//            metadata = RowMetadata(
//                canEdit = true, // You can later inject logic here to check roles
//                canDelete = true
//            )
//        )
//    }
//
//    /**
//     * The internal recursive mapper that builds the values map.
//     */
//    private fun mapEntityToValues(entity: Any?): Map<String, Any?> {
//        if (entity == null) return emptyMap()
//        val result = mutableMapOf<String, Any?>()
//        val kClass = entity::class
//
//        kClass.memberProperties.forEach { prop ->
//            val field = prop.javaField ?: return@forEach
//
//            // 1. Skip non-persistent or ignored fields
//            if (field.isAnnotationPresent(Transient::class.java) ||
//                Modifier.isStatic(field.modifiers) ||
//                field.isAnnotationPresent(jakarta.persistence.Transient::class.java)
//            ) {
//                return@forEach
//            }
//
//            val rawValue = try {
//                field.isAccessible = true
//                field.get(entity)
//            } catch (e: Exception) {
//                null
//            }
//
//            // ✅ Unproxy before inspection — handles ByteBuddy/CGLIB proxies
//            val value = unproxy(rawValue)
//
//            when {
//                // 2. Handle Embedded (Value Objects)
//                field.isAnnotationPresent(Embedded::class.java) -> {
//                    if (value == null) {
//                        result[prop.name] = null
//                    } else {
//                        val fullMap = mapEntityToValues(value) // Recursive call
//                        val summary = fullMap.values
//                            .filterIsInstance<String>()
//                            .filter { it.isNotBlank() }
//                            .take(2)
//                            .joinToString(", ")
//
//                        result[prop.name] = EmbeddedResponse(summary, fullMap)
//                    }
//                }
//
//
//                // 3. Handle Single Relationships
//                field.isAnnotationPresent(ManyToOne::class.java) || field.isAnnotationPresent(OneToOne::class.java) -> {
//                    if (value == null) {
//                        result[prop.name] = null
//                    } else {
//                        val id = extractId(value).toString()
//                        val label = resolveDisplayLabel(value) ?: id
//                        result[prop.name] = ObjectResponse(id, label)
//                    }
//                }
//
//                // 4. NEW: Handle Element Collections (Tags, Highlights, etc.)
//                field.isAnnotationPresent(ElementCollection::class.java) -> {
//                    result[prop.name] = if (value is Collection<*>) {
//                        value.map { it?.toString() } // Return raw strings
//                    } else {
//                        emptyList<String>()
//                    }
//                }
//
//                // 5. Handle Collection Relationships (ManyToMany / OneToMany)
//                field.isAnnotationPresent(ManyToMany::class.java) || field.isAnnotationPresent(OneToMany::class.java) -> {
//                    result[prop.name] = if (value is Collection<*>) {
//                        value.map { item ->
//                            if (item == null) null
//                            else {
//                                val id = extractId(item).toString()
//                                val label = resolveDisplayLabel(item) ?: id
//                                ObjectResponse(id, label) // Wrap them for the UI chips
//                            }
//                        }
//                    } else emptyList<Any>()
//                }
//
//                // 6. Primitive / Simple types
//                else -> result[prop.name] = value
//            }
//        }
//        return result
//    }
//
//    /**
//     * Helper to find a "Name" or "Title" field in a related entity.
//     */
//    private fun resolveDisplayLabel(entity: Any): String? {
//        // 1. Handle Hibernate Proxies
//        // We get the actual class, even if it's a proxy, so reflection finds the fields
//        val actualEntity = if (entity is HibernateProxy) {
//            entity.hibernateLazyInitializer.implementation
//        } else {
//            entity
//        }
//
//        val kClass = actualEntity::class
//        val props = kClass.memberProperties
//
//        //  Logic remains the same, but we operate on the unproxied 'actualEntity'
//        val targetProp = props.find { prop ->
//            val field = prop.javaField
//            field?.isAnnotationPresent(KraftAdminField::class.java) == true &&
//                    field.getAnnotation(KraftAdminField::class.java).displayField
//        } ?: props.find {
//            it.name.lowercase() in listOf("name", "title", "label", "username", "displayname")
//        } ?: props.filter { prop ->
//            val classifier = prop.returnType.classifier as? KClass<*>
//            classifier != null && isSimpleType(classifier) && prop.name.lowercase() != "id"
//        }.getOrNull(0)
//
//        return try {
//            val field = targetProp?.javaField
//            if (field != null) {
//                field.isAccessible = true
//                field.get(actualEntity)?.toString()
//            } else {
//                targetProp?.getter?.call(actualEntity)?.toString()
//            }
//        } catch (e: Exception) {
//            null
//        } ?: "Unknown ${kClass.simpleName?.removeSuffix("\$HibernateProxy")}"
//    }
//
//    /**
//     * Handles converting incoming UI data (Strings or Lists) into the correct Entity collection type.
//     */
//    private fun handleCollectionMapping(prop: KMutableProperty<*>, value: Any?): Any? {
//        // Get the generic type of the List (e.g., String in List<String>)
//        val typeArgument = prop.returnType.arguments.firstOrNull()?.type?.classifier as? KClass<*>
//
//        val listValues = when (value) {
//            is String -> value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
//            is Collection<*> -> value.toList()
//            else -> emptyList<Any>()
//        }
//
//        // If the target is List<String>, we are done.
//        // If it's List<Long>, we'd convert here.
//        return if (typeArgument == String::class) {
//            listValues.map { it.toString() }
//        } else {
//            listValues // Fallback
//        }
//    }
//
//    /**
//     * Generic helper to convert a raw value (usually String) into the ID type
//     * required by a specific entity class.
//     */
//    private fun convertIdForClass(targetKClass: KClass<*>, idValue: Any?): Any? {
//        if (idValue == null) return null
//        val idString = idValue.toString()
//
//        // 1. Get the underlying Java class to handle Proxy objects correctly
//        val targetJavaClass = targetKClass.java
//
//        // 2. Use JPA Metamodel to find the ID type reliably,
//        // even if the class is proxied.
//        return try {
//            val entityType = entityManager.metamodel.entity(targetJavaClass)
//            val idTypeDescriptor = entityType.idType
//            val actualJavaType = idTypeDescriptor.javaType
//
//            logger.info("idType $idTypeDescriptor, actualJavaType $actualJavaType")
//
//            // 3. Convert based on the Metamodel's ID type
//            when (actualJavaType) {
//                UUID::class.java -> UUID.fromString(idString)
//                Long::class.java, Long::class.javaObjectType -> idString.toLong()
//                Int::class.java, Int::class.javaObjectType -> idString.toInt()
//                else -> idString
//            }
//        } catch (e: Exception) {
//            // Fallback: If not a managed JPA entity, return original
//            idString
//        }
//    }
//
//    /**
//     * Unwraps a Hibernate proxy to its real underlying entity.
//     * Safe to call on non-proxy objects too.
//     */
//    private fun unproxy(value: Any?): Any? {
//        if (value == null) return null
//        return if (value is HibernateProxy) {
//            val lazyInitializer = value.hibernateLazyInitializer
//            // This forces initialization and returns the real object
//            lazyInitializer.implementation
//        } else {
//            value
//        }
//    }
//
//    fun getCurrentUser() : AdminUserDTO {
////        return adminSecurityProvider.getCurrentUser()!!
//        val adminDto = securityChain.resolveCurrentUser()!!
//        logger.info("Current authenticated user $adminDto")
//        return adminDto
//    }
//
//}


package persistence.jpa.provider

import api.responses.PagedResponse
import api.utils.ObjectResponse
import api.utils.ResourceRow
import com.kraftadmin.spi.KraftAdminColumn
import com.kraftadmin.spi.KraftDataProvider
import com.kraftadmin.ui_descriptors.LookupDescriptor
import com.kraftadmin.utils.files.AdminStorageProvider
import config.KraftPulseSpringKraftAdminProperties
import config.PaginationConfig
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.transaction.support.TransactionTemplate
import persistence.jpa.conversion.TypeConverter
import persistence.jpa.delete.EntityDeleter
import persistence.jpa.fetch.FetchAll
import persistence.jpa.fetch.FetchById
import persistence.jpa.lookup.LookupProvider
import persistence.jpa.mapper.ResourceRowMapper
import persistence.jpa.metadata.EntityMetadata
import persistence.jpa.save.EntityInstantiator
import persistence.jpa.save.EntitySaver
import persistence.jpa.save.PropertyWriter
import persistence.jpa.save.RelationshipWriter
import events.SpringKraftLifecycleService
import security.SecurityProviderChain
import kotlin.reflect.KClass


/**
 * Thin orchestrator. All logic lives in focused collaborators.
 * Implements KraftDataProvider<T> exactly as defined in the interface.
 */
class JpaDataProvider<T : Any>(
    private val entityClass: KClass<T>,
    private val entityManager: EntityManager,
    private val transactionTemplate: TransactionTemplate,
    private val applicationContext: ApplicationContext,
    private val adminStorageProvider: AdminStorageProvider,
//    private val kraftAdminAuditor: KraftAdminAuditor,
    private val securityChain: SecurityProviderChain,
    private val properties: KraftPulseSpringKraftAdminProperties,
//    private val telemetryService: KraftTelemetryService
    paginationProperties: PaginationConfig,
    lifecycleService: SpringKraftLifecycleService
) : KraftDataProvider<T> {

    private val logger = LoggerFactory.getLogger(JpaDataProvider::class.java)

    private val entityMetadata = EntityMetadata(entityClass)
    private val rowMapper = ResourceRowMapper(entityClass, applicationContext)

    private val fetchAllExecutor = FetchAll(
        entityClass, entityManager, transactionTemplate,
        entityMetadata, rowMapper, paginationProperties
    )

    private val fetchByIdExecutor = FetchById(
        entityClass, entityManager, transactionTemplate,
        entityMetadata, rowMapper
    )

    private val entitySaver = EntitySaver(
        entityClass = entityClass,
        entityManager = entityManager,
        transactionTemplate = transactionTemplate,
        metadata = entityMetadata,
        instantiator = EntityInstantiator(entityClass),
        propertyWriter = PropertyWriter(TypeConverter),
        relationshipWriter = RelationshipWriter(entityManager),
        lifecycle = lifecycleService
    )

    private val entityDeleter = EntityDeleter(
        entityClass, entityManager, transactionTemplate, entityMetadata, adminStorageProvider
    )

    private val lookupProvider = LookupProvider(entityManager, applicationContext)

    override fun fetchAll(
        page: Int,
        size: Int,
        query: String?,
        columns: List<KraftAdminColumn>,
        sortField: String?,
        sortDirection: String?
    ): PagedResponse<ResourceRow> = fetchAllExecutor.execute(page, size,  columns, query, sortField, sortDirection)

    override fun fetchById(
        id: String,
        columns: List<KraftAdminColumn>
    ): ResourceRow? = fetchByIdExecutor.execute(id, columns)

    /**
     * Unified save — determines CREATE vs UPDATE from whether `id`
     * is present and non-blank in [data].
     *
     * Returns a map of the saved entity's values so the UI can
     * reflect the server-assigned fields (e.g. generated ID, timestamps).
     */
    override fun save(name: String, data: Map<String, Any?>): Map<String, Any?> {
        logger.error("name {}, data {}", name, data)
        val id = data["id"]?.toString()?.takeIf { it.isNotBlank() }

        val savedEntity = if (id != null) {
            logger.error("save() → UPDATE {} #{}", entityClass.simpleName, id)
            entitySaver.update(id, data)
        } else {
            logger.error("save() → CREATE {}", entityClass.simpleName)
            entitySaver.create(data)
        }

        if (savedEntity == null) {
            logger.error("save() returned null for {} data={}", entityClass.simpleName, data.keys)
            return emptyMap()
        }

        // Re-map the saved entity to a flat values map so the caller
        // gets server-assigned fields (generated ID, timestamps, etc.)
        return try {
            rowMapper.mapEntityToData(savedEntity)
        } catch (e: Exception) {
            logger.warn("Could not map saved entity back to data map: ${e.message}")
            emptyMap()
        }
    }

    /**
     * Deletes the entity with the given [id]. No-ops silently if not found
     * (idempotent — consistent with REST DELETE semantics).
     */
    override fun delete(id: String) {
        val deleted = entityDeleter.delete(id)
        if (!deleted) {
            logger.debug("delete() — entity {} #{} not found or already deleted", entityClass.simpleName, id)
        }
    }

    /**
     * Resolves lookup options from a [LookupDescriptor].
     * Used by relation fields to power typeahead search in the UI.
     */
    override fun getLookupData(
        lookup: LookupDescriptor,
        limit: Int,
        searchQuery: String?
    ): List<ObjectResponse> {
        return lookupProvider.lookup(
            lookup = lookup,
            searchQuery = searchQuery,
            limit = limit
        )
    }

    /**
     * Total count of all (non-deleted) records for the given resource [name].
     * Used by the dashboard and table headers.
     * [name] is ignored here — this provider is already scoped to [entityClass].
     */
    override fun countAll(name: String): Long? {
        return try {
            transactionTemplate.execute {
                val cb = entityManager.criteriaBuilder
                val cq = cb.createQuery(Long::class.java)
                cq.select(cb.count(cq.from(entityClass.java)))
                entityManager.createQuery(cq).singleResult
            }
        } catch (e: Exception) {
            logger.error("countAll failed for ${entityClass.simpleName}: ${e.message}", e)
            null
        }
    }

    override fun getLookupDataByIds(
        lookup: LookupDescriptor,
        ids: List<String>
    ): List<ObjectResponse> {
        if (ids.isEmpty()) return emptyList()
        return lookupProvider.lookupByIds(lookup, ids)
    }

    override fun findById(id: String) : T? {
        return fetchByIdExecutor.fetchEntity(id)
    }



}
