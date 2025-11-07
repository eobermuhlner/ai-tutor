import { describe, it, expect, vi, beforeEach } from 'vitest';
import { 
  getRateLimitStatus,
  updateUserSubscriptionPlan,
  type RateLimitStatus
} from './rateLimits';
import apiClient from './client';

// Mock the apiClient
vi.mock('./client');

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const mockApiClient = apiClient as any as {
  get: typeof vi.fn;
  patch: typeof vi.fn;
};

describe('rateLimits API module', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('getRateLimitStatus', () => {
    it('should fetch rate limit status', async () => {
      const mockStatus: RateLimitStatus = {
        availableTokens: 50,
        hourlyLimit: 100,
        dailyLimit: 1000,
        hourlyRemaining: 80,
        dailyRemaining: 950,
        hourlyResetSeconds: 3600,
        dailyResetSeconds: 86400,
        percentageUsed: 5,
        planName: 'Free Plan',
        subscriptionPlan: 'FREE'
      };
      
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      (mockApiClient.get as any).mockResolvedValue({ data: mockStatus });
      
      const result = await getRateLimitStatus();
      
      expect(mockApiClient.get).toHaveBeenCalledWith('/rate-limits/status');
      expect(result).toEqual(mockStatus);
    });
  });

  describe('updateUserSubscriptionPlan', () => {
    it('should update user subscription plan', async () => {
      const mockStatus: RateLimitStatus = {
        availableTokens: 500,
        hourlyLimit: 600,
        dailyLimit: 5000,
        hourlyRemaining: 600,
        dailyRemaining: 5000,
        hourlyResetSeconds: 3600,
        dailyResetSeconds: 86400,
        percentageUsed: 0,
        planName: 'Premium Plan',
        subscriptionPlan: 'SUBSCRIPTION_10'
      };
      
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      (mockApiClient.patch as any).mockResolvedValue({ data: mockStatus });
      
      const result = await updateUserSubscriptionPlan('SUBSCRIPTION_10');
      
      expect(mockApiClient.patch).toHaveBeenCalledWith(
        '/rate-limits/subscription-plan',
        { subscriptionPlan: 'SUBSCRIPTION_10' }
      );
      expect(result).toEqual(mockStatus);
    });

    it('should update to FREE_BYOK plan', async () => {
      const mockStatus: RateLimitStatus = {
        availableTokens: 300,
        hourlyLimit: 60,
        dailyLimit: 300,
        hourlyRemaining: 60,
        dailyRemaining: 300,
        hourlyResetSeconds: 3600,
        dailyResetSeconds: 86400,
        percentageUsed: 0,
        planName: 'Free + BYOK Plan',
        subscriptionPlan: 'FREE_BYOK'
      };
      
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      (mockApiClient.patch as any).mockResolvedValue({ data: mockStatus });
      
      const result = await updateUserSubscriptionPlan('FREE_BYOK');
      
      expect(mockApiClient.patch).toHaveBeenCalledWith(
        '/rate-limits/subscription-plan',
        { subscriptionPlan: 'FREE_BYOK' }
      );
      expect(result).toEqual(mockStatus);
    });
  });
});