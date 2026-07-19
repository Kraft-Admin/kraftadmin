package config

import com.kraftadmin.logging.KraftAdminLogging
import com.kraftadmin.spi.EntityDiscoverer
import discovery.discoverer.mongo.MongoDocumentDiscoverer
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.data.mongodb.core.mapping.MongoMappingContext

@AutoConfiguration
@ConditionalOnClass(name = ["org.springframework.data.mongodb.core.MongoTemplate"])
@ConditionalOnProperty(prefix = "kraftadmin", name = ["enabled"], havingValue = "true")
class KraftAdminMongoAutoConfiguration {

    private val logger = KraftAdminLogging.logger(javaClass)


    @Bean
    @ConditionalOnBean(MongoMappingContext::class)
    fun mongoEntityDiscoverer(
        applicationContext: ApplicationContext
    ): EntityDiscoverer {
        logger.info("Registering MongoDB Document Discoverer")
        return MongoDocumentDiscoverer(applicationContext)
    }
}

