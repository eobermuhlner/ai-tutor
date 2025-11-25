package ch.obermuhlner.aitutor.catalog.service

import ch.obermuhlner.aitutor.catalog.config.CatalogProperties
import ch.obermuhlner.aitutor.catalog.domain.LanguageEntity
import ch.obermuhlner.aitutor.catalog.repository.LanguageRepository
import jakarta.annotation.PostConstruct
import java.time.Instant
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["ai-tutor.catalog.use-seeding"], havingValue = "true", matchIfMissing = true)
class SeedLanguageService(
    private val languageRepository: LanguageRepository,
    private val catalogProperties: CatalogProperties
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun seedLanguages() {
        // Only seed if database is empty
        if (languageRepository.count() > 0) {
            logger.info("Language data already exists in database, skipping seed...")
            return
        }

        logger.debug("Seeding language data from configuration...")

        val languageEntities = catalogProperties.languages.map { config ->
            LanguageEntity(
                code = config.code,
                nameJson = config.nameJson,
                flagEmoji = config.flagEmoji,
                nativeName = config.nativeName,
                difficulty = config.difficulty,
                descriptionJson = config.descriptionJson,
                isActive = true,
                displayOrder = 0, // Default display order
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        }

        languageRepository.saveAll(languageEntities)
        logger.info("Seeded ${languageEntities.size} languages: ${languageEntities.joinToString(", ") { it.code }}")
    }
}