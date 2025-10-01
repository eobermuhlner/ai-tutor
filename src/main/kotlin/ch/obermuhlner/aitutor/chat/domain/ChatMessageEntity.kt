package ch.obermuhlner.aitutor.chat.domain

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant
import java.util.UUID

enum class MessageRole {
    USER,
    ASSISTANT
}

@Entity
@Table(name = "chat_messages")
class ChatMessageEntity(
    @Id
    @Column(nullable = false, updatable = false)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    val session: ChatSessionEntity,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    val role: MessageRole,

    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,

    @Column(name = "corrections_json", columnDefinition = "TEXT")
    val correctionsJson: String? = null,

    @Column(name = "vocabulary_json", columnDefinition = "TEXT")
    val vocabularyJson: String? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant? = null
)
