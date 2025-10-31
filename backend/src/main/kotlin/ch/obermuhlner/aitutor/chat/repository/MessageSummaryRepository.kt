package ch.obermuhlner.aitutor.chat.repository

import ch.obermuhlner.aitutor.chat.domain.MessageSummaryEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface MessageSummaryRepository : JpaRepository<MessageSummaryEntity, UUID> {
    /**
     * Get all active summaries at a specific level for a session, ordered by sequence.
     */
    fun findBySessionIdAndSummaryLevelAndIsActiveTrueOrderByStartSequenceAsc(
        sessionId: UUID,
        summaryLevel: Int
    ): List<MessageSummaryEntity>

    /**
     * Get all active summaries for a session, ordered by level then sequence.
     */
    fun findBySessionIdAndIsActiveTrueOrderBySummaryLevelAscStartSequenceAsc(
        sessionId: UUID
    ): List<MessageSummaryEntity>

    /**
     * Get all summaries for a session (including inactive), ordered by level then sequence.
     */
    fun findBySessionIdOrderBySummaryLevelAscStartSequenceAsc(
        sessionId: UUID
    ): List<MessageSummaryEntity>

    /**
     * Get the most recent summary (highest end_sequence) for a session at level 1.
     */
    fun findTopBySessionIdAndSummaryLevelAndIsActiveTrueOrderByEndSequenceDesc(
        sessionId: UUID,
        summaryLevel: Int
    ): MessageSummaryEntity?

    /**
     * Mark summaries as superseded.
     */
    @Modifying
    @Query("UPDATE MessageSummaryEntity m SET m.isActive = false, m.supersededById = :supersededById WHERE m.id IN :ids")
    fun markAsSuperseded(ids: List<UUID>, supersededById: UUID)
}
