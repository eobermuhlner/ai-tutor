package ch.obermuhlner.aitutor.vocabulary.service

import ch.obermuhlner.aitutor.vocabulary.domain.VocabularyContextEntity
import ch.obermuhlner.aitutor.vocabulary.domain.VocabularyItemEntity
import ch.obermuhlner.aitutor.vocabulary.repository.VocabularyContextRepository
import ch.obermuhlner.aitutor.vocabulary.repository.VocabularyItemRepository
import org.springframework.stereotype.Service
import java.util.*

@Service
class VocabularyQueryService(
    private val vocabularyItemRepository: VocabularyItemRepository,
    private val vocabularyContextRepository: VocabularyContextRepository
) {

    data class VocabularyItemWithContexts(
        val item: VocabularyItemEntity,
        val contexts: List<VocabularyContextEntity>
    )

    fun getUserVocabulary(userId: UUID, lang: String? = null): List<VocabularyItemEntity> {
        return if (lang != null) {
            vocabularyItemRepository.findByUserIdAndLangOrderByLastSeenAtDesc(userId, lang)
        } else {
            vocabularyItemRepository.findByUserIdOrderByLastSeenAtDesc(userId)
        }
    }

    fun getVocabularyItemWithContexts(itemId: UUID): VocabularyItemWithContexts? {
        val item = vocabularyItemRepository.findById(itemId).orElse(null) ?: return null
        val contexts = vocabularyContextRepository.findByVocabItemId(itemId)
        return VocabularyItemWithContexts(item, contexts)
    }
}
