import apiClient from './client';
import type { VoicesResponse, SynthesizeRequest } from '../types';

/**
 * Check TTS availability and get available voices
 */
export async function getAvailableVoices(): Promise<VoicesResponse | null> {
  try {
    const response = await apiClient.get<VoicesResponse>('/chat/audio/voices');
    return response.data;
  } catch (error: any) {
    // TTS not available (404) or other error
    if (error.response?.status === 404) {
      return null;
    }
    throw error;
  }
}

/**
 * Synthesize audio for a specific message
 */
export async function synthesizeMessageAudio(
  sessionId: string,
  messageId: string,
  speed: number = 1.0
): Promise<Blob> {
  const response = await apiClient.post(
    `/chat/sessions/${sessionId}/messages/${messageId}/audio`,
    null,
    {
      params: { speed },
      responseType: 'blob',
    }
  );
  return response.data;
}

/**
 * Synthesize arbitrary text with a specific voice
 */
export async function synthesizeText(
  text: string,
  voiceId: string,
  speed: number = 1.0
): Promise<Blob> {
  const request: SynthesizeRequest = {
    text,
    voiceId,
    speed,
  };

  const response = await apiClient.post('/chat/synthesize', request, {
    responseType: 'blob',
  });
  return response.data;
}
