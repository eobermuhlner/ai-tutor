import { describe, it, expect, vi, beforeEach } from 'vitest';
import { 
  getSkillBreakdown,
  triggerReassessment,
} from './assessment';
import apiClient from './client';
import type { SkillBreakdown } from '../types';

// Define the mock functions
const mockGet = vi.fn();
const mockPost = vi.fn();

// Mock the apiClient
vi.mock('./client', () => ({
  default: {
    get: mockGet,
    post: mockPost,
  }
}));

describe('assessment API module', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('getSkillBreakdown', () => {
    it('should fetch skill breakdown for a session', async () => {
      const sessionId = 'session123';
      const mockBreakdown: SkillBreakdown = {
        grammar: 'B1',
        vocabulary: 'A2',
        fluency: 'B2',
        comprehension: 'B1',
      };
      
      mockGet.mockResolvedValue({ data: mockBreakdown });
      
      const result = await getSkillBreakdown(sessionId);
      
      expect(mockGet).toHaveBeenCalledWith(`/assessment/sessions/${sessionId}/skills`);
      expect(result).toEqual(mockBreakdown);
    });
  });

  describe('triggerReassessment', () => {
    it('should trigger reassessment for a session', async () => {
      const sessionId = 'session123';
      const mockBreakdown: SkillBreakdown = {
        grammar: 'B2',
        vocabulary: 'B1',
        fluency: 'B2',
        comprehension: 'B1',
      };
      
      mockPost.mockResolvedValue({ data: mockBreakdown });
      
      const result = await triggerReassessment(sessionId);
      
      expect(mockPost).toHaveBeenCalledWith(`/assessment/sessions/${sessionId}/reassess`);
      expect(result).toEqual(mockBreakdown);
    });
  });
});