import apiClient from './client';

export interface RateLimitStatus {
  availableTokens: number;
  hourlyLimit: number;
  dailyLimit: number;
  hourlyRemaining: number;
  dailyRemaining: number;
  hourlyResetSeconds: number;
  dailyResetSeconds: number;
  percentageUsed: number;
  planName: string;
  subscriptionPlan: string;
}

export interface PlanLimits {
  hourlyLimit: number;
  dailyLimit: number;
}

export interface SubscriptionPlanLimits {
  free: PlanLimits;
  freeByok: PlanLimits;
  premium: PlanLimits;
}

export async function getRateLimitStatus(): Promise<RateLimitStatus> {
  const response = await apiClient.get<RateLimitStatus>('/rate-limits/status');
  return response.data;
}

export async function getPlanLimits(): Promise<SubscriptionPlanLimits> {
  const response = await apiClient.get<SubscriptionPlanLimits>('/rate-limits/plan-limits');
  return response.data;
}

export interface UpdateUserSubscriptionPlanRequest {
  subscriptionPlan: 'FREE_BYOK' | 'SUBSCRIPTION_10';
}

export async function updateUserSubscriptionPlan(
  subscriptionPlan: 'FREE_BYOK' | 'SUBSCRIPTION_10'
): Promise<RateLimitStatus> {
  const response = await apiClient.patch<RateLimitStatus>(
    '/rate-limits/subscription-plan',
    { subscriptionPlan }
  );
  return response.data;
}
