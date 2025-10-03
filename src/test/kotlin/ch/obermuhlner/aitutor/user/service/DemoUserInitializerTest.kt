package ch.obermuhlner.aitutor.user.service

import ch.obermuhlner.aitutor.user.domain.UserEntity
import ch.obermuhlner.aitutor.user.domain.UserRole
import io.mockk.*
import org.junit.jupiter.api.Test

class DemoUserInitializerTest {

    @Test
    fun `should create demo user when enabled and user does not exist`() {
        val userService = mockk<UserService>()
        val properties = DemoUserProperties(
            enabled = true,
            username = "demo",
            password = "demo",
            email = "demo@localhost"
        )

        every { userService.findByUsername("demo") } returns null
        every { userService.createUser(any()) } returns mockk()

        val initializer = DemoUserInitializer(userService, properties)
        initializer.initializeDemoUser()

        verify {
            userService.createUser(match { user: UserEntity ->
                user.username == "demo" &&
                user.email == "demo@localhost" &&
                user.passwordHash == "demo" &&
                user.roles.contains(UserRole.USER)
            })
        }
    }

    @Test
    fun `should not create demo user when already exists`() {
        val userService = mockk<UserService>()
        val properties = DemoUserProperties(
            enabled = true,
            username = "demo",
            password = "demo",
            email = "demo@localhost"
        )

        val existingUser = UserEntity(
            username = "demo",
            email = "demo@localhost"
        )
        every { userService.findByUsername("demo") } returns existingUser

        val initializer = DemoUserInitializer(userService, properties)
        initializer.initializeDemoUser()

        verify(exactly = 0) { userService.createUser(any()) }
    }

    @Test
    fun `should not create demo user when disabled`() {
        val userService = mockk<UserService>()
        val properties = DemoUserProperties(
            enabled = false,
            username = "demo",
            password = "demo",
            email = "demo@localhost"
        )

        val initializer = DemoUserInitializer(userService, properties)
        initializer.initializeDemoUser()

        verify(exactly = 0) { userService.findByUsername(any()) }
        verify(exactly = 0) { userService.createUser(any()) }
    }
}
