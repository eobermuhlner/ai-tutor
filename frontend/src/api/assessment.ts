import apiClient from './client';
import type { SkillBreakdown } from '../types';

export async function getSkillBreakdown(sessionId: string): Promise<SkillBreakdown> {
  const response = await apiClient.get(`/assessment/sessions/${sessionId}/skills`);
  return response.data;
}

export async function triggerReassessment(sessionId: string): Promise<SkillBreakdown> {
  const response = await apiClient.post(`/assessment/sessions/${sessionId}/reassess`);
  return response.data;
}
