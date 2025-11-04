import { describe, it, expect, vi, beforeEach } from 'vitest';
import { 
  getVocabulary,
  getVocabularyItem,
  getDueVocabulary,
  getDueCount,
  recordReview,
  type VocabularyItemWithContexts
} from './vocabulary';
import apiClient from './client';

// Mock the apiClient
vi.mock('./client');

const mockApiClient = apiClient as { 
  get: typeof vi.fn;
  post: typeof vi.fn;
};

describe('vocabulary API module', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('getVocabulary', () => {
    it('should fetch user vocabulary without language filter', async () => {
      const userId = 'user123';
      const mockVocabulary = [
        {
          id: 'vocab1',
          userId: userId,
          word: 'hello',
          languageCode: 'en',
          targetLanguageCode: 'es',
          translation: 'hola',
          partOfSpeech: 'noun',
          frequency: 10,
          difficulty: 1,
          nextReviewAt: new Date().toISOString(),
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        }
      ];
      
      (mockApiClient.get as any).mockResolvedValue({ data: mockVocabulary });
      
      const result = await getVocabulary(userId);
      
      expect(mockApiClient.get).toHaveBeenCalledWith(`/vocabulary?userId=${userId}`);
      expect(result).toEqual(mockVocabulary);
    });

    it('should fetch user vocabulary with language filter', async () => {
      const userId = 'user123';
      const language = 'es';
      const mockVocabulary = [
        {
          id: 'vocab2',
          userId: userId,
          word: 'gracias',
          languageCode: 'es',
          targetLanguageCode: 'en',
          translation: 'thanks',
          partOfSpeech: 'noun',
          frequency: 15,
          difficulty: 0.5,
          nextReviewAt: new Date().toISOString(),
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        }
      ];
      
      (mockApiClient.get as any).mockResolvedValue({ data: mockVocabulary });
      
      const result = await getVocabulary(userId, language);
      
      expect(mockApiClient.get).toHaveBeenCalledWith(`/vocabulary?userId=${userId}&lang=${language}`);
      expect(result).toEqual(mockVocabulary);
    });
  });

  describe('getVocabularyItem', () => {
    it('should fetch a specific vocabulary item with contexts', async () => {
      const itemId = 'vocab1';
      const mockItem: VocabularyItemWithContexts = {
        id: itemId,
        userId: 'user123',
        word: 'hello',
        languageCode: 'en',
        targetLanguageCode: 'es',
        translation: 'hola',
        partOfSpeech: 'noun',
        frequency: 10,
        difficulty: 1,
        nextReviewAt: new Date().toISOString(),
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        contexts: [
          {
            id: 'context1',
            vocabularyId: itemId,
            source: 'Hello world',
            target: 'Hola mundo',
            createdAt: new Date().toISOString(),
          }
        ]
      };
      
      (mockApiClient.get as any).mockResolvedValue({ data: mockItem });
      
      const result = await getVocabularyItem(itemId);
      
      expect(mockApiClient.get).toHaveBeenCalledWith(`/vocabulary/${itemId}`);
      expect(result).toEqual(mockItem);
    });
  });

  describe('getDueVocabulary', () => {
    it('should fetch due vocabulary with default limit', async () => {
      const lang = 'es';
      const mockVocabulary = [
        {
          id: 'vocab1',
          userId: 'user123',
          word: 'gracias',
          languageCode: 'es',
          targetLanguageCode: 'en',
          translation: 'thanks',
          partOfSpeech: 'noun',
          frequency: 15,
          difficulty: 0.5,
          nextReviewAt: new Date().toISOString(),
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        }
      ];
      
      (mockApiClient.get as any).mockResolvedValue({ data: mockVocabulary });
      
      const result = await getDueVocabulary(lang);
      
      expect(mockApiClient.get).toHaveBeenCalledWith(`/vocabulary/due?lang=${lang}&limit=20`);
      expect(result).toEqual(mockVocabulary);
    });

    it('should fetch due vocabulary with custom limit', async () => {
      const lang = 'fr';
      const limit = 10;
      const mockVocabulary = [
        {
          id: 'vocab2',
          userId: 'user123',
          word: 'merci',
          languageCode: 'fr',
          targetLanguageCode: 'en',
          translation: 'thank you',
          partOfSpeech: 'noun',
          frequency: 12,
          difficulty: 0.8,
          nextReviewAt: new Date().toISOString(),
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        }
      ];
      
      (mockApiClient.get as any).mockResolvedValue({ data: mockVocabulary });
      
      const result = await getDueVocabulary(lang, limit);
      
      expect(mockApiClient.get).toHaveBeenCalledWith(`/vocabulary/due?lang=${lang}&limit=${limit}`);
      expect(result).toEqual(mockVocabulary);
    });
  });

  describe('getDueCount', () => {
    it('should fetch due vocabulary count', async () => {
      const lang = 'de';
      const mockCount = { count: 5 };
      
      (mockApiClient.get as any).mockResolvedValue({ data: mockCount });
      
      const result = await getDueCount(lang);
      
      expect(mockApiClient.get).toHaveBeenCalledWith(`/vocabulary/due/count?lang=${lang}`);
      expect(result).toEqual(mockCount);
    });
  });

  describe('recordReview', () => {
    it('should record vocabulary review success', async () => {
      const itemId = 'vocab1';
      const success = true;
      const mockVocabulary = {
        id: itemId,
        userId: 'user123',
        word: 'hello',
        languageCode: 'en',
        targetLanguageCode: 'es',
        translation: 'hola',
        partOfSpeech: 'noun',
        frequency: 10,
        difficulty: 0.9,
        nextReviewAt: new Date().toISOString(),
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };
      
      (mockApiClient.post as any).mockResolvedValue({ data: mockVocabulary });
      
      const result = await recordReview(itemId, success);
      
      expect(mockApiClient.post).toHaveBeenCalledWith(`/vocabulary/${itemId}/review`, { success });
      expect(result).toEqual(mockVocabulary);
    });

    it('should record vocabulary review failure', async () => {
      const itemId = 'vocab1';
      const success = false;
      const mockVocabulary = {
        id: itemId,
        userId: 'user123',
        word: 'hello',
        languageCode: 'en',
        targetLanguageCode: 'es',
        translation: 'hola',
        partOfSpeech: 'noun',
        frequency: 10,
        difficulty: 1.2,
        nextReviewAt: new Date().toISOString(),
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };
      
      (mockApiClient.post as any).mockResolvedValue({ data: mockVocabulary });
      
      const result = await recordReview(itemId, success);
      
      expect(mockApiClient.post).toHaveBeenCalledWith(`/vocabulary/${itemId}/review`, { success });
      expect(result).toEqual(mockVocabulary);
    });
  });
});