import apiClient from './client';
import type { ErrorPattern, ErrorTrendResponse, ErrorSample } from '../types';

export async function getErrorPatterns(
  lang: string,
  limit: number = 5
): Promise<ErrorPattern[]> {
  const params = new URLSearchParams({ lang, limit: limit.toString() });
  const response = await apiClient.get(`/analytics/errors/patterns?${params.toString()}`);
  return response.data;
}

export async function getErrorTrend(
  errorType: string,
  lang: string
): Promise<ErrorTrendResponse> {
  const params = new URLSearchParams({ lang });
  const response = await apiClient.get(`/analytics/errors/trends/${errorType}?${params.toString()}`);
  return response.data;
}

export async function getRecentErrorSamples(
  limit: number = 20
): Promise<ErrorSample[]> {
  const params = new URLSearchParams({ limit: limit.toString() });
  const response = await apiClient.get(`/analytics/errors/samples?${params.toString()}`);
  return response.data;
}
