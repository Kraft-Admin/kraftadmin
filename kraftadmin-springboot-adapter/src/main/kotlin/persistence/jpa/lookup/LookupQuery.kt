package persistence.jpa.lookup

import api.utils.ObjectResponse
import com.kraftadmin.annotations.KraftAdminField
import com.kraftadmin.logging.KraftAdminLogging
import jakarta.persistence.EntityManager
import jakarta.persistence.Id
import persistence.jpa.metadata.AssociationResolver
import persistence.jpa.metadata.EntityMetadata
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField

class LookupQuery(private val entityManager: EntityManager) {

    private val logger = KraftAdminLogging.logger(javaClass)

    private val LABEL_CANDIDATES = listOf(
        "name", "title", "label", "email", "username", "code", "fullName", "displayName"
    )

    /**
     * Search-based lookup — called as user types in the relation input.
     */
    fun <T : Any> execute(
        entityClass: KClass<T>,
        query: String,
        limit: Int = 20
    ): List<ObjectResponse> {
        val entityMetadata = EntityMetadata(entityClass)
        val displayField = entityMetadata.displayField

        return try {
            val cb = entityManager.criteriaBuilder
            val cq = cb.createQuery(entityClass.java)
            val root = cq.from(entityClass.java)

            if (query.isNotBlank()) {
                // ✅ Dynamically search across all searchable fields
                val predicates = entityMetadata.searchableFields.map {
                    cb.like(cb.lower(root.get(it)), "%${query.lowercase()}%")
                }
                cq.where(cb.or(*predicates.toTypedArray()))
            }

            entityManager.createQuery(cq)
                .setMaxResults(limit)
                .resultList
                .mapNotNull { entity -> toObjectResponse(entity, displayField) }
        } catch (e: Exception) {
            logger.error("Lookup failed: ${e.message}")
            emptyList()
        }
    }

    /**
     * ID-based lookup — called on form load to resolve labels for
     * existing relation IDs so chips show names instead of UUIDs.
     */
    fun <T : Any> executeByIds(
        entityClass: KClass<T>,
        ids: List<String>
    ): List<ObjectResponse> {
        if (ids.isEmpty()) return emptyList()

        return try {
            val cb = entityManager.criteriaBuilder
            val cq = cb.createQuery(entityClass.java)
            val root = cq.from(entityClass.java)

            // Fix: Traverse hierarchy to find @Id, even if in BaseEntity
            var currentClass: Class<*>? = entityClass.java
            var idField: java.lang.reflect.Field? = null

            while (currentClass != null && currentClass != Any::class.java) {
                idField = currentClass.declaredFields.firstOrNull {
                    it.isAnnotationPresent(Id::class.java) ||
                            it.isAnnotationPresent(org.springframework.data.annotation.Id::class.java)
                }
                if (idField != null) break
                currentClass = currentClass.superclass
            }

            // Fallback: If still null, try finding a field named "id"
            if (idField == null) {
                idField = entityClass.java.declaredFields.firstOrNull { it.name == "id" }
            }

            if (idField == null) throw IllegalArgumentException("No @Id field found on ${entityClass.simpleName} or its superclasses")

            idField.isAccessible = true
            val idFieldName = idField.name
            val idType = idField.type

            val coercedIds = ids.mapNotNull { id ->
                try {
                    when {
                        Long::class.java.isAssignableFrom(idType) -> id.toLong()
                        Int::class.java.isAssignableFrom(idType) -> id.toInt()
                        UUID::class.java.isAssignableFrom(idType) -> UUID.fromString(id)
                        else -> id
                    }
                } catch (e: Exception) { null }
            }

            @Suppress("UNCHECKED_CAST")
            val idPath = root.get<Any>(idFieldName).`as`(idType as Class<Any>)
            cq.where(idPath.`in`(coercedIds))

            entityManager.createQuery(cq).resultList.mapNotNull { toObjectResponse(it, "h1") }

        } catch (e: Exception) {
            logger.error("LookupQuery.executeByIds failed for ${entityClass.simpleName}: ${e.message}", e)
            emptyList()
        }
    }


    private fun toObjectResponse(
        entity: Any,
        preferredDisplayField: String?
    ): ObjectResponse? {

        val id = AssociationResolver.extractId(entity)?.toString()
            ?: return null

        val props = entity::class.memberProperties.associateBy { it.name }

        fun value(field: String?): String? {
            if (field.isNullOrBlank()) return null

            val property = props[field] ?: return null

            property.isAccessible = true

            return property
                .getter
                .call(entity)
                ?.toString()
                ?.takeIf(String::isNotBlank)
        }

        val annotatedField =
            entity::class.memberProperties
                .firstOrNull { property ->
                    property.isAccessible = true
                    property.javaField
                        ?.getAnnotation(KraftAdminField::class.java)
                        ?.displayField == true
                }
                ?.name

        val label =
            value(annotatedField)
                ?: value(preferredDisplayField)
                ?: LABEL_CANDIDATES.firstNotNullOfOrNull(::value)
                ?: id

        return ObjectResponse(
            id = id,
            label = label
        )
    }


}