package ch.obermuhlner.aitutor.chat.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID
import org.hibernate.annotations.CreationTimestamp

@Entity
@Table(
    name = "message_summaries",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_summary_range",
            columnNames = ["session_id", "summary_level", "start_sequence", "end_sequence"]
        )
    ],
    indexes = [
        Index(name = "idx_summaries_session_level", columnList = "session_id, summary_level"),
        Index(name = "idx_summaries_session_active", columnList = "session_id, is_active"),
        Index(name = "idx_summaries_end_seq", columnList = "session_id, end_sequence")
    ]
)
class MessageSummaryEntity(
    @Id
    @Column(nullable = false, updatable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "session_id", nullable = false)
    val sessionId: UUID,

    @Column(name = "summary_level", nullable = false)
    val summaryLevel: Int,

    @Column(name = "start_sequence", nullable = false)
    val startSequence: Int,

    @Column(name = "end_sequence", nullable = false)
    val endSequence: Int,

    @Column(name = "summary_text", nullable = false, columnDefinition = "TEXT")
    val summaryText: String,

    @Column(name = "token_count", nullable = false)
    val tokenCount: Int,

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 16)
    val sourceType: SummarySourceType,

    @Column(name = "source_ids_json", nullable = false, columnDefinition = "TEXT")
    val sourceIdsJson: String,

    @Column(name = "superseded_by_id")
    var supersededById: UUID? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant? = null
)

enum class SummarySourceType {
    MESSAGE,  // Level-1 summaries (from raw messages)
    SUMMARY   // Level-2+ summaries (from lower-level summaries)
}
