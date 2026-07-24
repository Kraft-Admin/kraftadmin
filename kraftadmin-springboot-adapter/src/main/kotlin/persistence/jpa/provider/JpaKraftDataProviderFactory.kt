package persistence.jpa.provider

import com.kraftadmin.enums.ProviderType
import com.kraftadmin.spi.*
import config.JpaDataProviderFactory
import config.KraftAdminProperties
import events.SpringKraftLifecycleService
import jakarta.persistence.Entity
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import persistence.jpa.validation.PersistenceValidationService
import security.SecurityProviderChain
import com.kraftadmin.utils.files.AdminStorageProvider
import persistence.KraftDataProviderFactory

@Component
class JpaKraftDataProviderFactory : KraftDataProviderFactory<Any> {

    override fun supports(providerType: ProviderType) = providerType == ProviderType.JPA

    override fun create(
        discoveredEntity: DiscoveredEntity<Any>,
        context: ApplicationContext,
        properties: KraftAdminProperties
    ): KraftDataProvider<Any>? {
        if (!discoveredEntity.entityClass.isAnnotationPresent(Entity::class.java)) return null
        val emFactory = context.getBeanProvider(JpaDataProviderFactory::class.java).ifAvailable ?: return null

        return JpaDataProvider(
            entityClass = discoveredEntity.entityClass.kotlin,
            transactionTemplate = context.getBean(TransactionTemplate::class.java),
            adminStorageProvider = context.getBean(AdminStorageProvider::class.java),
            securityChain = context.getBean(SecurityProviderChain::class.java),
            properties = properties,
            entityManager = emFactory.entityManager,
            applicationContext = context,
            paginationProperties = properties.pagination,
            lifecycleService = context.getBean(SpringKraftLifecycleService::class.java),
            persistenceValidationService = context.getBean(PersistenceValidationService::class.java)
        )
    }

}