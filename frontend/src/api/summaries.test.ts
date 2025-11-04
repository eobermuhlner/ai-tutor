import { describe, it, expect, vi, beforeEach } from 'vitest';
import { 
  getSessionSummaryInfo,
  getSessionSummaryDetails,
  triggerSummarization,
  getGlobalStats,
  type SessionSummaryInfo,
  type SummaryDetail,
  type GlobalSummaryStats
} from './summaries';
import apiClient from './client';

// Mock the apiClient
vi.mock('./client');

const mockApiClient = apiClient as { 
  get: typeof vi.fn;
  post: typeof vi.fn;
};

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
        totalSummaries: 5,
        totalCompressionRatio: 0.75,
        totalTokensSaved: 500,
        totalProcessingTime: 120,
        lastSummaryAt: new Date().toISOString(),
      };
      
      (mockApiClient.get as any).mockResolvedValue({ data: mockSummaryInfo });
      
      const result = await getSessionSummaryInfo(sessionId);
      
      expect(mockApiClient.get).toHaveBeenCalledWith(`/summaries/sessions/${sessionId}/info`);
      expect(result).toEqual(mockSummaryInfo);
    });
  });

  describe('getSessionSummaryDetails', () => {
    it('should fetch detailed summary information for a session', async () => {
      const sessionId = 'session1';
      const mockSummaryDetails: SummaryDetail[] = [
        {
          id: 'summary1',
          sessionId: sessionId,
          sourceType: 'LEVEL_1',
          sourceIds: ['msg1', 'msg2', 'msg3'],
          summaryText: 'This is a summary of the conversation so far.',
          tokenCount: 100,
          compressionRatio: 0.5,
          processingTime: 20,
          createdAt: new Date().toISOString(),
        }
      ];
      
      (mockApiClient.get as any).mockResolvedValue({ data: mockSummaryDetails });
      
      const result = await getSessionSummaryDetails(sessionId);
      
      expect(mockApiClient.get).toHaveBeenCalledWith(`/summaries/sessions/${sessionId}/details`);
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
      
      (mockApiClient.post as any).mockResolvedValue({ data: mockResponse });
      
      const result = await triggerSummarization(sessionId);
      
      expect(mockApiClient.post).toHaveBeenCalledWith(`/summaries/sessions/${sessionId}/trigger`);
      expect(result).toEqual(mockResponse);
    });
  });

  describe('getGlobalStats', () => {
    it('should fetch global summarization statistics', async () => {
      const mockGlobalStats: GlobalSummaryStats = {
        totalSessionsProcessed: 1000,
        totalSummariesCreated: 5000,
        totalTokensProcessed: 1000000,
        totalTokensSaved: 500000,
        avgCompressionRatio: 0.5,
        activeProcesses: 2,
        lastUpdated: new Date().toISOString(),
      };
      
      (mockApiClient.get as any).mockResolvedValue({ data: mockGlobalStats });
      
      const result = await getGlobalStats();
      
      expect(mockApiClient.get).toHaveBeenCalledWith('/summaries/stats');
      expect(result).toEqual(mockGlobalStats);
    });
  });
});