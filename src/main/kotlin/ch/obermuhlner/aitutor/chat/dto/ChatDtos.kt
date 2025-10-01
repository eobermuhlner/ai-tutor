package ch.obermuhlner.aitutor.chat.dto

import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.ConversationPhase
import ch.obermuhlner.aitutor.core.model.Correction
import ch.obermuhlner.aitutor.core.model.NewVocabulary
import java.time.Instant
import java.util.UUID

data class CreateSessionRequest(
    val userId: UUID,
    val tutorName: String,
    val tutorPersona: String = "patient coach",
    val tutorDomain: String = "general conversation, grammar, typography",
    val sourceLanguageCode: String,
    val targetLanguageCode: String,
    val conversationPhase: ConversationPhase = ConversationPhase.Auto,
    val estimatedCEFRLevel: CEFRLevel = CEFRLevel.A1
)

data class SessionResponse(
    val id: UUID,
    val userId: UUID,
    val tutorName: String,
    val tutorPersona: String,
    val tutorDomain: String,
    val sourceLanguageCode: String,
    val targetLanguageCode: String,
    val conversationPhase: ConversationPhase,
    val estimatedCEFRLevel: CEFRLevel,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class SendMessageRequest(
    val content: String
)

data class MessageResponse(
    val id: UUID,
    val role: String,
    val content: String,
    val corrections: List<Correction>?,
    val newVocabulary: List<NewVocabulary>?,
    val createdAt: Instant
)

data class SessionWithMessagesResponse(
    val session: SessionResponse,
    val messages: List<MessageResponse>
)

data class UpdatePhaseRequest(
    val phase: ConversationPhase
)
