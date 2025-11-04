import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { getAccessToken, getRefreshToken, setTokens, clearTokens } from './storage';

// Mock localStorage
const localStorageMock = {
  getItem: vi.fn(),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn(),
};

Object.defineProperty(window, 'localStorage', {
  value: localStorageMock,
});

describe('storage utils', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  describe('getAccessToken', () => {
    it('returns access token from localStorage', () => {
      localStorageMock.getItem.mockReturnValue('mock-access-token');
      
      const result = getAccessToken();
      
      expect(localStorageMock.getItem).toHaveBeenCalledWith('accessToken');
      expect(result).toBe('mock-access-token');
    });

    it('returns null when access token is not in localStorage', () => {
      localStorageMock.getItem.mockReturnValue(null);
      
      const result = getAccessToken();
      
      expect(result).toBeNull();
    });
  });

  describe('getRefreshToken', () => {
    it('returns refresh token from localStorage', () => {
      localStorageMock.getItem.mockReturnValue('mock-refresh-token');
      
      const result = getRefreshToken();
      
      expect(localStorageMock.getItem).toHaveBeenCalledWith('refreshToken');
      expect(result).toBe('mock-refresh-token');
    });

    it('returns null when refresh token is not in localStorage', () => {
      localStorageMock.getItem.mockReturnValue(null);
      
      const result = getRefreshToken();
      
      expect(result).toBeNull();
    });
  });

  describe('setTokens', () => {
    it('sets both access and refresh tokens in localStorage', () => {
      setTokens('access-token', 'refresh-token');
      
      expect(localStorageMock.setItem).toHaveBeenCalledWith('accessToken', 'access-token');
      expect(localStorageMock.setItem).toHaveBeenCalledWith('refreshToken', 'refresh-token');
    });
  });

  describe('clearTokens', () => {
    it('removes both access and refresh tokens from localStorage', () => {
      clearTokens();
      
      expect(localStorageMock.removeItem).toHaveBeenCalledWith('accessToken');
      expect(localStorageMock.removeItem).toHaveBeenCalledWith('refreshToken');
    });
  });
});