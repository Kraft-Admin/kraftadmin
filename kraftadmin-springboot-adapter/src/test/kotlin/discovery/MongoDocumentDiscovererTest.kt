package discovery

import com.kraftadmin.discovery.MongoDocumentDiscoverer
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationContext
import org.springframework.data.mongodb.core.mapping.BasicMongoPersistentEntity
import org.springframework.data.mongodb.core.mapping.MongoMappingContext
import kotlin.jvm.java
import kotlin.test.assertEquals

private class SampleMongoDoc

class MongoDocumentDiscovererTest {

    @Test
    fun `discover should return empty set if application context lacks MongoMappingContext bean`() {
        // Arrange
        val mockContext = mockk<ApplicationContext>()
        // Simulate exception thrown when grabbing the bean
        every { mockContext.getBean(any<Class<*>>()) } throws RuntimeException("Bean not found")

        val discoverer = MongoDocumentDiscoverer(mockContext)

        // Act
        val result = discoverer.discover()

        // Assert
        assertEquals("Mongo", discoverer.name)
        assertTrue(result.isEmpty())

    }


    
    @Test
    fun `discover should cleanly extract mapped types from persistent entities metadata`() {
        // Arrange
        val mockContext = mockk<ApplicationContext>()
        val mockMappingContext = mockk<MongoMappingContext>()
        val mockPersistentEntity = mockk<BasicMongoPersistentEntity<*>>()

        every { mockContext.getBean(MongoMappingContext::class.java) } returns mockMappingContext
        every { mockMappingContext.persistentEntities } returns listOf(mockPersistentEntity)
        every { mockPersistentEntity.type } returns SampleMongoDoc::class.java

        val discoverer = MongoDocumentDiscoverer(mockContext)

        // Act
        val result = discoverer.discover()

        // Assert
        assertTrue(result.contains(SampleMongoDoc::class.java))
    }
}