package discovery.discoverer.mongo

import com.kraftadmin.enums.ProviderType
import com.kraftadmin.spi.DiscoveredEntity
import com.kraftadmin.spi.EntityDiscoverer
import org.springframework.context.ApplicationContext

class MongoDocumentDiscoverer(
    private val applicationContext: ApplicationContext
) : EntityDiscoverer {

    override val provider: ProviderType = ProviderType.JPA

    override fun discover(): Set<DiscoveredEntity<*>> {
        val mongoContextClass = try {
            Class.forName("org.springframework.data.mongodb.core.mapping.MongoMappingContext")
        } catch (_: ClassNotFoundException) {
            return emptySet()
        }

        val mongoContext = try {
            applicationContext.getBean(mongoContextClass)
        } catch (_: Exception) {
            return emptySet()
        }

        val persistentEntities =
            mongoContextClass.getMethod("getPersistentEntities").invoke(mongoContext) as Iterable<*>

//        return persistentEntities.mapNotNull {
//            it!!.javaClass.getMethod("getType").invoke(it) as Class<*>
//        }.toSet()

        return emptySet()
    }
}