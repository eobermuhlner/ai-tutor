package ch.obermuhlner.aitutor.chat.repository

import ch.obermuhlner.aitutor.chat.domain.ChatMessageEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ChatMessageRepository : JpaRepository<ChatMessageEntity, UUID> {
    fun findBySessionIdOrderByCreatedAtAsc(sessionId: UUID): List<ChatMessageEntity>
    fun countBySessionId(sessionId: UUID): Long

    /**
     * Find messages by session starting from a specific sequence number.
     */
    fun findBySessionIdAndSequenceNumberGreaterThanEqualOrderBySequenceNumberAsc(
        sessionId: UUID,
        startSequence: Int
    ): List<ChatMessageEntity>

    /**
     * Find all messages by session, ordered by sequence.
     */
    fun findBySessionIdOrderBySequenceNumberAsc(
        sessionId: UUID
    ): List<ChatMessageEntity>

    /**
     * Count messages in a session starting from a sequence number.
     */
    fun countBySessionIdAndSequenceNumberGreaterThanEqual(
        sessionId: UUID,
        startSequence: Int
    ): Long
}
