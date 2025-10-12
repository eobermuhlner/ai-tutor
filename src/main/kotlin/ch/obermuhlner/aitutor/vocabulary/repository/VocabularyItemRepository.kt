package ch.obermuhlner.aitutor.vocabulary.repository

import ch.obermuhlner.aitutor.vocabulary.domain.VocabularyItemEntity
import java.time.Instant
import java.util.UUID
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface VocabularyItemRepository : JpaRepository<VocabularyItemEntity, UUID> {
    fun findByUserIdAndLangAndLemma(userId: UUID, lang: String, lemma: String): VocabularyItemEntity?
    fun findByUserId(userId: UUID): List<VocabularyItemEntity>
    fun findByUserIdAndLang(userId: UUID, lang: String): List<VocabularyItemEntity>
    fun findByUserIdOrderByLastSeenAtDesc(userId: UUID): List<VocabularyItemEntity>
    fun findByUserIdAndLangOrderByLastSeenAtDesc(userId: UUID, lang: String): List<VocabularyItemEntity>
    fun countByUserIdAndLang(userId: UUID, lang: String): Long

    // SRS (Spaced Repetition System) queries
    @Query("""
        SELECT v FROM VocabularyItemEntity v
        WHERE v.userId = :userId
          AND v.lang = :lang
          AND v.nextReviewAt <= :now
        ORDER BY v.nextReviewAt ASC
    """)
    fun findDueForReview(
        userId: UUID,
        lang: String,
        now: Instant,
        pageable: Pageable
    ): List<VocabularyItemEntity>

    @Query("""
        SELECT COUNT(v) FROM VocabularyItemEntity v
        WHERE v.userId = :userId
          AND v.lang = :lang
          AND v.nextReviewAt <= :now
    """)
    fun countDueForReview(
        userId: UUID,
        lang: String,
        now: Instant
    ): Long
}
