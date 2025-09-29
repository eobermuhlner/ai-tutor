package ch.obermuhlner.aitutor.vocabulary.repository

import ch.obermuhlner.aitutor.vocabulary.domain.VocabularyItemEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface VocabularyItemRepository : JpaRepository<VocabularyItemEntity, UUID> {
    fun findByUserIdAndLangAndLemma(userId: UUID, lang: String, lemma: String): VocabularyItemEntity?
}
