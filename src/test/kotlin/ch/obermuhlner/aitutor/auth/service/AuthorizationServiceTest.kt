package ch.obermuhlner.aitutor.auth.service

import ch.obermuhlner.aitutor.auth.exception.InsufficientPermissionsException
import ch.obermuhlner.aitutor.chat.domain.ChatSessionEntity
import ch.obermuhlner.aitutor.chat.repository.ChatSessionRepository
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import ch.obermuhlner.aitutor.user.domain.AuthProvider
import ch.obermuhlner.aitutor.user.domain.UserEntity
import ch.obermuhlner.aitutor.user.domain.UserRole
import ch.obermuhlner.aitutor.user.service.UserService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import java.util.Optional
import java.util.UUID

class AuthorizationServiceTest {

    private lateinit var authorizationService: AuthorizationService
    private lateinit var userService: UserService
    private lateinit var sessionRepository: ChatSessionRepository

    @BeforeEach
    fun setUp() {
        userService = mockk()
        sessionRepository = mockk()
        authorizationService = AuthorizationService(userService, sessionRepository)
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `getCurrentUserId should return current user ID when authenticated`() {
        val userId = UUID.randomUUID()
        val username = "testuser"
        val userDetails = User(username, "password", emptyList())
        val userEntity = createUser(userId, username)

        setupAuthentication(userDetails)
        every { userService.findByUsername(username) } returns userEntity

        val result = authorizationService.getCurrentUserId()

        assertEquals(userId, result)
    }

    @Test
    fun `getCurrentUserId should throw IllegalStateException when no authentication`() {
        SecurityContextHolder.clearContext()

        assertThrows(IllegalStateException::class.java) {
            authorizationService.getCurrentUserId()
        }
    }

    @Test
    fun `getCurrentUserId should throw IllegalStateException when principal is not UserDetails`() {
        val authentication = UsernamePasswordAuthenticationToken("not-user-details", null, emptyList())
        SecurityContextHolder.getContext().authentication = authentication

        assertThrows(IllegalStateException::class.java) {
            authorizationService.getCurrentUserId()
        }
    }

    @Test
    fun `getCurrentUserId should throw IllegalStateException when user not found`() {
        val username = "testuser"
        val userDetails = User(username, "password", emptyList())

        setupAuthentication(userDetails)
        every { userService.findByUsername(username) } returns null

        assertThrows(IllegalStateException::class.java) {
            authorizationService.getCurrentUserId()
        }
    }

    @Test
    fun `getCurrentUser should return current user entity when authenticated`() {
        val userId = UUID.randomUUID()
        val username = "testuser"
        val userDetails = User(username, "password", emptyList())
        val userEntity = createUser(userId, username)

        setupAuthentication(userDetails)
        every { userService.findByUsername(username) } returns userEntity

        val result = authorizationService.getCurrentUser()

        assertEquals(userEntity, result)
    }

    @Test
    fun `getCurrentUser should throw IllegalStateException when no authentication`() {
        SecurityContextHolder.clearContext()

        assertThrows(IllegalStateException::class.java) {
            authorizationService.getCurrentUser()
        }
    }

    @Test
    fun `isAdmin should return true for admin user`() {
        val userId = UUID.randomUUID()
        val username = "admin"
        val userDetails = User(username, "password", listOf(SimpleGrantedAuthority("ROLE_ADMIN")))
        val userEntity = createUser(userId, username, roles = setOf(UserRole.ADMIN))

        setupAuthentication(userDetails)
        every { userService.findByUsername(username) } returns userEntity

        val result = authorizationService.isAdmin()

        assertTrue(result)
    }

    @Test
    fun `isAdmin should return false for non-admin user`() {
        val userId = UUID.randomUUID()
        val username = "user"
        val userDetails = User(username, "password", emptyList())
        val userEntity = createUser(userId, username)

        setupAuthentication(userDetails)
        every { userService.findByUsername(username) } returns userEntity

        val result = authorizationService.isAdmin()

        assertFalse(result)
    }

    @Test
    fun `isAdmin should return false when no authentication`() {
        SecurityContextHolder.clearContext()

        val result = authorizationService.isAdmin()

        assertFalse(result)
    }

    @Test
    fun `canAccessUser should return true for own user`() {
        val userId = UUID.randomUUID()
        val username = "testuser"
        val userDetails = User(username, "password", emptyList())
        val userEntity = createUser(userId, username)

        setupAuthentication(userDetails)
        every { userService.findByUsername(username) } returns userEntity

        val result = authorizationService.canAccessUser(userId)

        assertTrue(result)
    }

    @Test
    fun `canAccessUser should return true for admin accessing another user`() {
        val adminId = UUID.randomUUID()
        val targetUserId = UUID.randomUUID()
        val username = "admin"
        val userDetails = User(username, "password", listOf(SimpleGrantedAuthority("ROLE_ADMIN")))
        val adminEntity = createUser(adminId, username, roles = setOf(UserRole.ADMIN))

        setupAuthentication(userDetails)
        every { userService.findByUsername(username) } returns adminEntity

        val result = authorizationService.canAccessUser(targetUserId)

        assertTrue(result)
    }

    @Test
    fun `canAccessUser should return false for non-admin accessing another user`() {
        val userId = UUID.randomUUID()
        val targetUserId = UUID.randomUUID()
        val username = "testuser"
        val userDetails = User(username, "password", emptyList())
        val userEntity = createUser(userId, username)

        setupAuthentication(userDetails)
        every { userService.findByUsername(username) } returns userEntity

        val result = authorizationService.canAccessUser(targetUserId)

        assertFalse(result)
    }

    @Test
    fun `canAccessUser should return false when no authentication`() {
        SecurityContextHolder.clearContext()

        val result = authorizationService.canAccessUser(UUID.randomUUID())

        assertFalse(result)
    }

    @Test
    fun `requireAccessToUser should not throw for own user`() {
        val userId = UUID.randomUUID()
        val username = "testuser"
        val userDetails = User(username, "password", emptyList())
        val userEntity = createUser(userId, username)

        setupAuthentication(userDetails)
        every { userService.findByUsername(username) } returns userEntity

        authorizationService.requireAccessToUser(userId)
    }

    @Test
    fun `requireAccessToUser should not throw for admin`() {
        val adminId = UUID.randomUUID()
        val targetUserId = UUID.randomUUID()
        val username = "admin"
        val userDetails = User(username, "password", listOf(SimpleGrantedAuthority("ROLE_ADMIN")))
        val adminEntity = createUser(adminId, username, roles = setOf(UserRole.ADMIN))

        setupAuthentication(userDetails)
        every { userService.findByUsername(username) } returns adminEntity

        authorizationService.requireAccessToUser(targetUserId)
    }

    @Test
    fun `requireAccessToUser should throw InsufficientPermissionsException for non-admin accessing another user`() {
        val userId = UUID.randomUUID()
        val targetUserId = UUID.randomUUID()
        val username = "testuser"
        val userDetails = User(username, "password", emptyList())
        val userEntity = createUser(userId, username)

        setupAuthentication(userDetails)
        every { userService.findByUsername(username) } returns userEntity

        assertThrows(InsufficientPermissionsException::class.java) {
            authorizationService.requireAccessToUser(targetUserId)
        }
    }

    @Test
    fun `resolveUserId should return current user ID when requested ID is null`() {
        val userId = UUID.randomUUID()
        val username = "testuser"
        val userDetails = User(username, "password", emptyList())
        val userEntity = createUser(userId, username)

        setupAuthentication(userDetails)
        every { userService.findByUsername(username) } returns userEntity

        val result = authorizationService.resolveUserId(null)

        assertEquals(userId, result)
    }

    @Test
    fun `resolveUserId should return current user ID when requested ID matches current user`() {
        val userId = UUID.randomUUID()
        val username = "testuser"
        val userDetails = User(username, "password", emptyList())
        val userEntity = createUser(userId, username)

        setupAuthentication(userDetails)
        every { userService.findByUsername(username) } returns userEntity

        val result = authorizationService.resolveUserId(userId)

        assertEquals(userId, result)
    }

    @Test
    fun `resolveUserId should return requested ID for admin`() {
        val adminId = UUID.randomUUID()
        val targetUserId = UUID.randomUUID()
        val username = "admin"
        val userDetails = User(username, "password", listOf(SimpleGrantedAuthority("ROLE_ADMIN")))
        val adminEntity = createUser(adminId, username, roles = setOf(UserRole.ADMIN))

        setupAuthentication(userDetails)
        every { userService.findByUsername(username) } returns adminEntity

        val result = authorizationService.resolveUserId(targetUserId)

        assertEquals(targetUserId, result)
    }

    @Test
    fun `resolveUserId should throw InsufficientPermissionsException for non-admin accessing another user`() {
        val userId = UUID.randomUUID()
        val targetUserId = UUID.randomUUID()
        val username = "testuser"
        val userDetails = User(username, "password", emptyList())
        val userEntity = createUser(userId, username)

        setupAuthentication(userDetails)
        every { userService.findByUsername(username) } returns userEntity

        assertThrows(InsufficientPermissionsException::class.java) {
            authorizationService.resolveUserId(targetUserId)
        }
    }

    @Test
    fun `requireSessionAccessOrAdmin should not throw for session owner`() {
        val userId = UUID.randomUUID()
        val sessionId = UUID.randomUUID()
        val username = "testuser"
        val userDetails = User(username, "password", emptyList())
        val userEntity = createUser(userId, username)
        val session = createSession(sessionId, userId)
        val authentication = createAuthentication(userDetails)

        every { sessionRepository.findById(sessionId) } returns Optional.of(session)
        every { userService.findByUsername(username) } returns userEntity

        authorizationService.requireSessionAccessOrAdmin(sessionId, authentication)
    }

    @Test
    fun `requireSessionAccessOrAdmin should not throw for admin`() {
        val adminId = UUID.randomUUID()
        val ownerId = UUID.randomUUID()
        val sessionId = UUID.randomUUID()
        val username = "admin"
        val userDetails = User(username, "password", listOf(SimpleGrantedAuthority("ROLE_ADMIN")))
        val adminEntity = createUser(adminId, username, roles = setOf(UserRole.ADMIN))
        val session = createSession(sessionId, ownerId)
        val authentication = createAuthenticationWithAdminRole(userDetails)

        every { sessionRepository.findById(sessionId) } returns Optional.of(session)
        every { userService.findByUsername(username) } returns adminEntity

        authorizationService.requireSessionAccessOrAdmin(sessionId, authentication)
    }

    @Test
    fun `requireSessionAccessOrAdmin should throw AccessDeniedException for non-owner non-admin`() {
        val userId = UUID.randomUUID()
        val ownerId = UUID.randomUUID()
        val sessionId = UUID.randomUUID()
        val username = "testuser"
        val userDetails = User(username, "password", emptyList())
        val userEntity = createUser(userId, username)
        val session = createSession(sessionId, ownerId)
        val authentication = createAuthentication(userDetails)

        every { sessionRepository.findById(sessionId) } returns Optional.of(session)
        every { userService.findByUsername(username) } returns userEntity

        assertThrows(AccessDeniedException::class.java) {
            authorizationService.requireSessionAccessOrAdmin(sessionId, authentication)
        }
    }

    @Test
    fun `requireSessionAccessOrAdmin should throw IllegalArgumentException when session not found`() {
        val sessionId = UUID.randomUUID()
        val username = "testuser"
        val userDetails = User(username, "password", emptyList())
        val authentication = createAuthentication(userDetails)

        every { sessionRepository.findById(sessionId) } returns Optional.empty()

        assertThrows(IllegalArgumentException::class.java) {
            authorizationService.requireSessionAccessOrAdmin(sessionId, authentication)
        }
    }

    @Test
    fun `requireAdmin should not throw for admin`() {
        val username = "admin"
        val userDetails = User(username, "password", listOf(SimpleGrantedAuthority("ROLE_ADMIN")))
        val authentication = createAuthenticationWithAdminRole(userDetails)

        authorizationService.requireAdmin(authentication)
    }

    @Test
    fun `requireAdmin should throw AccessDeniedException for non-admin`() {
        val username = "testuser"
        val userDetails = User(username, "password", emptyList())
        val authentication = createAuthentication(userDetails)

        assertThrows(AccessDeniedException::class.java) {
            authorizationService.requireAdmin(authentication)
        }
    }

    private fun setupAuthentication(userDetails: UserDetails) {
        val authentication = UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
        SecurityContextHolder.getContext().authentication = authentication
    }

    private fun createAuthentication(userDetails: UserDetails): Authentication {
        return UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
    }

    private fun createAuthenticationWithAdminRole(userDetails: UserDetails): Authentication {
        return UsernamePasswordAuthenticationToken(
            userDetails,
            null,
            listOf(SimpleGrantedAuthority("ROLE_ADMIN"))
        )
    }

    private fun createUser(
        id: UUID,
        username: String,
        roles: Set<UserRole> = emptySet()
    ): UserEntity {
        return UserEntity(
            id = id,
            username = username,
            email = "$username@test.com",
            passwordHash = "hash",
            roles = roles.toMutableSet()
        )
    }

    private fun createSession(id: UUID, userId: UUID): ChatSessionEntity {
        return ChatSessionEntity(
            id = id,
            userId = userId,
            tutorName = "Test Tutor",
            sourceLanguageCode = "en",
            targetLanguageCode = "es",
            conversationPhase = ConversationPhase.Free,
            estimatedCEFRLevel = CEFRLevel.A1
        )
    }
}
