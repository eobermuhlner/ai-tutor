package ch.obermuhlner.aitutor.analytics.repository

import ch.obermuhlner.aitutor.analytics.domain.RecentErrorSampleEntity
import ch.obermuhlner.aitutor.core.model.ErrorType
import java.util.UUID
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface RecentErrorSampleRepository : JpaRepository<RecentErrorSampleEntity, UUID> {

    fun findByUserIdAndErrorTypeOrderByOccurredAtDesc(
        userId: UUID,
        errorType: ErrorType,
        pageable: Pageable
    ): List<RecentErrorSampleEntity>

    fun findByUserIdOrderByOccurredAtDesc(
        userId: UUID,
        pageable: Pageable
    ): List<RecentErrorSampleEntity>

    @Query("""
        SELECT COUNT(e) FROM RecentErrorSampleEntity e
        WHERE e.userId = :userId
    """)
    fun countByUserId(userId: UUID): Long

    @Modifying
    @Query("""
        DELETE FROM RecentErrorSampleEntity e
        WHERE e.userId = :userId
          AND e.id IN (
              SELECT e2.id FROM RecentErrorSampleEntity e2
              WHERE e2.userId = :userId
              ORDER BY e2.occurredAt ASC
          )
    """)
    fun pruneOldestSamples(userId: UUID, excessCount: Int): Int
}
