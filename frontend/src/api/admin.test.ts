import { describe, it, expect, vi, beforeEach } from 'vitest';
import { 
  updateUserSubscriptionPlan,
} from './admin';
import { PronunciationPreference } from '../types';
import type { User } from '../types';

// Define the mock type
const mockPatch = vi.fn();

vi.mock('./client', () => ({
  default: {
    patch: mockPatch,
  }
}));

describe('admin API module', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('updateUserSubscriptionPlan', () => {
    it('should update user subscription plan to FREE', async () => {
      const userId = 'user123';
      const subscriptionPlan: 'FREE' | 'FREE_BYOK' | 'SUBSCRIPTION_10' = 'FREE';
      
      const mockUser: User = {
        id: userId,
        username: 'testuser',
        email: 'test@example.com',
        firstName: null,
        lastName: null,
        roles: [],
        enabled: true,
        emailVerified: false,
        createdAt: new Date().toISOString(),
        lastLoginAt: null,
        subscriptionPlan: 'FREE',
        pronunciationPreference: 'NONE' as PronunciationPreference,
      };
      
      mockPatch.mockResolvedValue({ data: mockUser });
      
      const result = await updateUserSubscriptionPlan(userId, subscriptionPlan);
      
      expect(mockPatch).toHaveBeenCalledWith(
        `/admin/users/${userId}/subscription-plan`,
        { subscriptionPlan }
      );
      expect(result).toEqual(mockUser);
    });

    it('should update user subscription plan to SUBSCRIPTION_10', async () => {
      const userId = 'user123';
      const subscriptionPlan: 'FREE' | 'FREE_BYOK' | 'SUBSCRIPTION_10' = 'SUBSCRIPTION_10';
      
      const mockUser: User = {
        id: userId,
        username: 'testuser',
        email: 'test@example.com',
        firstName: null,
        lastName: null,
        roles: [],
        enabled: true,
        emailVerified: false,
        createdAt: new Date().toISOString(),
        lastLoginAt: null,
        subscriptionPlan: 'SUBSCRIPTION_10',
        pronunciationPreference: 'NONE' as PronunciationPreference,
      };
      
      mockPatch.mockResolvedValue({ data: mockUser });
      
      const result = await updateUserSubscriptionPlan(userId, subscriptionPlan);
      
      expect(mockPatch).toHaveBeenCalledWith(
        `/admin/users/${userId}/subscription-plan`,
        { subscriptionPlan }
      );
      expect(result).toEqual(mockUser);
    });

    it('should update user subscription plan to FREE_BYOK', async () => {
      const userId = 'user123';
      const subscriptionPlan: 'FREE' | 'FREE_BYOK' | 'SUBSCRIPTION_10' = 'FREE_BYOK';
      
      const mockUser: User = {
        id: userId,
        username: 'testuser',
        email: 'test@example.com',
        firstName: null,
        lastName: null,
        roles: [],
        enabled: true,
        emailVerified: false,
        createdAt: new Date().toISOString(),
        lastLoginAt: null,
        subscriptionPlan: 'FREE_BYOK',
        pronunciationPreference: 'NONE' as PronunciationPreference,
      };
      
      mockPatch.mockResolvedValue({ data: mockUser });
      
      const result = await updateUserSubscriptionPlan(userId, subscriptionPlan);
      
      expect(mockPatch).toHaveBeenCalledWith(
        `/admin/users/${userId}/subscription-plan`,
        { subscriptionPlan }
      );
      expect(result).toEqual(mockUser);
    });
  });
});