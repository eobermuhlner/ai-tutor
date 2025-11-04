import { describe, it, expect, vi, beforeEach } from 'vitest';
import { 
  getAvailableVoices,
  synthesizeMessageAudio,
  synthesizeText,
  type VoicesResponse,
  type SynthesizeRequest
} from './tts';
import apiClient from './client';

// Mock the apiClient
vi.mock('./client');

const mockApiClient = apiClient as { 
  get: typeof vi.fn;
  post: typeof vi.fn;
};

describe('tts API module', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('getAvailableVoices', () => {
    it('should fetch available voices', async () => {
      const mockVoices: VoicesResponse = {
        voices: [
          {
            id: 'voice1',
            name: 'English Female',
            language: 'en-US',
            gender: 'Female',
            previewUrl: 'https://example.com/preview.mp3'
          }
        ]
      };
      
      (mockApiClient.get as any).mockResolvedValue({ data: mockVoices });
      
      const result = await getAvailableVoices();
      
      expect(mockApiClient.get).toHaveBeenCalledWith('/chat/audio/voices');
      expect(result).toEqual(mockVoices);
    });

    it('should return null when TTS is not available (404)', async () => {
      const error404 = { response: { status: 404 } };
      (mockApiClient.get as any).mockRejectedValue(error404);
      
      const result = await getAvailableVoices();
      
      expect(mockApiClient.get).toHaveBeenCalledWith('/chat/audio/voices');
      expect(result).toBeNull();
    });

    it('should re-throw other errors', async () => {
      const error500 = { response: { status: 500 } };
      (mockApiClient.get as any).mockRejectedValue(error500);
      
      await expect(getAvailableVoices()).rejects.toEqual(error500);
    });
  });

  describe('synthesizeMessageAudio', () => {
    it('should synthesize audio for a message with default speed', async () => {
      const sessionId = 'session1';
      const messageId = 'message1';
      const speed = 1.0;
      const mockBlob = new Blob(['audio data'], { type: 'audio/mpeg' });
      
      (mockApiClient.post as any).mockResolvedValue({ data: mockBlob });
      
      const result = await synthesizeMessageAudio(sessionId, messageId, speed);
      
      expect(mockApiClient.post).toHaveBeenCalledWith(
        `/chat/sessions/${sessionId}/messages/${messageId}/audio`,
        null,
        {
          params: { speed: 1.0 },
          responseType: 'blob',
        }
      );
      expect(result).toEqual(mockBlob);
    });

    it('should synthesize audio for a message with custom speed', async () => {
      const sessionId = 'session2';
      const messageId = 'message2';
      const speed = 1.2;
      const mockBlob = new Blob(['audio data'], { type: 'audio/mpeg' });
      
      (mockApiClient.post as any).mockResolvedValue({ data: mockBlob });
      
      const result = await synthesizeMessageAudio(sessionId, messageId, speed);
      
      expect(mockApiClient.post).toHaveBeenCalledWith(
        `/chat/sessions/${sessionId}/messages/${messageId}/audio`,
        null,
        {
          params: { speed: 1.2 },
          responseType: 'blob',
        }
      );
      expect(result).toEqual(mockBlob);
    });
  });

  describe('synthesizeText', () => {
    it('should synthesize arbitrary text with a specific voice', async () => {
      const text = 'Hello, world!';
      const voiceId = 'voice1';
      const speed = 1.0;
      const mockBlob = new Blob(['audio data'], { type: 'audio/mpeg' });
      
      (mockApiClient.post as any).mockResolvedValue({ data: mockBlob });
      
      const result = await synthesizeText(text, voiceId, speed);
      
      const expectedRequest: SynthesizeRequest = {
        text,
        voiceId,
        speed,
      };
      
      expect(mockApiClient.post).toHaveBeenCalledWith(
        '/chat/synthesize',
        expectedRequest,
        {
          responseType: 'blob',
        }
      );
      expect(result).toEqual(mockBlob);
    });

    it('should synthesize text with custom speed', async () => {
      const text = 'Goodbye, world!';
      const voiceId = 'voice2';
      const speed = 0.8;
      const mockBlob = new Blob(['audio data'], { type: 'audio/mpeg' });
      
      (mockApiClient.post as any).mockResolvedValue({ data: mockBlob });
      
      const result = await synthesizeText(text, voiceId, speed);
      
      const expectedRequest: SynthesizeRequest = {
        text,
        voiceId,
        speed,
      };
      
      expect(mockApiClient.post).toHaveBeenCalledWith(
        '/chat/synthesize',
        expectedRequest,
        {
          responseType: 'blob',
        }
      );
      expect(result).toEqual(mockBlob);
    });
  });
});