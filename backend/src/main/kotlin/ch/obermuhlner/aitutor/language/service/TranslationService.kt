package ch.obermuhlner.aitutor.language.service

interface TranslationService {
    /**
     * Translate text from one language to another using AI.
     */
    fun translate(text: String, from: String, to: String): String

    /**
     * Batch translate multiple texts for efficiency.
     */
    fun batchTranslate(texts: List<String>, from: String, to: String): List<String>
}
