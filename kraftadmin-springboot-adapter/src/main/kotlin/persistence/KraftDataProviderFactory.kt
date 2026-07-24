package persistence

import com.kraftadmin.enums.ProviderType
import com.kraftadmin.spi.DiscoveredEntity
import com.kraftadmin.spi.KraftDataProvider
import config.KraftAdminProperties
import org.springframework.context.ApplicationContext

/**
 * One implementation per persistence backend (JPA, Mongo, JDBC...).
 * Registered as a Spring bean; ResourceGenerator discovers all of them
 * and picks the one whose supports() matches the entity's provider type.
 */
interface KraftDataProviderFactory<T : Any> {

    fun supports(providerType: ProviderType): Boolean

    /** Return null if this backend can't actually build a provider for this entity
     *  (e.g. missing @Entity annotation, no EntityManager bean, etc). */
    fun create(
        discoveredEntity: DiscoveredEntity<T>,
        context: ApplicationContext,
        properties: KraftAdminProperties
    ): KraftDataProvider<T>?


}