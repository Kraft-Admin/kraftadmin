package discovery

import com.kraftadmin.enums.ProviderType
import com.kraftadmin.spi.DiscoveredEntity
import discovery.discoverer.jpa.JpaEntityDiscoverer
import io.mockk.every
import io.mockk.mockk
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.metamodel.EntityType
import jakarta.persistence.metamodel.Metamodel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationContext

private class SampleJpaEntity

class JpaEntityDiscovererTest {

    @Test
    fun `discover should extract entities from all registered EntityManagerFactories`() {
        // Arrange
        val mockContext = mockk<ApplicationContext>()
        val mockEmf = mockk<EntityManagerFactory>()
        val mockMetamodel = mockk<Metamodel>()
        val mockEntityType = mockk<EntityType<SampleJpaEntity>>()

        every {
            mockContext.getBeansOfType(EntityManagerFactory::class.java)
        } returns mapOf("emf" to mockEmf)

        every {
            mockEmf.metamodel
        } returns mockMetamodel

        every {
            mockMetamodel.entities
        } returns setOf(mockEntityType)

        every {
            mockEntityType.javaType
        } returns SampleJpaEntity::class.java

        val discoverer = JpaEntityDiscoverer(mockContext)

        // Act
        val result = discoverer.discover()

        // Assert
        assertEquals(ProviderType.JPA, discoverer.provider)
        assertEquals(1, result.size)

        val discoveredEntity = result.first()

        assertEquals(
            SampleJpaEntity::class.java,
            discoveredEntity.entityClass
        )

        assertEquals(
            ProviderType.JPA,
            discoveredEntity.provider
        )
    }

    @Test
    fun `discover should return empty set if no EntityManagerFactories exist`() {
        // Arrange
        val mockContext = mockk<ApplicationContext>()

        every {
            mockContext.getBeansOfType(EntityManagerFactory::class.java)
        } returns emptyMap()

        val discoverer = JpaEntityDiscoverer(mockContext)

        // Act
        val result = discoverer.discover()

        // Assert
        assertTrue(result.isEmpty())
    }
}