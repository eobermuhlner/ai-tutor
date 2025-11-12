package ch.obermuhlner.aitutor.catalog.repository

import ch.obermuhlner.aitutor.catalog.domain.LanguageEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LanguageRepository : JpaRepository<LanguageEntity, String> {
    fun findByIsActiveTrue(): List<LanguageEntity>
    fun findByIsActiveFalse(): List<LanguageEntity>
}