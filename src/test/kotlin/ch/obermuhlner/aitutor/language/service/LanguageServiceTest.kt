package ch.obermuhlner.aitutor.language.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LanguageServiceTest {

    private lateinit var languageService: LanguageService

    @BeforeEach
    fun setup() {
        languageService = LanguageService()
    }

    @Test
    fun `getLanguageName returns correct name for simple language code`() {
        assertEquals("English", languageService.getLanguageName("en"))
        assertEquals("Spanish", languageService.getLanguageName("es"))
        assertEquals("French", languageService.getLanguageName("fr"))
        assertEquals("German", languageService.getLanguageName("de"))
        assertEquals("Italian", languageService.getLanguageName("it"))
        assertEquals("Portuguese", languageService.getLanguageName("pt"))
        assertEquals("Chinese", languageService.getLanguageName("zh"))
        assertEquals("Japanese", languageService.getLanguageName("ja"))
        assertEquals("Korean", languageService.getLanguageName("ko"))
        assertEquals("Russian", languageService.getLanguageName("ru"))
    }

    @Test
    fun `getLanguageName returns correct name for regional variants`() {
        assertEquals("English (US)", languageService.getLanguageName("en-US"))
        assertEquals("English (UK)", languageService.getLanguageName("en-GB"))
        assertEquals("Spanish (Spain)", languageService.getLanguageName("es-ES"))
        assertEquals("Spanish (Mexico)", languageService.getLanguageName("es-MX"))
        assertEquals("French (France)", languageService.getLanguageName("fr-FR"))
        assertEquals("French (Canada)", languageService.getLanguageName("fr-CA"))
        assertEquals("Portuguese (Brazil)", languageService.getLanguageName("pt-BR"))
        assertEquals("Portuguese (Portugal)", languageService.getLanguageName("pt-PT"))
        assertEquals("Chinese (Simplified)", languageService.getLanguageName("zh-CN"))
        assertEquals("Chinese (Traditional)", languageService.getLanguageName("zh-TW"))
    }

    @Test
    fun `getLanguageName throws exception for unsupported language code`() {
        val exception = assertThrows<IllegalArgumentException> {
            languageService.getLanguageName("xx")
        }
        assertTrue(exception.message!!.contains("Unsupported language code: xx"))
        assertTrue(exception.message!!.contains("Supported codes:"))
    }

    @Test
    fun `getLanguageName handles Hebrew old and new codes`() {
        assertEquals("Hebrew", languageService.getLanguageName("he"))
        assertEquals("Hebrew", languageService.getLanguageName("iw"))
        assertEquals("Hebrew (Israel)", languageService.getLanguageName("he-IL"))
    }

    @Test
    fun `isSupported returns true for supported languages`() {
        assertTrue(languageService.isSupported("en"))
        assertTrue(languageService.isSupported("es"))
        assertTrue(languageService.isSupported("fr"))
        assertTrue(languageService.isSupported("de"))
        assertTrue(languageService.isSupported("en-US"))
        assertTrue(languageService.isSupported("es-MX"))
        assertTrue(languageService.isSupported("zh-CN"))
    }

    @Test
    fun `isSupported returns false for unsupported languages`() {
        assertFalse(languageService.isSupported("xx"))
        assertFalse(languageService.isSupported("xyz"))
        assertFalse(languageService.isSupported("en-XY"))
        assertFalse(languageService.isSupported(""))
    }

    @Test
    fun `getSupportedLanguageCodes returns all supported codes`() {
        val supportedCodes = languageService.getSupportedLanguageCodes()

        assertTrue(supportedCodes.isNotEmpty())
        assertTrue(supportedCodes.contains("en"))
        assertTrue(supportedCodes.contains("es"))
        assertTrue(supportedCodes.contains("fr"))
        assertTrue(supportedCodes.contains("de"))
        assertTrue(supportedCodes.contains("en-US"))
        assertTrue(supportedCodes.contains("es-MX"))
        assertTrue(supportedCodes.contains("zh-CN"))

        // Verify some of the expected language codes exist
        assertTrue(supportedCodes.size > 50) // Should have at least 50+ language codes
    }

    @Test
    fun `getSupportedLanguageCodes contains all major languages`() {
        val supportedCodes = languageService.getSupportedLanguageCodes()

        val majorLanguages = listOf(
            "en", "es", "fr", "de", "it", "pt", "zh", "ja", "ko", "ru",
            "ar", "nl", "pl", "tr", "sv", "no", "da", "fi", "el", "hi"
        )

        majorLanguages.forEach { code ->
            assertTrue(supportedCodes.contains(code), "Expected $code to be supported")
        }
    }

    @Test
    fun `all language codes in map have corresponding names`() {
        val supportedCodes = languageService.getSupportedLanguageCodes()

        supportedCodes.forEach { code ->
            val name = languageService.getLanguageName(code)
            assertNotNull(name)
            assertTrue(name.isNotEmpty())
        }
    }
}
