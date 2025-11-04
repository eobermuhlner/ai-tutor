import { describe, it, expect, vi, beforeEach } from 'vitest';
import { 
  getErrorPatterns,
  getErrorTrend,
  getRecentErrorSamples,
} from './analytics';
import apiClient from './client';
import type { ErrorPattern, ErrorTrendResponse, ErrorSample } from '../types';

// Define the mock function
const mockGet = vi.fn();

// Mock the apiClient
vi.mock('./client', () => ({
  default: {
    get: mockGet,
  }
}));

describe('analytics API module', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('getErrorPatterns', () => {
    it('should fetch error patterns for a language with default limit', async () => {
      const lang = 'es';
      const mockPatterns: ErrorPattern[] = [
        {
          errorType: 'GRAMMAR',
          errorPattern: 'missing article',
          weightedScore: 95.5,
          frequency: 120,
          lastSeenAt: new Date().toISOString(),
        }
      ];
      
      mockGet.mockResolvedValue({ data: mockPatterns });
      
      const result = await getErrorPatterns(lang);
      
      expect(mockGet).toHaveBeenCalledWith(`/analytics/errors/patterns?lang=${lang}&limit=5`);
      expect(result).toEqual(mockPatterns);
    });

    it('should fetch error patterns with custom limit', async () => {
      const lang = 'fr';
      const limit = 10;
      const mockPatterns: ErrorPattern[] = [
        {
          errorType: 'SPELLING',
          errorPattern: 'missing accent',
          weightedScore: 85.2,
          frequency: 80,
          lastSeenAt: new Date().toISOString(),
        },
        {
          errorType: 'VERB_FORM',
          errorPattern: 'wrong conjugation',
          weightedScore: 75.8,
          frequency: 65,
          lastSeenAt: new Date().toISOString(),
        }
      ];
      
      mockGet.mockResolvedValue({ data: mockPatterns });
      
      const result = await getErrorPatterns(lang, limit);
      
      expect(mockGet).toHaveBeenCalledWith(`/analytics/errors/patterns?lang=${lang}&limit=${limit}`);
      expect(result).toEqual(mockPatterns);
    });
  });

  describe('getErrorTrend', () => {
    it('should fetch error trend for a specific error type', async () => {
      const errorType = 'GRAMMAR';
      const lang = 'de';
      const mockTrend: ErrorTrendResponse = {
        errorType: errorType,
        language: lang,
        trend: 'IMPROVING',
        changePercentage: -15.5,
        period: '30d',
        previousValue: 25,
        currentValue: 21,
      };
      
      mockGet.mockResolvedValue({ data: mockTrend });
      
      const result = await getErrorTrend(errorType, lang);
      
      expect(mockGet).toHaveBeenCalledWith(`/analytics/errors/trends/${errorType}?lang=${lang}`);
      expect(result).toEqual(mockTrend);
    });

    it('should handle different trend responses', async () => {
      const errorType = 'SPELLING';
      const lang = 'it';
      const mockTrend: ErrorTrendResponse = {
        errorType: errorType,
        language: lang,
        trend: 'WORSENING',
        changePercentage: 22.3,
        period: '30d',
        previousValue: 15,
        currentValue: 18,
      };
      
      mockGet.mockResolvedValue({ data: mockTrend });
      
      const result = await getErrorTrend(errorType, lang);
      
      expect(mockGet).toHaveBeenCalledWith(`/analytics/errors/trends/${errorType}?lang=${lang}`);
      expect(result).toEqual(mockTrend);
    });
  });

  describe('getRecentErrorSamples', () => {
    it('should fetch recent error samples with default limit', async () => {
      const mockSamples: ErrorSample[] = [
        {
          id: 'sample1',
          userId: 'user123',
          sessionId: 'session1',
          messageId: 'msg1',
          originalText: 'I goes to school',
          correctedText: 'I go to school',
          errorType: 'VERB_FORM',
          timestamp: new Date().toISOString(),
        }
      ];
      
      mockGet.mockResolvedValue({ data: mockSamples });
      
      const result = await getRecentErrorSamples();
      
      expect(mockGet).toHaveBeenCalledWith('/analytics/errors/samples?limit=20');
      expect(result).toEqual(mockSamples);
    });

    it('should fetch recent error samples with custom limit', async () => {
      const limit = 5;
      const mockSamples: ErrorSample[] = [
        {
          id: 'sample1',
          userId: 'user123',
          sessionId: 'session1',
          messageId: 'msg1',
          originalText: 'I goes to school',
          correctedText: 'I go to school',
          errorType: 'VERB_FORM',
          timestamp: new Date().toISOString(),
        },
        {
          id: 'sample2',
          userId: 'user456',
          sessionId: 'session2',
          messageId: 'msg2',
          originalText: 'She don\'t like apples',
          correctedText: 'She doesn\'t like apples',
          errorType: 'VERB_FORM',
          timestamp: new Date().toISOString(),
        }
      ];
      
      mockGet.mockResolvedValue({ data: mockSamples });
      
      const result = await getRecentErrorSamples(limit);
      
      expect(mockGet).toHaveBeenCalledWith(`/analytics/errors/samples?limit=${limit}`);
      expect(result).toEqual(mockSamples);
    });
  });
});