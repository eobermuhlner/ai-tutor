package ch.obermuhlner.aitutor.admin.controller

import ch.obermuhlner.aitutor.admin.dto.UpdateSubscriptionPlanRequest
import ch.obermuhlner.aitutor.admin.dto.UpdateUserRequest
import ch.obermuhlner.aitutor.auth.dto.UserResponse
import ch.obermuhlner.aitutor.user.domain.AuthProvider
import ch.obermuhlner.aitutor.auth.exception.InsufficientPermissionsException
import ch.obermuhlner.aitutor.auth.service.AuthorizationService
import ch.obermuhlner.aitutor.user.domain.PronunciationPreference
import ch.obermuhlner.aitutor.user.domain.UserRole
import ch.obermuhlner.aitutor.user.domain.SubscriptionPlan
import ch.obermuhlner.aitutor.user.domain.UserEntity
import ch.obermuhlner.aitutor.user.repository.RefreshTokenRepository
import ch.obermuhlner.aitutor.user.repository.UserRepository
import ch.obermuhlner.aitutor.user.service.RateLimitingService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.time.Instant
import java.util.*

class AdminControllerTest {
    private lateinit var authorizationService: AuthorizationService
    private lateinit var userRepository: UserRepository
    private lateinit var refreshTokenRepository: RefreshTokenRepository
    private lateinit var rateLimitingService: RateLimitingService
    private lateinit var controller: AdminController

    @BeforeEach
    fun setup() {
        authorizationService = mockk()
        userRepository = mockk()
        refreshTokenRepository = mockk()
        rateLimitingService = mockk()
        controller = AdminController(
            authorizationService = authorizationService,
            userRepository = userRepository,
            refreshTokenRepository = refreshTokenRepository,
            rateLimitingService = rateLimitingService
        )
    }

    @Test
    fun `updateUserSubscriptionPlan should update user subscription plan when admin`() {
        val userId = UUID.randomUUID()
        val user = createUserEntity(userId, SubscriptionPlan.FREE)
        val request = UpdateSubscriptionPlanRequest(SubscriptionPlan.SUBSCRIPTION_10)
        
        every { authorizationService.isAdmin() } returns true
        every { userRepository.findById(userId) } returns Optional.of(user)
        every { userRepository.save(user) } returns user
        every { rateLimitingService.resetRateLimit(userId) } returns Unit

        val result = controller.updateUserSubscriptionPlan(userId, request)

        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(SubscriptionPlan.SUBSCRIPTION_10, result.body?.subscriptionPlan)
        verify { userRepository.findById(userId) }
        verify { userRepository.save(user) }
        verify { rateLimitingService.resetRateLimit(userId) }
    }

    @Test
    fun `updateUserSubscriptionPlan should throw exception when not admin`() {
        val userId = UUID.randomUUID()
        val request = UpdateSubscriptionPlanRequest(SubscriptionPlan.SUBSCRIPTION_10)
        
        every { authorizationService.isAdmin() } returns false

        assertThrows(InsufficientPermissionsException::class.java) {
            controller.updateUserSubscriptionPlan(userId, request)
        }
        verify { authorizationService.isAdmin() }
    }

    @Test
    fun `updateUserSubscriptionPlan should throw exception when user not found`() {
        val userId = UUID.randomUUID()
        val request = UpdateSubscriptionPlanRequest(SubscriptionPlan.SUBSCRIPTION_10)
        
        every { authorizationService.isAdmin() } returns true
        every { userRepository.findById(userId) } returns Optional.empty()

        assertThrows(RuntimeException::class.java) {
            controller.updateUserSubscriptionPlan(userId, request)
        }
        verify { userRepository.findById(userId) }
    }

    @Test
    fun `getAllUsers should return paginated users list when admin`() {
        val user1 = createUserEntity(UUID.randomUUID(), SubscriptionPlan.FREE, "john", "doe", "john@example.com")
        val user2 = createUserEntity(UUID.randomUUID(), SubscriptionPlan.SUBSCRIPTION_10, "jane", "smith", "jane@example.com")
        val allUsers = listOf(user1, user2)
        
        every { authorizationService.isAdmin() } returns true
        every { userRepository.findAll() } returns allUsers

        val result = controller.getAllUsers(0, 20, null, null, null, null, null)

        assertEquals(HttpStatus.OK, result.statusCode)
        assertNotNull(result.body)
        assertEquals(2, result.body?.users?.size)
        verify { authorizationService.isAdmin() }
        verify { userRepository.findAll() }
    }

    @Test
    fun `getAllUsers should throw exception when not admin`() {
        every { authorizationService.isAdmin() } returns false

        assertThrows(InsufficientPermissionsException::class.java) {
            controller.getAllUsers(0, 20, null, null, null, null, null)
        }
        verify { authorizationService.isAdmin() }
    }

    @Test
    fun `getUser should return user when admin`() {
        val userId = UUID.randomUUID()
        val user = createUserEntity(userId, SubscriptionPlan.FREE)
        
        every { authorizationService.isAdmin() } returns true
        every { userRepository.findById(userId) } returns Optional.of(user)

        val result = controller.getUser(userId)

        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(userId, result.body?.id)
        verify { authorizationService.isAdmin() }
        verify { userRepository.findById(userId) }
    }

    @Test
    fun `getUser should throw exception when not admin`() {
        val userId = UUID.randomUUID()
        
        every { authorizationService.isAdmin() } returns false

        assertThrows(InsufficientPermissionsException::class.java) {
            controller.getUser(userId)
        }
        verify { authorizationService.isAdmin() }
    }

    @Test
    fun `getUser should throw exception when user not found`() {
        val userId = UUID.randomUUID()
        
        every { authorizationService.isAdmin() } returns true
        every { userRepository.findById(userId) } returns Optional.empty()

        assertThrows(RuntimeException::class.java) {
            controller.getUser(userId)
        }
        verify { userRepository.findById(userId) }
    }


    @Test
    fun `forceLogout should throw exception when not admin`() {
        val userId = UUID.randomUUID()
        
        every { authorizationService.isAdmin() } returns false

        assertThrows(InsufficientPermissionsException::class.java) {
            controller.forceLogout(userId)
        }
        verify { authorizationService.isAdmin() }
    }

    private fun createUserEntity(
        id: UUID = UUID.randomUUID(),
        subscriptionPlan: SubscriptionPlan = SubscriptionPlan.FREE,
        firstName: String? = null,
        lastName: String? = null,
        email: String = "test@example.com"
    ): UserEntity {
        return UserEntity(
            id = id,
            username = "testuser",
            email = email,
            firstName = firstName,
            lastName = lastName,
            roles = mutableSetOf(UserRole.USER),
            enabled = true,
            locked = false,
            emailVerified = true,
            createdAt = Instant.now(),
            lastLoginAt = null,
            subscriptionPlan = subscriptionPlan,
            pronunciationPreference = PronunciationPreference.NONE,
            provider = AuthProvider.CREDENTIALS
        )
    }
}