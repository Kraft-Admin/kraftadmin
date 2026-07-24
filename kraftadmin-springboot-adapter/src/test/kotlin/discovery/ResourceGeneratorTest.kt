package discovery

import com.kraftadmin.enums.FormInputType
import com.kraftadmin.enums.ProviderType
import com.kraftadmin.spi.DiscoveredEntity
import config.KraftAdminProperties
import events.SpringActionRegistry
import io.mockk.every
import io.mockk.mockk
import jakarta.persistence.Entity
import jakarta.persistence.Id
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationContext
import persistence.KraftDataProviderFactory

@Entity
private class ManagedTestEntity {

    @Id
    var id: Long = 1L

    var title: String = "Test Title"

    var isActive: Boolean = true
}

class ResourceGeneratorTest {

    private fun discoveredEntity(): DiscoveredEntity<ManagedTestEntity> =
        DiscoveredEntity(
            entityClass = ManagedTestEntity::class.java,
            provider = ProviderType.JPA
        )

    private fun mockContext(): ApplicationContext {
        val context = mockk<ApplicationContext>()

        every {
            context.getBean(SpringActionRegistry::class.java)
        } returns mockk(relaxed = true)

        every {
            context.getBeansOfType(KraftDataProviderFactory::class.java)
        } returns emptyMap()

        return context
    }

    @Test
    fun `generate should extract entity properties and construct structural descriptors`() {

        val context = mockContext()

        val resource = ResourceGenerator.generate(
            discoveredEntity = discoveredEntity(),
            context = context,
            properties = KraftAdminProperties()
        )

        assertNotNull(resource)

        assertEquals(
            "ManagedTestEntity",
            resource.name
        )

        assertEquals(
            "ManagedTestEntity",
            resource.label
        )

        assertEquals(
            ManagedTestEntity::class,
            resource.entityClass
        )

        assertEquals(
            ProviderType.JPA,
            resource.provider
        )

        val titleColumn =
            resource.columns.find { it.name == "title" }

        assertNotNull(titleColumn)

        assertEquals(
            FormInputType.TEXT,
            titleColumn?.type
        )

        assertEquals(
            "",
            titleColumn?.defaultValue
        )

        val activeColumn =
            resource.columns.find { it.name == "isActive" }

        assertNotNull(activeColumn)

        assertEquals(
            FormInputType.CHECKBOX,
            activeColumn?.type
        )

        assertEquals(
            false,
            activeColumn?.defaultValue
        )
    }

    @Test
    fun `getIdentifier should resolve entity identifier`() {

        val context = mockContext()

        val resource = ResourceGenerator.generate(
            discoveredEntity = discoveredEntity(),
            context = context,
            properties = KraftAdminProperties()
        )

        val entity =
            ManagedTestEntity().apply {
                id = 99L
            }

        val resolvedId =
            resource.getIdentifier(entity)

        assertEquals(
            99L,
            resolvedId
        )
    }

    @Test
    fun `generate should not attach a data provider when no matching factory exists`() {

        val context = mockContext()

        val resource = ResourceGenerator.generate(
            discoveredEntity = discoveredEntity(),
            context = context,
            properties = KraftAdminProperties()
        )

        assertFalse(
            resource.dataProvider != null
        )
    }
}