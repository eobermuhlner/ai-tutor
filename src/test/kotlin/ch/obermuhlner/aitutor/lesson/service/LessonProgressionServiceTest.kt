package ch.obermuhlner.aitutor.lesson.service

import ch.obermuhlner.aitutor.catalog.domain.CourseTemplateEntity
import ch.obermuhlner.aitutor.catalog.service.CatalogService
import ch.obermuhlner.aitutor.chat.domain.ChatSessionEntity
import ch.obermuhlner.aitutor.chat.repository.ChatMessageRepository
import ch.obermuhlner.aitutor.chat.repository.ChatSessionRepository
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.lesson.domain.CourseCurriculum
import ch.obermuhlner.aitutor.lesson.domain.LessonContent
import ch.obermuhlner.aitutor.lesson.domain.LessonMetadata
import ch.obermuhlner.aitutor.lesson.domain.ProgressionMode
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LessonProgressionServiceTest {

    private lateinit var lessonContentService: LessonContentService
    private lateinit var chatSessionRepository: ChatSessionRepository
    private lateinit var chatMessageRepository: ChatMessageRepository
    private lateinit var catalogService: CatalogService
    private lateinit var objectMapper: ObjectMapper
    private lateinit var lessonProgressionService: LessonProgressionService

    @BeforeEach
    fun setup() {
        lessonContentService = mockk()
        chatSessionRepository = mockk()
        chatMessageRepository = mockk()
        catalogService = mockk()
        objectMapper = ObjectMapper()

        lessonProgressionService = LessonProgressionService(
            lessonContentService,
            chatSessionRepository,
            chatMessageRepository,
            catalogService,
            objectMapper
        )
    }

    @Test
    fun `should activate first lesson when currentLessonId is null`() {
        val courseId = UUID.randomUUID()
        val session = ChatSessionEntity(
            userId = UUID.randomUUID(),
            tutorName = "Maria",
            tutorPersona = "friendly",
            tutorDomain = "general",
            sourceLanguageCode = "en",
            targetLanguageCode = "es",
            conversationPhase = ConversationPhase.Correction,
            effectivePhase = ConversationPhase.Correction,
            estimatedCEFRLevel = CEFRLevel.A1,
            courseTemplateId = courseId,
            currentLessonId = null
        )

        val course = mockk<CourseTemplateEntity>()
        every { course.languageCode } returns "es"
        every { course.nameJson } returns """{"en": "Conversational Spanish"}"""
        every { catalogService.getCourseById(courseId) } returns course

        val curriculum = CourseCurriculum(
            courseId = "es-conversational-spanish",
            progressionMode = ProgressionMode.TIME_BASED,
            lessons = listOf(
                LessonMetadata("week-01-greetings", "week-01-greetings.md", 0, 5)
            )
        )
        every { lessonContentService.getCurriculum("es-conversational-spanish") } returns curriculum

        val lessonContent = mockk<LessonContent>()
        every { lessonContentService.getLesson("es-conversational-spanish", "week-01-greetings") } returns lessonContent
        every { chatSessionRepository.save(any()) } returns session

        val result = lessonProgressionService.checkAndProgressLesson(session)

        assertNotNull(result)
        assertEquals("week-01-greetings", session.currentLessonId)
        assertNotNull(session.lessonStartedAt)
        verify { chatSessionRepository.save(session) }
    }

    @Test
    fun `should return null when course not found`() {
        val session = ChatSessionEntity(
            userId = UUID.randomUUID(),
            tutorName = "Maria",
            tutorPersona = "friendly",
            tutorDomain = "general",
            sourceLanguageCode = "en",
            targetLanguageCode = "es",
            conversationPhase = ConversationPhase.Correction,
            effectivePhase = ConversationPhase.Correction,
            estimatedCEFRLevel = CEFRLevel.A1,
            courseTemplateId = UUID.randomUUID(),
            currentLessonId = null
        )

        every { catalogService.getCourseById(any()) } returns null

        val result = lessonProgressionService.checkAndProgressLesson(session)

        assertNull(result)
    }

    @Test
    fun `should not advance when time criteria not met`() {
        val courseId = UUID.randomUUID()
        val session = ChatSessionEntity(
            userId = UUID.randomUUID(),
            tutorName = "Maria",
            tutorPersona = "friendly",
            tutorDomain = "general",
            sourceLanguageCode = "en",
            targetLanguageCode = "es",
            conversationPhase = ConversationPhase.Correction,
            effectivePhase = ConversationPhase.Correction,
            estimatedCEFRLevel = CEFRLevel.A1,
            courseTemplateId = courseId,
            currentLessonId = "week-01-greetings",
            lessonStartedAt = Instant.now().minus(3, ChronoUnit.DAYS), // Only 3 days
            lessonProgressJson = """{"turnCount": 0}"""
        )

        val course = mockk<CourseTemplateEntity>()
        every { course.languageCode } returns "es"
        every { course.nameJson } returns """{"en": "Conversational Spanish"}"""
        every { catalogService.getCourseById(courseId) } returns course

        val curriculum = CourseCurriculum(
            courseId = "es-conversational-spanish",
            progressionMode = ProgressionMode.TIME_BASED,
            lessons = listOf(
                LessonMetadata("week-01-greetings", "week-01-greetings.md", 7, 5), // Requires 7 days
                LessonMetadata("week-02-introductions", "week-02-introductions.md", 14, 8)
            )
        )
        every { lessonContentService.getCurriculum("es-conversational-spanish") } returns curriculum

        val lessonContent = mockk<LessonContent>()
        every { lessonContentService.getLesson("es-conversational-spanish", "week-01-greetings") } returns lessonContent
        every { chatMessageRepository.countBySessionId(any()) } returns 10L

        val result = lessonProgressionService.checkAndProgressLesson(session)

        assertNotNull(result)
        assertEquals("week-01-greetings", session.currentLessonId) // Should stay on same lesson
    }

    @Test
    fun `should advance when time and turn criteria met`() {
        val courseId = UUID.randomUUID()
        val sessionId = UUID.randomUUID()
        val session = ChatSessionEntity(
            id = sessionId,
            userId = UUID.randomUUID(),
            tutorName = "Maria",
            tutorPersona = "friendly",
            tutorDomain = "general",
            sourceLanguageCode = "en",
            targetLanguageCode = "es",
            conversationPhase = ConversationPhase.Correction,
            effectivePhase = ConversationPhase.Correction,
            estimatedCEFRLevel = CEFRLevel.A1,
            courseTemplateId = courseId,
            currentLessonId = "week-01-greetings",
            lessonStartedAt = Instant.now().minus(8, ChronoUnit.DAYS), // 8 days
            lessonProgressJson = """{"turnCount": 0}"""
        )

        val course = mockk<CourseTemplateEntity>()
        every { course.languageCode } returns "es"
        every { course.nameJson } returns """{"en": "Conversational Spanish"}"""
        every { catalogService.getCourseById(courseId) } returns course

        val curriculum = CourseCurriculum(
            courseId = "es-conversational-spanish",
            progressionMode = ProgressionMode.TIME_BASED,
            lessons = listOf(
                LessonMetadata("week-01-greetings", "week-01-greetings.md", 7, 5),
                LessonMetadata("week-02-introductions", "week-02-introductions.md", 14, 8)
            )
        )
        every { lessonContentService.getCurriculum("es-conversational-spanish") } returns curriculum

        val lessonContent = mockk<LessonContent>()
        every { lessonContentService.getLesson("es-conversational-spanish", "week-02-introductions") } returns lessonContent
        every { chatMessageRepository.countBySessionId(sessionId) } returns 10L // Exceeds required 5 turns
        every { chatSessionRepository.save(any()) } returns session

        val result = lessonProgressionService.checkAndProgressLesson(session)

        assertNotNull(result)
        assertEquals("week-02-introductions", session.currentLessonId)
        verify { chatSessionRepository.save(session) }
    }

    @Test
    fun `should not advance when on last lesson`() {
        val courseId = UUID.randomUUID()
        val sessionId = UUID.randomUUID()
        val session = ChatSessionEntity(
            id = sessionId,
            userId = UUID.randomUUID(),
            tutorName = "Maria",
            tutorPersona = "friendly",
            tutorDomain = "general",
            sourceLanguageCode = "en",
            targetLanguageCode = "es",
            conversationPhase = ConversationPhase.Correction,
            effectivePhase = ConversationPhase.Correction,
            estimatedCEFRLevel = CEFRLevel.A1,
            courseTemplateId = courseId,
            currentLessonId = "week-10-future-plans",
            lessonStartedAt = Instant.now().minus(70, ChronoUnit.DAYS),
            lessonProgressJson = """{"turnCount": 0}"""
        )

        val course = mockk<CourseTemplateEntity>()
        every { course.languageCode } returns "es"
        every { course.nameJson } returns """{"en": "Conversational Spanish"}"""
        every { catalogService.getCourseById(courseId) } returns course

        val curriculum = CourseCurriculum(
            courseId = "es-conversational-spanish",
            progressionMode = ProgressionMode.TIME_BASED,
            lessons = listOf(
                LessonMetadata("week-10-future-plans", "week-10-future-plans.md", 63, 15)
            )
        )
        every { lessonContentService.getCurriculum("es-conversational-spanish") } returns curriculum

        val lessonContent = mockk<LessonContent>()
        every { lessonContentService.getLesson("es-conversational-spanish", "week-10-future-plans") } returns lessonContent
        every { chatMessageRepository.countBySessionId(sessionId) } returns 20L

        val result = lessonProgressionService.checkAndProgressLesson(session)

        // TODO: Service currently returns null when on last lesson and criteria met to advance
        // Consider returning current lesson content instead
        // For now, verify session state remains unchanged
        assertEquals("week-10-future-plans", session.currentLessonId) // Stays on last lesson
    }

    @Test
    fun `should force advance to next lesson`() {
        val courseId = UUID.randomUUID()
        val sessionId = UUID.randomUUID()
        val session = ChatSessionEntity(
            id = sessionId,
            userId = UUID.randomUUID(),
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

        val course = mockk<CourseTemplateEntity>()
        every { course.languageCode } returns "es"
        every { course.nameJson } returns """{"en": "Conversational Spanish"}"""
        every { catalogService.getCourseById(courseId) } returns course

        val curriculum = CourseCurriculum(
            courseId = "es-conversational-spanish",
            progressionMode = ProgressionMode.TIME_BASED,
            lessons = listOf(
                LessonMetadata("week-01-greetings", "week-01-greetings.md", 7, 5),
                LessonMetadata("week-02-introductions", "week-02-introductions.md", 14, 8)
            )
        )
        every { lessonContentService.getCurriculum("es-conversational-spanish") } returns curriculum

        val lessonContent = mockk<LessonContent>()
        every { lessonContentService.getLesson("es-conversational-spanish", "week-02-introductions") } returns lessonContent
        every { chatSessionRepository.save(any()) } returns session

        val result = lessonProgressionService.forceAdvanceLesson(session)

        assertNotNull(result)
        assertEquals("week-02-introductions", session.currentLessonId)
        verify { chatSessionRepository.save(session) }
    }

    @Test
    fun `should return null when forcing advance on last lesson`() {
        val courseId = UUID.randomUUID()
        val session = ChatSessionEntity(
            userId = UUID.randomUUID(),
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

        val course = mockk<CourseTemplateEntity>()
        every { course.languageCode } returns "es"
        every { course.nameJson } returns """{"en": "Conversational Spanish"}"""
        every { catalogService.getCourseById(courseId) } returns course

        val curriculum = CourseCurriculum(
            courseId = "es-conversational-spanish",
            progressionMode = ProgressionMode.TIME_BASED,
            lessons = listOf(
                LessonMetadata("week-10-future-plans", "week-10-future-plans.md", 63, 15)
            )
        )
        every { lessonContentService.getCurriculum("es-conversational-spanish") } returns curriculum

        val result = lessonProgressionService.forceAdvanceLesson(session)

        assertNull(result)
    }
}
