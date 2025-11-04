import { describe, it, expect, vi, beforeEach } from 'vitest';
import { 
  getApiKeyConfiguration, 
  setApiKey, 
  removeApiKey,
  LlmProvider,
} from './apiKeys';
import apiClient from './client';
import type { ApiKeyConfiguration } from '../types';

// Define mock functions
const mockGet = vi.fn();
const mockPut = vi.fn();
const mockDelete = vi.fn();

// Mock the apiClient
vi.mock('./client', () => ({
  default: {
    get: mockGet,
    put: mockPut,
    delete: mockDelete,
  }
}));

describe('apiKeys API module', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('getApiKeyConfiguration', () => {
    it('should fetch API key configuration', async () => {
      const mockConfig: ApiKeyConfiguration = {
        hasApiKey: true,
        requiresEndpoint: false,
        endpoint: null,
        activeProvider: LlmProvider.OPENAI,
      };
      
      mockGet.mockResolvedValue({ data: mockConfig });
      
      const result = await getApiKeyConfiguration();
      
      expect(mockGet).toHaveBeenCalledWith('/users/me/api-key');
      expect(result).toEqual(mockConfig);
    });

    it('should handle different provider configurations', async () => {
      const mockConfig: ApiKeyConfiguration = {
        hasApiKey: false,
        requiresEndpoint: true,
        endpoint: 'https://example.com',
        activeProvider: LlmProvider.AZURE_OPENAI,
      };
      
      mockGet.mockResolvedValue({ data: mockConfig });
      
      const result = await getApiKeyConfiguration();
      
      expect(result).toEqual(mockConfig);
      expect(mockGet).toHaveBeenCalledWith('/users/me/api-key');
    });
  });

  describe('setApiKey', () => {
    it('should set API key without endpoint', async () => {
      const apiKey = 'test-api-key';
      
      await setApiKey(apiKey);
      
      expect(mockPut).toHaveBeenCalledWith('/users/me/api-key', { 
        apiKey,
        endpoint: undefined 
      });
    });

    it('should set API key with endpoint', async () => {
      const apiKey = 'test-api-key';
      const endpoint = 'https://api.example.com';
      
      await setApiKey(apiKey, endpoint);
      
      expect(mockPut).toHaveBeenCalledWith('/users/me/api-key', { 
        apiKey,
        endpoint 
      });
    });
  });

  describe('removeApiKey', () => {
    it('should remove API key', async () => {
      await removeApiKey();
      
      expect(mockDelete).toHaveBeenCalledWith('/users/me/api-key');
    });
  });
});