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

        // 1. Try requested language
        translations[languageCode]?.let { return it }

        // 2. Try fallback language (usually English)
        translations[fallbackLanguage]?.let { return it }

        // 3. AI TRANSLATION: Generate on-the-fly
        if (englishFallback.isNotBlank()) {
            return try {
                translationService.translate(englishFallback, "en", languageCode)
            } catch (e: Exception) {
                englishFallback  // Fallback to English if translation fails
            }
        }

        // 4. Last resort
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
