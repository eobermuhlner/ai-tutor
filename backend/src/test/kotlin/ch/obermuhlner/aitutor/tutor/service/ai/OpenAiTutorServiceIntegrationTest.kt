package ch.obermuhlner.aitutor.tutor.service.ai

import ch.obermuhlner.aitutor.catalog.domain.CourseTemplateEntity
import ch.obermuhlner.aitutor.catalog.domain.CurriculumRuleEntity
import ch.obermuhlner.aitutor.catalog.domain.LessonContentEntity
import ch.obermuhlner.aitutor.catalog.domain.SourceType
import ch.obermuhlner.aitutor.catalog.repository.CourseTemplateRepository
import ch.obermuhlner.aitutor.catalog.repository.CurriculumRuleRepository
import ch.obermuhlner.aitutor.catalog.repository.LessonContentRepository
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.CourseCategory
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import ch.obermuhlner.aitutor.tutor.domain.ConversationState
import ch.obermuhlner.aitutor.tutor.domain.TeachingStyle
import ch.obermuhlner.aitutor.tutor.domain.Tutor
import ch.obermuhlner.aitutor.tutor.service.TutorService
import ch.obermuhlner.aitutor.vocabulary.service.VocabularyContext
import ch.obermuhlner.aitutor.vocabulary.service.VocabularyContextService
import ch.obermuhlner.aitutor.lesson.service.LessonContentService
import ch.obermuhlner.aitutor.lesson.service.LessonProgressionService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import java.time.Instant
import java.util.UUID
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/**
 * Integration test for TutorService that uses realistic Spring configuration.
 * This test requires SPRING_AI_OPENAI_API_KEY environment variable to be set and will make
 * actual API calls to OpenAI, so it's disabled by default.
 *
 * Run with: SPRING_AI_OPENAI_API_KEY=your-key ./gradlew :backend:test --tests TutorServiceIntegrationTest
 */
@SpringBootTest(properties = [
    "ai-tutor.metadata-evaluation.lesson-check-interval=1",  // Check lesson progression after every turn
    "ai-tutor.context.summarization.progressive.enabled=false"  // Disable progressive summarization for testing
])
@ActiveProfiles("dev", "h2-mem", "ai-openai", "prompts-small")
@EnabledIfEnvironmentVariable(named = "SPRING_AI_OPENAI_API_KEY", matches = ".+")
class OpenAiTutorServiceIntegrationTest {

    @Autowired
    private lateinit var tutorService: TutorService

    @Autowired
    private lateinit var courseTemplateRepository: CourseTemplateRepository

    @Autowired
    private lateinit var lessonContentRepository: LessonContentRepository

    @Autowired
    private lateinit var curriculumRuleRepository: CurriculumRuleRepository

    @Autowired
    private lateinit var chatModel: ChatModel

    @Autowired
    private lateinit var chatSessionRepository: ch.obermuhlner.aitutor.chat.repository.ChatSessionRepository

    @Autowired
    private lateinit var lessonContentService: LessonContentService

    @Autowired
    private lateinit var lessonProgressionService: LessonProgressionService

    @MockkBean
    private lateinit var vocabularyContextService: VocabularyContextService

    private val testUserId = UUID.randomUUID()
    private lateinit var testCourseId: UUID
    private lateinit var testSession: ch.obermuhlner.aitutor.chat.domain.ChatSessionEntity

    @BeforeEach
    fun setUp() {
        // Mock vocabulary context to avoid database dependencies
        val emptyVocabContext = VocabularyContext(
            totalWordCount = 0,
            wordsForReinforcement = emptyList(),
            recentNewWords = emptyList(),
            masteredWords = emptyList()
        )

        every { vocabularyContextService.getVocabularyContext(any(), any()) } returns emptyVocabContext

        // Create a test course with a simple lesson
        // NOTE: nameJson must use "en" key (not "en-US") for slug generation to work correctly
        val course = CourseTemplateEntity(
            languageCode = "es-ES",
            nameJson = """{"en": "Basic Spanish Greetings"}""",
            shortDescriptionJson = """{"en": "Learn common Spanish greetings"}""",
            descriptionJson = """{"en": "A beginner course focusing on basic Spanish greetings and introductions"}""",
            category = CourseCategory.Conversational,
            targetAudienceJson = """{"en": "Complete beginners"}""",
            startingLevel = CEFRLevel.A1,
            targetLevel = CEFRLevel.A1,
            learningGoalsJson = """{"en": ["Greet people in Spanish", "Introduce yourself"]}""",
            isActive = true,
            displayOrder = 0,
            sourceType = SourceType.CREATED
        )
        val savedCourse = courseTemplateRepository.save(course)
        testCourseId = savedCourse.id

        // Create a simple lesson
        val lessonMarkdown = """
---
lessonId: lesson-01-colors
title: Colors
lessonNumber: 1
focusAreas:
  - Colors
targetCEFR: A1
---
## Lesson Goals

- Learn the basic colors red and green

## Vocabulary
- **Rojo** - Red
- **Verde** - Green
        """.trimIndent()

        val lesson1 = LessonContentEntity(
            courseId = testCourseId,
            lessonId = "lesson-01-colors",
            title = "Basic Colors",
            content = lessonMarkdown,
            displayOrder = 1,
            requiredTurns = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        lessonContentRepository.save(lesson1)

        // Create a second lesson for days of the week
        val lesson2Markdown = """
---
lessonId: lesson-02-days
title: Days of the Week
lessonNumber: 2
focusAreas:
  - Days of the Week
  - Time expressions
targetCEFR: A1
---
## Lesson Goals

- Learn the seven days of the week in Spanish
- Practice asking about days

## Vocabulary
- **Lunes** - Monday
- **Martes** - Tuesday
- **Miércoles** - Wednesday
- **Jueves** - Thursday
- **Viernes** - Friday
- **Sábado** - Saturday
- **Domingo** - Sunday

## Common Phrases
- ¿Qué día es hoy? - What day is today?
- Hoy es... - Today is...
        """.trimIndent()

        val lesson2 = LessonContentEntity(
            courseId = testCourseId,
            lessonId = "lesson-02-days",
            title = "Days of the Week",
            content = lesson2Markdown,
            displayOrder = 2,
            requiredTurns = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        lessonContentRepository.save(lesson2)

        // Create a curriculum rule to enable database-based curriculum loading
        val curriculumRule = CurriculumRuleEntity(
            courseId = testCourseId,
            progressionMode = "COMPLETION_BASED",
            allowSkipping = false,
            requireCompletion = false,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        curriculumRuleRepository.save(curriculumRule)

        // Create a ChatSessionEntity linked to the course and lesson
        val session = ch.obermuhlner.aitutor.chat.domain.ChatSessionEntity(
            userId = testUserId,
            tutorName = "Maria",
            tutorPersona = "friendly and encouraging",
            tutorDomain = "casual conversation",
            tutorTeachingStyle = TeachingStyle.Reactive,
            sourceLanguageCode = "en-US",
            targetLanguageCode = "es-ES",
            courseTemplateId = testCourseId,
            tutorProfileId = null,
            currentLessonId = "lesson-01-colors",
            conversationPhase = ConversationPhase.Free,
            estimatedCEFRLevel = CEFRLevel.A1,
            currentTopic = "colors"
        )
        testSession = chatSessionRepository.save(session)

        // Verify course and lesson setup
        println("=== Setup Verification ===")
        println("Course ID: $testCourseId")
        println("Session ID: ${testSession.id}")
        println("Session courseTemplateId: ${testSession.courseTemplateId}")
        println("Session currentLessonId: ${testSession.currentLessonId}")

        // Test curriculum loading via UUID
        println("Testing curriculum loading with UUID: $testCourseId")
        val curriculumViaUuid = lessonContentService.getCurriculum(testCourseId.toString())
        println("Curriculum loaded via UUID: ${curriculumViaUuid != null}")
        if (curriculumViaUuid != null) {
            println("Curriculum course ID: ${curriculumViaUuid.courseId}")
            println("Curriculum lessons: ${curriculumViaUuid.lessons.map { it.id }}")
            println("Number of lessons: ${curriculumViaUuid.lessons.size}")
        }

        // Test direct lesson loading
        println("\nTesting direct lesson loading...")
        val directLesson = lessonContentService.getLesson(testCourseId.toString(), "lesson-01-colors")
        println("Direct lesson loaded: ${directLesson != null}")
        if (directLesson != null) {
            println("Direct lesson ID: ${directLesson.id}")
            println("Direct lesson title: ${directLesson.title}")
        }

        // Test lesson loading through progression service
        println("\nTesting lesson loading through LessonProgressionService...")
        val currentLesson = lessonProgressionService.getCurrentLesson(testSession.id)
        println("Current lesson loaded: ${currentLesson != null}")
        if (currentLesson != null) {
            println("Lesson ID: ${currentLesson.id}")
            println("Lesson title: ${currentLesson.title}")
            println("Lesson content preview: ${currentLesson.fullMarkdown.take(100)}...")
        } else {
            println("ERROR: getCurrentLesson returned null")
            println("Debugging getCurrentLesson flow:")

            // Re-check session
            val sessionCheck = chatSessionRepository.findById(testSession.id).orElse(null)
            println("  Session exists: ${sessionCheck != null}")
            println("  Session courseTemplateId: ${sessionCheck?.courseTemplateId}")
            println("  Session currentLessonId: ${sessionCheck?.currentLessonId}")

            // Check curriculum via course lookup
            println("  Checking if curriculum can be loaded...")
            val curriculumCheck = lessonContentService.getCurriculum(testCourseId.toString())
            println("  Curriculum loaded: ${curriculumCheck != null}")
        }

        // Assertions
        Assertions.assertThat(curriculumViaUuid).withFailMessage("Curriculum should be loaded from database").isNotNull()
        Assertions.assertThat(curriculumViaUuid?.lessons?.size).withFailMessage("Curriculum should have 2 lessons").isEqualTo(2)
        Assertions.assertThat(directLesson).withFailMessage("Direct lesson should be loaded").isNotNull()
        Assertions.assertThat(currentLesson).withFailMessage("Current lesson should be loaded via LessonProgressionService").isNotNull()
        Assertions.assertThat(currentLesson?.id).withFailMessage("Lesson ID should be lesson-01-colors").isEqualTo("lesson-01-colors")

        println("=== Setup Verification Complete ===\n")
    }

    @Test
    fun `respond generates valid response and maintains conversation loop`() {
        // Given
        val tutor = Tutor(
            name = "Maria",
            sourceLanguageCode = "en-US",
            targetLanguageCode = "es-ES",
            persona = "friendly and encouraging",
            domain = "casual conversation",
            teachingStyle = TeachingStyle.Reactive
        )

        val conversationState = ConversationState(
            phase = ConversationPhase.Free,
            estimatedCEFRLevel = CEFRLevel.A1,
            currentTopic = "colors",
            phaseReason = "Starting conversation",
            topicEligibilityStatus = "Active conversation"
        )

        val messages = mutableListOf<Message>(UserMessage("Hola"))
        val maxTurns = 20

        println("=== Starting Conversation Loop (Max $maxTurns turns) ===\n")

        // Track current lesson
        var currentLessonId = testSession.currentLessonId
        println("Initial lesson: $currentLessonId\n")

        // Conversation loop
        for (turn in 1..maxTurns) {
            println("--- Turn $turn ---")

            // Tutor responds
            val tutorResult = tutorService.respond(
                tutor = tutor,
                conversationState = conversationState,
                userId = testUserId,
                messages = messages,
                sessionId = testSession.id,
                session = testSession
            )

            // Validate tutor response
            Assertions.assertThat(tutorResult).isNotNull
            Assertions.assertThat(tutorResult!!.reply).isNotEmpty()
            Assertions.assertThat(tutorResult.conversationResponse).isNotNull

            println("TUTOR: ${tutorResult.reply}")

            if (tutorResult.conversationResponse.newVocabulary.isNotEmpty()) {
                println("New Vocabulary: ${tutorResult.conversationResponse.newVocabulary}")
            }
            if (tutorResult.conversationResponse.wordCards.isNotEmpty()) {
                println("Word Cards: ${tutorResult.conversationResponse.wordCards.map { it.titleTargetLanguage }}")
            }
            if (tutorResult.conversationResponse.characterCards.isNotEmpty()) {
                println("Character Cards: ${tutorResult.conversationResponse.characterCards.map { it.character }}")
            }

            // Add tutor's response to conversation
            messages.add(AssistantMessage(tutorResult.reply))

            // Check for lesson progression (lesson-check-interval is set to 1)
            val updatedSession = chatSessionRepository.findById(testSession.id).orElse(null)
            if (updatedSession != null && updatedSession.currentLessonId != currentLessonId) {
                println(">>> LESSON PROGRESSION DETECTED <<<")
                println("    Previous lesson: $currentLessonId")
                println("    New lesson: ${updatedSession.currentLessonId}")

                val newLesson = lessonProgressionService.getCurrentLesson(testSession.id)
                if (newLesson != null) {
                    println("    New lesson title: ${newLesson.title}")
                    println("    New lesson content preview: ${newLesson.fullMarkdown.take(150)}...")
                }

                currentLessonId = updatedSession.currentLessonId
                println()
            } else {
                println("Current lesson: $currentLessonId (no progression)")
            }

            // Learner responds
            val learnerReply = chatModel.call("""
You are a Spanish language student at A1 level.
You are having a conversation with your teacher Maria.
Respond naturally to your teacher's message in Spanish. Keep your response short (1-2 sentences).

Teacher's message: ${tutorResult.reply}
            """.trimIndent())

            println("LEARNER: $learnerReply")
            println()

            // Add learner's response to conversation
            messages.add(UserMessage(learnerReply))
        }

        println("=== Conversation Complete ($maxTurns turns) ===")

        println("Messages:")
        messages.forEach { println(it) }

        // Final assertions
        Assertions.assertThat(messages.size).isGreaterThan(maxTurns)
        println("\nTotal messages exchanged: ${messages.size}")
    }
}