package ch.obermuhlner.aitutor.chat.domain

import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.util.UUID

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

    @Column(name = "source_language_code", nullable = false, length = 32)
    var sourceLanguageCode: String,

    @Column(name = "target_language_code", nullable = false, length = 32)
    var targetLanguageCode: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "conversation_phase", nullable = false, length = 16)
    var conversationPhase: ConversationPhase = ConversationPhase.Free,

    @Enumerated(EnumType.STRING)
    @Column(name = "estimated_cefr_level", nullable = false, length = 8)
    var estimatedCEFRLevel: CEFRLevel = CEFRLevel.A1,

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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null
)
