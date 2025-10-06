package ch.obermuhlner.aitutor.tutor.service

import ch.obermuhlner.aitutor.chat.domain.ChatMessageEntity
import ch.obermuhlner.aitutor.chat.domain.ChatSessionEntity
import ch.obermuhlner.aitutor.chat.domain.MessageRole
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.Correction
import ch.obermuhlner.aitutor.core.model.ErrorSeverity
import ch.obermuhlner.aitutor.core.model.ErrorType
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PhaseDecisionServiceTest {

    private lateinit var objectMapper: ObjectMapper
    private lateinit var phaseDecisionService: PhaseDecisionService
    private lateinit var testSession: ChatSessionEntity

    @BeforeEach
    fun setup() {
        objectMapper = jacksonObjectMapper()
        phaseDecisionService = PhaseDecisionService(objectMapper)
        testSession = ChatSessionEntity(
            id = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            tutorName = "Maria",
            sourceLanguageCode = "en",
            targetLanguageCode = "es",
            conversationPhase = ConversationPhase.Auto,
            estimatedCEFRLevel = CEFRLevel.A1
        )
    }

    @Test
    fun `returns current phase if not Auto`() {
        val messages = emptyList<ChatMessageEntity>()

        assertEquals(ConversationPhase.Free, phaseDecisionService.decidePhase(ConversationPhase.Free, messages).phase)
        assertEquals(ConversationPhase.Correction, phaseDecisionService.decidePhase(ConversationPhase.Correction, messages).phase)
        assertEquals(ConversationPhase.Drill, phaseDecisionService.decidePhase(ConversationPhase.Drill, messages).phase)
    }

    @Test
    fun `starts with Correction phase when less than 3 user messages`() {
        val messages = listOf(
            createUserMessage("Hello"),
            createAssistantMessage("Hi!", emptyList())
        )

        val decision = phaseDecisionService.decidePhase(ConversationPhase.Auto, messages)
        assertEquals(ConversationPhase.Correction, decision.phase)
    }

    @Test
    fun `switches to Drill with 2+ critical errors in last 3 messages`() {
        val messages = listOf(
            createUserMessage("msg1"),
            createAssistantMessage("resp1", listOf(
                createCorrection(ErrorSeverity.Critical)
            )),
            createUserMessage("msg2"),
            createAssistantMessage("resp2", listOf(
                createCorrection(ErrorSeverity.Critical)
            )),
            createUserMessage("msg3"),
            createAssistantMessage("resp3", emptyList())
        )

        val decision = phaseDecisionService.decidePhase(ConversationPhase.Auto, messages)
        assertEquals(ConversationPhase.Drill, decision.phase)
    }

    @Test
    fun `switches to Drill with 3+ repeated high-severity errors`() {
        val messages = listOf(
            createUserMessage("msg1"),
            createAssistantMessage("resp1", listOf(
                createCorrection(ErrorSeverity.High, ErrorType.TenseAspect)
            )),
            createUserMessage("msg2"),
            createAssistantMessage("resp2", listOf(
                createCorrection(ErrorSeverity.High, ErrorType.TenseAspect)
            )),
            createUserMessage("msg3"),
            createAssistantMessage("resp3", listOf(
                createCorrection(ErrorSeverity.High, ErrorType.TenseAspect)
            ))
        )

        val decision = phaseDecisionService.decidePhase(ConversationPhase.Auto, messages)
        assertEquals(ConversationPhase.Drill, decision.phase)
    }

    @Test
    fun `switches to Drill with severity score greater or equal to 6_0`() {
        val messages = listOf(
            createUserMessage("msg1"),
            createAssistantMessage("resp1", listOf(
                createCorrection(ErrorSeverity.Critical), // 3.0
                createCorrection(ErrorSeverity.High)       // 2.0
            )),
            createUserMessage("msg2"),
            createAssistantMessage("resp2", listOf(
                createCorrection(ErrorSeverity.Medium)     // 1.0
            )),
            createUserMessage("msg3"),
            createAssistantMessage("resp3", emptyList())
        )

        val decision = phaseDecisionService.decidePhase(ConversationPhase.Auto, messages)
        assertEquals(ConversationPhase.Drill, decision.phase)
    }

    @Test
    fun `switches to Free with only low-severity errors in last 3 and score less than 1_0`() {
        val messages = listOf(
            createUserMessage("msg1"),
            createAssistantMessage("resp1", listOf(
                createCorrection(ErrorSeverity.Low) // 0.3
            )),
            createUserMessage("msg2"),
            createAssistantMessage("resp2", listOf(
                createCorrection(ErrorSeverity.Low) // 0.3
            )),
            createUserMessage("msg3"),
            createAssistantMessage("resp3", listOf(
                createCorrection(ErrorSeverity.Low) // 0.3
            ))
        )

        val decision = phaseDecisionService.decidePhase(ConversationPhase.Auto, messages)
        assertEquals(ConversationPhase.Free, decision.phase)
    }

    @Test
    fun `returns Correction by default with mixed errors`() {
        val messages = listOf(
            createUserMessage("msg1"),
            createAssistantMessage("resp1", listOf(
                createCorrection(ErrorSeverity.Medium)
            )),
            createUserMessage("msg2"),
            createAssistantMessage("resp2", listOf(
                createCorrection(ErrorSeverity.Low)
            )),
            createUserMessage("msg3"),
            createAssistantMessage("resp3", emptyList())
        )

        val decision = phaseDecisionService.decidePhase(ConversationPhase.Auto, messages)
        assertEquals(ConversationPhase.Correction, decision.phase)
    }

    @Test
    fun `returns Free with no errors`() {
        val messages = listOf(
            createUserMessage("msg1"),
            createAssistantMessage("resp1", emptyList()),
            createUserMessage("msg2"),
            createAssistantMessage("resp2", emptyList()),
            createUserMessage("msg3"),
            createAssistantMessage("resp3", emptyList())
        )

        // With no errors at all (score = 0, onlyLowSeverity = true), should return Free
        val decision = phaseDecisionService.decidePhase(ConversationPhase.Auto, messages)
        assertEquals(ConversationPhase.Free, decision.phase)
    }

    @Test
    fun `handles messages without assistant responses`() {
        val messages = listOf(
            createUserMessage("msg1"),
            createUserMessage("msg2"),
            createUserMessage("msg3")
        )

        // With no assistant responses, no corrections can be found, so score = 0 and should return Free
        val decision = phaseDecisionService.decidePhase(ConversationPhase.Auto, messages)
        assertEquals(ConversationPhase.Free, decision.phase)
    }

    @Test
    fun `handles invalid corrections JSON gracefully`() {
        val userMessage = createUserMessage("test")
        val assistantMessage = ChatMessageEntity(
            session = testSession,
            role = MessageRole.ASSISTANT,
            content = "response",
            correctionsJson = "invalid json"
        )

        val messages = listOf(userMessage, assistantMessage)

        // Should not throw, treats as no corrections
        val decision = phaseDecisionService.decidePhase(ConversationPhase.Auto, messages)
        assertEquals(ConversationPhase.Correction, decision.phase)
    }

    @Test
    fun `calculates severity score correctly`() {
        val messages = listOf(
            createUserMessage("msg1"),
            createAssistantMessage("resp1", listOf(
                createCorrection(ErrorSeverity.Critical), // 3.0
                createCorrection(ErrorSeverity.High),      // 2.0
                createCorrection(ErrorSeverity.Medium),    // 1.0
                createCorrection(ErrorSeverity.Low)        // 0.3
            )),
            createUserMessage("msg2"),
            createAssistantMessage("resp2", emptyList()),
            createUserMessage("msg3"),
            createAssistantMessage("resp3", emptyList())
        )

        // Total score: 3.0 + 2.0 + 1.0 + 0.3 = 6.3, should trigger Drill
        val decision = phaseDecisionService.decidePhase(ConversationPhase.Auto, messages)
        assertEquals(ConversationPhase.Drill, decision.phase)
    }

    private fun createUserMessage(content: String): ChatMessageEntity {
        return ChatMessageEntity(
            session = testSession,
            role = MessageRole.USER,
            content = content
        )
    }

    private fun createAssistantMessage(content: String, corrections: List<Correction>): ChatMessageEntity {
        val correctionsJson = if (corrections.isNotEmpty()) {
            objectMapper.writeValueAsString(corrections)
        } else {
            null
        }

        return ChatMessageEntity(
            session = testSession,
            role = MessageRole.ASSISTANT,
            content = content,
            correctionsJson = correctionsJson
        )
    }

    private fun createCorrection(
        severity: ErrorSeverity,
        errorType: ErrorType = ErrorType.TenseAspect
    ): Correction {
        return Correction(
            span = "original",
            errorType = errorType,
            severity = severity,
            correctedTargetLanguage = "corrected",
            whySourceLanguage = "explanation",
            whyTargetLanguage = "explicación"
        )
    }
}
