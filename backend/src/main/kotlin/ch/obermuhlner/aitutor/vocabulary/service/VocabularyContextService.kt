package ch.obermuhlner.aitutor.vocabulary.service

import ch.obermuhlner.aitutor.vocabulary.repository.VocabularyItemRepository
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import org.springframework.stereotype.Service

data class VocabularyContext(
    val wordsForReinforcement: List<String>,
    val masteredWords: List<String>,
    val recentNewWords: List<String>,
    val totalWordCount: Int
)

@Service
class VocabularyContextService(
    private val vocabularyItemRepository: VocabularyItemRepository
) {

    fun getVocabularyContext(userId: UUID, targetLang: String): VocabularyContext {
        val allVocab = vocabularyItemRepository.findByUserIdAndLangOrderByLastSeenAtDesc(userId, targetLang)

        if (allVocab.isEmpty()) {
            return VocabularyContext(
                wordsForReinforcement = emptyList(),
                masteredWords = emptyList(),
                recentNewWords = emptyList(),
                totalWordCount = 0
            )
        }

        val now = Instant.now()
        val twoDaysAgo = now.minus(2, ChronoUnit.DAYS)

        // Words seen 2-4 times and not in last 2 days (need reinforcement)
        // Sort by lastSeenAt ASC - oldest first (spaced repetition: reinforce what hasn't been seen longest)
        val wordsForReinforcement = allVocab
            .filter { it.exposures in 2..4 && it.lastSeenAt.isBefore(twoDaysAgo) }
            .sortedBy { it.lastSeenAt }
            .take(10)
            .map { it.lemma }

        // Words seen 5+ times (mastered, don't over-explain)
        val masteredWords = allVocab
            .filter { it.exposures >= 5 }
            .take(30)
            .map { it.lemma }

        // Words introduced in last 2 days (don't overwhelm with new vocabulary)
        val recentNewWords = allVocab
            .filter { it.lastSeenAt.isAfter(twoDaysAgo) }
            .take(10)
            .map { it.lemma }

        return VocabularyContext(
            wordsForReinforcement = wordsForReinforcement,
            masteredWords = masteredWords,
            recentNewWords = recentNewWords,
            totalWordCount = allVocab.size
        )
    }
}
