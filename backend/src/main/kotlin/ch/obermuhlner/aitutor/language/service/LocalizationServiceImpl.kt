package ch.obermuhlner.aitutor.language.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service

@Service
class LocalizationServiceImpl(
    private val translationService: TranslationService,
    private val objectMapper: ObjectMapper
) : LocalizationService {

    override fun getLocalizedText(
        jsonText: String,
        languageCode: String,
        englishFallback: String,
        fallbackLanguage: String
    ): String {
        val translations = parseMultilingualJson(jsonText)

        // 1. Try requested language (exact match, e.g., "de-CH")
        translations[languageCode]?.let { return it }

        // 2. Try base language if locale variant provided (e.g., "de-CH" -> "de")
        if (languageCode.contains("-") || languageCode.contains("_")) {
            val baseLanguageCode = languageCode.split("-", "_").first()
            translations[baseLanguageCode]?.let { return it }
        }

        // 3. Try fallback language (usually English)
        translations[fallbackLanguage]?.let { return it }

        // 4. AI TRANSLATION: Generate on-the-fly
        if (englishFallback.isNotBlank()) {
            return try {
                translationService.translate(englishFallback, "en", languageCode)
            } catch (e: Exception) {
                englishFallback  // Fallback to English if translation fails
            }
        }

        // 5. Last resort
        return "Translation missing"
    }

    override fun parseMultilingualJson(jsonText: String): Map<String, String> {
        return try {
            objectMapper.readValue(jsonText, object : TypeReference<Map<String, String>>() {})
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
