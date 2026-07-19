package discovery

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
    fun `discover should extract and return javaTypes from all registered EntityManagerFactories`() {
        // Arrange
        val mockContext = mockk<ApplicationContext>()
        val mockEmf = mockk<EntityManagerFactory>()
        val mockMetamodel = mockk<Metamodel>()
        val mockEntityType = mockk<EntityType<*>>()

        every { mockContext.getBeansOfType(EntityManagerFactory::class.java) } returns mapOf("emf" to mockEmf)
        every { mockEmf.metamodel } returns mockMetamodel
        every { mockEmf.persistenceUnitUtil } returns mockk(relaxed = true)
        every { mockMetamodel.entities } returns setOf(mockEntityType)
        every { mockEntityType.javaType } returns SampleJpaEntity::class.java

        val discoverer = JpaEntityDiscoverer(mockContext)

        // Act
        val result = discoverer.discover()

        // Assert
        assertEquals("JPA", discoverer.provider)
        assertEquals(1, result.size)
        assertTrue(result.contains(SampleJpaEntity::class.java))
    }

    @Test
    fun `discover should return empty set if no EntityManagerFactories exist`() {
        // Arrange
        val mockContext = mockk<ApplicationContext>()
        every { mockContext.getBeansOfType(EntityManagerFactory::class.java) } returns emptyMap()

        val discoverer = JpaEntityDiscoverer(mockContext)

        // Act
        val result = discoverer.discover()

        // Assert
        assertTrue(result.isEmpty())
    }
}