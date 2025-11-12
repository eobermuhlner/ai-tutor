package ch.obermuhlner.aitutor.catalog.controller

import ch.obermuhlner.aitutor.auth.service.AuthorizationService
import ch.obermuhlner.aitutor.catalog.domain.LanguageEntity
import ch.obermuhlner.aitutor.catalog.dto.LanguageResponse
import ch.obermuhlner.aitutor.catalog.service.CatalogLanguageService
import ch.obermuhlner.aitutor.catalog.service.UnifiedCatalogImportService
import ch.obermuhlner.aitutor.core.model.catalog.LanguageMetadata
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/api/v1/languages")
class LanguageController(
    private val catalogLanguageService: CatalogLanguageService,
    private val unifiedCatalogImportService: UnifiedCatalogImportService,
    private val authorizationService: AuthorizationService
) {
    private val objectMapper = jacksonObjectMapper()
    private val logger = LoggerFactory.getLogger(javaClass)

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

    /**
     * Import languages from unified catalog YAML format.
     *
     * POST /api/v1/languages/import
     *
     * Request: multipart/form-data
     * - catalogFile: catalog.yml file (containing languages section)
     *
     * Response: Import statistics
     *
     * Requires: ADMIN role
     */
    @PostMapping("/import", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun importLanguages(
        @RequestParam("catalogFile") catalogFile: MultipartFile
    ): ResponseEntity<Map<String, Any>> {
        authorizationService.requireAdmin()
        val currentUser = authorizationService.getCurrentUser()

        logger.info("User ${currentUser.id} importing languages from unified catalog format")

        try {
            // Parse catalog file
            val catalog = unifiedCatalogImportService.parseCatalog(catalogFile)

            if (catalog.languages.isEmpty()) {
                return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(
                        mapOf(
                            "success" to false,
                            "error" to "Catalog file contains no languages",
                            "languagesImported" to 0
                        )
                    )
            }

            // Import languages (upsert by code)
            var created = 0
            var updated = 0

            catalog.languages.forEach { langImport ->
                val entity = LanguageEntity(
                    code = langImport.code,
                    nameJson = objectMapper.writeValueAsString(langImport.name),
                    flagEmoji = langImport.flagEmoji,
                    nativeName = langImport.nativeName,
                    difficulty = langImport.difficulty,
                    descriptionJson = objectMapper.writeValueAsString(langImport.description),
                    isActive = langImport.isActive,
                    displayOrder = langImport.displayOrder,
                    createdAt = java.time.Instant.now(),
                    updatedAt = java.time.Instant.now()
                )

                val existing = catalogLanguageService.getLanguageByCode(langImport.code)
                if (existing != null) {
                    catalogLanguageService.updateLanguage(langImport.code, entity)
                    updated++
                    logger.info("Updated existing language: ${langImport.code}")
                } else {
                    catalogLanguageService.createLanguage(entity)
                    created++
                    logger.info("Created new language: ${langImport.code}")
                }
            }

            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                    mapOf(
                        "success" to true,
                        "languagesImported" to catalog.languages.size,
                        "created" to created,
                        "updated" to updated
                    )
                )
        } catch (e: Exception) {
            logger.error("Failed to import languages", e)
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                    mapOf(
                        "success" to false,
                        "error" to "Import failed: ${e.message}",
                        "languagesImported" to 0
                    )
                )
        }
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