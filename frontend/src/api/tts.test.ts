import { describe, it, expect, vi, beforeEach } from 'vitest';
import { 
  getAvailableVoices,
  synthesizeMessageAudio,
  synthesizeText,
} from './tts';
import type { VoicesResponse, SynthesizeRequest } from '../types';

// Use vi.hoisted to properly handle the hoisting issue
const { mockGet, mockPost } = vi.hoisted(() => {
  return {
    mockGet: vi.fn(),
    mockPost: vi.fn(),
  };
});

vi.mock('./client', () => {
  return {
    default: {
      get: mockGet,
      post: mockPost,
    }
  };
});

describe('tts API module', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('getAvailableVoices', () => {
    it('should fetch available voices', async () => {
      const mockVoices: VoicesResponse = {
        abstractVoices: ['Warm', 'Professional', 'Energetic', 'Calm', 'Authoritative', 'Friendly'],
        voiceMappings: {
          'Warm': 'voice_warm_123',
          'Professional': 'voice_professional_456'
        },
        defaultVoice: 'Warm'
      };
      
      mockGet.mockResolvedValue({ data: mockVoices });
      
      const result = await getAvailableVoices();
      
      expect(mockGet).toHaveBeenCalledWith('/chat/audio/voices');
      expect(result).toEqual(mockVoices);
    });

    it('should return null when TTS is not available (404)', async () => {
      const error404 = { response: { status: 404 } };
      mockGet.mockRejectedValue(error404);
      
      const result = await getAvailableVoices();
      
      expect(mockGet).toHaveBeenCalledWith('/chat/audio/voices');
      expect(result).toBeNull();
    });

    it('should re-throw other errors', async () => {
      const error500 = { response: { status: 500 } };
      mockGet.mockRejectedValue(error500);
      
      await expect(getAvailableVoices()).rejects.toEqual(error500);
    });
  });

  describe('synthesizeMessageAudio', () => {
    it('should synthesize audio for a message with default speed', async () => {
      const sessionId = 'session1';
      const messageId = 'message1';
      const speed = 1.0;
      const mockBlob = new Blob(['audio data'], { type: 'audio/mpeg' });
      
      mockPost.mockResolvedValue({ data: mockBlob });
      
      const result = await synthesizeMessageAudio(sessionId, messageId, speed);
      
      expect(mockPost).toHaveBeenCalledWith(
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
      
      mockPost.mockResolvedValue({ data: mockBlob });
      
      const result = await synthesizeMessageAudio(sessionId, messageId, speed);
      
      expect(mockPost).toHaveBeenCalledWith(
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
      
      mockPost.mockResolvedValue({ data: mockBlob });
      
      const result = await synthesizeText(text, voiceId, speed);
      
      const expectedRequest: SynthesizeRequest = {
        text,
        voiceId,
        speed,
      };
      
      expect(mockPost).toHaveBeenCalledWith(
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
      
      mockPost.mockResolvedValue({ data: mockBlob });
      
      const result = await synthesizeText(text, voiceId, speed);
      
      const expectedRequest: SynthesizeRequest = {
        text,
        voiceId,
        speed,
      };
      
      expect(mockPost).toHaveBeenCalledWith(
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