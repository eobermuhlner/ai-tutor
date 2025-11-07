import { describe, it, expect, vi, beforeEach } from 'vitest';
import { 
  createCheckoutSession,
  createBillingPortalSession,
  getSubscriptionStatus,
  cancelSubscription,
  type CheckoutSessionResponse,
  type BillingPortalSessionResponse,
  type SubscriptionStatusResponse,
  type CancelSubscriptionResponse
} from './payment';
import apiClient from './client';

// Mock the apiClient
vi.mock('./client');

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const mockApiClient = apiClient as any as {
  post: typeof vi.fn;
  get: typeof vi.fn;
};

describe('payment API module', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('createCheckoutSession', () => {
    it('should create a checkout session', async () => {
      const mockResponse: CheckoutSessionResponse = {
        sessionId: 'cs_test_123',
        url: 'https://checkout.stripe.com/c/pay/test_123'
      };
      
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      (mockApiClient.post as any).mockResolvedValue({ data: mockResponse });
      
      const result = await createCheckoutSession();
      
      expect(mockApiClient.post).toHaveBeenCalledWith('/payment/checkout-session');
      expect(result).toEqual(mockResponse);
    });
  });

  describe('createBillingPortalSession', () => {
    it('should create a billing portal session', async () => {
      const mockResponse: BillingPortalSessionResponse = {
        url: 'https://billing.stripe.com/test_123'
      };
      
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      (mockApiClient.post as any).mockResolvedValue({ data: mockResponse });
      
      const result = await createBillingPortalSession();
      
      expect(mockApiClient.post).toHaveBeenCalledWith('/payment/billing-portal');
      expect(result).toEqual(mockResponse);
    });
  });

  describe('getSubscriptionStatus', () => {
    it('should get subscription status for active subscription', async () => {
      const mockResponse: SubscriptionStatusResponse = {
        hasActiveSubscription: true,
        stripeSubscriptionId: 'sub_123',
        currentPeriodEnd: '2023-12-31T23:59:59.000Z',
        cancelAtPeriodEnd: false,
        status: 'active'
      };
      
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      (mockApiClient.get as any).mockResolvedValue({ data: mockResponse });
      
      const result = await getSubscriptionStatus();
      
      expect(mockApiClient.get).toHaveBeenCalledWith('/payment/subscription-status');
      expect(result).toEqual(mockResponse);
    });

    it('should get subscription status for canceled subscription', async () => {
      const mockResponse: SubscriptionStatusResponse = {
        hasActiveSubscription: false,
        stripeSubscriptionId: 'sub_123',
        currentPeriodEnd: '2023-12-31T23:59:59.000Z',
        cancelAtPeriodEnd: true,
        status: 'canceled'
      };
      
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      (mockApiClient.get as any).mockResolvedValue({ data: mockResponse });
      
      const result = await getSubscriptionStatus();
      
      expect(mockApiClient.get).toHaveBeenCalledWith('/payment/subscription-status');
      expect(result).toEqual(mockResponse);
    });
  });

  describe('cancelSubscription', () => {
    it('should cancel subscription', async () => {
      const mockResponse: CancelSubscriptionResponse = {
        subscriptionId: 'sub_123',
        status: 'canceled',
        canceledAt: '2023-11-03T10:00:00.000Z',
        cancelAtPeriodEnd: false
      };
      
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      (mockApiClient.post as any).mockResolvedValue({ data: mockResponse });
      
      const result = await cancelSubscription();
      
      expect(mockApiClient.post).toHaveBeenCalledWith('/payment/cancel-subscription');
      expect(result).toEqual(mockResponse);
    });

    it('should schedule subscription cancellation', async () => {
      const mockResponse: CancelSubscriptionResponse = {
        subscriptionId: 'sub_123',
        status: 'active',
        cancelAtPeriodEnd: true,
        currentPeriodEnd: '2023-12-31T23:59:59.000Z'
      };
      
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      (mockApiClient.post as any).mockResolvedValue({ data: mockResponse });
      
      const result = await cancelSubscription();
      
      expect(mockApiClient.post).toHaveBeenCalledWith('/payment/cancel-subscription');
      expect(result).toEqual(mockResponse);
    });
  });
});