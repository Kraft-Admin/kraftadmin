package config

import com.kraftadmin.logging.KraftAdminLogging
import com.kraftadmin.spi.EntityDiscoverer
import com.kraftadmin.spi.EntityDiscoveryService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean

@AutoConfiguration
@AutoConfigureAfter(
    KraftAdminJpaAutoConfiguration::class,
    KraftAdminMongoAutoConfiguration::class
)
@ConditionalOnProperty(prefix = "kraftadmin", name = ["enabled"], havingValue = "true")
class KraftAdminDiscoveryAutoConfiguration {

    private val logger = KraftAdminLogging.logger(javaClass)


    @Bean
    fun entityDiscoveryService(
        discoverers: List<EntityDiscoverer>
    ): EntityDiscoveryService {
//        logger.info("Creating EntityDiscoveryService")
//        logger.info("   Discoverers found: ${discoverers.size}")

        discoverers.forEach { discoverer ->
            logger.info("   - ${discoverer.provider} Discoverer")
        }

        if (discoverers.isEmpty()) {
            logger.warn("No discoverers registered! Entities won't be found.")
        }

        return EntityDiscoveryService(discoverers)
    }
}