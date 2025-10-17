package ch.obermuhlner.aitutor.core.model.catalog

/**
 * Abstract voice characteristics for tutors.
 * These are mapped to provider-specific voices in application-ai-*.yml configuration files.
 *
 * Examples:
 * - OpenAI: Warm → nova, Professional → onyx
 * - Azure: Warm → en-US-JennyNeural, Professional → en-US-GuyNeural
 */
enum class TutorVoice {
    Warm,           // Friendly, welcoming, nurturing
    Professional,   // Clear, neutral, business-like
    Energetic,      // Upbeat, enthusiastic, dynamic
    Calm,           // Soothing, patient, gentle
    Authoritative,  // Confident, commanding, formal
    Friendly        // Casual, approachable, conversational
}
