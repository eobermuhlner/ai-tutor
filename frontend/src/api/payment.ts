import apiClient from './client';

export interface CheckoutSessionResponse {
  sessionId: string;
  url: string;
}

export interface BillingPortalSessionResponse {
  url: string;
}

export interface SubscriptionStatusResponse {
  hasActiveSubscription: boolean;
  stripeSubscriptionId?: string;
  currentPeriodEnd?: string;
  cancelAtPeriodEnd: boolean;
  status: string;
}

export interface CancelSubscriptionResponse {
  subscriptionId: string;
  status: string;
  canceledAt?: string;
  cancelAtPeriodEnd: boolean;
  currentPeriodEnd?: string;
}

export async function createCheckoutSession(): Promise<CheckoutSessionResponse> {
  const response = await apiClient.post<CheckoutSessionResponse>(
    '/payment/checkout-session'
  );
  return response.data;
}

export async function createBillingPortalSession(): Promise<BillingPortalSessionResponse> {
  const response = await apiClient.post<BillingPortalSessionResponse>(
    '/payment/billing-portal'
  );
  return response.data;
}

export async function getSubscriptionStatus(): Promise<SubscriptionStatusResponse> {
  const response = await apiClient.get<SubscriptionStatusResponse>(
    '/payment/subscription-status'
  );
  return response.data;
}

export async function cancelSubscription(): Promise<CancelSubscriptionResponse> {
  const response = await apiClient.post<CancelSubscriptionResponse>(
    '/payment/cancel-subscription'
  );
  return response.data;
}
