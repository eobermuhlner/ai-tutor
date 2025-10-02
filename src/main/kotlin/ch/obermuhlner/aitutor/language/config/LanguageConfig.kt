package ch.obermuhlner.aitutor.language.config

import ch.obermuhlner.aitutor.core.model.catalog.Difficulty
import ch.obermuhlner.aitutor.core.model.catalog.LanguageMetadata
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class LanguageConfig {

    @Bean
    fun supportedLanguages(): Map<String, LanguageMetadata> {
        return mapOf(
            "es" to LanguageMetadata(
                code = "es",
                nameJson = """{"en": "Spanish", "es": "Español", "de": "Spanisch", "fr": "Espagnol"}""",
                flagEmoji = "🇪🇸",
                nativeName = "Español",
                difficulty = Difficulty.Easy,
                descriptionJson = """{"en": "One of the most spoken languages worldwide, with clear pronunciation rules", "es": "Uno de los idiomas más hablados del mundo, con reglas de pronunciación claras"}"""
            ),
            "fr" to LanguageMetadata(
                code = "fr",
                nameJson = """{"en": "French", "es": "Francés", "de": "Französisch", "fr": "Français"}""",
                flagEmoji = "🇫🇷",
                nativeName = "Français",
                difficulty = Difficulty.Medium,
                descriptionJson = """{"en": "Romance language spoken across five continents", "fr": "Langue romane parlée sur cinq continents"}"""
            ),
            "de" to LanguageMetadata(
                code = "de",
                nameJson = """{"en": "German", "es": "Alemán", "de": "Deutsch", "fr": "Allemand"}""",
                flagEmoji = "🇩🇪",
                nativeName = "Deutsch",
                difficulty = Difficulty.Medium,
                descriptionJson = """{"en": "Germanic language with precise grammar rules", "de": "Germanische Sprache mit präzisen Grammatikregeln"}"""
            ),
            "ja" to LanguageMetadata(
                code = "ja",
                nameJson = """{"en": "Japanese", "es": "Japonés", "de": "Japanisch", "fr": "Japonais", "ja": "日本語"}""",
                flagEmoji = "🇯🇵",
                nativeName = "日本語",
                difficulty = Difficulty.Hard,
                descriptionJson = """{"en": "East Asian language with three writing systems", "ja": "三つの文字体系を持つ東アジアの言語"}"""
            )
        )
    }
}
