package ch.obermuhlner.aitutor.language.config

import ch.obermuhlner.aitutor.catalog.config.CatalogProperties
import ch.obermuhlner.aitutor.core.model.catalog.LanguageMetadata
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class LanguageConfig(
    private val catalogProperties: CatalogProperties
) {

    @Bean
    fun supportedLanguages(): Map<String, LanguageMetadata> {
        return catalogProperties.languages.associate { config ->
            config.code to LanguageMetadata(
                code = config.code,
                nameJson = config.nameJson,
                flagEmoji = config.flagEmoji,
                nativeName = config.nativeName,
                difficulty = config.difficulty,
                descriptionJson = config.descriptionJson
            )
        }
    }
}
