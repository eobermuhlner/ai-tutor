package ch.obermuhlner.aitutor.language.service

interface LocalizationService {
    /**
     * Get localized text from multilingual JSON.
     * If translation missing, uses AI translation from englishFallback.
     */
    fun getLocalizedText(
        jsonText: String,
        languageCode: String,
        englishFallback: String,
        fallbackLanguage: String = "en"
    ): String

    /**
     * Parse multilingual JSON string into map.
     */
    fun parseMultilingualJson(jsonText: String): Map<String, String>
}
