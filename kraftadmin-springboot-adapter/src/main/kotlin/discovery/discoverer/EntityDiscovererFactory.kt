package discovery.discoverer

import com.kraftadmin.enums.ProviderType
import com.kraftadmin.spi.EntityDiscoverer

class EntityDiscovererFactory(
    discoverers: List<EntityDiscoverer>
) {

    private val discoverersByProvider =
        discoverers.associateBy { it.provider }

    fun get(provider: ProviderType): EntityDiscoverer? =
        discoverersByProvider[provider]

    fun getAll(): List<EntityDiscoverer> =
        discoverersByProvider.values.toList()

}