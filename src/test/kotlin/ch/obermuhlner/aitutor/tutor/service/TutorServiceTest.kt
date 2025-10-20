package ch.obermuhlner.aitutor.tutor.service

import ch.obermuhlner.aitutor.conversation.dto.AiChatResponse
import ch.obermuhlner.aitutor.conversation.service.AiChatService
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.Correction
import ch.obermuhlner.aitutor.core.model.ErrorSeverity
import ch.obermuhlner.aitutor.core.model.ErrorType
import ch.obermuhlner.aitutor.core.model.catalog.Difficulty
import ch.obermuhlner.aitutor.core.model.catalog.LanguageMetadata
import ch.obermuhlner.aitutor.language.service.LanguageService
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import ch.obermuhlner.aitutor.tutor.domain.ConversationResponse
import ch.obermuhlner.aitutor.tutor.domain.ConversationState
import ch.obermuhlner.aitutor.tutor.domain.Tutor
import ch.obermuhlner.aitutor.vocabulary.service.VocabularyContext
import ch.obermuhlner.aitutor.vocabulary.service.VocabularyContextService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.UserMessage

class TutorServiceTest {

    private lateinit var aiChatService: AiChatService
    private lateinit var languageService: LanguageService
    private lateinit var vocabularyContextService: VocabularyContextService
    private lateinit var lessonProgressionService: ch.obermuhlner.aitutor.lesson.service.LessonProgressionService
    private lateinit var tutorService: TutorService

    private val systemPromptTemplate = "You are {tutorName}, teaching {targetLanguage} to {sourceLanguage} speakers. Persona: {tutorPersona}. Domain: {tutorDomain}. {vocabularyGuidance}"
    private val phaseFreePromptTemplate = "Phase: Free conversation in {targetLanguage}."
    private val phaseCorrectionPromptTemplate = "Phase: Correction mode in {targetLanguage}."
    private val phaseDrillPromptTemplate = "Phase: Drill mode in {targetLanguage}."
    private val developerPromptTemplate = "Developer context: {targetLanguage} to {sourceLanguage}"
    private val vocabularyNoTrackingTemplate = "No vocabulary tracking yet."
    private val vocabularyWithTrackingTemplate = "Total words: {totalWordCount}. Reinforce: {wordsForReinforcement}. Recent: {recentNewWords}. Mastered: {masteredWords}."

    @BeforeEach
    fun setup() {
        aiChatService = mockk()
        languageService = mockk()
        vocabularyContextService = mockk()
        lessonProgressionService = mockk(relaxed = true)
        val mockSummarizationService = mockk<ConversationSummarizationService>(relaxed = true)
        val mockProgressiveSummarizationService = mockk<ProgressiveSummarizationService>(relaxed = true)
        val messageCompactionService = MessageCompactionService(
            maxTokens = 100000,
            recentMessageCount = 15,
            summarizationEnabled = false, // Disable for tests to avoid mocking
            progressiveEnabled = false,
            summaryPrefixPrompt = "Previous conversation summary: {summary}",
            summarizationService = mockSummarizationService,
            progressiveSummarizationService = mockProgressiveSummarizationService
        )

        val supportedLanguages = mapOf(
            "es" to LanguageMetadata(
                code = "es",
                nameJson = """{"en": "Spanish"}""",
                flagEmoji = "🇪🇸",
                nativeName = "Español",
                difficulty = Difficulty.Easy,
                descriptionJson = """{"en": "Romance language"}"""
            )
        )

        tutorService = TutorService(
            aiChatService = aiChatService,
            languageService = languageService,
            vocabularyContextService = vocabularyContextService,
            messageCompactionService = messageCompactionService,
            lessonProgressionService = lessonProgressionService,
            supportedLanguages = supportedLanguages,
            systemPromptTemplate = systemPromptTemplate,
            levelNonePromptTemplate = "CEFR Level None template",
            levelA1PromptTemplate = "CEFR Level A1 template",
            levelA2PromptTemplate = "CEFR Level A2 template",
            levelB1PromptTemplate = "CEFR Level B1 template",
            levelB2PromptTemplate = "CEFR Level B2 template",
            levelC1PromptTemplate = "CEFR Level C1 template",
            levelC2PromptTemplate = "CEFR Level C2 template",
            phaseFreePromptTemplate = phaseFreePromptTemplate,
            phaseCorrectionPromptTemplate = phaseCorrectionPromptTemplate,
            phaseDrillPromptTemplate = phaseDrillPromptTemplate,
            developerPromptTemplate = developerPromptTemplate,
            errorClassificationGuidance = "Error classification decision tree guidance",
            vocabularyNoTrackingTemplate = vocabularyNoTrackingTemplate,
            vocabularyWithTrackingTemplate = vocabularyWithTrackingTemplate,
            teachingStyleCourseBasedTemplate = "Course-based teaching style guidance",
            teachingStyleReactiveTemplate = "Reactive teaching style guidance",
            teachingStyleGuidedTemplate = "Guided teaching style guidance",
            teachingStyleDirectiveTemplate = "Directive teaching style guidance",
            lessonPrompt = "Lesson template"
        )
    }

    @Test
    fun `respond with free phase and no vocabulary tracking`() {
        val userId = UUID.randomUUID()
        val tutor = Tutor(
            name = "Maria",
            sourceLanguageCode = "en",
            targetLanguageCode = "es",
            persona = "friendly",
            domain = "casual"
        )
        val conversationState = ConversationState(phase = ConversationPhase.Free, estimatedCEFRLevel = CEFRLevel.A1)
        val messages = listOf<Message>(UserMessage("Hello"))

        val vocabContext = VocabularyContext(
            totalWordCount = 0,
            wordsForReinforcement = emptyList(),
            recentNewWords = emptyList(),
            masteredWords = emptyList()
        )

        val conversationResponse = ConversationResponse(
            conversationState = conversationState,
            corrections = emptyList(),
            newVocabulary = emptyList()
        )

        val aiResponse = AiChatResponse(
            reply = "Hola! How can I help?",
            conversationResponse = conversationResponse
        )

        every { languageService.getLanguageName("en") } returns "English"
        every { languageService.getLanguageName("es") } returns "Spanish"
        every { vocabularyContextService.getVocabularyContext(userId, "es") } returns vocabContext
        every { aiChatService.call(any(), any()) } returns aiResponse

        val result = tutorService.respond(tutor, conversationState, userId, messages)

        assertNotNull(result)
        assertEquals("Hola! How can I help?", result?.reply)
        assertEquals(ConversationPhase.Free, result?.conversationResponse?.conversationState?.phase)
        verify { aiChatService.call(any(), any()) }
    }

    @Test
    fun `respond with correction phase and vocabulary tracking`() {
        val userId = UUID.randomUUID()
        val tutor = Tutor(
            name = "Maria",
            sourceLanguageCode = "en",
            targetLanguageCode = "es",
            persona = "encouraging",
            domain = "general"
        )
        val conversationState = ConversationState(phase = ConversationPhase.Correction, estimatedCEFRLevel = CEFRLevel.A1)
        val messages = listOf<Message>(UserMessage("Yo hablo español"))

        val vocabContext = VocabularyContext(
            totalWordCount = 50,
            wordsForReinforcement = listOf("hablar", "comer"),
            recentNewWords = listOf("nuevo", "viejo"),
            masteredWords = listOf("hola", "adiós", "gracias")
        )

        val conversationResponse = ConversationResponse(
            conversationState = conversationState,
            corrections = listOf(
                Correction(
                    span = "espanol",
                    errorType = ErrorType.Typography,
                    severity = ErrorSeverity.Low,
                    correctedTargetLanguage = "español",
                    whySourceLanguage = "Missing accent",
                    whyTargetLanguage = "Falta el acento"
                )
            ),
            newVocabulary = emptyList()
        )

        val aiResponse = AiChatResponse(
            reply = "¡Muy bien!",
            conversationResponse = conversationResponse
        )

        every { languageService.getLanguageName("en") } returns "English"
        every { languageService.getLanguageName("es") } returns "Spanish"
        every { vocabularyContextService.getVocabularyContext(userId, "es") } returns vocabContext
        every { aiChatService.call(any(), any()) } returns aiResponse

        val result = tutorService.respond(tutor, conversationState, userId, messages)

        assertNotNull(result)
        assertEquals("¡Muy bien!", result?.reply)
        assertEquals(1, result?.conversationResponse?.corrections?.size)
        verify { aiChatService.call(any(), any()) }
    }

    @Test
    fun `respond with drill phase`() {
        val userId = UUID.randomUUID()
        val tutor = Tutor(
            name = "Maria",
            sourceLanguageCode = "en",
            targetLanguageCode = "es",
            persona = "patient",
            domain = "grammar"
        )
        val conversationState = ConversationState(phase = ConversationPhase.Drill, estimatedCEFRLevel = CEFRLevel.A1)
        val messages = listOf<Message>(UserMessage("Practice verbs"))

        val vocabContext = VocabularyContext(
            totalWordCount = 100,
            wordsForReinforcement = listOf("ser", "estar"),
            recentNewWords = listOf("tener", "hacer"),
            masteredWords = listOf("ir", "ver", "dar")
        )

        val conversationResponse = ConversationResponse(
            conversationState = conversationState,
            corrections = emptyList(),
            newVocabulary = emptyList()
        )

        val aiResponse = AiChatResponse(
            reply = "Let's practice ser and estar!",
            conversationResponse = conversationResponse
        )

        every { languageService.getLanguageName("en") } returns "English"
        every { languageService.getLanguageName("es") } returns "Spanish"
        every { vocabularyContextService.getVocabularyContext(userId, "es") } returns vocabContext
        every { aiChatService.call(any(), any()) } returns aiResponse

        val result = tutorService.respond(tutor, conversationState, userId, messages)

        assertNotNull(result)
        assertEquals("Let's practice ser and estar!", result?.reply)
        assertEquals(ConversationPhase.Drill, result?.conversationResponse?.conversationState?.phase)
        verify { aiChatService.call(any(), any()) }
    }

    @Test
    fun `respond handles null AI response`() {
        val userId = UUID.randomUUID()
        val tutor = Tutor(
            name = "Maria",
            sourceLanguageCode = "en",
            targetLanguageCode = "es",
            persona = "friendly",
            domain = "casual"
        )
        val conversationState = ConversationState(phase = ConversationPhase.Free, estimatedCEFRLevel = CEFRLevel.A1)
        val messages = listOf<Message>(UserMessage("Hello"))

        val vocabContext = VocabularyContext(
            totalWordCount = 0,
            wordsForReinforcement = emptyList(),
            recentNewWords = emptyList(),
            masteredWords = emptyList()
        )

        every { languageService.getLanguageName("en") } returns "English"
        every { languageService.getLanguageName("es") } returns "Spanish"
        every { vocabularyContextService.getVocabularyContext(userId, "es") } returns vocabContext
        every { aiChatService.call(any(), any()) } returns null

        val result = tutorService.respond(tutor, conversationState, userId, messages)

        assertNull(result)
        verify { aiChatService.call(any(), any()) }
    }

    @Test
    fun `buildVocabularyGuidance with no vocabulary`() {
        val userId = UUID.randomUUID()
        val tutor = Tutor(
            name = "Maria",
            sourceLanguageCode = "en",
            targetLanguageCode = "es"
        )
        val conversationState = ConversationState(phase = ConversationPhase.Free, estimatedCEFRLevel = CEFRLevel.A1)
        val messages = listOf<Message>(UserMessage("Hello"))

        val vocabContext = VocabularyContext(
            totalWordCount = 0,
            wordsForReinforcement = emptyList(),
            recentNewWords = emptyList(),
            masteredWords = emptyList()
        )

        val conversationResponse = ConversationResponse(
            conversationState = conversationState,
            corrections = emptyList(),
            newVocabulary = emptyList()
        )

        val aiResponse = AiChatResponse(
            reply = "Response",
            conversationResponse = conversationResponse
        )

        every { languageService.getLanguageName("en") } returns "English"
        every { languageService.getLanguageName("es") } returns "Spanish"
        every { vocabularyContextService.getVocabularyContext(userId, "es") } returns vocabContext
        every { aiChatService.call(any(), any()) } returns aiResponse

        tutorService.respond(tutor, conversationState, userId, messages)

        verify { vocabularyContextService.getVocabularyContext(userId, "es") }
    }

    @Test
    fun `onReplyChunk callback is invoked`() {
        val userId = UUID.randomUUID()
        val tutor = Tutor(
            name = "Maria",
            sourceLanguageCode = "en",
            targetLanguageCode = "es"
        )
        val conversationState = ConversationState(phase = ConversationPhase.Free, estimatedCEFRLevel = CEFRLevel.A1)
        val messages = listOf<Message>(UserMessage("Hello"))

        val vocabContext = VocabularyContext(
            totalWordCount = 0,
            wordsForReinforcement = emptyList(),
            recentNewWords = emptyList(),
            masteredWords = emptyList()
        )

        val conversationResponse = ConversationResponse(
            conversationState = conversationState,
            corrections = emptyList(),
            newVocabulary = emptyList()
        )

        val aiResponse = AiChatResponse(
            reply = "Response",
            conversationResponse = conversationResponse
        )

        every { languageService.getLanguageName("en") } returns "English"
        every { languageService.getLanguageName("es") } returns "Spanish"
        every { vocabularyContextService.getVocabularyContext(userId, "es") } returns vocabContext
        every { aiChatService.call(any(), any()) } answers {
            val callback = secondArg<(String) -> Unit>()
            callback("chunk1")
            callback("chunk2")
            aiResponse
        }

        val chunks = mutableListOf<String>()
        tutorService.respond(tutor, conversationState, userId, messages) { chunks.add(it) }

        assertEquals(listOf("chunk1", "chunk2"), chunks)
    }
}
