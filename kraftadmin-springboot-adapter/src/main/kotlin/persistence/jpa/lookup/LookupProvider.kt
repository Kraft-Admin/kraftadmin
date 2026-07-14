package persistence.jpa.lookup

import api.utils.ObjectResponse
import com.kraftadmin.ui_descriptors.LookupDescriptor
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import kotlin.reflect.KClass

/**
 * Resolves entity classes for lookup operations and delegates
 * to LookupQuery. Returns List<ObjectResponse> to match the
 * KraftDataProvider interface contract.
 */

class LookupProvider(
    private val entityManager: EntityManager,
    private val applicationContext: ApplicationContext
) {
    private val logger = LoggerFactory.getLogger(LookupProvider::class.java)
    private val query = LookupQuery(entityManager)

    fun lookup(
        lookup: LookupDescriptor,
        searchQuery: String?,
        limit: Int = 20
    ): List<ObjectResponse> {
        logger.info("Looking up lookup for $lookup")
        val targetEntity = lookup.targetEntity ?: run {
            logger.warn("LookupProvider: targetEntity is null")
            return emptyList()
        }

        val entityClass = resolveEntityClass(targetEntity) ?: run {
            logger.warn("LookupProvider: could not resolve entity class for '$targetEntity'")
            return emptyList()
        }

//        return query.execute(
//            entityClass = entityClass,
//            searchField = lookup.searchField ?: "name",
//            displayField = lookup.displayField ?: lookup.searchField ?: "name",
//            query = searchQuery ?: "",
//            limit = limit
//        )

        return query.execute(
            entityClass = entityClass,
            query = searchQuery ?: "",
            limit = limit,
        )

    }

//    fun lookupByIds1(
//        lookup: LookupDescriptor,
//        ids: List<String>
//    ): List<ObjectResponse> {
//        val targetEntity = lookup.targetEntity ?: return emptyList()
//        val entityClass = resolveEntityClass(targetEntity) ?: return emptyList()
//        return query.executeByIds(
//            entityClass = entityClass,
//            displayField = lookup.displayField ?: lookup.searchField ?: "name",
//            ids = ids
//        )
//    }

    fun lookupByIds(
        lookup: LookupDescriptor,
        ids: List<String>
    ): List<ObjectResponse> {
        logger.info("lookup by ids $lookup and ids $ids")
        if (ids.isEmpty()) return emptyList()

        val targetEntity = lookup.targetEntity ?: run {
            logger.warn("lookupByIds: targetEntity is null")
            return emptyList()
        }

        val entityClass = resolveEntityClass(targetEntity) ?: run {
            logger.warn("lookupByIds: could not resolve entity class for '$targetEntity'")
            return emptyList()
        }

        return query.executeByIds(
            entityClass = entityClass,
//            displayField = lookup.displayField ?: lookup.searchField ?: "name",
            ids = ids
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun resolveEntityClass(entityName: String): KClass<Any>? {
        return try {
            entityManager.entityManagerFactory.metamodel.entities
                .firstOrNull { entityType ->
                    // Match JPA entity provider (@Entity(provider=...))
                    entityType.name.equals(entityName, ignoreCase = true) ||
                            // Match Java class simple provider (ProductCategory)
                            entityType.javaType.simpleName.equals(entityName, ignoreCase = true)
                }
                ?.javaType
                ?.kotlin as? KClass<Any>
        } catch (e: Exception) {
            logger.error("resolveEntityClass failed for '$entityName': ${e.message}")
            null
        }
    }
}