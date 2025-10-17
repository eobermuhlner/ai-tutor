# Task 0012: Text-to-Speech Audio Support

## Overview
Add text-to-speech (TTS) audio support for the AI tutor with provider-agnostic voice abstraction. Tutors will have abstract voice characteristics (Warm, Professional, etc.) configured in `application.yml`, which map to provider-specific voices in `application-ai-*.yml`.

## Architecture Design

### Voice Abstraction Strategy
- **Abstract Layer**: Define tutor voices as semantic characteristics (Warm, Professional, Energetic, etc.)
- **Provider Mapping**: Map abstract voices to provider-specific voices in AI configuration files
- **Configuration**: Tutors configured with abstract voice in `application.yml`, mapped to provider voices in `application-ai-*.yml`

Example flow:
1. Tutor "María" configured with voice: `Warm` in `application.yml`
2. OpenAI profile maps `Warm` → `nova` in `application-ai-openai.yml`
3. Azure profile maps `Warm` → `en-US-JennyNeural` in `application-ai-azure-openai.yml`
4. Service resolves abstract voice to provider-specific voice at runtime

## Implementation Plan

### 1. Voice Abstraction Layer

#### Create `TutorVoice.kt` enum
**Location**: `src/main/kotlin/ch/obermuhlner/aitutor/core/model/catalog/TutorVoice.kt`

```kotlin
package ch.obermuhlner.aitutor.core.model.catalog

enum class TutorVoice {
    Warm,           // Friendly, welcoming, nurturing
    Professional,   // Clear, neutral, business-like
    Energetic,      // Upbeat, enthusiastic, dynamic
    Calm,           // Soothing, patient, gentle
    Authoritative,  // Confident, commanding, formal
    Friendly        // Casual, approachable, conversational
}
```

#### Update Configuration Data Classes
**File**: `src/main/kotlin/ch/obermuhlner/aitutor/catalog/config/CatalogProperties.kt`

Add `voiceId` field to `TutorArchetypeConfig`:
```kotlin
data class TutorArchetypeConfig(
    val id: String,
    val emoji: String,
    val personaEnglish: String,
    val domainEnglish: String,
    val descriptionTemplateEnglish: String,
    val personality: TutorPersonality,
    val teachingStyle: TeachingStyle = TeachingStyle.Reactive,
    val displayOrder: Int,
    val voiceId: TutorVoice? = null  // NEW FIELD
)
```

#### Update Database Entity
**File**: `src/main/kotlin/ch/obermuhlner/aitutor/catalog/domain/TutorProfileEntity.kt`

Add `voiceId` column:
```kotlin
@Enumerated(EnumType.STRING)
@Column(name = "voice_id", length = 32)
var voiceId: TutorVoice? = null
```

### 2. Configuration Updates

#### Update `application.yml`
**Location**: `src/main/resources/application.yml`

Add voice assignments to tutor archetypes:
```yaml
ai-tutor:
  catalog:
    tutorArchetypes:
      - id: encouraging-general
        emoji: "👩‍🏫"
        personaEnglish: patient coach
        # ... existing fields ...
        voiceId: Warm

      - id: strict-academic
        emoji: "🎓"
        personaEnglish: strict academic
        # ... existing fields ...
        voiceId: Authoritative

      - id: casual-friend
        emoji: "😊"
        personaEnglish: casual friend
        # ... existing fields ...
        voiceId: Friendly

      - id: academic-professor
        emoji: "👨‍🏫"
        personaEnglish: academic professor
        # ... existing fields ...
        voiceId: Professional
```

#### Update `application-ai-openai.yml`
**Location**: `src/main/resources/application-ai-openai.yml`

```yaml
# Enable TTS
spring.ai.model.audio.speech: openai

# OpenAI TTS configuration
spring.ai.openai.audio.speech.options.model: tts-1  # or tts-1-hd for higher quality
spring.ai.openai.audio.speech.options.response-format: mp3
spring.ai.openai.audio.speech.options.speed: 1.0

# Voice mappings (abstract voice → OpenAI voice)
ai-tutor:
  audio:
    enabled: true
    voice-mappings:
      Warm: nova
      Professional: onyx
      Energetic: shimmer
      Calm: alloy
      Authoritative: echo
      Friendly: fable
    default-voice: alloy
    default-speed: 1.0
```

**OpenAI Voice Options**:
- `alloy` - Neutral, balanced
- `echo` - Deep, authoritative male
- `fable` - Clear, articulate British accent
- `onyx` - Deep, smooth male
- `nova` - Warm, friendly female
- `shimmer` - Bright, energetic female

#### Update `application-ai-azure-openai.yml`
**Location**: `src/main/resources/application-ai-azure-openai.yml`

```yaml
# Enable TTS
spring.ai.model.audio.speech: azure-openai

# Azure OpenAI TTS configuration
spring.ai.azure.openai.audio.speech.options.deployment-name: tts  # Your Azure deployment
spring.ai.azure.openai.audio.speech.options.response-format: mp3

# Voice mappings (abstract voice → Azure Neural voice)
ai-tutor:
  audio:
    enabled: true
    voice-mappings:
      Warm: en-US-JennyNeural
      Professional: en-US-GuyNeural
      Energetic: en-US-AriaNeural
      Calm: en-US-DavisNeural
      Authoritative: en-US-TonyNeural
      Friendly: en-US-SaraNeural
    default-voice: en-US-JennyNeural
    default-speed: 1.0
```

**Note**: Azure voices can be language-specific. May need multi-language support later:
- Spanish: `es-ES-ElviraNeural`, `es-ES-AlvaroNeural`
- German: `de-DE-KatjaNeural`, `de-DE-ConradNeural`
- French: `fr-FR-DeniseNeural`, `fr-FR-HenriNeural`
- Japanese: `ja-JP-NanamiNeural`, `ja-JP-KeitaNeural`

#### Update `application-ai-ollama.yml`
**Location**: `src/main/resources/application-ai-ollama.yml`

```yaml
# TTS not supported by Ollama
spring.ai.model.audio.speech: none

ai-tutor:
  audio:
    enabled: false
    # Ollama does not include native TTS
    # External integration would be needed (e.g., Piper TTS, Coqui TTS)
```

### 3. Configuration Properties

#### Create `AudioProperties.kt`
**Location**: `src/main/kotlin/ch/obermuhlner/aitutor/conversation/config/AudioProperties.kt`

```kotlin
package ch.obermuhlner.aitutor.conversation.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "ai-tutor.audio")
data class AudioProperties(
    var enabled: Boolean = false,
    var voiceMappings: Map<String, String> = emptyMap(),
    var defaultVoice: String = "alloy",
    var defaultSpeed: Double = 1.0,
    var defaultModel: String? = null  // For OpenAI: tts-1 or tts-1-hd
)
```

### 4. Service Layer

#### Create `AiAudioService` interface
**Location**: `src/main/kotlin/ch/obermuhlner/aitutor/conversation/service/AiAudioService.kt`

```kotlin
package ch.obermuhlner.aitutor.conversation.service

import ch.obermuhlner.aitutor.core.model.catalog.TutorVoice

interface AiAudioService {
    /**
     * Synthesize speech from text using specified voice
     * @param text The text to synthesize
     * @param voiceId Abstract voice identifier (maps to provider-specific voice)
     * @param languageCode Target language code (for future multi-language support)
     * @param speed Speech speed multiplier (0.25 to 4.0)
     * @return Audio bytes (MP3 format)
     */
    fun synthesizeSpeech(
        text: String,
        voiceId: TutorVoice?,
        languageCode: String? = null,
        speed: Double? = null
    ): ByteArray

    /**
     * Get available abstract voices and their provider mappings
     * @return Map of abstract voice → provider voice
     */
    fun getVoiceMappings(): Map<String, String>

    /**
     * Check if TTS is enabled and available
     */
    fun isAvailable(): Boolean
}
```

#### Implement `SpringAiAudioService`
**Location**: `src/main/kotlin/ch/obermuhlner/aitutor/conversation/service/SpringAiAudioService.kt`

```kotlin
package ch.obermuhlner.aitutor.conversation.service

import ch.obermuhlner.aitutor.conversation.config.AudioProperties
import ch.obermuhlner.aitutor.core.model.catalog.TutorVoice
import org.slf4j.LoggerFactory
import org.springframework.ai.audio.speech.SpeechModel
import org.springframework.ai.audio.speech.SpeechPrompt
import org.springframework.ai.audio.speech.SpeechResponse
import org.springframework.stereotype.Service

@Service
class SpringAiAudioService(
    private val speechModel: SpeechModel?,  // Nullable - may not be configured
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
                "TTS is not enabled or SpeechModel is not configured for this provider"
            )
        }

        // Resolve abstract voice to provider-specific voice
        val providerVoice = resolveVoice(voiceId)
        val effectiveSpeed = speed ?: audioProperties.defaultSpeed

        logger.info("Synthesizing speech: voice={}, providerVoice={}, speed={}, textLength={}",
            voiceId, providerVoice, effectiveSpeed, text.length)

        // Build speech prompt with options
        val prompt = SpeechPrompt(text, mapOf(
            "voice" to providerVoice,
            "speed" to effectiveSpeed.toString(),
            "model" to (audioProperties.defaultModel ?: "tts-1")
        ))

        val response: SpeechResponse = speechModel!!.call(prompt)
        return response.result.output

        // Note: Error handling for rate limits, invalid voices, etc.
        // should be added in production
    }

    override fun getVoiceMappings(): Map<String, String> {
        return audioProperties.voiceMappings
    }

    override fun isAvailable(): Boolean {
        return audioProperties.enabled && speechModel != null
    }

    private fun resolveVoice(voiceId: TutorVoice?): String {
        if (voiceId == null) {
            return audioProperties.defaultVoice
        }

        val mappedVoice = audioProperties.voiceMappings[voiceId.name]
        if (mappedVoice == null) {
            logger.warn("No voice mapping found for {}, using default: {}",
                voiceId, audioProperties.defaultVoice)
            return audioProperties.defaultVoice
        }

        return mappedVoice
    }
}
```

### 5. REST API Layer

#### Add DTOs
**Location**: `src/main/kotlin/ch/obermuhlner/aitutor/conversation/dto/AudioDTOs.kt`

```kotlin
package ch.obermuhlner.aitutor.conversation.dto

data class SynthesizeRequest(
    val text: String,
    val voiceId: String? = null,  // Abstract voice name (Warm, Professional, etc.)
    val speed: Double? = null     // 0.25 to 4.0
)

data class VoiceListResponse(
    val abstractVoices: List<String>,           // [Warm, Professional, ...]
    val voiceMappings: Map<String, String>,     // Warm → nova
    val defaultVoice: String
)
```

#### Add Endpoints to `ChatController`
**Location**: `src/main/kotlin/ch/obermuhlner/aitutor/chat/controller/ChatController.kt`

```kotlin
// Add to existing ChatController

/**
 * Synthesize speech from arbitrary text
 */
@PostMapping("/synthesize")
fun synthesizeSpeech(
    @RequestBody request: SynthesizeRequest
): ResponseEntity<ByteArray> {
    if (!audioService.isAvailable()) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()
    }

    val voiceEnum = request.voiceId?.let {
        try { TutorVoice.valueOf(it) }
        catch (e: IllegalArgumentException) { null }
    }

    val audioBytes = audioService.synthesizeSpeech(
        text = request.text,
        voiceId = voiceEnum,
        speed = request.speed
    )

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("audio/mpeg"))
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"speech.mp3\"")
        .body(audioBytes)
}

/**
 * Synthesize audio for a specific message in a session
 */
@PostMapping("/sessions/{sessionId}/messages/{messageId}/audio")
fun synthesizeMessageAudio(
    @PathVariable sessionId: UUID,
    @PathVariable messageId: UUID,
    @RequestParam(required = false) speed: Double?
): ResponseEntity<ByteArray> {
    if (!audioService.isAvailable()) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()
    }

    val session = chatService.getSession(sessionId)
        ?: return ResponseEntity.notFound().build()

    val message = chatService.getMessage(sessionId, messageId)
        ?: return ResponseEntity.notFound().build()

    // Get tutor's voice from session
    val tutorVoice = session.tutorVoiceId  // Assuming this field is added

    val audioBytes = audioService.synthesizeSpeech(
        text = message.content,
        voiceId = tutorVoice,
        languageCode = session.targetLanguageCode,
        speed = speed
    )

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("audio/mpeg"))
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "inline; filename=\"message-${messageId}.mp3\"")
        .body(audioBytes)
}

/**
 * Get available voices and mappings
 */
@GetMapping("/audio/voices")
fun getAvailableVoices(): ResponseEntity<VoiceListResponse> {
    if (!audioService.isAvailable()) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()
    }

    val response = VoiceListResponse(
        abstractVoices = TutorVoice.values().map { it.name },
        voiceMappings = audioService.getVoiceMappings(),
        defaultVoice = audioProperties.defaultVoice
    )

    return ResponseEntity.ok(response)
}
```

### 6. Database Schema Updates (Optional - for audio caching)

#### Migration SQL (future enhancement)
**Location**: `src/main/resources/db/migration/V{version}__add_audio_fields.sql`

```sql
-- Add voice_id to tutor_profiles
ALTER TABLE tutor_profiles
ADD COLUMN voice_id VARCHAR(32);

-- Add audio metadata to chat_messages (for caching)
ALTER TABLE chat_messages
ADD COLUMN audio_url VARCHAR(512),
ADD COLUMN audio_format VARCHAR(16),
ADD COLUMN audio_voice VARCHAR(64);
```

### 7. Integration with ChatService

#### Update `ChatSessionEntity`
**File**: `src/main/kotlin/ch/obermuhlner/aitutor/chat/domain/ChatSessionEntity.kt`

Add field to store tutor's voice:
```kotlin
@Enumerated(EnumType.STRING)
@Column(name = "tutor_voice_id", length = 32)
var tutorVoiceId: TutorVoice? = null
```

#### Update Session Creation
When creating a session from a tutor or course, copy the tutor's voice:
```kotlin
// In ChatService.createSessionFromCourse or similar
newSession.tutorVoiceId = tutorProfile.voiceId
```

### 8. Testing

#### Unit Tests
**Location**: `src/test/kotlin/ch/obermuhlner/aitutor/conversation/service/SpringAiAudioServiceTest.kt`

```kotlin
package ch.obermuhlner.aitutor.conversation.service

import ch.obermuhlner.aitutor.conversation.config.AudioProperties
import ch.obermuhlner.aitutor.core.model.catalog.TutorVoice
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.ai.audio.speech.SpeechModel
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpringAiAudioServiceTest {

    @Test
    fun `isAvailable returns true when enabled and speechModel present`() {
        val speechModel = mock<SpeechModel>()
        val properties = AudioProperties(enabled = true)
        val service = SpringAiAudioService(speechModel, properties)

        assertTrue(service.isAvailable())
    }

    @Test
    fun `isAvailable returns false when disabled`() {
        val speechModel = mock<SpeechModel>()
        val properties = AudioProperties(enabled = false)
        val service = SpringAiAudioService(speechModel, properties)

        assertFalse(service.isAvailable())
    }

    @Test
    fun `resolveVoice uses mapping when available`() {
        val properties = AudioProperties(
            enabled = true,
            voiceMappings = mapOf("Warm" to "nova"),
            defaultVoice = "alloy"
        )
        val service = SpringAiAudioService(null, properties)

        val mappings = service.getVoiceMappings()
        assertEquals("nova", mappings["Warm"])
    }

    // Additional tests for synthesizeSpeech with mocked SpeechModel
}
```

#### HTTP Client Tests
**Location**: `src/test/http/http-client-requests.http`

```http
### Synthesize arbitrary text
POST {{baseUrl}}/api/v1/chat/synthesize
Authorization: Bearer {{token}}
Content-Type: application/json

{
  "text": "Hello, I am your language tutor. Let's start learning!",
  "voiceId": "Warm",
  "speed": 1.0
}

### Synthesize message audio
POST {{baseUrl}}/api/v1/chat/sessions/{{sessionId}}/messages/{{messageId}}/audio
Authorization: Bearer {{token}}

### Get available voices
GET {{baseUrl}}/api/v1/chat/audio/voices
Authorization: Bearer {{token}}
```

### 9. Documentation Updates

#### Update `README.md`
Add TTS API endpoints to the API documentation section:
- POST `/api/v1/chat/synthesize` - Synthesize speech from text
- POST `/api/v1/chat/sessions/{id}/messages/{messageId}/audio` - Get audio for message
- GET `/api/v1/chat/audio/voices` - List available voices

#### Update `CLAUDE.md`
- Add `TutorVoice` enum to core models
- Add `AudioProperties` to configuration components
- Add `AiAudioService` / `SpringAiAudioService` to service layer
- Add TTS endpoints to REST API Layer section
- Update package structure with conversation/config/

## Implementation Checklist

### Phase 1: Core Infrastructure
- [ ] Create `TutorVoice.kt` enum
- [ ] Update `TutorArchetypeConfig` with `voiceId` field
- [ ] Update `TutorProfileEntity` with `voiceId` column
- [ ] Create `AudioProperties.kt` configuration class

### Phase 2: Configuration
- [ ] Update `application.yml` with voice assignments for all tutor archetypes
- [ ] Update `application-ai-openai.yml` with TTS configuration and voice mappings
- [ ] Update `application-ai-azure-openai.yml` with TTS configuration and voice mappings
- [ ] Document Ollama TTS limitations in `application-ai-ollama.yml`

### Phase 3: Service Implementation
- [ ] Create `AiAudioService` interface
- [ ] Implement `SpringAiAudioService` with voice resolution logic
- [ ] Add error handling for unsupported providers
- [ ] Add logging for TTS operations

### Phase 4: REST API
- [ ] Create `AudioDTOs.kt` (SynthesizeRequest, VoiceListResponse)
- [ ] Add TTS endpoints to `ChatController`
- [ ] Update `ChatSessionEntity` with `tutorVoiceId` field
- [ ] Update session creation to copy tutor voice

### Phase 5: Testing
- [ ] Write unit tests for `SpringAiAudioService`
- [ ] Add HTTP client test examples
- [ ] Manual testing with OpenAI API
- [ ] Manual testing with Azure OpenAI (if available)
- [ ] Verify error handling for unsupported providers

### Phase 6: Documentation
- [ ] Update `README.md` with TTS endpoints
- [ ] Update `CLAUDE.md` with architecture changes
- [ ] Add comments explaining voice abstraction strategy
- [ ] Update http-client.env.json if needed

## Future Enhancements

### Multi-Language Voice Support
- Map abstract voices to language-specific provider voices
- Example: `Warm` + `es-ES` → `es-ES-ElviraNeural` (Azure)
- Requires extending `AudioProperties.voiceMappings` structure

### Audio Caching
- Store synthesized audio bytes or URLs in database
- Cache key: `message_id` + `voice_id` + `speed`
- Reduces API calls and improves response time

### Streaming Audio
- Use SSE (Server-Sent Events) for streaming audio chunks
- Improves perceived latency for long text

### Prosody Control (Azure only)
- Add SSML support for fine-grained control
- Control: emphasis, pitch, rate, volume
- Useful for pedagogical purposes (emphasizing corrections)

### Voice Cloning (Advanced)
- Integrate with ElevenLabs or other voice cloning services
- Allow custom tutor voices
- Higher cost, requires careful UX design

## Notes

### OpenAI TTS Pricing (as of 2024)
- `tts-1`: $0.015 per 1,000 characters (~$15 per million chars)
- `tts-1-hd`: $0.030 per 1,000 characters (~$30 per million chars)
- Average message: ~100 characters = $0.0015-0.003 per audio

### Azure TTS Pricing
- Neural voices: ~$16 per 1 million characters
- Comparable to OpenAI `tts-1`

### Rate Limits
- OpenAI: 50 requests per minute (tier 1)
- Azure: Depends on deployment quota
- Consider implementing request queuing or caching

### Provider Capabilities Comparison
| Feature | OpenAI | Azure OpenAI | Ollama |
|---------|--------|--------------|--------|
| TTS Support | ✅ | ✅ | ❌ |
| Voice Options | 6 voices | 100+ neural voices | N/A |
| Multi-Language | English only | 70+ languages | N/A |
| Speed Control | ✅ (0.25-4.0x) | ✅ | N/A |
| SSML | ❌ | ✅ | N/A |
| Streaming | ❌ | ✅ | N/A |
