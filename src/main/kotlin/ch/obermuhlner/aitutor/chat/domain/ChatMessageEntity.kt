package ch.obermuhlner.aitutor.chat.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID
import org.hibernate.annotations.CreationTimestamp

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

    @Column(name = "word_cards_json", columnDefinition = "TEXT")
    val wordCardsJson: String? = null,

    @Column(name = "sequence_number", nullable = false)
    var sequenceNumber: Int = 0,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant? = null
)
