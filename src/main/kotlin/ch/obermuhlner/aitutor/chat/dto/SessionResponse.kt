package ch.obermuhlner.aitutor.chat.dto

import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import java.time.Instant
import java.util.UUID

data class SessionResponse(
    val id: UUID,
    val userId: UUID,
    val tutorName: String,
    val tutorPersona: String,
    val tutorDomain: String,
    val sourceLanguageCode: String,
    val targetLanguageCode: String,
    val conversationPhase: ConversationPhase,
    val effectivePhase: ConversationPhase,
    val estimatedCEFRLevel: CEFRLevel,
    val currentTopic: String? = null,
    val courseTemplateId: UUID? = null,
    val tutorProfileId: UUID? = null,
    val customName: String? = null,
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant
)
