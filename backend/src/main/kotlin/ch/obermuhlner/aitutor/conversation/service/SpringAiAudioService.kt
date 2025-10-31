package ch.obermuhlner.aitutor.conversation.service

import ch.obermuhlner.aitutor.conversation.config.AudioProperties
import ch.obermuhlner.aitutor.core.model.catalog.TutorVoice
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

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
    private val audioProperties: AudioProperties,
    private val environment: Environment,
    private val objectMapper: ObjectMapper,
    @Value("\${spring.ai.openai.api-key:}") private val openAiApiKey: String,
    @Value("\${spring.ai.openai.base-url:https://api.openai.com}") private val openAiBaseUrl: String,
    @Value("\${spring.ai.azure.openai.api-key:}") private val azureApiKey: String,
    @Value("\${spring.ai.azure.openai.endpoint:}") private val azureEndpoint: String
) : AiAudioService {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val restTemplate = RestTemplate()

    private enum class TtsProvider {
        OPENAI, AZURE_OPENAI, OLLAMA, NONE
    }

    private val activeProvider: TtsProvider by lazy {
        detectProvider()
    }

    override fun synthesizeSpeech(
        text: String,
        voiceId: TutorVoice?,
        gender: ch.obermuhlner.aitutor.core.model.catalog.TutorGender?,
        languageCode: String?,
        speed: Double?
    ): ByteArray {
        if (!isAvailable()) {
            throw UnsupportedOperationException(
                "TTS is not enabled. Check application-ai-*.yml configuration."
            )
        }

        // Resolve abstract voice to provider-specific voice (considering gender)
        val providerVoice = resolveVoice(voiceId, gender)
        val effectiveSpeed = speed ?: audioProperties.defaultSpeed

        logger.info(
            "Synthesizing speech: provider={}, abstractVoice={}, gender={}, providerVoice={}, speed={}, textLength={}",
            activeProvider, voiceId, gender, providerVoice, effectiveSpeed, text.length
        )

        return when (activeProvider) {
            TtsProvider.OPENAI -> synthesizeWithOpenAI(text, providerVoice, effectiveSpeed)
            TtsProvider.AZURE_OPENAI -> synthesizeWithAzure(text, providerVoice, effectiveSpeed)
            TtsProvider.OLLAMA -> throw UnsupportedOperationException(
                "TTS not supported with Ollama provider. Use OpenAI or Azure OpenAI."
            )
            TtsProvider.NONE -> throw UnsupportedOperationException(
                "No TTS provider configured. Check application-ai-*.yml configuration."
            )
        }
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
     * Detect which AI provider is currently active based on configuration.
     */
    private fun detectProvider(): TtsProvider {
        val chatModel = environment.getProperty("spring.ai.model.chat", "")
        val speechModel = environment.getProperty("spring.ai.model.audio.speech", "none")

        logger.info("Detecting TTS provider: chatModel={}, speechModel={}", chatModel, speechModel)

        return when {
            speechModel == "openai" && openAiApiKey.isNotBlank() -> TtsProvider.OPENAI
            speechModel == "azure-openai" && azureApiKey.isNotBlank() -> TtsProvider.AZURE_OPENAI
            chatModel.contains("ollama", ignoreCase = true) -> TtsProvider.OLLAMA
            else -> TtsProvider.NONE
        }
    }

    /**
     * Synthesize speech using OpenAI TTS API.
     */
    private fun synthesizeWithOpenAI(text: String, voice: String, speed: Double): ByteArray {
        val url = "$openAiBaseUrl/v1/audio/speech"
        val model = audioProperties.defaultModel ?: "tts-1"

        val requestBody = mapOf(
            "model" to model,
            "input" to text,
            "voice" to voice,
            "speed" to speed,
            "response_format" to "mp3"
        )

        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer $openAiApiKey")
            contentType = MediaType.APPLICATION_JSON
            accept = listOf(MediaType.parseMediaType("audio/mpeg"))
        }

        val request = HttpEntity(requestBody, headers)

        logger.debug("OpenAI TTS request: url={}, model={}, voice={}, speed={}", url, model, voice, speed)

        return try {
            val response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                ByteArray::class.java
            )

            val audioBytes = response.body ?: throw RuntimeException("Empty response from OpenAI TTS API")
            logger.info("OpenAI TTS synthesis successful: {} bytes", audioBytes.size)
            audioBytes

        } catch (e: Exception) {
            logger.error("OpenAI TTS synthesis failed: {}", e.message, e)
            throw RuntimeException("Failed to synthesize speech with OpenAI: ${e.message}", e)
        }
    }

    /**
     * Synthesize speech using Azure OpenAI TTS API.
     */
    private fun synthesizeWithAzure(text: String, voice: String, speed: Double): ByteArray {
        // Azure OpenAI TTS endpoint format:
        // https://{endpoint}/openai/deployments/{deployment-name}/audio/speech?api-version=2024-02-15-preview

        val deploymentName = environment.getProperty(
            "spring.ai.azure.openai.audio.speech.options.deployment-name",
            "tts"
        )
        val apiVersion = "2024-02-15-preview"
        val url = "$azureEndpoint/openai/deployments/$deploymentName/audio/speech?api-version=$apiVersion"

        val requestBody = mapOf(
            "input" to text,
            "voice" to voice,
            "speed" to speed,
            "response_format" to "mp3"
        )

        val headers = HttpHeaders().apply {
            set("api-key", azureApiKey)
            contentType = MediaType.APPLICATION_JSON
            accept = listOf(MediaType.parseMediaType("audio/mpeg"))
        }

        val request = HttpEntity(requestBody, headers)

        logger.debug("Azure TTS request: url={}, deployment={}, voice={}, speed={}", url, deploymentName, voice, speed)

        return try {
            val response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                ByteArray::class.java
            )

            val audioBytes = response.body ?: throw RuntimeException("Empty response from Azure TTS API")
            logger.info("Azure TTS synthesis successful: {} bytes", audioBytes.size)
            audioBytes

        } catch (e: Exception) {
            logger.error("Azure TTS synthesis failed: {}", e.message, e)
            throw RuntimeException("Failed to synthesize speech with Azure: ${e.message}", e)
        }
    }

    /**
     * Resolve abstract voice to provider-specific voice using configuration mappings.
     * Considers both voice type and gender for optimal voice selection.
     *
     * Resolution strategy:
     * 1. Try gender-specific mapping: "Warm-Female" → "nova"
     * 2. Fall back to gender-neutral mapping: "Warm" → "alloy"
     * 3. Fall back to default voice if no mapping found
     *
     * @param voiceId Abstract voice identifier (Warm, Professional, etc.)
     * @param gender Tutor gender (Male/Female/Neutral)
     * @return Provider-specific voice name (e.g., "nova" for OpenAI, "en-US-JennyNeural" for Azure)
     */
    private fun resolveVoice(
        voiceId: TutorVoice?,
        gender: ch.obermuhlner.aitutor.core.model.catalog.TutorGender?
    ): String {
        if (voiceId == null) {
            logger.debug("No voice specified, using default: {}", audioProperties.defaultVoice)
            return audioProperties.defaultVoice
        }

        // Try gender-specific mapping first (e.g., "Warm-Female")
        if (gender != null) {
            val genderSpecificKey = "${voiceId.name}-${gender.name}"
            val genderSpecificVoice = audioProperties.voiceMappings[genderSpecificKey]
            if (genderSpecificVoice != null) {
                logger.debug("Resolved gender-specific voice: {} + {} → {}", voiceId, gender, genderSpecificVoice)
                return genderSpecificVoice
            }
        }

        // Fall back to gender-neutral mapping (e.g., "Warm")
        val neutralVoice = audioProperties.voiceMappings[voiceId.name]
        if (neutralVoice != null) {
            logger.debug("Resolved gender-neutral voice: {} → {}", voiceId, neutralVoice)
            return neutralVoice
        }

        // No mapping found - use default
        logger.warn(
            "No voice mapping found for {} (gender={}), using default: {}. Check application-ai-*.yml configuration.",
            voiceId, gender, audioProperties.defaultVoice
        )
        return audioProperties.defaultVoice
    }
}
