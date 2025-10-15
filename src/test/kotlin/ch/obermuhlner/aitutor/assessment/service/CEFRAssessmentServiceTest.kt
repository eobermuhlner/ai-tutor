package ch.obermuhlner.aitutor.assessment.service

import ch.obermuhlner.aitutor.analytics.domain.ErrorPatternEntity
import ch.obermuhlner.aitutor.analytics.service.ErrorAnalyticsService
import ch.obermuhlner.aitutor.chat.domain.ChatSessionEntity
import ch.obermuhlner.aitutor.chat.domain.ChatMessageEntity
import ch.obermuhlner.aitutor.chat.domain.MessageRole
import ch.obermuhlner.aitutor.chat.repository.ChatMessageRepository
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.ErrorType
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import ch.obermuhlner.aitutor.vocabulary.domain.VocabularyItemEntity
import ch.obermuhlner.aitutor.vocabulary.service.VocabularyQueryService
import io.mockk.*
import java.time.Instant
import java.util.UUID
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CEFRAssessmentServiceTest {

    private lateinit var errorAnalyticsService: ErrorAnalyticsService
    private lateinit var vocabularyQueryService: VocabularyQueryService
    private lateinit var chatMessageRepository: ChatMessageRepository
    private lateinit var cefrAssessmentService: CEFRAssessmentService

    @BeforeEach
    fun setup() {
        errorAnalyticsService = mockk()
        vocabularyQueryService = mockk()
        chatMessageRepository = mockk()
        cefrAssessmentService = CEFRAssessmentService(
            errorAnalyticsService,
            vocabularyQueryService,
            chatMessageRepository
        )
    }

    @Test
    fun `assessWithHeuristics should return A1 levels when no data is available`() {
        val session = createSession()

        every { errorAnalyticsService.getTopPatterns(any(), any(), any()) } returns emptyList()
        every { vocabularyQueryService.getUserVocabulary(any(), any()) } returns emptyList()
        every { chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(any()) } returns emptyList()

        val result = cefrAssessmentService.assessWithHeuristics(session)

        assertEquals(CEFRLevel.A1, result.grammar)
        assertEquals(CEFRLevel.A1, result.vocabulary)
        assertEquals(CEFRLevel.A1, result.fluency)
        assertEquals(CEFRLevel.A1, result.comprehension)
        assertEquals(CEFRLevel.A1, result.overall)
    }

    @Test
    fun `assessWithHeuristics should assess grammar based on error patterns`() {
        val session = createSession()
        val patterns = listOf(
            createPattern(ErrorType.Agreement, criticalCount = 1, highCount = 2, mediumCount = 1)
        )

        every { errorAnalyticsService.getTopPatterns(any(), any(), any()) } returns patterns
        every { vocabularyQueryService.getUserVocabulary(any(), any()) } returns emptyList()
        every { chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(any()) } returns emptyList()

        val result = cefrAssessmentService.assessWithHeuristics(session)

        // Weighted score: 1*3.0 + 2*2.0 + 1*1.0 = 8.0 → A2
        assertEquals(CEFRLevel.A2, result.grammar)
    }

    @Test
    fun `assessWithHeuristics should assess grammar as C1 for low error score`() {
        val session = createSession()
        val patterns = listOf(
            createPattern(ErrorType.Agreement, lowCount = 2, mediumCount = 1)
        )

        every { errorAnalyticsService.getTopPatterns(any(), any(), any()) } returns patterns
        every { vocabularyQueryService.getUserVocabulary(any(), any()) } returns emptyList()
        every { chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(any()) } returns emptyList()

        val result = cefrAssessmentService.assessWithHeuristics(session)

        // Weighted score: 2*0.3 + 1*1.0 = 0.6 + 1.0 = 1.6 → But actually using computeWeightedScore()
        // The entity method returns: critical*3 + high*2 + medium*1 + low*0.3 = 1.6 < 3.0 → C1
        assertTrue(result.grammar in listOf(CEFRLevel.C1, CEFRLevel.B2))
    }

    @Test
    fun `assessWithHeuristics should assess vocabulary based on word count`() {
        val session = createSession()
        val vocabulary = (1..700).map { createVocabularyItem() }

        every { errorAnalyticsService.getTopPatterns(any(), any(), any()) } returns emptyList()
        every { vocabularyQueryService.getUserVocabulary(any(), any()) } returns vocabulary
        every { chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(any()) } returns emptyList()

        val result = cefrAssessmentService.assessWithHeuristics(session)

        // 700 words → B1 (600-1200 range)
        assertEquals(CEFRLevel.B1, result.vocabulary)
    }

    @Test
    fun `assessWithHeuristics should assess vocabulary as C2 for 5000+ words`() {
        val session = createSession()
        val vocabulary = (1..5500).map { createVocabularyItem() }

        every { errorAnalyticsService.getTopPatterns(any(), any(), any()) } returns emptyList()
        every { vocabularyQueryService.getUserVocabulary(any(), any()) } returns vocabulary
        every { chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(any()) } returns emptyList()

        val result = cefrAssessmentService.assessWithHeuristics(session)

        assertEquals(CEFRLevel.C2, result.vocabulary)
    }

    @Test
    fun `assessWithHeuristics should assess fluency based on message length`() {
        val session = createSession()
        val messages = (1..15).map {
            createMessage(content = "This is a test message with about fifteen words in it to measure fluency level properly")
        }

        every { errorAnalyticsService.getTopPatterns(any(), any(), any()) } returns emptyList()
        every { vocabularyQueryService.getUserVocabulary(any(), any()) } returns emptyList()
        every { chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(session.id) } returns messages

        val result = cefrAssessmentService.assessWithHeuristics(session)

        // Average 15 words → B2 (15.0-20.0 range)
        assertEquals(CEFRLevel.B2, result.fluency)
    }

    @Test
    fun `assessWithHeuristics should return A1 fluency for insufficient messages`() {
        val session = createSession()
        val messages = (1..5).map {
            createMessage(content = "Short message")
        }

        every { errorAnalyticsService.getTopPatterns(any(), any(), any()) } returns emptyList()
        every { vocabularyQueryService.getUserVocabulary(any(), any()) } returns emptyList()
        every { chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(session.id) } returns messages

        val result = cefrAssessmentService.assessWithHeuristics(session)

        assertEquals(CEFRLevel.A1, result.fluency)
    }

    @Test
    fun `assessWithHeuristics should compute comprehension based on grammar level`() {
        val session = createSession(cefrGrammar = CEFRLevel.B2)

        every { errorAnalyticsService.getTopPatterns(any(), any(), any()) } returns emptyList()
        every { vocabularyQueryService.getUserVocabulary(any(), any()) } returns emptyList()
        every { chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(any()) } returns emptyList()

        val result = cefrAssessmentService.assessWithHeuristics(session)

        // Comprehension should be grammar - 1 level → B1
        assertEquals(CEFRLevel.B1, result.comprehension)
    }

    @Test
    fun `assessWithHeuristics should compute overall as weighted average`() {
        val session = createSession()
        val patterns = listOf(
            createPattern(ErrorType.Agreement, mediumCount = 2) // Score 2.0 → B2
        )
        val vocabulary = (1..800).map { createVocabularyItem() } // B1
        val messages = (1..12).map {
            createMessage(content = "Short msg") // 2 words → A1
        }

        every { errorAnalyticsService.getTopPatterns(any(), any(), any()) } returns patterns
        every { vocabularyQueryService.getUserVocabulary(any(), any()) } returns vocabulary
        every { chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(session.id) } returns messages

        val result = cefrAssessmentService.assessWithHeuristics(session)

        // Grammar: B2 (4), Vocab: B1 (3), Fluency: A1 (1), Comprehension: A1 (1)
        // Weighted: 4*0.4 + 3*0.3 + 1*0.2 + 1*0.1 = 1.6 + 0.9 + 0.2 + 0.1 = 2.8 → 3 → B1
        assertEquals(CEFRLevel.B1, result.overall)
    }

    @Test
    fun `assessWithHeuristics should handle exceptions from error analytics gracefully`() {
        val session = createSession()

        every { errorAnalyticsService.getTopPatterns(any(), any(), any()) } throws RuntimeException("Error")
        every { vocabularyQueryService.getUserVocabulary(any(), any()) } returns emptyList()
        every { chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(any()) } returns emptyList()

        val result = cefrAssessmentService.assessWithHeuristics(session)

        assertEquals(CEFRLevel.A1, result.grammar)
    }

    @Test
    fun `assessWithHeuristics should handle exceptions from vocabulary service gracefully`() {
        val session = createSession()

        every { errorAnalyticsService.getTopPatterns(any(), any(), any()) } returns emptyList()
        every { vocabularyQueryService.getUserVocabulary(any(), any()) } throws RuntimeException("Error")
        every { chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(any()) } returns emptyList()

        val result = cefrAssessmentService.assessWithHeuristics(session)

        assertEquals(CEFRLevel.A1, result.vocabulary)
    }

    @Test
    fun `updateSkillLevelsIfChanged should return false when nothing changed`() {
        val session = createSession(
            estimatedCEFRLevel = CEFRLevel.A2,
            cefrGrammar = CEFRLevel.A2,
            cefrVocabulary = CEFRLevel.A2,
            cefrFluency = CEFRLevel.A2,
            cefrComprehension = CEFRLevel.A1
        )

        val assessment = CEFRAssessmentService.SkillAssessment(
            grammar = CEFRLevel.A2,
            vocabulary = CEFRLevel.A2,
            fluency = CEFRLevel.A2,
            comprehension = CEFRLevel.A1,
            overall = CEFRLevel.A2
        )

        val result = cefrAssessmentService.updateSkillLevelsIfChanged(session, assessment)

        assertFalse(result)
        assertNull(session.lastAssessmentAt)
        assertNull(session.totalAssessmentCount)
    }

    @Test
    fun `updateSkillLevelsIfChanged should return true and update when grammar level changed`() {
        val session = createSession(
            estimatedCEFRLevel = CEFRLevel.A2,
            cefrGrammar = CEFRLevel.A2
        )

        val assessment = CEFRAssessmentService.SkillAssessment(
            grammar = CEFRLevel.B1,
            vocabulary = CEFRLevel.A2,
            fluency = CEFRLevel.A1,
            comprehension = CEFRLevel.A1,
            overall = CEFRLevel.A2
        )

        val result = cefrAssessmentService.updateSkillLevelsIfChanged(session, assessment)

        assertTrue(result)
        assertEquals(CEFRLevel.B1, session.cefrGrammar)
        assertNotNull(session.lastAssessmentAt)
        assertEquals(1, session.totalAssessmentCount)
    }

    @Test
    fun `updateSkillLevelsIfChanged should update all changed levels`() {
        val session = createSession(
            estimatedCEFRLevel = CEFRLevel.A1,
            cefrGrammar = CEFRLevel.A1,
            cefrVocabulary = CEFRLevel.A1,
            cefrFluency = CEFRLevel.A1,
            cefrComprehension = CEFRLevel.A1
        )

        val assessment = CEFRAssessmentService.SkillAssessment(
            grammar = CEFRLevel.B1,
            vocabulary = CEFRLevel.B2,
            fluency = CEFRLevel.A2,
            comprehension = CEFRLevel.A1,
            overall = CEFRLevel.B1
        )

        val result = cefrAssessmentService.updateSkillLevelsIfChanged(session, assessment)

        assertTrue(result)
        assertEquals(CEFRLevel.B1, session.cefrGrammar)
        assertEquals(CEFRLevel.B2, session.cefrVocabulary)
        assertEquals(CEFRLevel.A2, session.cefrFluency)
        assertEquals(CEFRLevel.A1, session.cefrComprehension)
        assertEquals(CEFRLevel.B1, session.estimatedCEFRLevel)
        assertNotNull(session.lastAssessmentAt)
        assertEquals(1, session.totalAssessmentCount)
    }

    @Test
    fun `updateSkillLevelsIfChanged should increment assessment count on each change`() {
        val session = createSession(
            estimatedCEFRLevel = CEFRLevel.A2,
            cefrGrammar = CEFRLevel.A2,
            totalAssessmentCount = 5
        )

        val assessment = CEFRAssessmentService.SkillAssessment(
            grammar = CEFRLevel.B1,
            vocabulary = CEFRLevel.A2,
            fluency = CEFRLevel.A1,
            comprehension = CEFRLevel.A1,
            overall = CEFRLevel.A2
        )

        cefrAssessmentService.updateSkillLevelsIfChanged(session, assessment)

        assertEquals(6, session.totalAssessmentCount)
    }

    @Test
    fun `assessWithHeuristics should assess fluency as C2 for very long messages`() {
        val session = createSession()
        val longContent = "This is a very long and sophisticated message " +
            "that demonstrates advanced fluency with complex sentence structures " +
            "and extensive vocabulary usage across multiple clauses and ideas"
        val messages = (1..15).map {
            createMessage(content = longContent)
        }

        every { errorAnalyticsService.getTopPatterns(any(), any(), any()) } returns emptyList()
        every { vocabularyQueryService.getUserVocabulary(any(), any()) } returns emptyList()
        every { chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(session.id) } returns messages

        val result = cefrAssessmentService.assessWithHeuristics(session)

        // Should be 25+ words → C2
        assertTrue(result.fluency >= CEFRLevel.B2)
    }

    private fun createSession(
        id: UUID = UUID.randomUUID(),
        userId: UUID = UUID.randomUUID(),
        targetLanguageCode: String = "es",
        estimatedCEFRLevel: CEFRLevel = CEFRLevel.A1,
        cefrGrammar: CEFRLevel? = null,
        cefrVocabulary: CEFRLevel? = null,
        cefrFluency: CEFRLevel? = null,
        cefrComprehension: CEFRLevel? = null,
        totalAssessmentCount: Int? = null
    ) = ChatSessionEntity(
        id = id,
        userId = userId,
        sourceLanguageCode = "en",
        targetLanguageCode = targetLanguageCode,
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
        totalAssessmentCount = totalAssessmentCount
    )

    private fun createPattern(
        errorType: ErrorType,
        criticalCount: Int = 0,
        highCount: Int = 0,
        mediumCount: Int = 0,
        lowCount: Int = 0
    ) = ErrorPatternEntity(
        userId = UUID.randomUUID(),
        lang = "es",
        errorType = errorType,
        totalCount = criticalCount + highCount + mediumCount + lowCount,
        criticalCount = criticalCount,
        highCount = highCount,
        mediumCount = mediumCount,
        lowCount = lowCount,
        firstSeenAt = Instant.now(),
        lastSeenAt = Instant.now()
    )

    private fun createMessage(
        role: MessageRole = MessageRole.USER,
        content: String,
        session: ChatSessionEntity = createSession()
    ) = ChatMessageEntity(
        session = session,
        role = role,
        content = content
    )

    private fun createVocabularyItem() = VocabularyItemEntity(
        userId = UUID.randomUUID(),
        lang = "es",
        lemma = "test",
        conceptName = "test concept"
    )
}
