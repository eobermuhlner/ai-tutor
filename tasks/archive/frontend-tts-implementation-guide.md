# Frontend Implementation Guide: Text-to-Speech Feature

## Overview

The AI Tutor backend now supports text-to-speech (TTS) synthesis for tutor responses. This feature allows users to hear tutor messages spoken aloud in the target language with appropriate voice characteristics.

## Backend Capabilities

### Supported Providers
- **OpenAI TTS**: 6 voices (alloy, echo, fable, nova, onyx, shimmer)
- **Azure OpenAI TTS**: Neural voices with 70+ language support
- **Ollama**: Not supported (graceful degradation required)

### Voice Abstraction
The backend uses abstract voice IDs that map to provider-specific voices:
- `Warm` - Friendly, welcoming, nurturing
- `Professional` - Clear, neutral, business-like
- `Energetic` - Upbeat, enthusiastic, dynamic
- `Calm` - Soothing, patient, gentle
- `Authoritative` - Confident, commanding, formal
- `Friendly` - Casual, approachable, conversational

Each tutor profile has a `voiceId` field that specifies which abstract voice to use.

---

## API Endpoints

### 1. Get Available Voices
**Endpoint**: `GET /api/v1/chat/audio/voices`

**Headers**:
```http
Authorization: Bearer {accessToken}
Accept: application/json
```

**Response**:
```json
{
  "abstractVoices": ["Warm", "Professional", "Energetic", "Calm", "Authoritative", "Friendly"],
  "voiceMappings": {
    "Warm": "nova",
    "Professional": "onyx",
    "Energetic": "shimmer",
    "Calm": "alloy",
    "Authoritative": "echo",
    "Friendly": "fable"
  },
  "defaultVoice": "alloy"
}
```

**Use Case**: Call this endpoint on app initialization to check if TTS is available. If the endpoint returns 404 or the voiceMappings are empty, TTS is not configured.

---

### 2. Synthesize Audio for Message
**Endpoint**: `POST /api/v1/chat/sessions/{sessionId}/messages/{messageId}/audio`

**Headers**:
```http
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**Query Parameters**:
- `speed` (optional): Playback speed (0.25 - 4.0, default: 1.0)
  - 0.75 = slower for learning
  - 1.0 = normal speed
  - 1.25 = slightly faster

**Response**: Binary audio data (audio/mpeg format)

**Use Case**: Generate audio for a specific tutor message that was already sent. The backend automatically uses the session's tutor voice configuration.

**Example**:
```javascript
const response = await fetch(
  `/api/v1/chat/sessions/${sessionId}/messages/${messageId}/audio?speed=0.9`,
  {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${accessToken}`
    }
  }
);

if (response.ok) {
  const audioBlob = await response.blob();
  const audioUrl = URL.createObjectURL(audioBlob);
  const audio = new Audio(audioUrl);
  audio.play();
}
```

---

### 3. Synthesize Arbitrary Text
**Endpoint**: `POST /api/v1/chat/synthesize`

**Headers**:
```http
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**Request Body**:
```json
{
  "text": "Hola, ¿cómo estás? Me llamo María.",
  "voiceId": "Warm",
  "speed": 1.0
}
```

**Response**: Binary audio data (audio/mpeg format)

**Use Case**: Synthesize any text with a specific voice (e.g., vocabulary words, example sentences, user-typed practice text).

---

## Recommended UI/UX Implementation

### 1. Message Audio Playback

**Requirements**:
- Add a speaker icon 🔊 next to each tutor message (ASSISTANT role only)
- Icon should be visible on hover or always visible on mobile
- Show loading state while audio is being generated
- Show playing state with animation while audio plays
- Support playback speed control (0.75x, 1.0x, 1.25x)

**Example Component Structure**:
```tsx
interface MessageAudioButtonProps {
  sessionId: string;
  messageId: string;
  onError?: (error: Error) => void;
}

function MessageAudioButton({ sessionId, messageId, onError }: MessageAudioButtonProps) {
  const [loading, setLoading] = useState(false);
  const [playing, setPlaying] = useState(false);
  const [speed, setSpeed] = useState(1.0);
  const audioRef = useRef<HTMLAudioElement | null>(null);

  const playAudio = async () => {
    if (playing) {
      audioRef.current?.pause();
      return;
    }

    setLoading(true);
    try {
      const response = await fetch(
        `/api/v1/chat/sessions/${sessionId}/messages/${messageId}/audio?speed=${speed}`,
        {
          method: 'POST',
          headers: { 'Authorization': `Bearer ${getAccessToken()}` }
        }
      );

      if (!response.ok) {
        throw new Error(`TTS failed: ${response.status}`);
      }

      const audioBlob = await response.blob();
      const audioUrl = URL.createObjectURL(audioBlob);

      if (audioRef.current) {
        audioRef.current.src = audioUrl;
        audioRef.current.play();
        setPlaying(true);
      }
    } catch (error) {
      onError?.(error as Error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="message-audio-controls">
      <button
        onClick={playAudio}
        disabled={loading}
        aria-label={playing ? "Pause audio" : "Play audio"}
      >
        {loading ? <Spinner /> : playing ? <PauseIcon /> : <SpeakerIcon />}
      </button>

      <SpeedControl
        value={speed}
        onChange={setSpeed}
        options={[0.75, 1.0, 1.25]}
      />

      <audio
        ref={audioRef}
        onEnded={() => setPlaying(false)}
        onPause={() => setPlaying(false)}
      />
    </div>
  );
}
```

---

### 2. Vocabulary Pronunciation

**Requirements**:
- Add speaker icon to vocabulary cards
- Allow users to hear individual words/phrases
- Use slower speed (0.75x - 0.9x) for learning

**Example**:
```tsx
function VocabularyCard({ word, translation, conceptName }: VocabCardProps) {
  const playPronunciation = async () => {
    const response = await fetch('/api/v1/chat/synthesize', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${getAccessToken()}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        text: word,
        voiceId: "Warm",  // Or get from current tutor
        speed: 0.85  // Slower for pronunciation practice
      })
    });

    if (response.ok) {
      const audioBlob = await response.blob();
      const audio = new Audio(URL.createObjectURL(audioBlob));
      audio.play();
    }
  };

  return (
    <div className="vocab-card">
      <div className="vocab-word">
        {word}
        <button onClick={playPronunciation} aria-label="Hear pronunciation">
          🔊
        </button>
      </div>
      <div className="vocab-translation">{translation}</div>
    </div>
  );
}
```

---

### 3. Auto-Play Settings

**Requirements**:
- Add user preference: "Auto-play tutor messages"
- Store preference in user settings/local storage
- Respect system "reduce motion" preferences

**Example**:
```tsx
function ChatMessage({ message, sessionId, autoPlay }: MessageProps) {
  const prefersReducedMotion = useReducedMotion();
  const shouldAutoPlay = autoPlay && !prefersReducedMotion;

  useEffect(() => {
    if (shouldAutoPlay && message.role === 'ASSISTANT') {
      playMessageAudio(sessionId, message.id);
    }
  }, [message.id, shouldAutoPlay]);

  // ... rest of component
}
```

---

### 4. Speed Control UI

**Requirements**:
- Provide speed selection: 0.75x (slower), 1.0x (normal), 1.25x (faster)
- Show speed badge next to speaker icon
- Remember user's preferred speed per session

**Example**:
```tsx
function SpeedControl({ value, onChange, options }: SpeedControlProps) {
  return (
    <div className="speed-control">
      {options.map(speed => (
        <button
          key={speed}
          onClick={() => onChange(speed)}
          className={value === speed ? 'active' : ''}
          aria-label={`Set speed to ${speed}x`}
        >
          {speed}x
        </button>
      ))}
    </div>
  );
}
```

---

## Error Handling

### Common Error Scenarios

1. **TTS Not Available (404)**
   - Provider not configured (Ollama)
   - Hide TTS UI elements gracefully

2. **Authentication Error (401)**
   - Token expired
   - Refresh token and retry

3. **Text Too Long (413)**
   - Message exceeds provider limits
   - Show error: "Message too long for audio synthesis"

4. **Rate Limit (429)**
   - Too many requests
   - Show error: "Please wait before generating more audio"

5. **Server Error (500)**
   - Temporary failure
   - Show error: "Audio generation failed. Please try again."

**Example Error Handler**:
```tsx
function handleAudioError(error: Error, response?: Response) {
  if (response?.status === 404) {
    // TTS not available - hide feature
    return { shouldHide: true, message: null };
  }

  if (response?.status === 401) {
    // Refresh token and retry
    return { shouldRetry: true, message: null };
  }

  if (response?.status === 429) {
    return {
      shouldHide: false,
      message: "Please wait a moment before generating more audio"
    };
  }

  return {
    shouldHide: false,
    message: "Could not generate audio. Please try again later."
  };
}
```

---

## Performance Best Practices

### 1. Audio Caching
```tsx
const audioCache = new Map<string, string>(); // messageId -> blob URL

async function getCachedAudio(messageId: string): Promise<string | null> {
  return audioCache.get(messageId) || null;
}

async function fetchAndCacheAudio(sessionId: string, messageId: string): Promise<string> {
  const cached = await getCachedAudio(messageId);
  if (cached) return cached;

  const response = await fetch(
    `/api/v1/chat/sessions/${sessionId}/messages/${messageId}/audio`,
    { method: 'POST', headers: { 'Authorization': `Bearer ${token}` } }
  );

  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  audioCache.set(messageId, url);

  return url;
}
```

### 2. Preload Audio
```tsx
// Preload audio for recent messages when user opens session
function preloadRecentAudio(sessionId: string, messages: Message[]) {
  const recentMessages = messages
    .filter(m => m.role === 'ASSISTANT')
    .slice(-3); // Last 3 tutor messages

  recentMessages.forEach(msg => {
    fetchAndCacheAudio(sessionId, msg.id).catch(() => {
      // Silently fail - not critical
    });
  });
}
```

### 3. Cancel In-Flight Requests
```tsx
function useAudioPlayer() {
  const abortControllerRef = useRef<AbortController | null>(null);

  const playAudio = async (sessionId: string, messageId: string) => {
    // Cancel previous request
    abortControllerRef.current?.abort();
    abortControllerRef.current = new AbortController();

    try {
      const response = await fetch(
        `/api/v1/chat/sessions/${sessionId}/messages/${messageId}/audio`,
        {
          method: 'POST',
          headers: { 'Authorization': `Bearer ${token}` },
          signal: abortControllerRef.current.signal
        }
      );
      // ... handle response
    } catch (error) {
      if (error.name === 'AbortError') {
        // Request was cancelled, ignore
        return;
      }
      throw error;
    }
  };

  return { playAudio };
}
```

---

## Accessibility Requirements

### 1. Keyboard Navigation
- All audio controls must be keyboard accessible
- Use semantic HTML buttons with proper ARIA labels
- Support spacebar to play/pause

### 2. Screen Reader Support
```tsx
<button
  onClick={playAudio}
  aria-label={`${playing ? 'Pause' : 'Play'} audio for message`}
  aria-pressed={playing}
>
  <span aria-hidden="true">🔊</span>
</button>

<div
  role="status"
  aria-live="polite"
  className="sr-only"
>
  {loading && "Loading audio..."}
  {playing && "Playing audio"}
</div>
```

### 3. Respect User Preferences
```tsx
// Check if user has disabled auto-play
const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
const autoPlayDisabled = localStorage.getItem('disableAutoPlay') === 'true';

if (!prefersReducedMotion && !autoPlayDisabled) {
  // Enable auto-play
}
```

---

## Feature Detection

**Check TTS Availability on App Load**:
```tsx
async function checkTTSAvailability(): Promise<boolean> {
  try {
    const response = await fetch('/api/v1/chat/audio/voices', {
      headers: { 'Authorization': `Bearer ${token}` }
    });

    if (!response.ok) return false;

    const data = await response.json();
    return data.voiceMappings && Object.keys(data.voiceMappings).length > 0;
  } catch {
    return false;
  }
}

// Use in provider/context
function AppProvider({ children }) {
  const [ttsAvailable, setTTSAvailable] = useState(false);

  useEffect(() => {
    checkTTSAvailability().then(setTTSAvailable);
  }, []);

  return (
    <AppContext.Provider value={{ ttsAvailable }}>
      {children}
    </AppContext.Provider>
  );
}
```

---

## Mobile Considerations

### 1. Data Usage Warning
```tsx
function TTSSettings() {
  const [wifiOnly, setWifiOnly] = useState(
    localStorage.getItem('tts-wifi-only') === 'true'
  );

  return (
    <label>
      <input
        type="checkbox"
        checked={wifiOnly}
        onChange={e => {
          setWifiOnly(e.target.checked);
          localStorage.setItem('tts-wifi-only', String(e.target.checked));
        }}
      />
      Only play audio on Wi-Fi
    </label>
  );
}
```

### 2. Background Audio
```tsx
// Request wake lock to prevent screen sleep during playback
let wakeLock: WakeLockSentinel | null = null;

async function playAudioWithWakeLock(audioUrl: string) {
  try {
    if ('wakeLock' in navigator) {
      wakeLock = await navigator.wakeLock.request('screen');
    }

    const audio = new Audio(audioUrl);
    audio.addEventListener('ended', () => {
      wakeLock?.release();
      wakeLock = null;
    });
    audio.play();
  } catch (err) {
    console.warn('Wake lock failed:', err);
  }
}
```

---

## Testing Checklist

- [ ] Audio plays correctly for tutor messages
- [ ] Playback speed controls work (0.75x, 1.0x, 1.25x)
- [ ] Vocabulary word pronunciation works
- [ ] Loading states show while fetching audio
- [ ] Playing state shows with animation
- [ ] Errors display user-friendly messages
- [ ] Feature gracefully degrades when TTS unavailable
- [ ] Audio caching works to avoid duplicate requests
- [ ] Keyboard navigation works for all controls
- [ ] Screen reader announces states correctly
- [ ] Mobile: Audio plays in background
- [ ] Mobile: Wi-Fi only setting respected
- [ ] Auto-play respects user preferences
- [ ] Multiple audio instances handled correctly (only one plays)

---

## Example: Complete Message Component

```tsx
import { useState, useRef, useEffect } from 'react';

interface ChatMessageProps {
  message: {
    id: string;
    role: 'USER' | 'ASSISTANT';
    content: string;
  };
  sessionId: string;
  autoPlay?: boolean;
}

export function ChatMessage({ message, sessionId, autoPlay }: ChatMessageProps) {
  const [loading, setLoading] = useState(false);
  const [playing, setPlaying] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [speed, setSpeed] = useState(1.0);
  const audioRef = useRef<HTMLAudioElement>(null);
  const { ttsAvailable } = useAppContext();

  useEffect(() => {
    if (autoPlay && message.role === 'ASSISTANT' && ttsAvailable) {
      playAudio();
    }
  }, [message.id, autoPlay, ttsAvailable]);

  const playAudio = async () => {
    if (playing && audioRef.current) {
      audioRef.current.pause();
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const response = await fetch(
        `/api/v1/chat/sessions/${sessionId}/messages/${message.id}/audio?speed=${speed}`,
        {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${getAccessToken()}`
          }
        }
      );

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const blob = await response.blob();
      const url = URL.createObjectURL(blob);

      if (audioRef.current) {
        audioRef.current.src = url;
        await audioRef.current.play();
        setPlaying(true);
      }
    } catch (err) {
      console.error('Audio playback failed:', err);
      setError('Could not play audio');
    } finally {
      setLoading(false);
    }
  };

  const handleAudioEnded = () => {
    setPlaying(false);
  };

  if (message.role === 'USER') {
    return <div className="user-message">{message.content}</div>;
  }

  return (
    <div className="assistant-message">
      <div className="message-content">{message.content}</div>

      {ttsAvailable && (
        <div className="audio-controls">
          <button
            onClick={playAudio}
            disabled={loading}
            aria-label={playing ? 'Pause audio' : 'Play audio'}
            className="audio-button"
          >
            {loading ? (
              <LoadingSpinner />
            ) : playing ? (
              <PauseIcon />
            ) : (
              <SpeakerIcon />
            )}
          </button>

          <div className="speed-selector">
            {[0.75, 1.0, 1.25].map(s => (
              <button
                key={s}
                onClick={() => setSpeed(s)}
                className={speed === s ? 'active' : ''}
                aria-label={`Set speed to ${s}x`}
              >
                {s}x
              </button>
            ))}
          </div>

          {error && (
            <div className="error-message" role="alert">
              {error}
            </div>
          )}

          <audio
            ref={audioRef}
            onEnded={handleAudioEnded}
            onPause={() => setPlaying(false)}
          />
        </div>
      )}
    </div>
  );
}
```

---

## Summary

The TTS feature is fully implemented on the backend. The frontend needs to:

1. **Check availability** via `/api/v1/chat/audio/voices`
2. **Add audio controls** to tutor messages with play/pause/speed
3. **Handle errors gracefully** with user-friendly messages
4. **Implement caching** to avoid duplicate audio generation
5. **Support accessibility** with keyboard navigation and screen readers
6. **Add user preferences** for auto-play and data usage

The feature should enhance learning by allowing users to hear correct pronunciation while gracefully degrading when unavailable.
