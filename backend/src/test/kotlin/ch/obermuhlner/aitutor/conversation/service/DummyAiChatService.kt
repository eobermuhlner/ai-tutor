package ch.obermuhlner.aitutor.conversation.service

import ch.obermuhlner.aitutor.conversation.dto.AiChatRequest
import ch.obermuhlner.aitutor.conversation.dto.AiChatResponse
import ch.obermuhlner.aitutor.core.model.Correction
import ch.obermuhlner.aitutor.core.model.ErrorSeverity
import ch.obermuhlner.aitutor.core.model.ErrorType
import ch.obermuhlner.aitutor.core.model.NewVocabulary
import ch.obermuhlner.aitutor.tutor.domain.ConversationResponse
import ch.obermuhlner.aitutor.tutor.domain.ConversationState
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("test")
class DummyAiChatService : AiChatService {

    var responseToReturn: AiChatResponse? = null
    var lastRequest: AiChatRequest? = null

    override fun call(request: AiChatRequest, onReplyText: (String) -> Unit): AiChatResponse? {
        lastRequest = request

        // Simulate streaming response
        val replyText = responseToReturn?.reply ?: "Test response from AI tutor"
        onReplyText(replyText)

        return responseToReturn ?: createDefaultResponse(replyText)
    }

    fun reset() {
        responseToReturn = null
        lastRequest = null
    }

    private fun createDefaultResponse(reply: String) = AiChatResponse(
        reply = reply,
        conversationResponse = ConversationResponse(
            conversationState = ConversationState(
                phase = ch.obermuhlner.aitutor.tutor.domain.ConversationPhase.Correction,
                estimatedCEFRLevel = ch.obermuhlner.aitutor.core.model.CEFRLevel.A1,
                currentTopic = "greetings"
            ),
            corrections = listOf(
                Correction(
                    span = "helo",
                    correctedTargetLanguage = "hello",
                    whySourceLanguage = "Spelling error",
                    whyTargetLanguage = "Error de ortografía",
                    errorType = ErrorType.Typography,
                    severity = ErrorSeverity.Low
                )
            ),
            newVocabulary = listOf(
                NewVocabulary(
                    lemma = "hello",
                    context = "greeting"
                )
            )
        )
    )
}
