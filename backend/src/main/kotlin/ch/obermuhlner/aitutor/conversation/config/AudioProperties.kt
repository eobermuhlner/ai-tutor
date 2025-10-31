package ch.obermuhlner.aitutor.conversation.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/**
 * Configuration properties for text-to-speech audio synthesis.
 *
 * Maps abstract tutor voices (Warm, Professional, etc.) to provider-specific voices.
 * Configuration varies by AI provider:
 * - OpenAI: voice names like "nova", "onyx", "echo"
 * - Azure OpenAI: neural voice names like "en-US-JennyNeural", "en-US-GuyNeural"
 */
@Component
@ConfigurationProperties(prefix = "ai-tutor.audio")
data class AudioProperties(
    var enabled: Boolean = false,
    var voiceMappings: Map<String, String> = emptyMap(),
    var defaultVoice: String = "alloy",
    var defaultSpeed: Double = 1.0,
    var defaultModel: String? = null  // For OpenAI: tts-1 or tts-1-hd
)
