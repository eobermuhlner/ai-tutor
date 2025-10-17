package ch.obermuhlner.aitutor.conversation.service

import ch.obermuhlner.aitutor.core.model.catalog.TutorVoice

/**
 * Service for AI-powered text-to-speech audio synthesis.
 *
 * Provides provider-agnostic TTS capabilities with abstract voice mapping.
 * Abstract voices (Warm, Professional, etc.) are mapped to provider-specific voices
 * in configuration files (application-ai-*.yml).
 */
interface AiAudioService {
    /**
     * Synthesize speech from text using specified voice.
     *
     * @param text The text to synthesize
     * @param voiceId Abstract voice identifier (maps to provider-specific voice via configuration)
     * @param languageCode Target language code (for future multi-language support)
     * @param speed Speech speed multiplier (0.25 to 4.0), null uses default from configuration
     * @return Audio bytes (MP3 format)
     * @throws UnsupportedOperationException if TTS is not enabled or configured
     */
    fun synthesizeSpeech(
        text: String,
        voiceId: TutorVoice?,
        languageCode: String? = null,
        speed: Double? = null
    ): ByteArray

    /**
     * Get available abstract voices and their provider mappings.
     *
     * @return Map of abstract voice name → provider-specific voice name
     */
    fun getVoiceMappings(): Map<String, String>

    /**
     * Check if TTS is enabled and available for the current AI provider.
     *
     * @return true if TTS is configured and available, false otherwise
     */
    fun isAvailable(): Boolean
}
