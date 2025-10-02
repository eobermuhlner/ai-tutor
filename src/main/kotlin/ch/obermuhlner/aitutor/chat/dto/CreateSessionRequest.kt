package ch.obermuhlner.aitutor.chat.dto

import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
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
