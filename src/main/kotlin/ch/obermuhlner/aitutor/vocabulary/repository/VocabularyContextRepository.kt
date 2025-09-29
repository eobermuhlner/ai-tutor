package ch.obermuhlner.aitutor.vocabulary.repository

import ch.obermuhlner.aitutor.vocabulary.domain.VocabularyContextEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface VocabularyContextRepository : JpaRepository<VocabularyContextEntity, UUID>
