import { describe, it, expect, vi, beforeEach } from 'vitest';
import { 
  getErrorPatterns,
  getErrorTrend,
  getRecentErrorSamples,
} from './analytics';
import type { ErrorPattern, ErrorTrend, ErrorTrendResponse, ErrorSample } from '../types';

// Use vi.hoisted to properly handle the hoisting issue
const { mockGet } = vi.hoisted(() => {
  return {
    mockGet: vi.fn(),
  };
});

vi.mock('./client', () => {
  return {
    default: {
      get: mockGet,
    }
  };
});

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
          totalCount: 120,
          criticalCount: 20,
          highCount: 30,
          mediumCount: 50,
          lowCount: 20,
          weightedScore: 95.5,
          firstSeenAt: new Date().toISOString(),
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
          totalCount: 80,
          criticalCount: 10,
          highCount: 20,
          mediumCount: 35,
          lowCount: 15,
          weightedScore: 85.2,
          firstSeenAt: new Date().toISOString(),
          lastSeenAt: new Date().toISOString(),
        },
        {
          errorType: 'VERB_FORM',
          totalCount: 65,
          criticalCount: 15,
          highCount: 20,
          mediumCount: 25,
          lowCount: 5,
          weightedScore: 75.8,
          firstSeenAt: new Date().toISOString(),
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
        trend: 'IMPROVING' as ErrorTrend,
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
        trend: 'WORSENING' as ErrorTrend,
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
          errorType: 'VERB_FORM',
          severity: 'HIGH',
          errorSpan: 'goes',
          occurredAt: new Date().toISOString(),
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
          errorType: 'VERB_FORM',
          severity: 'HIGH',
          errorSpan: 'goes',
          occurredAt: new Date().toISOString(),
        },
        {
          id: 'sample2',
          errorType: 'VERB_FORM',
          severity: 'HIGH',
          errorSpan: 'don\'t',
          occurredAt: new Date().toISOString(),
        }
      ];
      
      mockGet.mockResolvedValue({ data: mockSamples });
      
      const result = await getRecentErrorSamples(limit);
      
      expect(mockGet).toHaveBeenCalledWith(`/analytics/errors/samples?limit=${limit}`);
      expect(result).toEqual(mockSamples);
    });
  });
});