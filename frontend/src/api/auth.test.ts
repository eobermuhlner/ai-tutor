import { describe, it, expect, vi, beforeEach } from 'vitest';
import { 
  register,
  login,
  refreshToken,
  getMe,
  logout,
  changePassword,
  changeEmail,
} from './auth';
import { PronunciationPreference } from '../types';
import type { User } from '../types';

// Mock the apiClient
const mockGet = vi.fn();
const mockPost = vi.fn();
const mockPut = vi.fn();

vi.mock('./client', () => ({
  default: {
    get: mockGet,
    post: mockPost,
    put: mockPut,
  }
}));

describe('auth API module', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('register', () => {
    it('should register a new user', async () => {
      const mockUser: User = {
        id: '1',
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
      
      mockPost.mockResolvedValue({ data: mockUser });
      
      const result = await register('testuser', 'test@example.com', 'password123');
      
      expect(mockPost).toHaveBeenCalledWith('/auth/register', {
        username: 'testuser',
        email: 'test@example.com',
        password: 'password123',
      });
      expect(result).toEqual(mockUser);
    });
  });

  describe('login', () => {
    it('should login user and return tokens', async () => {
      const loginResponse = {
        accessToken: 'access-token',
        refreshToken: 'refresh-token',
        tokenType: 'Bearer',
        expiresIn: 3600,
        user: {
          id: '1',
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
        }
      };
      
      mockPost.mockResolvedValue({ data: loginResponse });
      
      const result = await login('test@example.com', 'password123');
      
      expect(mockPost).toHaveBeenCalledWith('/auth/login', {
        username: 'test@example.com', // Backend expects 'username' field (can be username or email)
        password: 'password123',
      });
      expect(result).toEqual(loginResponse);
    });
  });

  describe('refreshToken', () => {
    it('should refresh access token', async () => {
      const refreshResponse = {
        accessToken: 'new-access-token',
        refreshToken: 'new-refresh-token',
        tokenType: 'Bearer',
        expiresIn: 3600,
        user: {
          id: '1',
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
        }
      };
      
      mockPost.mockResolvedValue({ data: refreshResponse });
      
      const result = await refreshToken('old-refresh-token');
      
      expect(mockPost).toHaveBeenCalledWith('/auth/refresh', {
        refreshToken: 'old-refresh-token',
      });
      expect(result).toEqual(refreshResponse);
    });
  });

  describe('getMe', () => {
    it('should get current user details', async () => {
      const mockUser: User = {
        id: '1',
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
      
      mockGet.mockResolvedValue({ data: mockUser });
      
      const result = await getMe();
      
      expect(mockGet).toHaveBeenCalledWith('/auth/me');
      expect(result).toEqual(mockUser);
    });
  });

  describe('logout', () => {
    it('should logout the user', async () => {
      mockPost.mockResolvedValue({});
      
      await logout();
      
      expect(mockPost).toHaveBeenCalledWith('/auth/logout');
    });
  });

  describe('changePassword', () => {
    it('should change user password', async () => {
      mockPost.mockResolvedValue({});
      
      await changePassword('current-password', 'new-password');
      
      expect(mockPost).toHaveBeenCalledWith('/auth/password', {
        currentPassword: 'current-password',
        newPassword: 'new-password',
      });
    });
  });

  describe('changeEmail', () => {
    it('should change user email', async () => {
      const mockUser: User = {
        id: '1',
        username: 'testuser',
        email: 'newemail@example.com',
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
      
      mockPost.mockResolvedValue({ data: mockUser });
      
      const result = await changeEmail('newemail@example.com');
      
      expect(mockPost).toHaveBeenCalledWith('/auth/email', {
        newEmail: 'newemail@example.com',
      });
      expect(result).toEqual(mockUser);
    });
  });
});