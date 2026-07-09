//package persistence.jpa.lookup
//
//import api.utils.ObjectResponse
//import jakarta.persistence.EntityManager
//import org.slf4j.LoggerFactory
//import persistence.jpa.metadata.AssociationResolver
//import kotlin.reflect.KClass
//
//class LookupQuery(private val entityManager: EntityManager) {
//
//    private val logger = LoggerFactory.getLogger(LookupQuery::class.java)
//
//    fun <T : Any> execute(
//        entityClass: KClass<T>,
//        searchField: String,
//        query: String,
//        limit: Int = 20
//    ): List<ObjectResponse> {
//        return try {
//            val cb = entityManager.criteriaBuilder
//            val cq = cb.createQuery(entityClass.java)
//            val root = cq.from(entityClass.java)
//
//            // Apply search filter if query is provided
//            if (query.isNotBlank()) {
//                cq.where(cb.like(cb.lower(root.get(searchField)), "%${query.lowercase()}%"))
//            }
//
//            entityManager.createQuery(cq)
//                .setMaxResults(limit)
//                .resultList
//                .mapNotNull { entity ->
//                    // ✅ Apply the shared philosophy: Delegate extraction
//                    val id = AssociationResolver.extractId(entity)?.toString() ?: return@mapNotNull null
//
//                    // You can keep resolveDisplayLabel here or move it to a LabelResolver utility
//                    val label = AssociationResolver.resolveDisplayLabel(entity) ?: id
//
//                    ObjectResponse(id = id, displayField = label)
//                }
//        } catch (e: Exception) {
//            logger.error("LookupQuery failed for ${entityClass.simpleName}: ${e.message}", e)
//            emptyList()
//        }
//    }
//}

package persistence.jpa.lookup

import api.utils.ObjectResponse
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import persistence.jpa.metadata.AssociationResolver
import persistence.jpa.metadata.PropertyResolver
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

class LookupQuery(private val entityManager: EntityManager) {

    private val logger = LoggerFactory.getLogger(LookupQuery::class.java)

    private val LABEL_CANDIDATES = listOf(
        "name", "title", "label", "email", "username", "code", "fullName", "displayName"
    )

    /**
     * Search-based lookup — called as user types in the relation input.
     */
    fun <T : Any> execute(
        entityClass: KClass<T>,
        searchField: String,
        displayField: String,
        query: String,
        limit: Int = 20
    ): List<ObjectResponse> {
        return try {
            val cb = entityManager.criteriaBuilder
            val cq = cb.createQuery(entityClass.java)
            val root = cq.from(entityClass.java)

            if (query.isNotBlank()) {
                cq.where(
                    cb.like(
                        cb.lower(root.get(searchField)),
                        "%${query.lowercase()}%"
                    )
                )
            }

            entityManager.createQuery(cq)
                .setMaxResults(limit)
                .resultList
                .mapNotNull { entity -> toObjectResponse(entity, displayField) }

        } catch (e: Exception) {
            logger.error("LookupQuery.execute failed for ${entityClass.simpleName}: ${e.message}", e)
            emptyList()
        }
    }

    fun <T : Any> executeByIds1(
        entityClass: KClass<T>,
        displayField: String,
        ids: List<String>
    ): List<ObjectResponse> {
        if (ids.isEmpty()) return emptyList()

        return try {
            val cb = entityManager.criteriaBuilder
            val cq = cb.createQuery(entityClass.java)
            val root = cq.from(entityClass.java)

            // Resolve ID field name and type from @Id annotation
            val idField = entityClass.java.declaredFields.firstOrNull {
                it.isAnnotationPresent(jakarta.persistence.Id::class.java) ||
                        it.isAnnotationPresent(org.springframework.data.annotation.Id::class.java)
            }
            val idFieldName = idField?.name ?: "id"

            // Coerce string IDs to the correct JPA type
            val coercedIds = ids.mapNotNull { id ->
                try {
                    when (idField?.type) {
                        Long::class.java,
                        java.lang.Long::class.java -> id.toLong()
                        Int::class.java,
                        java.lang.Integer::class.java -> id.toInt()
                        java.util.UUID::class.java -> java.util.UUID.fromString(id)
                        else -> id
                    }
                } catch (e: Exception) {
                    logger.warn("executeByIds: could not coerce id '$id' for ${entityClass.simpleName}: ${e.message}")
                    null
                }
            }

            if (coercedIds.isEmpty()) return emptyList()

            cq.where(root.get<Any>(idFieldName).`in`(coercedIds))

            entityManager.createQuery(cq)
                .resultList
                .mapNotNull { entity -> toObjectResponse(entity, displayField) }

        } catch (e: Exception) {
            logger.error("executeByIds failed for ${entityClass.simpleName}: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * ID-based lookup — called on form load to resolve labels for
     * existing relation IDs so chips show names instead of UUIDs.
     */
    fun <T : Any> executeByIds(
        entityClass: KClass<T>,
        displayField: String,
        ids: List<String>
    ): List<ObjectResponse> {
        if (ids.isEmpty()) return emptyList()
        return try {
            val cb = entityManager.criteriaBuilder
            val cq = cb.createQuery(entityClass.java)
            val root = cq.from(entityClass.java)

            // Resolve the ID field name from @Id annotation
            val idFieldName = entityClass.memberProperties
                .firstOrNull { prop ->
                    prop.javaField?.isAnnotationPresent(jakarta.persistence.Id::class.java) == true ||
                            prop.javaField?.isAnnotationPresent(
                                org.springframework.data.annotation.Id::class.java
                            ) == true
                }
                ?.name ?: "id"

            // Coerce string IDs to the correct type
            val idField = entityClass.java.declaredFields
                .firstOrNull { it.isAnnotationPresent(jakarta.persistence.Id::class.java) }
            val coercedIds = ids.mapNotNull { id ->
                try {
                    when (idField?.type) {
                        Long::class.java, java.lang.Long::class.java -> id.toLong()
                        Int::class.java, java.lang.Integer::class.java -> id.toInt()
                        java.util.UUID::class.java -> java.util.UUID.fromString(id)
                        else -> id
                    }
                } catch (e: Exception) { null }
            }

            cq.where(root.get<Any>(idFieldName).`in`(coercedIds))

            entityManager.createQuery(cq)
                .resultList
                .mapNotNull { entity -> toObjectResponse(entity, displayField) }

        } catch (e: Exception) {
            logger.error("LookupQuery.executeByIds failed for ${entityClass.simpleName}: ${e.message}", e)
            emptyList()
        }
    }

    private fun toObjectResponse(entity: Any, preferredDisplayField: String): ObjectResponse? {
        val id = AssociationResolver.extractId(entity)?.toString() ?: return null

        val props = entity::class.memberProperties.associateBy { it.name }

        // ✅ Try preferred display field first, then candidates, then id
        val label = props[preferredDisplayField]
            ?.runCatching { getter.call(entity)?.toString() }
            ?.getOrNull()
            ?: LABEL_CANDIDATES
                .firstOrNull { it in props }
                ?.let { props[it]?.runCatching { getter.call(entity)?.toString() }?.getOrNull() }
            ?: id

        return ObjectResponse(id = id, displayField = label)
    }
}