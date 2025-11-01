import apiClient from './client';

export enum LlmProvider {
  OPENAI = 'OPENAI',
  AZURE_OPENAI = 'AZURE_OPENAI',
  ANTHROPIC = 'ANTHROPIC',
  SYSTEM_DEFAULT = 'SYSTEM_DEFAULT',
}

export interface ApiKeyConfiguration {
  openaiConfigured: boolean;
  azureOpenaiConfigured: boolean;
  anthropicConfigured: boolean;
  preferredProvider: LlmProvider | null;
  azureOpenaiEndpoint: string | null;
}

export async function getApiKeyConfiguration(): Promise<ApiKeyConfiguration> {
  const response = await apiClient.get<ApiKeyConfiguration>(
    '/users/me/api-keys'
  );
  return response.data;
}

export async function setOpenAiKey(apiKey: string): Promise<void> {
  await apiClient.put('/users/me/api-keys/openai', { apiKey });
}

export async function setAzureOpenAiKey(
  apiKey: string,
  endpoint: string
): Promise<void> {
  await apiClient.put('/users/me/api-keys/azure-openai', {
    apiKey,
    endpoint,
  });
}

export async function setAnthropicKey(apiKey: string): Promise<void> {
  await apiClient.put('/users/me/api-keys/anthropic', { apiKey });
}

export async function removeApiKey(provider: LlmProvider): Promise<void> {
  const providerPath = provider.toLowerCase().replace('_', '-');
  await apiClient.delete(`/users/me/api-keys/${providerPath}`);
}

export async function setPreferredProvider(
  provider: LlmProvider
): Promise<void> {
  await apiClient.put('/users/me/api-keys/preferred-provider', { provider });
}
