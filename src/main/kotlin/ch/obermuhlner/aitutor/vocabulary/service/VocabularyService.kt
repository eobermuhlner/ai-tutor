package ch.obermuhlner.aitutor.vocabulary.service

import ch.obermuhlner.aitutor.vocabulary.domain.VocabularyContextEntity
import ch.obermuhlner.aitutor.vocabulary.domain.VocabularyItemEntity
import ch.obermuhlner.aitutor.vocabulary.dto.NewVocabularyDTO
import ch.obermuhlner.aitutor.vocabulary.repository.VocabularyContextRepository
import ch.obermuhlner.aitutor.vocabulary.repository.VocabularyItemRepository
import org.slf4j.LoggerFactory
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
    private val logger = LoggerFactory.getLogger(javaClass)
    @Transactional
    fun addNewVocabulary(
        userId: UUID,
        lang: String,
        items: List<NewVocabularyDTO>,
        turnId: UUID? = null
    ): List<VocabularyItemEntity> {
        if (items.isEmpty()) {
            return emptyList()
        }

        logger.debug("Adding ${items.size} vocabulary items for user $userId in $lang")

        val now = Instant.now()
        val saved = mutableListOf<VocabularyItemEntity>()

        for (nv in items) {
            val lemmaNorm = normalizeLemma(nv.lemma)
            if (lemmaNorm.isBlank()) continue

            val item = vocabularyItemRepository.findByUserIdAndLangAndLemma(userId, lang, lemmaNorm)
                ?.apply {
                    exposures += 1
                    lastSeenAt = now
                    if (conceptName == null && nv.conceptName != null) {
                        conceptName = nv.conceptName
                    }
                    logger.debug("Updated vocabulary item: $lemmaNorm (exposures: $exposures)")
                }
                ?: VocabularyItemEntity(
                    userId = userId,
                    lang = lang,
                    lemma = lemmaNorm,
                    exposures = 1,
                    lastSeenAt = now,
                    conceptName = nv.conceptName
                ).also {
                    logger.debug("Created new vocabulary item: $lemmaNorm")
                }

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

        logger.info("Saved ${saved.size} vocabulary items for user $userId in $lang")
        return saved
    }

    fun getVocabularyCountForLanguage(userId: UUID, lang: String): Int {
        return vocabularyItemRepository.countByUserIdAndLang(userId, lang).toInt()
    }

    private fun normalizeLemma(raw: String): String {
        val nfc = Normalizer.normalize(raw, Normalizer.Form.NFC)
        return nfc.trim()
    }
}
