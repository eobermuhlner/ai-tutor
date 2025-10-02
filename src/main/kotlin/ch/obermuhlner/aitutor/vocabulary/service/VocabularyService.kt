package ch.obermuhlner.aitutor.vocabulary.service

import ch.obermuhlner.aitutor.vocabulary.domain.VocabularyContextEntity
import ch.obermuhlner.aitutor.vocabulary.domain.VocabularyItemEntity
import ch.obermuhlner.aitutor.vocabulary.dto.NewVocabularyDTO
import ch.obermuhlner.aitutor.vocabulary.repository.VocabularyContextRepository
import ch.obermuhlner.aitutor.vocabulary.repository.VocabularyItemRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.text.Normalizer
import java.time.Instant
import java.util.*

@Service
class VocabularyService(
    private val vocabularyItemRepository: VocabularyItemRepository,
    private val vocabularyContextRepository: VocabularyContextRepository
) {
    @Transactional
    fun addNewVocabulary(
        userId: UUID,
        lang: String,
        items: List<NewVocabularyDTO>,
        turnId: UUID? = null
    ): List<VocabularyItemEntity> {
        val now = Instant.now()
        val saved = mutableListOf<VocabularyItemEntity>()

        for (nv in items) {
            val lemmaNorm = normalizeLemma(nv.lemma)
            if (lemmaNorm.isBlank()) continue

            val item = vocabularyItemRepository.findByUserIdAndLangAndLemma(userId, lang, lemmaNorm)
                ?.apply {
                    exposures += 1
                    lastSeenAt = now
                }
                ?: VocabularyItemEntity(
                    userId = userId,
                    lang = lang,
                    lemma = lemmaNorm,
                    exposures = 1,
                    lastSeenAt = now
                )

            val persisted = vocabularyItemRepository.save(item)
            vocabularyContextRepository.save(
                VocabularyContextEntity(
                    vocabItem = persisted,
                    context = nv.context.take(512),
                    turnId = turnId
                )
            )
            saved += persisted
        }
        return saved
    }

    private fun normalizeLemma(raw: String): String {
        val nfc = Normalizer.normalize(raw, Normalizer.Form.NFC)
        return nfc.trim()
    }
}
