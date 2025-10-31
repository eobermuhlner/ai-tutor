package ch.obermuhlner.aitutor.analytics.repository

import ch.obermuhlner.aitutor.analytics.domain.ErrorPatternEntity
import ch.obermuhlner.aitutor.core.model.ErrorType
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ErrorPatternRepository : JpaRepository<ErrorPatternEntity, UUID> {

    fun findByUserIdAndLang(userId: UUID, lang: String): List<ErrorPatternEntity>

    fun findByUserIdAndLangAndErrorType(
        userId: UUID,
        lang: String,
        errorType: ErrorType
    ): ErrorPatternEntity?

    @Query("""
        SELECT e FROM ErrorPatternEntity e
        WHERE e.userId = :userId AND e.lang = :lang
        ORDER BY (e.criticalCount * 3.0 + e.highCount * 2.0 + e.mediumCount * 1.0 + e.lowCount * 0.3) DESC
    """)
    fun findTopPatternsByWeightedScore(userId: UUID, lang: String): List<ErrorPatternEntity>
}
