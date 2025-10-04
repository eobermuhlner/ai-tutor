package ch.obermuhlner.aitutor.auth.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class JwtPropertiesTest {

    @Test
    fun `JwtProperties should have default values`() {
        val properties = JwtProperties()

        assertEquals("", properties.secret)
        assertEquals(3600000L, properties.expirationMs)  // 1 hour
        assertEquals(2592000000L, properties.refreshExpirationMs)  // 30 days
    }

    @Test
    fun `JwtProperties should allow setting custom values`() {
        val properties = JwtProperties(
            secret = "test-secret-key",
            expirationMs = 7200000L,
            refreshExpirationMs = 5184000000L
        )

        assertEquals("test-secret-key", properties.secret)
        assertEquals(7200000L, properties.expirationMs)
        assertEquals(5184000000L, properties.refreshExpirationMs)
    }

    @Test
    fun `JwtProperties should be mutable`() {
        val properties = JwtProperties()

        properties.secret = "updated-secret"
        properties.expirationMs = 1800000L
        properties.refreshExpirationMs = 1296000000L

        assertEquals("updated-secret", properties.secret)
        assertEquals(1800000L, properties.expirationMs)
        assertEquals(1296000000L, properties.refreshExpirationMs)
    }

    @Test
    fun `JwtProperties equality should work correctly`() {
        val properties1 = JwtProperties("secret", 3600000L, 2592000000L)
        val properties2 = JwtProperties("secret", 3600000L, 2592000000L)
        val properties3 = JwtProperties("different", 3600000L, 2592000000L)

        assertEquals(properties1, properties2)
        assertNotEquals(properties1, properties3)
    }
}
