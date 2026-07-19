package discovery.discoverer.jpa

import com.kraftadmin.enums.ProviderType
import com.kraftadmin.spi.DiscoveredEntity
import com.kraftadmin.spi.EntityDiscoverer
import com.kraftadmin.logging.KraftAdminLogging
import jakarta.persistence.EntityManagerFactory
import org.springframework.context.ApplicationContext

class JpaEntityDiscoverer(
    private val applicationContext: ApplicationContext
) : EntityDiscoverer {

    private val logger = KraftAdminLogging.logger(javaClass)

    override val provider: ProviderType = ProviderType.JPA

    override fun discover(): Set<DiscoveredEntity<*>> {
        logger.info("JPA Discoverer - Scanning")

        val entityManagerFactories =
            applicationContext.getBeansOfType(EntityManagerFactory::class.java).values.toList()

        val entities = mutableSetOf<DiscoveredEntity<*>>()
        entityManagerFactories.forEach { emf ->
            val emfEntities = emf.metamodel.entities.map {  it ->
                DiscoveredEntity(
                entityClass = it.javaType,
                provider = ProviderType.JPA
                )
            }
            logger.debug("EMF ${emf.persistenceUnitUtil}: ${emfEntities.size} entities")
            entities.addAll(emfEntities)
        }

        logger.info("JPA Discoverer - Found ${entities.size} entities")
        return entities
    }

}