package discovery

import config.JpaDataProviderFactory
import com.kraftadmin.spi.AbstractResource
import com.kraftadmin.spi.DiscoveredEntity
import com.kraftadmin.spi.KraftAdminResource
import com.kraftadmin.utils.files.AdminStorageProvider
import config.KraftAdminProperties
import discovery.descriptors.action.ActionDescriptorBuilder
import discovery.descriptors.column.ColumnBuildStrategyFactory
import discovery.descriptors.column.KraftColumnBuilder
import events.SpringActionRegistry
import events.SpringKraftLifecycleService
import jakarta.persistence.Entity
import org.springframework.context.ApplicationContext
import org.springframework.transaction.support.TransactionTemplate
import persistence.jpa.metadata.EntityMetadata
import persistence.jpa.provider.JpaDataProvider
import persistence.jpa.validation.PersistenceValidationService
import security.SecurityProviderChain

object ResourceGenerator {

    fun <T : Any> generate(
        discoveredEntity: DiscoveredEntity<T>,
        context: ApplicationContext,
        properties: KraftAdminProperties,
    ): KraftAdminResource<T> {


        val kClass = discoveredEntity.entityClass
        val metadata = EntityMetadata(discoveredEntity.entityClass.kotlin)

        val actionRegistry = context.getBean(SpringActionRegistry::class.java)

        val adminResource =
            discoveredEntity.entityClass.getAnnotation(com.kraftadmin.annotations.KraftAdminResource::class.java)

        val columnBuilder = KraftColumnBuilder(
            ColumnBuildStrategyFactory.create(discoveredEntity.provider)
        )

        val actionBuilder = ActionDescriptorBuilder(actionRegistry)


        val resource = object : AbstractResource<T>(
            name = kClass.simpleName ?: "Unknown",
            label = adminResource?.label?.ifBlank { kClass.simpleName ?: "Unknown" }
                ?: kClass.simpleName ?: "Unknown",
            entityClass = discoveredEntity.entityClass.kotlin,
            group = adminResource?.group ?: "Main",
            icon = adminResource?.icon ?: "📁",
            isHidden = adminResource?.hidden ?: false,
            isSearchable = adminResource?.searchable ?: true,
            defaultSort = adminResource?.defaultSort ?: "",
            isReadOnly = adminResource?.readOnly ?: false,
            pageSize = adminResource?.pageSize ?: 20,
            permissionScope = adminResource?.permissionScope ?: "ALL",
            isExportable = adminResource?.exportable ?: true,
            provider = discoveredEntity.provider,
        ) {

            init {

                val builtColumns = columnBuilder.build(
                    entityClass = discoveredEntity.entityClass.kotlin
                )

               this.columns = builtColumns

            }

            override val customActions by lazy {
                if (adminResource == null) {
                    emptyList()
                } else {
                    actionBuilder.build(adminResource::class)
                }
            }

            override val searchableColumns by lazy {
                metadata.searchableFields
            }

            override val sortableColumns by lazy {
                metadata.sortableFields
            }

            override fun getIdentifier(entity: T): Any = {}


        }

        attachDataProvider(
            resource,
            discoveredEntity,
            context,
            properties
        )

        return resource
    }

    private fun <T : Any> attachDataProvider(
        resource: AbstractResource<T>,
        discoveredEntity: DiscoveredEntity<T>,
        context: ApplicationContext,
        properties: KraftAdminProperties
    ) {
        val factory = context.getBeanProvider(JpaDataProviderFactory::class.java).ifAvailable
        if (factory != null && discoveredEntity.entityClass.isAnnotationPresent(Entity::class.java)) {
            resource.dataProvider = JpaDataProvider(
                entityClass = discoveredEntity.entityClass.kotlin,
                transactionTemplate = context.getBean(TransactionTemplate::class.java),
                adminStorageProvider = context.getBean(AdminStorageProvider::class.java),
                securityChain = context.getBean(SecurityProviderChain::class.java),
                properties = properties,
                entityManager = factory.entityManager,
                applicationContext = context,
                paginationProperties = properties.pagination,
                lifecycleService = context.getBean(SpringKraftLifecycleService::class.java),
                persistenceValidationService = context.getBean(PersistenceValidationService::class.java)
            )
        }

    }

}