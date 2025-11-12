package ch.obermuhlner.aitutor.catalog.service

import ch.obermuhlner.aitutor.catalog.domain.LanguageEntity
import ch.obermuhlner.aitutor.catalog.repository.LanguageRepository
import ch.obermuhlner.aitutor.core.model.catalog.LanguageMetadata
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class CatalogLanguageService(
    private val languageRepository: LanguageRepository
) {
    fun getAllLanguages(): List<LanguageEntity> {
        return languageRepository.findAll()
    }

    fun getActiveLanguages(): List<LanguageEntity> {
        return languageRepository.findByIsActiveTrue()
    }

    fun getInactiveLanguages(): List<LanguageEntity> {
        return languageRepository.findByIsActiveFalse()
    }

    fun getLanguageByCode(code: String): LanguageEntity? {
        return languageRepository.findById(code).orElse(null)
    }

    fun createLanguage(language: LanguageEntity): LanguageEntity {
        language.createdAt = Instant.now()
        language.updatedAt = Instant.now()
        return languageRepository.save(language)
    }

    fun updateLanguage(code: String, updatedLanguage: LanguageEntity): LanguageEntity? {
        if (!languageRepository.existsById(code)) {
            return null
        }
        updatedLanguage.code = code  // Ensure the ID doesn't change
        updatedLanguage.updatedAt = Instant.now()
        return languageRepository.save(updatedLanguage)
    }

    fun deleteLanguage(code: String): Boolean {
        if (!languageRepository.existsById(code)) {
            return false
        }
        languageRepository.deleteById(code)
        return true
    }

    fun activateLanguage(code: String): LanguageEntity? {
        val language = languageRepository.findById(code).orElse(null) ?: return null
        language.isActive = true
        language.updatedAt = Instant.now()
        return languageRepository.save(language)
    }

    fun deactivateLanguage(code: String): LanguageEntity? {
        val language = languageRepository.findById(code).orElse(null) ?: return null
        language.isActive = false
        language.updatedAt = Instant.now()
        return languageRepository.save(language)
    }

    fun toLanguageMetadata(language: LanguageEntity): LanguageMetadata {
        return LanguageMetadata(
            code = language.code,
            nameJson = language.nameJson,
            flagEmoji = language.flagEmoji,
            nativeName = language.nativeName,
            difficulty = language.difficulty,
            descriptionJson = language.descriptionJson
        )
    }
}