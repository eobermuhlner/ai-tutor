package ch.obermuhlner.aitutor.language.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LocalizationServiceImplTest {

    private val translationService = mockk<TranslationService>()
    private val objectMapper = ObjectMapper()
    private val localizationService = LocalizationServiceImpl(translationService, objectMapper)

    @Test
    fun `parseMultilingualJson should parse valid JSON`() {
        val jsonText = """{"en": "Hello", "es": "Hola", "fr": "Bonjour"}"""
        val result = localizationService.parseMultilingualJson(jsonText)

        assertEquals(3, result.size)
        assertEquals("Hello", result["en"])
        assertEquals("Hola", result["es"])
        assertEquals("Bonjour", result["fr"])
    }

    @Test
    fun `parseMultilingualJson should return empty map for invalid JSON`() {
        val jsonText = "not valid json"
        val result = localizationService.parseMultilingualJson(jsonText)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getLocalizedText should return requested language text`() {
        val jsonText = """{"en": "Hello", "es": "Hola"}"""
        val result = localizationService.getLocalizedText(jsonText, "es", "Hello", "en")

        assertEquals("Hola", result)
    }

    @Test
    fun `getLocalizedText should return fallback language when requested not found`() {
        val jsonText = """{"en": "Hello", "es": "Hola"}"""
        val result = localizationService.getLocalizedText(jsonText, "fr", "Hello", "en")

        assertEquals("Hello", result)
    }

    @Test
    fun `getLocalizedText should use AI translation when both languages missing`() {
        val jsonText = """{"de": "Hallo"}"""
        every { translationService.translate("Hello", "en", "es") } returns "Hola"

        val result = localizationService.getLocalizedText(jsonText, "es", "Hello", "en")

        assertEquals("Hola", result)
        verify { translationService.translate("Hello", "en", "es") }
    }

    @Test
    fun `getLocalizedText should return english fallback when translation fails`() {
        val jsonText = """{"de": "Hallo"}"""
        every { translationService.translate("Hello", "en", "es") } throws RuntimeException("Translation failed")

        val result = localizationService.getLocalizedText(jsonText, "es", "Hello", "en")

        assertEquals("Hello", result)
    }

    @Test
    fun `getLocalizedText should return missing message when no fallback`() {
        val jsonText = """{"de": "Hallo"}"""
        val result = localizationService.getLocalizedText(jsonText, "es", "", "en")

        assertEquals("Translation missing", result)
    }
}
