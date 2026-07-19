package discovery.discoverer.r2dbc

import com.kraftadmin.enums.ProviderType
import com.kraftadmin.spi.DiscoveredEntity
import com.kraftadmin.spi.EntityDiscoverer
import org.springframework.context.ApplicationContext

class R2dbcEntityDiscoverer(
    private val applicationContext: ApplicationContext
) : EntityDiscoverer {
    override fun discover(): Set<DiscoveredEntity<*>> {
        TODO("Not yet implemented")
    }

    override val provider: ProviderType = ProviderType.R2DBC

}