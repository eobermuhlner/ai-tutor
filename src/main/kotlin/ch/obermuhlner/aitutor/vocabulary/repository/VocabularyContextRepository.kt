package ch.obermuhlner.aitutor.vocabulary.repository

import ch.obermuhlner.aitutor.vocabulary.domain.VocabularyContextEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface VocabularyContextRepository : JpaRepository<VocabularyContextEntity, UUID> {
    fun findByVocabItemId(vocabItemId: UUID): List<VocabularyContextEntity>
}
