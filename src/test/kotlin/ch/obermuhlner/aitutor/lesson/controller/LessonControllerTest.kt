package ch.obermuhlner.aitutor.lesson.controller

import ch.obermuhlner.aitutor.auth.service.AuthorizationService
import ch.obermuhlner.aitutor.chat.domain.ChatSessionEntity
import ch.obermuhlner.aitutor.chat.repository.ChatSessionRepository
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.lesson.domain.CourseCurriculum
import ch.obermuhlner.aitutor.lesson.domain.LessonContent
import ch.obermuhlner.aitutor.lesson.domain.LessonMetadata
import ch.obermuhlner.aitutor.lesson.domain.ProgressionMode
import ch.obermuhlner.aitutor.lesson.service.LessonContentService
import ch.obermuhlner.aitutor.lesson.service.LessonProgressionService
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Optional
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class LessonControllerTest {

    private lateinit var lessonContentService: LessonContentService
    private lateinit var lessonProgressionService: LessonProgressionService
    private lateinit var chatSessionRepository: ChatSessionRepository
    private lateinit var authorizationService: AuthorizationService
    private lateinit var lessonController: LessonController

    @BeforeEach
    fun setup() {
        lessonContentService = mockk()
        lessonProgressionService = mockk()
        chatSessionRepository = mockk()
        authorizationService = mockk()

        lessonController = LessonController(
            lessonContentService,
            lessonProgressionService,
            chatSessionRepository,
            authorizationService
        )
    }

    @Test
    fun `should get course curriculum successfully`() {
        val curriculum = CourseCurriculum(
            courseId = "es-conversational-spanish",
            progressionMode = ProgressionMode.TIME_BASED,
            lessons = listOf(
                LessonMetadata("week-01-greetings", "week-01-greetings.md", 0, 5),
                LessonMetadata("week-02-introductions", "week-02-introductions.md", 7, 8)
            )
        )

        every { lessonContentService.getCurriculum("es-conversational-spanish") } returns curriculum

        val response = lessonController.getCourseCurriculum("es-conversational-spanish")

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals("es-conversational-spanish", response.body?.courseId)
        assertEquals(2, response.body?.lessons?.size)
        assertEquals(ProgressionMode.TIME_BASED, response.body?.progressionMode)
    }

    @Test
    fun `should return 404 for non-existent curriculum`() {
        every { lessonContentService.getCurriculum("non-existent") } returns null

        val response = lessonController.getCourseCurriculum("non-existent")

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `should get specific lesson successfully`() {
        val lesson = LessonContent(
            id = "week-01-greetings",
            title = "Greetings",
            weekNumber = 1,
            estimatedDuration = "1 week",
            focusAreas = listOf("greetings", "polite expressions"),
            targetCEFR = CEFRLevel.A1,
            goals = listOf("Learn greetings"),
            grammarPoints = emptyList(),
            essentialVocabulary = emptyList(),
            conversationScenarios = emptyList(),
            practicePatterns = emptyList(),
            commonMistakes = emptyList(),
            fullMarkdown = "# Greetings"
        )

        every { lessonContentService.getLesson("es-conversational-spanish", "week-01-greetings") } returns lesson

        val response = lessonController.getLesson("es-conversational-spanish", "week-01-greetings")

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals("week-01-greetings", response.body?.id)
        assertEquals("Greetings", response.body?.title)
        assertEquals(1, response.body?.weekNumber)
    }

    @Test
    fun `should return 404 for non-existent lesson`() {
        every { lessonContentService.getLesson("es-conversational-spanish", "non-existent") } returns null

        val response = lessonController.getLesson("es-conversational-spanish", "non-existent")

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `should get current lesson for session`() {
        val userId = UUID.randomUUID()
        val sessionId = UUID.randomUUID()
        val courseId = UUID.randomUUID()

        val session = ChatSessionEntity(
            id = sessionId,
            userId = userId,
            tutorName = "Maria",
            tutorPersona = "friendly",
            tutorDomain = "general",
            sourceLanguageCode = "en",
            targetLanguageCode = "es",
            conversationPhase = ConversationPhase.Correction,
            effectivePhase = ConversationPhase.Correction,
            estimatedCEFRLevel = CEFRLevel.A1,
            courseTemplateId = courseId,
            currentLessonId = "week-01-greetings"
        )

        val lesson = LessonContent(
            id = "week-01-greetings",
            title = "Greetings",
            weekNumber = 1,
            estimatedDuration = "1 week",
            focusAreas = listOf("greetings"),
            targetCEFR = CEFRLevel.A1,
            goals = emptyList(),
            grammarPoints = emptyList(),
            essentialVocabulary = emptyList(),
            conversationScenarios = emptyList(),
            practicePatterns = emptyList(),
            commonMistakes = emptyList(),
            fullMarkdown = "# Greetings"
        )

        every { authorizationService.getCurrentUserId() } returns userId
        every { chatSessionRepository.findById(sessionId) } returns Optional.of(session)
        every { lessonProgressionService.checkAndProgressLesson(session) } returns lesson

        val response = lessonController.getCurrentLesson(sessionId)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals("week-01-greetings", response.body?.id)
    }

    @Test
    fun `should return 404 for non-existent session`() {
        val sessionId = UUID.randomUUID()

        every { authorizationService.getCurrentUserId() } returns UUID.randomUUID()
        every { chatSessionRepository.findById(sessionId) } returns Optional.empty()

        val response = lessonController.getCurrentLesson(sessionId)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `should return 403 when accessing another user's session`() {
        val userId = UUID.randomUUID()
        val sessionId = UUID.randomUUID()

        val session = ChatSessionEntity(
            userId = userId,
            tutorName = "Maria",
            tutorPersona = "friendly",
            tutorDomain = "general",
            sourceLanguageCode = "en",
            targetLanguageCode = "es",
            conversationPhase = ConversationPhase.Correction,
            effectivePhase = ConversationPhase.Correction,
            estimatedCEFRLevel = CEFRLevel.A1,
            courseTemplateId = UUID.randomUUID()
        )

        every { authorizationService.getCurrentUserId() } returns UUID.randomUUID() // Different user
        every { chatSessionRepository.findById(sessionId) } returns Optional.of(session)

        val response = lessonController.getCurrentLesson(sessionId)

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `should return 400 for non-course-based session`() {
        val userId = UUID.randomUUID()
        val sessionId = UUID.randomUUID()

        val session = ChatSessionEntity(
            userId = userId,
            tutorName = "Maria",
            tutorPersona = "friendly",
            tutorDomain = "general",
            sourceLanguageCode = "en",
            targetLanguageCode = "es",
            conversationPhase = ConversationPhase.Correction,
            effectivePhase = ConversationPhase.Correction,
            estimatedCEFRLevel = CEFRLevel.A1,
            courseTemplateId = null // Not course-based
        )

        every { authorizationService.getCurrentUserId() } returns userId
        every { chatSessionRepository.findById(sessionId) } returns Optional.of(session)

        val response = lessonController.getCurrentLesson(sessionId)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `should advance lesson successfully`() {
        val userId = UUID.randomUUID()
        val sessionId = UUID.randomUUID()
        val courseId = UUID.randomUUID()

        val session = ChatSessionEntity(
            userId = userId,
            tutorName = "Maria",
            tutorPersona = "friendly",
            tutorDomain = "general",
            sourceLanguageCode = "en",
            targetLanguageCode = "es",
            conversationPhase = ConversationPhase.Correction,
            effectivePhase = ConversationPhase.Correction,
            estimatedCEFRLevel = CEFRLevel.A1,
            courseTemplateId = courseId,
            currentLessonId = "week-01-greetings"
        )

        val nextLesson = LessonContent(
            id = "week-02-introductions",
            title = "Introductions",
            weekNumber = 2,
            estimatedDuration = "1 week",
            focusAreas = listOf("introductions"),
            targetCEFR = CEFRLevel.A1,
            goals = emptyList(),
            grammarPoints = emptyList(),
            essentialVocabulary = emptyList(),
            conversationScenarios = emptyList(),
            practicePatterns = emptyList(),
            commonMistakes = emptyList(),
            fullMarkdown = "# Introductions"
        )

        every { authorizationService.getCurrentUserId() } returns userId
        every { chatSessionRepository.findById(sessionId) } returns Optional.of(session)
        every { lessonProgressionService.forceAdvanceLesson(session) } returns nextLesson

        val response = lessonController.advanceLesson(sessionId)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals("week-02-introductions", response.body?.id)
        verify { lessonProgressionService.forceAdvanceLesson(session) }
    }

    @Test
    fun `should return 404 when advancing on last lesson`() {
        val userId = UUID.randomUUID()
        val sessionId = UUID.randomUUID()
        val courseId = UUID.randomUUID()

        val session = ChatSessionEntity(
            userId = userId,
            tutorName = "Maria",
            tutorPersona = "friendly",
            tutorDomain = "general",
            sourceLanguageCode = "en",
            targetLanguageCode = "es",
            conversationPhase = ConversationPhase.Correction,
            effectivePhase = ConversationPhase.Correction,
            estimatedCEFRLevel = CEFRLevel.A1,
            courseTemplateId = courseId,
            currentLessonId = "week-10-future-plans"
        )

        every { authorizationService.getCurrentUserId() } returns userId
        every { chatSessionRepository.findById(sessionId) } returns Optional.of(session)
        every { lessonProgressionService.forceAdvanceLesson(session) } returns null

        val response = lessonController.advanceLesson(sessionId)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }
}
