import apiClient from './client';

export enum LlmProvider {
  OPENAI = 'OPENAI',
  AZURE_OPENAI = 'AZURE_OPENAI',
  ANTHROPIC = 'ANTHROPIC',
  OLLAMA = 'OLLAMA',
  SYSTEM_DEFAULT = 'SYSTEM_DEFAULT',
}

export interface ApiKeyConfiguration {
  hasApiKey: boolean;
  requiresEndpoint: boolean;
  endpoint: string | null;
  activeProvider: LlmProvider;
}

export async function getApiKeyConfiguration(): Promise<ApiKeyConfiguration> {
  const response = await apiClient.get<ApiKeyConfiguration>('/users/me/api-key');
  return response.data;
}

export async function setApiKey(
  apiKey: string,
  endpoint?: string
): Promise<void> {
  await apiClient.put('/users/me/api-key', { apiKey, endpoint });
}

export async function removeApiKey(): Promise<void> {
  await apiClient.delete('/users/me/api-key');
}
