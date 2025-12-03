package ch.obermuhlner.aitutor.tutor.service.ai

import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import ch.obermuhlner.aitutor.tutor.domain.ConversationState
import ch.obermuhlner.aitutor.tutor.domain.TeachingStyle
import ch.obermuhlner.aitutor.tutor.domain.Tutor
import ch.obermuhlner.aitutor.tutor.service.TutorService
import ch.obermuhlner.aitutor.vocabulary.service.VocabularyContext
import ch.obermuhlner.aitutor.vocabulary.service.VocabularyContextService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import java.util.UUID
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.ai.chat.messages.UserMessage
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
@SpringBootTest
@ActiveProfiles("dev", "h2-mem", "ai-openai", "prompts-large")
@EnabledIfEnvironmentVariable(named = "SPRING_AI_OPENAI_API_KEY", matches = ".+")
class OpenAiTutorServiceIntegrationTest {

    @Autowired
    private lateinit var tutorService: TutorService

    @MockkBean
    private lateinit var vocabularyContextService: VocabularyContextService

    private val testUserId = UUID.randomUUID()

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
    }

    @Test
    fun `respond generates valid response`() {
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
            currentTopic = "greetings",
            phaseReason = "Starting conversation",
            topicEligibilityStatus = "Active conversation"
        )

        val messages = listOf(UserMessage("Hola, ¿cómo estás?"))

        // When
        val result = tutorService.respond(
            tutor = tutor,
            conversationState = conversationState,
            userId = testUserId,
            messages = messages
        )

        // Then
        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result!!.reply).isNotEmpty()
        Assertions.assertThat(result.conversationResponse).isNotNull
        Assertions.assertThat(result.conversationResponse.newVocabulary).isNotNull
        Assertions.assertThat(result.conversationResponse.wordCards).isNotNull
        Assertions.assertThat(result.conversationResponse.characterCards).isNotNull

        println("AI Response: ${result.reply}")
        println("New Vocabulary: ${result.conversationResponse.newVocabulary}")
        println("Word Cards: ${result.conversationResponse.wordCards}")
    }
}