package ch.obermuhlner.aitutor.catalog.repository

import ch.obermuhlner.aitutor.catalog.domain.TutorProfileEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TutorProfileRepository : JpaRepository<TutorProfileEntity, UUID> {
    fun findByIsActiveTrueOrderByDisplayOrder(): List<TutorProfileEntity>
    fun findByTargetLanguageCodeAndIsActiveTrueOrderByDisplayOrder(languageCode: String): List<TutorProfileEntity>
}
