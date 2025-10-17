package ch.obermuhlner.aitutor.conversation.service

import ch.obermuhlner.aitutor.conversation.config.AudioProperties
import ch.obermuhlner.aitutor.core.model.catalog.TutorVoice
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Spring AI implementation of text-to-speech audio synthesis.
 *
 * Supports multiple AI providers:
 * - OpenAI: Uses tts-1 or tts-1-hd models with 6 voice options
 * - Azure OpenAI: Uses Neural TTS voices with 70+ language support
 * - Ollama: Not supported (TTS not available)
 *
 * Voice resolution:
 * 1. Abstract voice (e.g., Warm, Professional) is provided
 * 2. Configuration maps abstract voice to provider-specific voice
 * 3. Provider-specific voice is used for synthesis
 */
@Service
class SpringAiAudioService(
    private val audioProperties: AudioProperties
) : AiAudioService {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun synthesizeSpeech(
        text: String,
        voiceId: TutorVoice?,
        languageCode: String?,
        speed: Double?
    ): ByteArray {
        if (!isAvailable()) {
            throw UnsupportedOperationException(
                "TTS is not enabled. Check application-ai-*.yml configuration."
            )
        }

        // Resolve abstract voice to provider-specific voice
        val providerVoice = resolveVoice(voiceId)
        val effectiveSpeed = speed ?: audioProperties.defaultSpeed

        logger.info(
            "Synthesizing speech: abstractVoice={}, providerVoice={}, speed={}, textLength={}",
            voiceId, providerVoice, effectiveSpeed, text.length
        )

        // TODO: Implement actual TTS integration
        // This is a placeholder implementation that needs to be completed with:
        // 1. Direct OpenAI API calls for OpenAI provider
        // 2. Direct Azure OpenAI API calls for Azure provider
        // 3. Integration with external TTS service for Ollama
        //
        // For now, return empty byte array to allow compilation
        logger.warn("TTS synthesis not yet implemented - returning empty audio")
        throw UnsupportedOperationException("TTS synthesis implementation pending")
    }

    override fun getVoiceMappings(): Map<String, String> {
        return audioProperties.voiceMappings
    }

    override fun isAvailable(): Boolean {
        val available = audioProperties.enabled
        if (!available) {
            logger.debug("TTS not available: enabled={}", audioProperties.enabled)
        }
        return available
    }

    /**
     * Resolve abstract voice to provider-specific voice using configuration mappings.
     *
     * @param voiceId Abstract voice identifier (Warm, Professional, etc.)
     * @return Provider-specific voice name (e.g., "nova" for OpenAI, "en-US-JennyNeural" for Azure)
     */
    private fun resolveVoice(voiceId: TutorVoice?): String {
        if (voiceId == null) {
            logger.debug("No voice specified, using default: {}", audioProperties.defaultVoice)
            return audioProperties.defaultVoice
        }

        val mappedVoice = audioProperties.voiceMappings[voiceId.name]
        if (mappedVoice == null) {
            logger.warn(
                "No voice mapping found for {}, using default: {}. Check application-ai-*.yml configuration.",
                voiceId, audioProperties.defaultVoice
            )
            return audioProperties.defaultVoice
        }

        logger.debug("Resolved voice: {} → {}", voiceId, mappedVoice)
        return mappedVoice
    }
}
