package ch.obermuhlner.aitutor.lesson.service

import ch.obermuhlner.aitutor.catalog.service.CatalogService
import ch.obermuhlner.aitutor.chat.domain.ChatMessageEntity
import ch.obermuhlner.aitutor.chat.domain.ChatSessionEntity
import ch.obermuhlner.aitutor.chat.domain.MessageRole
import ch.obermuhlner.aitutor.chat.repository.ChatSessionRepository
import ch.obermuhlner.aitutor.conversation.service.ChatOptionsFactory
import ch.obermuhlner.aitutor.conversation.service.UserChatModelFactory
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.fixtures.TestDataFactory
import ch.obermuhlner.aitutor.language.service.LanguageService
import ch.obermuhlner.aitutor.lesson.domain.LessonContent
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.Prompt
import java.util.UUID

class LessonGoalsEvaluationServiceTest {

    private lateinit var service: LessonGoalsEvaluationService
    private lateinit var lessonContentService: LessonContentService
    private lateinit var catalogService: CatalogService
    private lateinit var userChatModelFactory: UserChatModelFactory
    private lateinit var languageService: LanguageService
    private lateinit var chatSessionRepository: ChatSessionRepository
    private lateinit var chatOptionsFactory: ChatOptionsFactory
    private lateinit var chatModel: ChatModel
    private lateinit var objectMapper: ObjectMapper

    private val testPrompt = "Evaluate lesson content: {lessonContent} for conversation: {conversationContext}"

    @BeforeEach
    fun setup() {
        lessonContentService = mockk()
        catalogService = mockk()
        userChatModelFactory = mockk()
        languageService = mockk()
        chatSessionRepository = mockk()
        chatModel = mockk()
        objectMapper = jacksonObjectMapper()

        chatOptionsFactory = mockk()

        service = LessonGoalsEvaluationService(
            lessonContentService,
            catalogService,
            userChatModelFactory,
            languageService,
            chatSessionRepository,
            objectMapper,
            chatOptionsFactory,
            testPrompt
        )
    }

    @Test
    fun `should evaluate lesson goals and update session`() {
        val session = createTestSession()
        val courseId = session.courseTemplateId!!
        val lessonId = session.currentLessonId!!

        val course = mockk<ch.obermuhlner.aitutor.catalog.domain.CourseTemplateEntity>()
        every { course.nameJson } returns """{"en": "Test Course"}"""
        every { course.languageCode } returns "de-DE"

        val lessonContent = LessonContent(
            id = lessonId,
            title = "Test Lesson",
            lessonNumber = 1,
            
            focusAreas = listOf("greetings", "phrases"),
            targetCEFR = CEFRLevel.A1,
            fullMarkdown = """
                # Test Lesson

                ## Lesson Goals
                - Practice greetings
                - Learn basic phrases

                ## Content
                Some content here
            """.trimIndent()
        )

        val messages = listOf(
            createMessage(session, MessageRole.USER, "Guten Tag!"),
            createMessage(session, MessageRole.ASSISTANT, "Sehr gut! Guten Tag!")
        )

        every { catalogService.getCourseById(courseId) } returns course
        every { lessonContentService.getLesson("de-test-course", lessonId) } returns lessonContent
        every { languageService.getLanguageName("de-DE") } returns "German"
        every { languageService.getLanguageName("en") } returns "English"
        every { userChatModelFactory.getChatModelForUser(session.userId) } returns chatModel
        every { chatOptionsFactory.createOptions(any(), any()) } returns null  // Return null to force fallback to soft enforcement
        every { chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()) } returns mockk {
            every { result.output.text } returns """{"goalsCompleted": true, "reasoning": "Good progress"}"""
        }
        every { chatSessionRepository.save(any()) } answers { firstArg() }

        service.evaluateLessonGoals(session, messages)

        verify { chatSessionRepository.save(match { savedSession ->
            savedSession.lessonProgressGoalsCompleted == true
        }) }
    }

    @Test
    fun `should skip evaluation when no active lesson`() {
        val session = createTestSession()
        session.currentLessonId = null

        val messages = emptyList<ChatMessageEntity>()

        service.evaluateLessonGoals(session, messages)

        verify(exactly = 0) { chatSessionRepository.save(any()) }
    }

    @Test
    fun `should skip evaluation when no course`() {
        val session = createTestSession()
        session.courseTemplateId = null

        val messages = emptyList<ChatMessageEntity>()

        service.evaluateLessonGoals(session, messages)

        verify(exactly = 0) { chatSessionRepository.save(any()) }
    }

    @Test
    fun `should evaluate even when no explicit goals section exists`() {
        val session = createTestSession()
        val courseId = session.courseTemplateId!!
        val lessonId = session.currentLessonId!!

        val course = mockk<ch.obermuhlner.aitutor.catalog.domain.CourseTemplateEntity>()
        every { course.nameJson } returns """{"en": "Test Course"}"""
        every { course.languageCode } returns "de-DE"

        val lessonContent = LessonContent(
            id = lessonId,
            title = "Test Lesson",
            lessonNumber = 1,
            
            focusAreas = listOf("content"),
            targetCEFR = CEFRLevel.A1,
            fullMarkdown = """
                # Test Lesson

                ## Content
                No explicit goals section, but lesson content is provided to LLM
            """.trimIndent()
        )

        every { catalogService.getCourseById(courseId) } returns course
        every { lessonContentService.getLesson("de-test-course", lessonId) } returns lessonContent
        every { languageService.getLanguageName("de-DE") } returns "German"
        every { languageService.getLanguageName("en") } returns "English"
        every { userChatModelFactory.getChatModelForUser(session.userId) } returns chatModel
        every { chatOptionsFactory.createOptions(any(), any()) } returns null
        every { chatModel.call(any<Prompt>()) } returns mockk {
            every { result.output.text } returns """{"goalsCompleted": true, "reasoning": "Content covered"}"""
        }
        every { chatSessionRepository.save(any()) } answers { firstArg() }

        service.evaluateLessonGoals(session, emptyList())

        verify { chatSessionRepository.save(match { it.lessonProgressGoalsCompleted == true }) }
    }

    @Test
    fun `should handle LLM errors gracefully`() {
        val session = createTestSession()
        val courseId = session.courseTemplateId!!
        val lessonId = session.currentLessonId!!

        val course = mockk<ch.obermuhlner.aitutor.catalog.domain.CourseTemplateEntity>()
        every { course.nameJson } returns """{"en": "Test Course"}"""
        every { course.languageCode } returns "de-DE"

        val lessonContent = LessonContent(
            id = lessonId,
            title = "Test Lesson",
            lessonNumber = 1,
            
            focusAreas = listOf("test"),
            targetCEFR = CEFRLevel.A1,
            fullMarkdown = """
                ## Goals
                - Test goal
            """.trimIndent()
        )

        every { catalogService.getCourseById(courseId) } returns course
        every { lessonContentService.getLesson("de-test-course", lessonId) } returns lessonContent
        every { languageService.getLanguageName("de-DE") } returns "German"
        every { languageService.getLanguageName("en") } returns "English"
        every { userChatModelFactory.getChatModelForUser(session.userId) } returns chatModel
        every { chatOptionsFactory.createOptions(any(), any()) } returns null  // Return null to force fallback to soft enforcement
        every { chatModel.call(any<Prompt>()) } throws RuntimeException("LLM Error")

        // Should not throw exception
        service.evaluateLessonGoals(session, emptyList())

        // Should not save if error occurred
        verify(exactly = 0) { chatSessionRepository.save(any()) }
    }

    @Test
    fun `should pass full lesson content to LLM including all sections`() {
        val session = createTestSession()
        val courseId = session.courseTemplateId!!
        val lessonId = session.currentLessonId!!

        val course = mockk<ch.obermuhlner.aitutor.catalog.domain.CourseTemplateEntity>()
        every { course.nameJson } returns """{"en": "Test"}"""
        every { course.languageCode } returns "en"

        val lessonContent = LessonContent(
            id = lessonId,
            title = "Test",
            lessonNumber = 1,
            
            focusAreas = listOf("vocabulary", "speaking"),
            targetCEFR = CEFRLevel.A1,
            fullMarkdown = """
                ## Goals
                - Learn vocabulary
                - Practice speaking

                ## Next Section
                Other content
            """.trimIndent()
        )

        every { catalogService.getCourseById(courseId) } returns course
        every { lessonContentService.getLesson(any(), lessonId) } returns lessonContent
        every { languageService.getLanguageName(any()) } returns "English"
        every { userChatModelFactory.getChatModelForUser(session.userId) } returns chatModel
        every { chatOptionsFactory.createOptions(any(), any()) } returns null  // Return null to force fallback to soft enforcement
        every { chatModel.call(any<Prompt>()) } returns mockk {
            every { result.output.text } returns """{"goalsCompleted": false, "reasoning": "Test"}"""
        }
        every { chatSessionRepository.save(any()) } answers { firstArg() }

        service.evaluateLessonGoals(session, emptyList())

        // Verify that full lesson content is passed (including both Goals and Next Section)
        verify { chatModel.call(match<Prompt> {
            it.instructions[0].text.contains("Learn vocabulary") &&
            it.instructions[0].text.contains("Practice speaking") &&
            it.instructions[0].text.contains("Next Section") &&
            it.instructions[0].text.contains("Other content")
        }) }
    }

    @Test
    fun `should pass full lesson content including activities and objectives`() {
        val session = createTestSession()
        val courseId = session.courseTemplateId!!
        val lessonId = session.currentLessonId!!

        val course = mockk<ch.obermuhlner.aitutor.catalog.domain.CourseTemplateEntity>()
        every { course.nameJson } returns """{"en": "Test"}"""
        every { course.languageCode } returns "en"

        val lessonContent = LessonContent(
            id = lessonId,
            title = "Test",
            lessonNumber = 1,
            
            focusAreas = listOf("pronunciation"),
            targetCEFR = CEFRLevel.A1,
            fullMarkdown = """
                ## Learning Objectives
                - Master pronunciation

                ## Activities
                Practice time
            """.trimIndent()
        )

        every { catalogService.getCourseById(courseId) } returns course
        every { lessonContentService.getLesson(any(), lessonId) } returns lessonContent
        every { languageService.getLanguageName(any()) } returns "English"
        every { userChatModelFactory.getChatModelForUser(session.userId) } returns chatModel
        every { chatOptionsFactory.createOptions(any(), any()) } returns null  // Return null to force fallback to soft enforcement
        every { chatModel.call(any<Prompt>()) } returns mockk {
            every { result.output.text } returns """{"goalsCompleted": false, "reasoning": "Test"}"""
        }
        every { chatSessionRepository.save(any()) } answers { firstArg() }

        service.evaluateLessonGoals(session, emptyList())

        // Verify that full lesson content is passed (including both Objectives and Activities sections)
        verify { chatModel.call(match<Prompt> {
            it.instructions[0].text.contains("Master pronunciation") &&
            it.instructions[0].text.contains("Activities") &&
            it.instructions[0].text.contains("Practice time")
        }) }
    }

    private fun createTestSession(): ChatSessionEntity {
        return ChatSessionEntity(
            id = TestDataFactory.TEST_SESSION_ID,
            userId = TestDataFactory.TEST_USER_ID,
            tutorName = "Test Tutor",
            sourceLanguageCode = "en",
            targetLanguageCode = "de-DE",
            estimatedCEFRLevel = CEFRLevel.A1,
            courseTemplateId = UUID.randomUUID(),
            currentLessonId = "lesson-01",
            lessonProgressTurnCount = 10,
            lessonProgressGoalsCompleted = false
        )
    }

    private fun createMessage(session: ChatSessionEntity, role: MessageRole, content: String): ChatMessageEntity {
        return ChatMessageEntity(
            session = session,
            role = role,
            content = content
        )
    }
}
