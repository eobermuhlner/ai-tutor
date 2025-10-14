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
    fun `switches to Drill with severity score greater or equal to 4_5`() {
        val messages = listOf(
            createUserMessage("msg1"),
            createAssistantMessage("resp1", listOf(
                createCorrection(ErrorSeverity.Medium), // 1.0
                createCorrection(ErrorSeverity.Medium)  // 1.0
            )),
            createUserMessage("msg2"),
            createAssistantMessage("resp2", listOf(
                createCorrection(ErrorSeverity.Medium)  // 1.0
            )),
            createUserMessage("msg3"),
            createAssistantMessage("resp3", listOf(
                createCorrection(ErrorSeverity.Medium), // 1.0
                createCorrection(ErrorSeverity.Medium)  // 1.0
            ))
        )

        // Total base score: 5.0, but without fossilization should still trigger
        val decision = phaseDecisionService.decidePhase(ConversationPhase.Auto, messages)
        assertEquals(ConversationPhase.Drill, decision.phase)
    }

    @Test
    fun `switches to Free with only low-severity errors in last 3 and score less than 1_0`() {
        val messages = listOf(
            createUserMessage("msg1"),
            createAssistantMessage("resp1", listOf(
                createCorrection(ErrorSeverity.Low, ErrorType.Typography) // 0.3
            )),
            createUserMessage("msg2"),
            createAssistantMessage("resp2", listOf(
                createCorrection(ErrorSeverity.Low, ErrorType.Prepositions) // 0.3
            )),
            createUserMessage("msg3"),
            createAssistantMessage("resp3", listOf(
                createCorrection(ErrorSeverity.Low, ErrorType.Articles) // 0.3
            ))
        )

        // Total: 0.9 < 1.0, different error types (no fossilization), should switch to Free
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

    // ======= Fossilization Detection Tests =======

    @Test
    fun `detects fossilization with repeated Agreement errors`() {
        val messages = listOf(
            createUserMessage("yo es Alex"),
            createAssistantMessage("Hola", listOf(
                createCorrection(ErrorSeverity.Medium, ErrorType.Agreement)
            )),
            createUserMessage("yo vive en Boston"),
            createAssistantMessage("¡Bien!", listOf(
                createCorrection(ErrorSeverity.Medium, ErrorType.Agreement)
            )),
            createUserMessage("yo tiene un gato"),
            createAssistantMessage("Interesante", listOf(
                createCorrection(ErrorSeverity.Medium, ErrorType.Agreement)
            )),
            createUserMessage("yo trabaja"),
            createAssistantMessage("Ya veo", listOf(
                createCorrection(ErrorSeverity.Medium, ErrorType.Agreement)
            ))
        )

        val decision = phaseDecisionService.decidePhase(ConversationPhase.Auto, messages)

        // With fossilization escalation:
        // 1st Agreement: 1.0 * 1.0 = 1.0
        // 2nd Agreement: 1.0 * 1.5 = 1.5
        // 3rd Agreement: 1.0 * 2.0 = 2.0
        // 4th Agreement: 1.0 * 2.0 = 2.0
        // Total: 6.5 >= 4.5, should trigger Drill
        assertEquals(ConversationPhase.Drill, decision.phase)
        assertEquals(true, decision.fossilizationDetected)
        assertEquals(listOf("Agreement"), decision.repeatedErrorTypes)
    }

    @Test
    fun `escalates severity score for repeated errors`() {
        val messages = listOf(
            createUserMessage("msg1"),
            createAssistantMessage("resp1", listOf(
                createCorrection(ErrorSeverity.Medium, ErrorType.Agreement) // 1.0 * 1.0 = 1.0
            )),
            createUserMessage("msg2"),
            createAssistantMessage("resp2", listOf(
                createCorrection(ErrorSeverity.Medium, ErrorType.Agreement) // 1.0 * 1.5 = 1.5
            )),
            createUserMessage("msg3"),
            createAssistantMessage("resp3", listOf(
                createCorrection(ErrorSeverity.Medium, ErrorType.Agreement) // 1.0 * 2.0 = 2.0
            ))
        )

        val decision = phaseDecisionService.decidePhase(ConversationPhase.Auto, messages)

        // Total: 1.0 + 1.5 + 2.0 = 4.5 >= 4.5, should trigger Drill
        assertEquals(ConversationPhase.Drill, decision.phase)
        assertEquals(true, decision.fossilizationDetected)
    }

    @Test
    fun `does not trigger Drill without fossilization below threshold`() {
        val messages = listOf(
            createUserMessage("msg1"),
            createAssistantMessage("resp1", listOf(
                createCorrection(ErrorSeverity.Medium, ErrorType.Agreement) // 1.0
            )),
            createUserMessage("msg2"),
            createAssistantMessage("resp2", listOf(
                createCorrection(ErrorSeverity.Medium, ErrorType.Lexis) // 1.0
            )),
            createUserMessage("msg3"),
            createAssistantMessage("resp3", listOf(
                createCorrection(ErrorSeverity.Medium, ErrorType.Prepositions) // 1.0
            ))
        )

        val decision = phaseDecisionService.decidePhase(ConversationPhase.Auto, messages)

        // Total: 3.0 < 4.5, different error types, should stay in Correction
        assertEquals(ConversationPhase.Correction, decision.phase)
        assertEquals(false, decision.fossilizationDetected)
    }

    @Test
    fun `tracks multiple fossilized error types`() {
        val messages = listOf(
            createUserMessage("msg1"),
            createAssistantMessage("resp1", listOf(
                createCorrection(ErrorSeverity.Medium, ErrorType.Agreement)
            )),
            createUserMessage("msg2"),
            createAssistantMessage("resp2", listOf(
                createCorrection(ErrorSeverity.Medium, ErrorType.Agreement),
                createCorrection(ErrorSeverity.Medium, ErrorType.Articles)
            )),
            createUserMessage("msg3"),
            createAssistantMessage("resp3", listOf(
                createCorrection(ErrorSeverity.Medium, ErrorType.Articles)
            ))
        )

        val decision = phaseDecisionService.decidePhase(ConversationPhase.Auto, messages)

        // Agreement: 1.0 + 1.5 = 2.5
        // Articles: 1.0 + 1.5 = 2.5
        // Total: 5.0 >= 4.5, should trigger Drill
        assertEquals(ConversationPhase.Drill, decision.phase)
        assertEquals(true, decision.fossilizationDetected)
        assertEquals(2, decision.repeatedErrorTypes.size)
    }
}
