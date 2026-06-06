package discovery

import com.kraftadmin.config.JpaDataProviderFactory
import com.kraftadmin.discovery.ResourceGenerator
import com.kraftadmin.enums.FormInputType
import config.KraftPulseSpringKraftAdminProperties
import jakarta.persistence.Entity
import jakarta.persistence.Id
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.ApplicationContext
import kotlin.reflect.KProperty

// Define a safe local testing entity model structure
@Entity
private class ManagedTestEntity {
    @Id
    var id: Long = 1L
    var title: String = "Test Title"
    var isActive: Boolean = true
}

class ResourceGeneratorTest {

    @Test
    fun `generate should extract entity properties fields and construct structural descriptors`() {
        // Arrange
        val mockContext = mockk<ApplicationContext>()
        val properties = KraftPulseSpringKraftAdminProperties()

        // Mock the conditional ObjectProvider behavior for JpaDataProviderFactory to prevent crashes
        val mockObjectProvider = mockk<ObjectProvider<JpaDataProviderFactory>>()
        every { mockContext.getBeanProvider(JpaDataProviderFactory::class.java) } returns mockObjectProvider
        every { mockObjectProvider.ifAvailable } returns null // Avoid injecting real JpaDataProvider dependencies here

        // Act
        val resource = ResourceGenerator.generate(ManagedTestEntity::class.java, mockContext, properties)

        // Assert
        assertNotNull(resource)
        assertEquals("ManagedTestEntity", resource.name)
        assertEquals("ManagedTestEntity", resource.label)
        assertEquals(ManagedTestEntity::class, resource.entityClass)

        // Verify the parsed descriptor definitions maps properties cleanly
        val titleColumn = resource.columns.find { it.name == "title" }
        assertNotNull(titleColumn)
        assertEquals(FormInputType.TEXT.name, titleColumn?.type)
        assertEquals("", titleColumn?.defaultValue)

        val activeColumn = resource.columns.find { it.name == "isActive" }
        assertNotNull(activeColumn)
        assertEquals(FormInputType.CHECKBOX.name, activeColumn?.type)
        assertEquals(false, activeColumn?.defaultValue)
    }

    @Test
    fun `getIdentifier should resolve entity id via getter execution patterns`() {
        // Arrange
        val mockContext = mockk<ApplicationContext>()
        val mockObjectProvider = mockk<ObjectProvider<JpaDataProviderFactory>>()
        every { mockContext.getBeanProvider(JpaDataProviderFactory::class.java) } returns mockObjectProvider
        every { mockObjectProvider.ifAvailable } returns null

        val resource = ResourceGenerator.generate(ManagedTestEntity::class.java, mockContext, KraftPulseSpringKraftAdminProperties())
        val entityInstance = ManagedTestEntity().apply { id = 99L }

        // Act
        val resolvedId = resource.getIdentifier(entityInstance)

        // Assert
        assertEquals(99L, resolvedId)
    }

    @Test
    fun `resolveAnnotation should catch metadata attributes across different field contexts`() {
        // Arrange
        val entityClass = ManagedTestEntity::class.java
        val javaField = entityClass.getDeclaredField("id")
        val kotlinProp = ManagedTestEntity::class.members.find { it.name == "id" } as? KProperty<*>

        // Act
        val result = ResourceGenerator.resolveAnnotation(javaField, kotlinProp, Id::class)

        // Assert
        assertNotNull(result)
        assertTrue(result is Id)
    }
}