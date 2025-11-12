package ch.obermuhlner.aitutor.catalog.controller

import ch.obermuhlner.aitutor.catalog.domain.LanguageEntity
import ch.obermuhlner.aitutor.catalog.dto.LanguageResponse
import ch.obermuhlner.aitutor.catalog.service.CatalogLanguageService
import ch.obermuhlner.aitutor.core.model.catalog.LanguageMetadata
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/languages")
class LanguageController(
    private val catalogLanguageService: CatalogLanguageService
) {
    private val objectMapper = jacksonObjectMapper()

    @GetMapping
    fun getAllLanguages(): List<LanguageResponse> {
        val languages = catalogLanguageService.getAllLanguages()
        return languages.map { toLanguageResponse(it) }
    }

    @GetMapping("/{code}")
    fun getLanguage(@PathVariable code: String): LanguageResponse? {
        val language = catalogLanguageService.getLanguageByCode(code)
        return language?.let { toLanguageResponse(it) }
    }

    @PostMapping
    fun createLanguage(@RequestBody language: LanguageEntity): LanguageResponse {
        // Ensure the language doesn't already exist
        if (catalogLanguageService.getLanguageByCode(language.code) != null) {
            throw IllegalArgumentException("Language with code ${language.code} already exists")
        }
        val createdLanguage = catalogLanguageService.createLanguage(language)
        return toLanguageResponse(createdLanguage)
    }

    @PutMapping("/{code}")
    fun updateLanguage(@PathVariable code: String, @RequestBody updatedLanguage: LanguageEntity): LanguageResponse {
        // Check if the language exists
        val existingLanguage = catalogLanguageService.getLanguageByCode(code)
        if (existingLanguage == null) {
            throw IllegalArgumentException("Language with code $code not found")
        }
        val updated = catalogLanguageService.updateLanguage(code, updatedLanguage)
        return toLanguageResponse(updated ?: throw RuntimeException("Failed to update language"))
    }

    @DeleteMapping("/{code}")
    fun deleteLanguage(@PathVariable code: String) {
        catalogLanguageService.deleteLanguage(code)
    }

    @PostMapping("/{code}/activate")
    fun activateLanguage(@PathVariable code: String): LanguageResponse {
        val language = catalogLanguageService.activateLanguage(code)
        return toLanguageResponse(language ?: throw IllegalArgumentException("Language with code $code not found"))
    }

    @PostMapping("/{code}/deactivate")
    fun deactivateLanguage(@PathVariable code: String): LanguageResponse {
        val language = catalogLanguageService.deactivateLanguage(code)
        return toLanguageResponse(language ?: throw IllegalArgumentException("Language with code $code not found"))
    }

    // Helper function to convert LanguageEntity to LanguageResponse
    private fun toLanguageResponse(language: LanguageEntity): LanguageResponse {
        val nameMap = objectMapper.readValue(language.nameJson, Map::class.java) as Map<String, String>
        val descMap = objectMapper.readValue(language.descriptionJson, Map::class.java) as Map<String, String>
        
        return LanguageResponse(
            code = language.code,
            name = nameMap["en"] ?: language.nativeName, // Default to English name or native name
            flagEmoji = language.flagEmoji,
            nativeName = language.nativeName,
            difficulty = language.difficulty,
            description = descMap["en"] ?: "", // Default to English description
            courseCount = 0 // This would need to be calculated from courses
        )
    }
}