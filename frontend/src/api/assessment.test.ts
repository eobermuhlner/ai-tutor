import { describe, it, expect, vi, beforeEach } from 'vitest';
import { 
  getSkillBreakdown,
  triggerReassessment,
} from './assessment';
import type { SkillBreakdown } from '../types';

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

describe('assessment API module', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('getSkillBreakdown', () => {
    it('should fetch skill breakdown for a session', async () => {
      const sessionId = 'session123';
      const mockBreakdown: SkillBreakdown = {
        overall: 'B1',
        grammar: 'B1',
        vocabulary: 'A2',
        fluency: 'B2',
        comprehension: 'B1',
        lastAssessedAt: new Date().toISOString(),
        assessmentCount: 3,
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
        overall: 'B2',
        grammar: 'B2',
        vocabulary: 'B1',
        fluency: 'B2',
        comprehension: 'B1',
        lastAssessedAt: new Date().toISOString(),
        assessmentCount: 5,
      };
      
      mockPost.mockResolvedValue({ data: mockBreakdown });
      
      const result = await triggerReassessment(sessionId);
      
      expect(mockPost).toHaveBeenCalledWith(`/assessment/sessions/${sessionId}/reassess`);
      expect(result).toEqual(mockBreakdown);
    });
  });
});