package ch.obermuhlner.aitutor.user.service

import ch.obermuhlner.aitutor.user.domain.AuthProvider
import ch.obermuhlner.aitutor.user.domain.UserEntity
import ch.obermuhlner.aitutor.user.domain.UserRole
import ch.obermuhlner.aitutor.user.repository.UserRepository
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import java.util.Optional
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.password.PasswordEncoder

class UserServiceTest {

    private lateinit var userService: UserService
    private lateinit var userRepository: UserRepository
    private lateinit var passwordEncoder: PasswordEncoder

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        passwordEncoder = mockk()
        userService = UserService(userRepository, passwordEncoder)
    }

    @Test
    fun `findById should return user when exists`() {
        val userId = UUID.randomUUID()
        val user = UserEntity(
            id = userId,
            username = "testuser",
            email = "test@example.com",
            roles = mutableSetOf(UserRole.USER),
            provider = AuthProvider.CREDENTIALS
        )

        every { userRepository.findById(userId) } returns Optional.of(user)

        val result = userService.findById(userId)

        assertNotNull(result)
        assertEquals(userId, result?.id)
        assertEquals("testuser", result?.username)
    }

    @Test
    fun `findById should return null when user does not exist`() {
        val userId = UUID.randomUUID()

        every { userRepository.findById(userId) } returns Optional.empty()

        val result = userService.findById(userId)

        assertNull(result)
    }

    @Test
    fun `findByUsername should return user when exists`() {
        val user = UserEntity(
            username = "testuser",
            email = "test@example.com",
            roles = mutableSetOf(UserRole.USER),
            provider = AuthProvider.CREDENTIALS
        )

        every { userRepository.findByUsername("testuser") } returns user

        val result = userService.findByUsername("testuser")

        assertNotNull(result)
        assertEquals("testuser", result?.username)
    }

    @Test
    fun `findByEmail should return user when exists`() {
        val user = UserEntity(
            username = "testuser",
            email = "test@example.com",
            roles = mutableSetOf(UserRole.USER),
            provider = AuthProvider.CREDENTIALS
        )

        every { userRepository.findByEmail("test@example.com") } returns user

        val result = userService.findByEmail("test@example.com")

        assertNotNull(result)
        assertEquals("test@example.com", result?.email)
    }

    @Test
    fun `existsByUsername should return true when username exists`() {
        every { userRepository.existsByUsername("testuser") } returns true

        val result = userService.existsByUsername("testuser")

        assertTrue(result)
    }

    @Test
    fun `existsByUsername should return false when username does not exist`() {
        every { userRepository.existsByUsername("nonexistent") } returns false

        val result = userService.existsByUsername("nonexistent")

        assertFalse(result)
    }

    @Test
    fun `createUser should hash password and save user`() {
        val user = UserEntity(
            username = "newuser",
            email = "new@example.com",
            passwordHash = "plainpassword",
            roles = mutableSetOf(UserRole.USER),
            provider = AuthProvider.CREDENTIALS
        )

        every { passwordEncoder.encode("plainpassword") } returns "hashedpassword"
        every { userRepository.save(any()) } answers { firstArg() }

        val result = userService.createUser(user)

        assertEquals("hashedpassword", result.passwordHash)
        verify { passwordEncoder.encode("plainpassword") }
        verify { userRepository.save(any()) }
    }

    @Test
    fun `createUser should not re-hash already hashed password`() {
        val hashedPassword = "\$2a\$12\$alreadyhashed"
        val user = UserEntity(
            username = "newuser",
            email = "new@example.com",
            passwordHash = hashedPassword,
            roles = mutableSetOf(UserRole.USER),
            provider = AuthProvider.CREDENTIALS
        )

        every { userRepository.save(any()) } answers { firstArg() }

        val result = userService.createUser(user)

        assertEquals(hashedPassword, result.passwordHash)
        verify(exactly = 0) { passwordEncoder.encode(any()) }
        verify { userRepository.save(any()) }
    }

    @Test
    fun `updatePassword should hash and save new password`() {
        val userId = UUID.randomUUID()
        val user = UserEntity(
            id = userId,
            username = "testuser",
            email = "test@example.com",
            passwordHash = "oldhash",
            roles = mutableSetOf(UserRole.USER),
            provider = AuthProvider.CREDENTIALS
        )

        every { userRepository.findById(userId) } returns Optional.of(user)
        every { passwordEncoder.encode("newpassword") } returns "newhash"
        every { userRepository.save(any()) } answers { firstArg() }

        userService.updatePassword(userId, "newpassword")

        verify { passwordEncoder.encode("newpassword") }
        verify { userRepository.save(match { it.passwordHash == "newhash" }) }
    }

    @Test
    fun `deleteUser should delete user from repository`() {
        val userId = UUID.randomUUID()

        every { userRepository.deleteById(userId) } just Runs

        userService.deleteUser(userId)

        verify { userRepository.deleteById(userId) }
    }

    @Test
    fun `findAll should return all users`() {
        val users = listOf(
            UserEntity(username = "user1", email = "user1@example.com", roles = mutableSetOf(UserRole.USER), provider = AuthProvider.CREDENTIALS),
            UserEntity(username = "user2", email = "user2@example.com", roles = mutableSetOf(UserRole.USER), provider = AuthProvider.CREDENTIALS)
        )

        every { userRepository.findAll() } returns users

        val result = userService.findAll()

        assertEquals(2, result.size)
        assertEquals("user1", result[0].username)
        assertEquals("user2", result[1].username)
    }
}
