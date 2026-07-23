package discovery.metrics

import com.kraftadmin.annotations.KraftAdminMetric
import com.kraftadmin.spi.DiscoveredEntity
import com.kraftadmin.spi.DiscoveredMetric

object MetricDiscoverer {
    fun discover(entities: Set<DiscoveredEntity<*>>): List<DiscoveredMetric> {
        return entities.mapNotNull { discovered ->
            val annotation = discovered.entityClass.getAnnotation(KraftAdminMetric::class.java)
                ?: return@mapNotNull null
            DiscoveredMetric(discovered.entityClass, discovered.provider, annotation)
        }
    }
}