import { describe, it, expect, vi, beforeEach } from 'vitest';
import { 
  getSessionSummaryInfo,
  getSessionSummaryDetails,
  triggerSummarization,
  getGlobalStats,
} from './summaries';
import type { SessionSummaryInfo, SummaryDetail, GlobalSummaryStats } from '../types';

// Mock the apiClient
const mockGet = vi.fn();
const mockPost = vi.fn();

vi.mock('./client', () => ({
  default: {
    get: mockGet,
    post: mockPost,
  }
}));

describe('summaries API module', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('getSessionSummaryInfo', () => {
    it('should fetch summary info for a session', async () => {
      const sessionId = 'session1';
      const mockSummaryInfo: SessionSummaryInfo = {
        sessionId,
        totalMessages: 100,
        summaryLevels: [
          { level: 1, count: 5, totalTokens: 100, coveredSequences: { start: 0, end: 4 } },
          { level: 2, count: 2, totalTokens: 50, coveredSequences: { start: 5, end: 9 } },
        ],
        lastSummarizedSequence: 10,
        estimatedTokenSavings: 200,
        compressionRatio: 0.75,
      };
      
      mockGet.mockResolvedValue({ data: mockSummaryInfo });
      
      const result = await getSessionSummaryInfo(sessionId);
      
      expect(mockGet).toHaveBeenCalledWith(`/summaries/sessions/${sessionId}/info`);
      expect(result).toEqual(mockSummaryInfo);
    });
  });

  describe('getSessionSummaryDetails', () => {
    it('should fetch detailed summary information for a session', async () => {
      const sessionId = 'session1';
      const mockSummaryDetails: SummaryDetail[] = [
        {
          id: 'summary1',
          summaryLevel: 1,
          startSequence: 0,
          endSequence: 4,
          summaryText: 'This is a summary of the conversation so far.',
          tokenCount: 100,
          sourceType: 'LEVEL_1',
          sourceIds: ['msg1', 'msg2', 'msg3', 'msg4', 'msg5'],
          supersededById: null,
          isActive: true,
          createdAt: new Date().toISOString(),
        }
      ];
      
      mockGet.mockResolvedValue({ data: mockSummaryDetails });
      
      const result = await getSessionSummaryDetails(sessionId);
      
      expect(mockGet).toHaveBeenCalledWith(`/summaries/sessions/${sessionId}/details`);
      expect(result).toEqual(mockSummaryDetails);
    });
  });

  describe('triggerSummarization', () => {
    it('should trigger manual summarization for a session', async () => {
      const sessionId = 'session1';
      const mockResponse = {
        status: 'success',
        message: 'Summarization triggered successfully'
      };
      
      mockPost.mockResolvedValue({ data: mockResponse });
      
      const result = await triggerSummarization(sessionId);
      
      expect(mockPost).toHaveBeenCalledWith(`/summaries/sessions/${sessionId}/trigger`);
      expect(result).toEqual(mockResponse);
    });
  });

  describe('getGlobalStats', () => {
    it('should fetch global summarization statistics', async () => {
      const mockGlobalStats: GlobalSummaryStats = {
        totalSessions: 1000,
        totalSummaries: 5000,
        averageCompressionRatio: 0.5,
        totalTokensSaved: 500000,
      };
      
      mockGet.mockResolvedValue({ data: mockGlobalStats });
      
      const result = await getGlobalStats();
      
      expect(mockGet).toHaveBeenCalledWith('/summaries/stats');
      expect(result).toEqual(mockGlobalStats);
    });
  });
});