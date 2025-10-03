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
            "es-ES" to LanguageMetadata(
                code = "es-ES",
                nameJson = """{"en": "Spanish (Spain)", "es": "Español (España)", "de": "Spanisch (Spanien)", "fr": "Espagnol (Espagne)"}""",
                flagEmoji = "🇪🇸",
                nativeName = "Español (España)",
                difficulty = Difficulty.Easy,
                descriptionJson = """{"en": "Castilian Spanish from Spain with clear pronunciation and vosotros form", "es": "Español castellano de España con pronunciación clara y forma vosotros"}"""
            ),
            "fr-FR" to LanguageMetadata(
                code = "fr-FR",
                nameJson = """{"en": "French (France)", "es": "Francés (Francia)", "de": "Französisch (Frankreich)", "fr": "Français (France)"}""",
                flagEmoji = "🇫🇷",
                nativeName = "Français (France)",
                difficulty = Difficulty.Medium,
                descriptionJson = """{"en": "Standard French from France with Parisian pronunciation", "fr": "Français standard de France avec prononciation parisienne"}"""
            ),
            "de-DE" to LanguageMetadata(
                code = "de-DE",
                nameJson = """{"en": "German (Germany)", "es": "Alemán (Alemania)", "de": "Deutsch (Deutschland)", "fr": "Allemand (Allemagne)"}""",
                flagEmoji = "🇩🇪",
                nativeName = "Deutsch (Deutschland)",
                difficulty = Difficulty.Medium,
                descriptionJson = """{"en": "Standard German from Germany with precise grammar rules", "de": "Standarddeutsch aus Deutschland mit präzisen Grammatikregeln"}"""
            ),
            "de-CH" to LanguageMetadata(
                code = "de-CH",
                nameJson = """{"en": "German (Switzerland)", "es": "Alemán (Suiza)", "de": "Deutsch (Schweiz)", "fr": "Allemand (Suisse)"}""",
                flagEmoji = "🇨🇭",
                nativeName = "Deutsch (Schweiz)",
                difficulty = Difficulty.Medium,
                descriptionJson = """{"en": "Swiss Standard German (Schweizer Hochdeutsch) with Swiss orthography (ss instead of ß) and vocabulary", "de": "Schweizer Hochdeutsch mit Schweizer Rechtschreibung (ss statt ß) und Wortschatz"}"""
            ),
            "ja-JP" to LanguageMetadata(
                code = "ja-JP",
                nameJson = """{"en": "Japanese (Japan)", "es": "Japonés (Japón)", "de": "Japanisch (Japan)", "fr": "Japonais (Japon)", "ja": "日本語（日本）"}""",
                flagEmoji = "🇯🇵",
                nativeName = "日本語（日本）",
                difficulty = Difficulty.Hard,
                descriptionJson = """{"en": "Standard Japanese from Japan with three writing systems", "ja": "日本の標準的な日本語、三つの文字体系"}"""
            )
        )
    }
}
