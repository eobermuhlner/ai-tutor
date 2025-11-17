package ch.obermuhlner.aitutor.assessment.controller

import ch.obermuhlner.aitutor.assessment.service.CEFRAssessmentService
import ch.obermuhlner.aitutor.auth.service.AuthorizationService
import ch.obermuhlner.aitutor.chat.domain.ChatSessionEntity
import ch.obermuhlner.aitutor.chat.repository.ChatSessionRepository
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import java.time.Instant
import java.util.Optional
import java.util.UUID
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(AssessmentController::class)
@Import(ch.obermuhlner.aitutor.auth.config.SecurityConfig::class)
class AssessmentControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean(relaxed = true)
    private lateinit var cefrAssessmentService: CEFRAssessmentService

    @MockkBean(relaxed = true)
    private lateinit var chatSessionRepository: ChatSessionRepository

    @MockkBean(relaxed = true)
    private lateinit var authorizationService: AuthorizationService

    @MockkBean(relaxed = true)
    private lateinit var jwtTokenService: ch.obermuhlner.aitutor.auth.service.JwtTokenService

    @MockkBean(relaxed = true)
    private lateinit var customUserDetailsService: ch.obermuhlner.aitutor.user.service.CustomUserDetailsService

    @Test
    @WithMockUser
    fun `getSkillBreakdown should return skill levels for valid session`() {
        val userId = UUID.randomUUID()
        val sessionId = UUID.randomUUID()
        val lastAssessed = Instant.parse("2025-01-15T10:00:00Z")
        val session = createSession(
            id = sessionId,
            userId = userId,
            estimatedCEFRLevel = CEFRLevel.B1,
            cefrGrammar = CEFRLevel.B1,
            cefrVocabulary = CEFRLevel.B2,
            cefrFluency = CEFRLevel.A2,
            cefrComprehension = CEFRLevel.B1,
            lastAssessmentAt = lastAssessed,
            totalAssessmentCount = 5
        )

        every { authorizationService.getCurrentUserId() } returns userId
        every { chatSessionRepository.findById(sessionId) } returns Optional.of(session)

        mockMvc.perform(get("/api/v1/assessment/sessions/$sessionId/skills"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.overall").value("B1"))
            .andExpect(jsonPath("$.grammar").value("B1"))
            .andExpect(jsonPath("$.vocabulary").value("B2"))
            .andExpect(jsonPath("$.fluency").value("A2"))
            .andExpect(jsonPath("$.comprehension").value("B1"))
            .andExpect(jsonPath("$.lastAssessedAt").value("2025-01-15T10:00:00Z"))
            .andExpect(jsonPath("$.assessmentCount").value(5))
    }

    @Test
    @WithMockUser
    fun `getSkillBreakdown should return Unknown for null skill levels`() {
        val userId = UUID.randomUUID()
        val sessionId = UUID.randomUUID()
        val session = createSession(
            id = sessionId,
            userId = userId,
            estimatedCEFRLevel = CEFRLevel.A1,
            cefrGrammar = null,
            cefrVocabulary = null,
            cefrFluency = null,
            cefrComprehension = null
        )

        every { authorizationService.getCurrentUserId() } returns userId
        every { chatSessionRepository.findById(sessionId) } returns Optional.of(session)

        mockMvc.perform(get("/api/v1/assessment/sessions/$sessionId/skills"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.overall").value("A1"))
            .andExpect(jsonPath("$.grammar").value("Unknown"))
            .andExpect(jsonPath("$.vocabulary").value("Unknown"))
            .andExpect(jsonPath("$.fluency").value("Unknown"))
            .andExpect(jsonPath("$.comprehension").value("Unknown"))
    }

    @Test
    @WithMockUser
    fun `getSkillBreakdown should return 403 for session not owned by user`() {
        val userId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()
        val sessionId = UUID.randomUUID()
        val session = createSession(
            id = sessionId,
            userId = otherUserId,
            estimatedCEFRLevel = CEFRLevel.A1
        )

        every { authorizationService.getCurrentUserId() } returns userId
        every { chatSessionRepository.findById(sessionId) } returns Optional.of(session)

        mockMvc.perform(get("/api/v1/assessment/sessions/$sessionId/skills"))
            .andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser
    fun `triggerReassessment should reassess and return updated skills`() {
        val userId = UUID.randomUUID()
        val sessionId = UUID.randomUUID()
        val session = createSession(
            id = sessionId,
            userId = userId,
            estimatedCEFRLevel = CEFRLevel.A1,
            cefrGrammar = CEFRLevel.A1
        )

        val assessment = CEFRAssessmentService.SkillAssessment(
            grammar = CEFRLevel.B1,
            vocabulary = CEFRLevel.B2,
            fluency = CEFRLevel.A2,
            comprehension = CEFRLevel.A2,
            overall = CEFRLevel.B1
        )

        // Mock needs to return the session multiple times (once for reassess, once for getSkillBreakdown)
        every { authorizationService.getCurrentUserId() } returns userId
        every { chatSessionRepository.findById(sessionId) } returns Optional.of(session)
        every { cefrAssessmentService.assessWithHeuristics(session) } answers {
            // Update session fields to simulate updateSkillLevelsIfChanged
            session.cefrGrammar = assessment.grammar
            session.cefrVocabulary = assessment.vocabulary
            session.cefrFluency = assessment.fluency
            session.cefrComprehension = assessment.comprehension
            session.estimatedCEFRLevel = assessment.overall
            assessment
        }
        every { cefrAssessmentService.updateSkillLevelsIfChanged(session, assessment) } returns true
        every { chatSessionRepository.save(session) } returns session

        mockMvc.perform(post("/api/v1/assessment/sessions/$sessionId/reassess"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.overall").value("B1"))
            .andExpect(jsonPath("$.grammar").value("B1"))

        verify { cefrAssessmentService.assessWithHeuristics(session) }
        verify { cefrAssessmentService.updateSkillLevelsIfChanged(session, assessment) }
        verify { chatSessionRepository.save(session) }
    }

    @Test
    @WithMockUser
    fun `triggerReassessment should return 403 for session not owned by user`() {
        val userId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()
        val sessionId = UUID.randomUUID()
        val session = createSession(
            id = sessionId,
            userId = otherUserId,
            estimatedCEFRLevel = CEFRLevel.A1
        )

        every { authorizationService.getCurrentUserId() } returns userId
        every { chatSessionRepository.findById(sessionId) } returns Optional.of(session)

        mockMvc.perform(post("/api/v1/assessment/sessions/$sessionId/reassess"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `getSkillBreakdown should require authentication`() {
        val sessionId = UUID.randomUUID()

        mockMvc.perform(get("/api/v1/assessment/sessions/$sessionId/skills"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `triggerReassessment should require authentication`() {
        val sessionId = UUID.randomUUID()

        mockMvc.perform(post("/api/v1/assessment/sessions/$sessionId/reassess"))
            .andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser
    fun `getSkillBreakdown should return 0 assessment count when null`() {
        val userId = UUID.randomUUID()
        val sessionId = UUID.randomUUID()
        val session = createSession(
            id = sessionId,
            userId = userId,
            estimatedCEFRLevel = CEFRLevel.A1,
            totalAssessmentCount = null
        )

        every { authorizationService.getCurrentUserId() } returns userId
        every { chatSessionRepository.findById(sessionId) } returns Optional.of(session)

        mockMvc.perform(get("/api/v1/assessment/sessions/$sessionId/skills"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.assessmentCount").value(0))
    }

    @Test
    @WithMockUser
    fun `triggerReassessment should save session after reassessment`() {
        val userId = UUID.randomUUID()
        val sessionId = UUID.randomUUID()
        val session = createSession(
            id = sessionId,
            userId = userId,
            estimatedCEFRLevel = CEFRLevel.A1
        )

        val assessment = CEFRAssessmentService.SkillAssessment(
            grammar = CEFRLevel.A2,
            vocabulary = CEFRLevel.A2,
            fluency = CEFRLevel.A1,
            comprehension = CEFRLevel.A1,
            overall = CEFRLevel.A2
        )

        every { authorizationService.getCurrentUserId() } returns userId
        every { chatSessionRepository.findById(sessionId) } returns Optional.of(session)
        every { cefrAssessmentService.assessWithHeuristics(session) } returns assessment
        every { cefrAssessmentService.updateSkillLevelsIfChanged(session, assessment) } returns false
        every { chatSessionRepository.save(session) } returns session

        mockMvc.perform(post("/api/v1/assessment/sessions/$sessionId/reassess"))
            .andExpect(status().isOk)

        verify(exactly = 1) { chatSessionRepository.save(session) }
    }

    private fun createSession(
        id: UUID = UUID.randomUUID(),
        userId: UUID = UUID.randomUUID(),
        estimatedCEFRLevel: CEFRLevel = CEFRLevel.A1,
        cefrGrammar: CEFRLevel? = null,
        cefrVocabulary: CEFRLevel? = null,
        cefrFluency: CEFRLevel? = null,
        cefrComprehension: CEFRLevel? = null,
        lastAssessmentAt: Instant? = null,
        totalAssessmentCount: Int? = null
    ) = ChatSessionEntity(
        id = id,
        userId = userId,
        sourceLanguageCode = "en",
        targetLanguageCode = "es",
        tutorName = "Test Tutor",
        tutorPersona = "Friendly",
        tutorDomain = "General",
        conversationPhase = ConversationPhase.Correction,
        effectivePhase = ConversationPhase.Correction,
        estimatedCEFRLevel = estimatedCEFRLevel,
        cefrGrammar = cefrGrammar,
        cefrVocabulary = cefrVocabulary,
        cefrFluency = cefrFluency,
        cefrComprehension = cefrComprehension,
        lastAssessmentAt = lastAssessmentAt,
        totalAssessmentCount = totalAssessmentCount
    )
}
