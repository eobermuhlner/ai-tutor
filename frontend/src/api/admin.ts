import apiClient from './client';
import type { User } from '../types';

export interface UpdateSubscriptionPlanRequest {
  subscriptionPlan: 'FREE' | 'FREE_BYOK' | 'SUBSCRIPTION_10';
}

export async function updateUserSubscriptionPlan(
  userId: string,
  subscriptionPlan: 'FREE' | 'FREE_BYOK' | 'SUBSCRIPTION_10'
): Promise<User> {
  const response = await apiClient.patch<User>(
    `/admin/users/${userId}/subscription-plan`,
    { subscriptionPlan }
  );
  return response.data;
}
