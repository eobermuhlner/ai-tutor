package ch.obermuhlner.aitutor.vocabulary.service

import ch.obermuhlner.aitutor.vocabulary.domain.VocabularyItemEntity
import ch.obermuhlner.aitutor.vocabulary.repository.VocabularyItemRepository
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.math.min
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class VocabularyReviewService(
    private val vocabularyItemRepository: VocabularyItemRepository
) {
    private val intervals = listOf(1, 3, 7, 14, 30, 90) // days

    /**
     * Get due vocabulary for a user in a specific language.
     * Returns items where nextReviewAt <= now, ordered by nextReviewAt ASC.
     */
    fun getDueVocabulary(
        userId: UUID,
        lang: String,
        limit: Int = 20
    ): List<VocabularyItemEntity> {
        val now = Instant.now()
        return vocabularyItemRepository.findDueForReview(userId, lang, now, PageRequest.of(0, limit))
    }

    /**
     * Get count of due vocabulary items for a user.
     */
    fun getDueCount(userId: UUID, lang: String): Long {
        val now = Instant.now()
        return vocabularyItemRepository.countDueForReview(userId, lang, now)
    }

    /**
     * Mark vocabulary as reviewed and schedule next review.
     * @param success true if user remembered, false if forgot
     */
    @Transactional
    fun recordReview(
        itemId: UUID,
        success: Boolean
    ): VocabularyItemEntity {
        val item = vocabularyItemRepository.findById(itemId)
            .orElseThrow { IllegalArgumentException("Vocabulary item not found: $itemId") }

        if (success) {
            // Advance to next stage
            item.reviewStage = min(item.reviewStage + 1, intervals.size - 1)
        } else {
            // Reset to beginning
            item.reviewStage = 0
        }

        // Schedule next review
        val daysToAdd = intervals[item.reviewStage].toLong()
        item.nextReviewAt = Instant.now().plus(daysToAdd, ChronoUnit.DAYS)

        return vocabularyItemRepository.save(item)
    }

    /**
     * Schedule initial review for newly added vocabulary.
     * Called by VocabularyService after creating/updating item.
     * Note: Caller must persist the entity after calling this method.
     */
    fun scheduleInitialReview(item: VocabularyItemEntity) {
        if (item.nextReviewAt == null) {
            // First review in 1 day
            item.nextReviewAt = Instant.now().plus(1, ChronoUnit.DAYS)
            item.reviewStage = 0
        }
    }
}
