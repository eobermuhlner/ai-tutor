import { createContext, useContext, useState, useEffect, useRef } from 'react';
import type { ReactNode } from 'react';
import { getAvailableVoices, synthesizeMessageAudio, synthesizeText } from '../api/tts';
import type { VoicesResponse, TTSPreferences } from '../types';

interface TTSContextType {
  available: boolean;
  loading: boolean;
  voices: VoicesResponse | null;
  preferences: TTSPreferences;
  updatePreferences: (prefs: Partial<TTSPreferences>) => void;
  playMessageAudio: (sessionId: string, messageId: string, speed?: number) => Promise<void>;
  playText: (text: string, voiceId: string, speed?: number) => Promise<void>;
  stopAudio: () => void;
  isPlaying: boolean;
  currentMessageId: string | null;
}

const TTSContext = createContext<TTSContextType | undefined>(undefined);

const DEFAULT_PREFERENCES: TTSPreferences = {
  autoPlay: false,
  defaultSpeed: 1.0,
  wifiOnly: false,
  enabled: true,
};

const TTS_PREFS_KEY = 'tts-preferences';

function loadPreferences(): TTSPreferences {
  try {
    const stored = localStorage.getItem(TTS_PREFS_KEY);
    if (stored) {
      return { ...DEFAULT_PREFERENCES, ...JSON.parse(stored) };
    }
  } catch (error) {
    console.error('Failed to load TTS preferences:', error);
  }
  return DEFAULT_PREFERENCES;
}

function savePreferences(prefs: TTSPreferences): void {
  try {
    localStorage.setItem(TTS_PREFS_KEY, JSON.stringify(prefs));
  } catch (error) {
    console.error('Failed to save TTS preferences:', error);
  }
}

export function TTSProvider({ children }: { children: ReactNode }) {
  const [available, setAvailable] = useState(false);
  const [loading, setLoading] = useState(true);
  const [voices, setVoices] = useState<VoicesResponse | null>(null);
  const [preferences, setPreferences] = useState<TTSPreferences>(loadPreferences);
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentMessageId, setCurrentMessageId] = useState<string | null>(null);

  const audioRef = useRef<HTMLAudioElement | null>(null);
  const audioUrlRef = useRef<string | null>(null);

  // Check TTS availability on mount
  useEffect(() => {
    const checkAvailability = async () => {
      try {
        const voicesData = await getAvailableVoices();
        if (voicesData && Object.keys(voicesData.voiceMappings).length > 0) {
          setVoices(voicesData);
          setAvailable(true);
        } else {
          setAvailable(false);
        }
      } catch (error) {
        console.error('Failed to check TTS availability:', error);
        setAvailable(false);
      } finally {
        setLoading(false);
      }
    };

    checkAvailability();
  }, []);

  // Cleanup audio on unmount
  useEffect(() => {
    return () => {
      if (audioRef.current) {
        audioRef.current.pause();
        audioRef.current = null;
      }
      if (audioUrlRef.current) {
        URL.revokeObjectURL(audioUrlRef.current);
        audioUrlRef.current = null;
      }
    };
  }, []);

  const updatePreferences = (prefs: Partial<TTSPreferences>) => {
    const updated = { ...preferences, ...prefs };
    setPreferences(updated);
    savePreferences(updated);
  };

  const stopAudio = () => {
    if (audioRef.current) {
      audioRef.current.pause();
      audioRef.current.currentTime = 0;
    }
    setIsPlaying(false);
    setCurrentMessageId(null);
    if (audioUrlRef.current) {
      URL.revokeObjectURL(audioUrlRef.current);
      audioUrlRef.current = null;
    }
  };

  const playMessageAudio = async (
    sessionId: string,
    messageId: string,
    speed?: number
  ): Promise<void> => {
    if (!available || !preferences.enabled) {
      return;
    }

    // Check Wi-Fi only preference
    if (preferences.wifiOnly && 'connection' in navigator) {
      const connection = (navigator as any).connection;
      if (connection && connection.type !== 'wifi') {
        return; // Don't play audio when not on Wi-Fi if wifiOnly is enabled
      }
    }
    
    // If already playing this message, stop it
    if (isPlaying && currentMessageId === messageId) {
      stopAudio();
      return;
    }

    // Stop any currently playing audio
    stopAudio();

    try {
      const playbackSpeed = speed ?? preferences.defaultSpeed;
      const audioBlob = await synthesizeMessageAudio(sessionId, messageId, playbackSpeed);
      const audioUrl = URL.createObjectURL(audioBlob);
      audioUrlRef.current = audioUrl;

      // Create or reuse audio element
      if (!audioRef.current) {
        audioRef.current = new Audio();
      }

      audioRef.current.src = audioUrl;
      audioRef.current.playbackRate = playbackSpeed;

      audioRef.current.onended = () => {
        setIsPlaying(false);
        setCurrentMessageId(null);
        if (audioUrlRef.current) {
          URL.revokeObjectURL(audioUrlRef.current);
          audioUrlRef.current = null;
        }
      };

      audioRef.current.onpause = () => {
        setIsPlaying(false);
      };

      audioRef.current.onplay = () => {
        setIsPlaying(true);
        setCurrentMessageId(messageId);
      };

      await audioRef.current.play();
    } catch (error: any) {
      console.error('Failed to play message audio:', error);
      setIsPlaying(false);
      setCurrentMessageId(null);

      // Don't throw on 404 (TTS not available for this provider)
      if (error.response?.status !== 404) {
        throw error;
      }
    }
  };

  const playText = async (
    text: string,
    voiceId: string,
    speed?: number
  ): Promise<void> => {
    if (!available || !preferences.enabled) {
      return;
    }

    // Check Wi-Fi only preference
    if (preferences.wifiOnly && 'connection' in navigator) {
      const connection = (navigator as any).connection;
      if (connection && connection.type !== 'wifi') {
        return; // Don't play audio when not on Wi-Fi if wifiOnly is enabled
      }
    }
    
    // Stop any currently playing audio
    stopAudio();

    try {
      const playbackSpeed = speed ?? preferences.defaultSpeed;
      const audioBlob = await synthesizeText(text, voiceId, playbackSpeed);
      const audioUrl = URL.createObjectURL(audioBlob);
      audioUrlRef.current = audioUrl;

      // Create or reuse audio element
      if (!audioRef.current) {
        audioRef.current = new Audio();
      }

      audioRef.current.src = audioUrl;
      audioRef.current.playbackRate = playbackSpeed;

      audioRef.current.onended = () => {
        setIsPlaying(false);
        if (audioUrlRef.current) {
          URL.revokeObjectURL(audioUrlRef.current);
          audioUrlRef.current = null;
        }
      };

      audioRef.current.onpause = () => {
        setIsPlaying(false);
      };

      audioRef.current.onplay = () => {
        setIsPlaying(true);
      };

      await audioRef.current.play();
    } catch (error: any) {
      console.error('Failed to play text audio:', error);
      setIsPlaying(false);

      // Don't throw on 404 (TTS not available for this provider)
      if (error.response?.status !== 404) {
        throw error;
      }
    }
  };

  return (
    <TTSContext.Provider
      value={{
        available,
        loading,
        voices,
        preferences,
        updatePreferences,
        playMessageAudio,
        playText,
        stopAudio,
        isPlaying,
        currentMessageId,
      }}
    >
      {children}
    </TTSContext.Provider>
  );
}

export function useTTS() {
  const context = useContext(TTSContext);
  if (context === undefined) {
    throw new Error('useTTS must be used within a TTSProvider');
  }
  return context;
}
