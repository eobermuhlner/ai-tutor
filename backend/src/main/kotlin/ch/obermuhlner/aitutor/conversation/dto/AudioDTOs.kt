package ch.obermuhlner.aitutor.conversation.dto

/**
 * Request to synthesize speech from arbitrary text.
 *
 * @param text The text to synthesize (required)
 * @param voiceId Abstract voice name (Warm, Professional, Energetic, Calm, Authoritative, Friendly)
 * @param speed Speech speed multiplier (0.25 to 4.0), null uses provider default (1.0)
 */
data class SynthesizeRequest(
    val text: String,
    val voiceId: String? = null,
    val speed: Double? = null
)

/**
 * Response containing available voices and their mappings.
 *
 * @param abstractVoices List of available abstract voice names
 * @param voiceMappings Map of abstract voice → provider-specific voice
 * @param defaultVoice Default voice used when no voice is specified
 */
data class VoiceListResponse(
    val abstractVoices: List<String>,
    val voiceMappings: Map<String, String>,
    val defaultVoice: String
)
