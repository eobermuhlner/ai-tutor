import apiClient from './client';

export interface RateLimitStatus {
  availableTokens: number;
  hourlyLimit: number;
  dailyLimit: number;
  percentageUsed: number;
  planName: string;
  subscriptionPlan: string;
}

export async function getRateLimitStatus(): Promise<RateLimitStatus> {
  const response = await apiClient.get<RateLimitStatus>('/rate-limits/status');
  return response.data;
}
