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
    val tutorTeachingStyle: ch.obermuhlner.aitutor.tutor.domain.TeachingStyle,
    val sourceLanguageCode: String,
    val targetLanguageCode: String,
    val conversationPhase: ConversationPhase,
    val effectivePhase: ConversationPhase,
    val estimatedCEFRLevel: CEFRLevel,
    val currentTopic: String? = null,
    // NEW: Skill-specific CEFR levels (Task 0010, nullable for backward compatibility)
    val cefrGrammar: CEFRLevel? = null,
    val cefrVocabulary: CEFRLevel? = null,
    val cefrFluency: CEFRLevel? = null,
    val cefrComprehension: CEFRLevel? = null,
    val lastAssessmentAt: Instant? = null,
    val courseTemplateId: UUID? = null,
    val tutorProfileId: UUID? = null,
    val customName: String? = null,
    val isActive: Boolean = true,
    val vocabularyReviewMode: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant
)
