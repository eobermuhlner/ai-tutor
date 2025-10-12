package ch.obermuhlner.aitutor.chat.domain

import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp

@Entity
@Table(name = "chat_sessions")
class ChatSessionEntity(
    @Id
    @Column(nullable = false, updatable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "tutor_name", nullable = false, length = 128)
    var tutorName: String,

    @Column(name = "tutor_persona", nullable = false, length = 256)
    var tutorPersona: String = "patient coach",

    @Column(name = "tutor_domain", nullable = false, length = 256)
    var tutorDomain: String = "general conversation, grammar, typography",

    @Enumerated(EnumType.STRING)
    @Column(name = "tutor_teaching_style", nullable = false, length = 16)
    var tutorTeachingStyle: ch.obermuhlner.aitutor.tutor.domain.TeachingStyle = ch.obermuhlner.aitutor.tutor.domain.TeachingStyle.Reactive,

    @Column(name = "source_language_code", nullable = false, length = 32)
    var sourceLanguageCode: String,

    @Column(name = "target_language_code", nullable = false, length = 32)
    var targetLanguageCode: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "conversation_phase", nullable = false, length = 16)
    var conversationPhase: ConversationPhase = ConversationPhase.Free,

    @Enumerated(EnumType.STRING)
    @Column(name = "effective_phase", nullable = true, length = 16)
    var effectivePhase: ConversationPhase? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "estimated_cefr_level", nullable = false, length = 8)
    var estimatedCEFRLevel: CEFRLevel = CEFRLevel.A1,

    // NEW: Skill-specific CEFR levels (Task 0010)
    @Enumerated(EnumType.STRING)
    @Column(name = "cefr_grammar", length = 8)
    var cefrGrammar: CEFRLevel? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "cefr_vocabulary", length = 8)
    var cefrVocabulary: CEFRLevel? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "cefr_fluency", length = 8)
    var cefrFluency: CEFRLevel? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "cefr_comprehension", length = 8)
    var cefrComprehension: CEFRLevel? = null,

    // Assessment metadata
    @Column(name = "last_assessment_at")
    var lastAssessmentAt: Instant? = null,

    @Column(name = "total_assessment_count", nullable = false)
    var totalAssessmentCount: Int = 0,

    @Column(name = "last_llm_validation_at")
    var lastLLMValidationAt: Instant? = null,

    @Column(name = "llm_validation_count", nullable = false)
    var llmValidationCount: Int = 0,

    @Column(name = "current_topic", nullable = true, length = 128)
    var currentTopic: String? = null,

    @Column(name = "past_topics_json", nullable = true, columnDefinition = "TEXT")
    var pastTopicsJson: String? = null,

    // NEW: Course-related fields (nullable for backward compatibility)
    @Column(name = "course_template_id")
    var courseTemplateId: UUID? = null,

    @Column(name = "tutor_profile_id")
    var tutorProfileId: UUID? = null,

    @Column(name = "custom_session_name", length = 256)
    var customName: String? = null,

    @Column(name = "is_active")
    var isActive: Boolean = true,

    // Vocabulary review mode control (similar to conversationPhase)
    @Column(name = "vocabulary_review_mode", nullable = false)
    var vocabularyReviewMode: Boolean = false,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null
)
