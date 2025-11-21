package ch.obermuhlner.aitutor.testutil

import ch.obermuhlner.aitutor.auth.service.AuthorizationService
import ch.obermuhlner.aitutor.catalog.repository.CourseTemplateRepository
import ch.obermuhlner.aitutor.catalog.repository.TutorProfileRepository
import ch.obermuhlner.aitutor.chat.repository.ChatSessionRepository
import ch.obermuhlner.aitutor.chat.repository.ChatMessageRepository
import ch.obermuhlner.aitutor.chat.repository.MessageSummaryRepository
import ch.obermuhlner.aitutor.config.TestConfig
import ch.obermuhlner.aitutor.user.domain.UserEntity
import ch.obermuhlner.aitutor.user.repository.RefreshTokenRepository
import ch.obermuhlner.aitutor.user.repository.UserLanguageProficiencyRepository
import ch.obermuhlner.aitutor.user.repository.UserRepository
import ch.obermuhlner.aitutor.vocabulary.repository.VocabularyContextRepository
import ch.obermuhlner.aitutor.vocabulary.repository.VocabularyItemRepository
import ch.obermuhlner.aitutor.analytics.repository.ErrorPatternRepository
import ch.obermuhlner.aitutor.analytics.repository.RecentErrorSampleRepository
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import java.util.UUID

/**
 * Base class for controller integration tests that provides common setup and utilities.
 *
 * This class handles common Spring Boot test configuration, dependency injection,
 * test user setup, and utility methods for making HTTP requests.
 */
@ActiveProfiles("test", "noauth")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Import(TestConfig::class)
abstract class BaseControllerIntegrationTest {
    
    @LocalServerPort
    protected var port: Int = 0

    @Autowired
    protected lateinit var restTemplate: TestRestTemplate

    // Common repositories for test data cleanup/setup
    @Autowired
    protected lateinit var userRepository: UserRepository

    @Autowired
    protected lateinit var courseTemplateRepository: CourseTemplateRepository

    @Autowired
    protected lateinit var tutorProfileRepository: TutorProfileRepository

    @Autowired
    protected lateinit var chatSessionRepository: ChatSessionRepository

    @Autowired
    protected lateinit var chatMessageRepository: ChatMessageRepository

    @Autowired
    protected lateinit var messageSummaryRepository: MessageSummaryRepository

    @Autowired
    protected lateinit var userLanguageProficiencyRepository: UserLanguageProficiencyRepository

    @Autowired
    protected lateinit var vocabularyItemRepository: VocabularyItemRepository

    @Autowired
    protected lateinit var vocabularyContextRepository: VocabularyContextRepository

    @Autowired
    protected lateinit var errorPatternRepository: ErrorPatternRepository

    @Autowired
    protected lateinit var recentErrorSampleRepository: RecentErrorSampleRepository

    @Autowired
    protected lateinit var refreshTokenRepository: RefreshTokenRepository

    @MockkBean(relaxed = true)
    protected lateinit var authorizationService: AuthorizationService

    protected fun baseUrl(path: String): String = "http://localhost:$port/api/v1$path"

    protected val testUserId = UUID.randomUUID()

    protected val testUser = UserEntity(
        id = testUserId,
        username = "testuser",
        email = "test@example.com",
        passwordHash = "password",
        roles = mutableSetOf(
            ch.obermuhlner.aitutor.user.domain.UserRole.USER,
            ch.obermuhlner.aitutor.user.domain.UserRole.ADMIN,
            ch.obermuhlner.aitutor.user.domain.UserRole.EDITOR
        )
    )

    @BeforeEach
    protected open fun setUpBase() {
        // Clean up common repositories before each test
        errorPatternRepository.deleteAll()
        recentErrorSampleRepository.deleteAll()
        refreshTokenRepository.deleteAll()
        vocabularyContextRepository.deleteAll()
        vocabularyItemRepository.deleteAll()
        messageSummaryRepository.deleteAll()
        chatMessageRepository.deleteAll()
        chatSessionRepository.deleteAll()
        userLanguageProficiencyRepository.deleteAll()
        courseTemplateRepository.deleteAll()
        tutorProfileRepository.deleteAll()
        userRepository.deleteAll()

        // Create default test user for the noauth profile
        val adminUser = UserEntity(
            id = testUserId,
            username = "testuser",
            email = "test@example.com",
            passwordHash = "password",
            roles = mutableSetOf(
                ch.obermuhlner.aitutor.user.domain.UserRole.USER,
                ch.obermuhlner.aitutor.user.domain.UserRole.ADMIN,
                ch.obermuhlner.aitutor.user.domain.UserRole.EDITOR
            )
        )
        userRepository.save(adminUser)

        // Setup common authorization mocks
        every { authorizationService.getCurrentUserId() } returns testUserId
        every { authorizationService.getCurrentUser() } returns adminUser
        every { authorizationService.isAdmin() } returns true
        every { authorizationService.isEditor() } returns true
        every { authorizationService.isEditorOrAdmin() } returns true
        every { authorizationService.requireAccessToUser(match { it != null }) } returns Unit
        every { authorizationService.resolveUserId(match { it != null }) } returns testUserId
    }

    /**
     * Helper method to create HTTP entity with JSON content type
     */
    protected fun createJsonEntity(body: Any): HttpEntity<Any> {
        val headers = HttpHeaders()
        headers.contentType = org.springframework.http.MediaType.APPLICATION_JSON
        return HttpEntity(body, headers)
    }

    /**
     * Generic method to make HTTP requests
     */
    protected fun <T> makeRequest(
        method: HttpMethod,
        path: String,
        body: Any? = null,
        responseType: Class<T>,
        headers: HttpHeaders? = null
    ): ResponseEntity<T> {
        val httpHeaders = headers ?: HttpHeaders().apply {
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
        }
        val entity = if (body != null) HttpEntity(body, httpHeaders) else HttpEntity(httpHeaders)
        return restTemplate.exchange(baseUrl(path), method, entity, responseType)
    }

    // Test Data Builders
    protected fun createTestUser(
        id: UUID = UUID.randomUUID(),
        username: String = "testuser",
        email: String = "test@example.com",
        roles: Set<ch.obermuhlner.aitutor.user.domain.UserRole> = setOf(ch.obermuhlner.aitutor.user.domain.UserRole.USER),
        passwordHash: String = "password"
    ): UserEntity {
        return UserEntity(
            id = id,
            username = username,
            email = email,
            passwordHash = passwordHash,
            roles = roles.toMutableSet()
        )
    }

    // Common Assertions
    protected fun <T> assertOkResponse(response: ResponseEntity<T>): T {
        org.assertj.core.api.Assertions.assertThat(response.statusCode).isEqualTo(org.springframework.http.HttpStatus.OK)
        org.assertj.core.api.Assertions.assertThat(response.body).isNotNull
        return response.body!!
    }

    protected fun <T> assertCreatedResponse(response: ResponseEntity<T>): T {
        org.assertj.core.api.Assertions.assertThat(response.statusCode).isEqualTo(org.springframework.http.HttpStatus.CREATED)
        org.assertj.core.api.Assertions.assertThat(response.body).isNotNull
        return response.body!!
    }

    protected fun <T> assertNotFoundResponse(response: ResponseEntity<T>) {
        org.assertj.core.api.Assertions.assertThat(response.statusCode).isEqualTo(org.springframework.http.HttpStatus.NOT_FOUND)
    }

    protected fun <T> assertBadRequestResponse(response: ResponseEntity<T>) {
        org.assertj.core.api.Assertions.assertThat(response.statusCode).isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST)
    }

    protected fun <T> assertNoContentResponse(response: ResponseEntity<T>) {
        org.assertj.core.api.Assertions.assertThat(response.statusCode).isEqualTo(org.springframework.http.HttpStatus.NO_CONTENT)
    }
}