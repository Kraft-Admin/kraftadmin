package discovery.descriptors.column

import com.kraftadmin.enums.ProviderType
import discovery.descriptors.column.jpa.JpaColumnBuildStrategy
import discovery.descriptors.column.mongo.MongoColumnBuildStrategy
import discovery.descriptors.column.r2dbc.R2dbcColumnBuildStrategy
import org.slf4j.LoggerFactory

/**
 * Selects the correct strategy based on the active provider.
 */
object ColumnBuildStrategyFactory {

    private val logger = LoggerFactory.getLogger(ColumnBuildStrategyFactory::class.java)

    fun create(provider: ProviderType): ColumnBuildStrategy {

        logger.debug("Creating column build strategy for provider: $provider")


        return when (provider) {
            ProviderType.JPA -> JpaColumnBuildStrategy()
            ProviderType.MONGO -> MongoColumnBuildStrategy()
            ProviderType.R2DBC -> R2dbcColumnBuildStrategy()
        } as ColumnBuildStrategy

    }


}