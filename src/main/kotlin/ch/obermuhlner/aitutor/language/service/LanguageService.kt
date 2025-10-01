package ch.obermuhlner.aitutor.language.service

import org.springframework.stereotype.Service

@Service
class LanguageService {

    private val languageMap = mapOf(
        // English variants
        "en" to "English",
        "en-US" to "English (US)",
        "en-GB" to "English (UK)",
        "en-AU" to "English (Australia)",
        "en-CA" to "English (Canada)",

        // Spanish variants
        "es" to "Spanish",
        "es-ES" to "Spanish (Spain)",
        "es-MX" to "Spanish (Mexico)",
        "es-AR" to "Spanish (Argentina)",

        // French variants
        "fr" to "French",
        "fr-FR" to "French (France)",
        "fr-CA" to "French (Canada)",

        // German
        "de" to "German",
        "de-DE" to "German (Germany)",
        "de-AT" to "German (Austria)",
        "de-CH" to "German (Switzerland)",

        // Italian
        "it" to "Italian",
        "it-IT" to "Italian (Italy)",

        // Portuguese
        "pt" to "Portuguese",
        "pt-BR" to "Portuguese (Brazil)",
        "pt-PT" to "Portuguese (Portugal)",

        // Chinese
        "zh" to "Chinese",
        "zh-CN" to "Chinese (Simplified)",
        "zh-TW" to "Chinese (Traditional)",

        // Japanese
        "ja" to "Japanese",
        "ja-JP" to "Japanese (Japan)",

        // Korean
        "ko" to "Korean",
        "ko-KR" to "Korean (Korea)",

        // Russian
        "ru" to "Russian",
        "ru-RU" to "Russian (Russia)",

        // Arabic
        "ar" to "Arabic",
        "ar-SA" to "Arabic (Saudi Arabia)",

        // Dutch
        "nl" to "Dutch",
        "nl-NL" to "Dutch (Netherlands)",

        // Polish
        "pl" to "Polish",
        "pl-PL" to "Polish (Poland)",

        // Turkish
        "tr" to "Turkish",
        "tr-TR" to "Turkish (Turkey)",

        // Swedish
        "sv" to "Swedish",
        "sv-SE" to "Swedish (Sweden)",

        // Norwegian
        "no" to "Norwegian",
        "nb" to "Norwegian Bokmål",

        // Danish
        "da" to "Danish",
        "da-DK" to "Danish (Denmark)",

        // Finnish
        "fi" to "Finnish",
        "fi-FI" to "Finnish (Finland)",

        // Greek
        "el" to "Greek",
        "el-GR" to "Greek (Greece)",

        // Hindi
        "hi" to "Hindi",
        "hi-IN" to "Hindi (India)",

        // Czech
        "cs" to "Czech",
        "cs-CZ" to "Czech (Czech Republic)",

        // Hungarian
        "hu" to "Hungarian",
        "hu-HU" to "Hungarian (Hungary)",

        // Romanian
        "ro" to "Romanian",
        "ro-RO" to "Romanian (Romania)",

        // Thai
        "th" to "Thai",
        "th-TH" to "Thai (Thailand)",

        // Vietnamese
        "vi" to "Vietnamese",
        "vi-VN" to "Vietnamese (Vietnam)",

        // Indonesian
        "id" to "Indonesian",
        "id-ID" to "Indonesian (Indonesia)",

        // Malay
        "ms" to "Malay",
        "ms-MY" to "Malay (Malaysia)",

        // Hebrew
        "he" to "Hebrew",
        "iw" to "Hebrew",  // Old code
        "he-IL" to "Hebrew (Israel)",

        // Ukrainian
        "uk" to "Ukrainian",
        "uk-UA" to "Ukrainian (Ukraine)"
    )

    /**
     * Get the English language name for a given language code.
     *
     * @param languageCode ISO 639-1 language code (e.g., "en", "es") or
     *                     BCP 47 language tag (e.g., "en-US", "es-MX")
     * @return English name of the language (e.g., "English", "Spanish (Mexico)")
     * @throws IllegalArgumentException if the language code is not supported
     */
    fun getLanguageName(languageCode: String): String {
        return languageMap[languageCode]
            ?: throw IllegalArgumentException("Unsupported language code: $languageCode. Supported codes: ${languageMap.keys.sorted()}")
    }

    /**
     * Check if a language code is supported.
     *
     * @param languageCode The language code to check
     * @return true if the language code is supported, false otherwise
     */
    fun isSupported(languageCode: String): Boolean {
        return languageMap.containsKey(languageCode)
    }

    /**
     * Get all supported language codes.
     *
     * @return Set of all supported language codes
     */
    fun getSupportedLanguageCodes(): Set<String> {
        return languageMap.keys
    }
}
