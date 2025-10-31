import apiClient from './client';
import type {
  SessionSummaryInfo,
  SummaryDetail,
  GlobalSummaryStats,
} from '../types';

/**
 * Get summary statistics for a session.
 * Accessible by: session owner OR admin
 */
export async function getSessionSummaryInfo(
  sessionId: string
): Promise<SessionSummaryInfo> {
  const response = await apiClient.get<SessionSummaryInfo>(
    `/summaries/sessions/${sessionId}/info`
  );
  return response.data;
}

/**
 * Get detailed view of all summaries for a session (admin only).
 */
export async function getSessionSummaryDetails(
  sessionId: string
): Promise<SummaryDetail[]> {
  const response = await apiClient.get<SummaryDetail[]>(
    `/summaries/sessions/${sessionId}/details`
  );
  return response.data;
}

/**
 * Trigger manual summarization for a session (admin only).
 */
export async function triggerSummarization(
  sessionId: string
): Promise<{ status: string; message: string }> {
  const response = await apiClient.post<{ status: string; message: string }>(
    `/summaries/sessions/${sessionId}/trigger`
  );
  return response.data;
}

/**
 * Get global summarization statistics (admin only).
 */
export async function getGlobalStats(): Promise<GlobalSummaryStats> {
  const response = await apiClient.get<GlobalSummaryStats>(
    '/summaries/stats'
  );
  return response.data;
}
