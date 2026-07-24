package discovery

import com.kraftadmin.enums.ProviderType
import com.kraftadmin.spi.DiscoveredEntity
import com.kraftadmin.spi.EntityDiscoverer
import com.kraftadmin.spi.EntityDiscoveryService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private class MockUserEntity
private class MockPostEntity
private class MockTenantEntity

class EntityDiscoveryServiceTest {

    @Test
    fun `discoverAll should aggregate unique entities from all discoverers`() {
        val discoverer1 = mockk<EntityDiscoverer> {
            every { provider } returns ProviderType.JPA
            every { discover() } returns setOf(
                DiscoveredEntity(MockUserEntity::class.java, ProviderType.JPA),
                DiscoveredEntity(MockPostEntity::class.java, ProviderType.JPA)
            )
        }

        val discoverer2 = mockk<EntityDiscoverer> {
            every { provider } returns ProviderType.MONGO
            every { discover() } returns setOf(
                DiscoveredEntity(MockPostEntity::class.java, ProviderType.MONGO),
                DiscoveredEntity(MockTenantEntity::class.java, ProviderType.MONGO)
            )
        }

        val discoveryService =
            EntityDiscoveryService(listOf(discoverer1, discoverer2))

        val result = discoveryService.discoverAll()

        assertEquals(4, result.size)

        assertTrue(
            result.contains(
                DiscoveredEntity(
                    MockUserEntity::class.java,
                    ProviderType.JPA
                )
            )
        )

        assertTrue(
            result.contains(
                DiscoveredEntity(
                    MockPostEntity::class.java,
                    ProviderType.JPA
                )
            )
        )

        assertTrue(
            result.contains(
                DiscoveredEntity(
                    MockPostEntity::class.java,
                    ProviderType.MONGO
                )
            )
        )

        assertTrue(
            result.contains(
                DiscoveredEntity(
                    MockTenantEntity::class.java,
                    ProviderType.MONGO
                )
            )
        )
    }

    @Test
    fun `discoverAll should handle empty discoverer list gracefully`() {
        val discoveryService =
            EntityDiscoveryService(emptyList())

        val result = discoveryService.discoverAll()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `discoverAll should continue processing if a discoverer throws an exception`() {
        val failingDiscoverer = mockk<EntityDiscoverer> {
            every { provider } returns ProviderType.JPA
            every { discover() } throws RuntimeException(
                "Database connection failure"
            )
        }

        val healthyDiscoverer = mockk<EntityDiscoverer> {
            every { provider } returns ProviderType.MONGO
            every { discover() } returns setOf(
                DiscoveredEntity(
                    MockUserEntity::class.java,
                    ProviderType.MONGO
                )
            )
        }

        val discoveryService =
            EntityDiscoveryService(
                listOf(
                    failingDiscoverer,
                    healthyDiscoverer
                )
            )

        val result = discoveryService.discoverAll()

        assertEquals(1, result.size)

        assertTrue(
            result.contains(
                DiscoveredEntity(
                    MockUserEntity::class.java,
                    ProviderType.MONGO
                )
            )
        )

        verify(exactly = 1) {
            failingDiscoverer.discover()
        }

        verify(exactly = 1) {
            healthyDiscoverer.discover()
        }
    }

    @Test
    fun `discover should only return entities from the requested provider`() {
        val jpaDiscoverer = mockk<EntityDiscoverer> {
            every { provider } returns ProviderType.JPA
            every { discover() } returns setOf(
                DiscoveredEntity(
                    MockUserEntity::class.java,
                    ProviderType.JPA
                )
            )
        }

        val mongoDiscoverer = mockk<EntityDiscoverer> {
            every { provider } returns ProviderType.MONGO
            every { discover() } returns setOf(
                DiscoveredEntity(
                    MockTenantEntity::class.java,
                    ProviderType.MONGO
                )
            )
        }

        val discoveryService =
            EntityDiscoveryService(
                listOf(
                    jpaDiscoverer,
                    mongoDiscoverer
                )
            )

        val result =
            discoveryService.discover(ProviderType.JPA)

        assertEquals(1, result.size)

        assertTrue(
            result.contains(
                DiscoveredEntity(
                    MockUserEntity::class.java,
                    ProviderType.JPA
                )
            )
        )
    }

    @Test
    fun `discover should continue processing if matching discoverer throws`() {
        val failingDiscoverer = mockk<EntityDiscoverer> {
            every { provider } returns ProviderType.JPA
            every { discover() } throws RuntimeException(
                "Database connection failure"
            )
        }

        val healthyDiscoverer = mockk<EntityDiscoverer> {
            every { provider } returns ProviderType.JPA
            every { discover() } returns setOf(
                DiscoveredEntity(
                    MockUserEntity::class.java,
                    ProviderType.JPA
                )
            )
        }

        val discoveryService =
            EntityDiscoveryService(
                listOf(
                    failingDiscoverer,
                    healthyDiscoverer
                )
            )

        val result =
            discoveryService.discover(ProviderType.JPA)

        assertEquals(1, result.size)

        verify(exactly = 1) {
            failingDiscoverer.discover()
        }

        verify(exactly = 1) {
            healthyDiscoverer.discover()
        }
    }
}