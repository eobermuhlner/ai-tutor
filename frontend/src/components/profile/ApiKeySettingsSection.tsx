import { useState, useEffect } from 'react';
import { Key, Check, Loader2 } from 'lucide-react';
import Button from '../ui/Button';
import Input from '../ui/Input';
import toast from 'react-hot-toast';
import {
  getApiKeyConfiguration,
  setApiKey,
  removeApiKey,
  LlmProvider,
} from '../../api/apiKeys';
import type { ApiKeyConfiguration } from '../../api/apiKeys';

export default function ApiKeySettingsSection() {
  const [config, setConfig] = useState<ApiKeyConfiguration | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // Form state
  const [apiKey, setApiKeyInput] = useState('');
  const [endpoint, setEndpointInput] = useState('');

  // Submission state
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    loadConfiguration();
  }, []);

  const loadConfiguration = async () => {
    try {
      const data = await getApiKeyConfiguration();
      setConfig(data);
      if (data.endpoint) {
        setEndpointInput(data.endpoint);
      }
    } catch (error) {
      console.error('API Key config load error:', error);
      toast.error('Failed to load API key configuration');
      setConfig({
        hasApiKey: false,
        requiresEndpoint: false,
        endpoint: null,
        activeProvider: LlmProvider.SYSTEM_DEFAULT,
      });
    } finally {
      setIsLoading(false);
    }
  };

  const handleSave = async () => {
    const provider = config?.activeProvider || LlmProvider.SYSTEM_DEFAULT;
    const needsApiKey = requiresApiKey(provider);

    // Validate API key for providers that require it
    if (needsApiKey && !apiKey.trim()) {
      toast.error('Please enter an API key');
      return;
    }

    // Validate endpoint for providers that require it
    if (config?.requiresEndpoint && !endpoint.trim()) {
      toast.error('Please enter an endpoint');
      return;
    }

    setIsSaving(true);
    try {
      await setApiKey(apiKey, config?.requiresEndpoint ? endpoint : undefined);
      toast.success(`Configuration for ${getProviderDisplayName(provider)} saved and validated successfully`);
      setApiKeyInput('');
      await loadConfiguration();
    } catch (error: any) {
      const message = error.response?.data?.error || 'Failed to save configuration';
      toast.error(message);
    } finally {
      setIsSaving(false);
    }
  };

  const handleRemove = async () => {
    if (!confirm('Are you sure you want to remove your API key?')) {
      return;
    }

    try {
      await removeApiKey();
      toast.success('API key removed successfully');
      await loadConfiguration();
    } catch (error) {
      toast.error('Failed to remove API key');
    }
  };

  const getProviderDisplayName = (provider: LlmProvider): string => {
    switch (provider) {
      case LlmProvider.OPENAI:
        return 'OpenAI';
      case LlmProvider.AZURE_OPENAI:
        return 'Azure OpenAI';
      case LlmProvider.ANTHROPIC:
        return 'Anthropic (Claude)';
      case LlmProvider.OLLAMA:
        return 'Ollama';
      default:
        return 'System Default';
    }
  };

  const getPlaceholder = (provider: LlmProvider): string => {
    switch (provider) {
      case LlmProvider.OPENAI:
        return 'sk-...';
      case LlmProvider.AZURE_OPENAI:
        return 'API Key';
      case LlmProvider.ANTHROPIC:
        return 'sk-ant-...';
      case LlmProvider.OLLAMA:
        return 'Not required (self-hosted)';
      default:
        return 'API Key';
    }
  };

  const requiresApiKey = (provider: LlmProvider): boolean => {
    return provider !== LlmProvider.OLLAMA;
  };

  if (isLoading) {
    return (
      <section className="bg-white rounded-2xl shadow-soft border border-slate-200 p-6 mb-6">
        <div className="flex items-center justify-center py-8">
          <Loader2 className="w-6 h-6 animate-spin text-brand-500" />
        </div>
      </section>
    );
  }

  return (
    <section className="bg-white rounded-2xl shadow-soft border border-slate-200 p-6 mb-6">
      <div className="flex items-center gap-3 mb-6">
        <div className="w-10 h-10 rounded-full bg-gradient-to-br from-brand-500 to-brand-600 flex items-center justify-center">
          <Key className="w-6 h-6 text-white" />
        </div>
        <h2 className="text-xl font-semibold text-slate-900">
          AI Provider Settings (BYOK)
        </h2>
      </div>

      <div className="space-y-6">
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
          <p className="text-sm text-blue-800">
            <strong>System is using: {getProviderDisplayName(config?.activeProvider || LlmProvider.SYSTEM_DEFAULT)}</strong>
          </p>
          <p className="text-sm text-blue-700 mt-1">
            Configure your own API key below to override the system default. Your API key is encrypted and never shared.
          </p>
        </div>

        <div className="border border-slate-200 rounded-lg p-4">
          <div className="flex items-center justify-between mb-3">
            <h3 className="font-semibold text-slate-900 flex items-center gap-2">
              {getProviderDisplayName(config?.activeProvider || LlmProvider.SYSTEM_DEFAULT)}
              {config?.hasApiKey && (
                <Check className="w-4 h-4 text-green-600" />
              )}
            </h3>
            {config?.hasApiKey && (
              <Button variant="outline" size="sm" onClick={handleRemove}>
                Remove
              </Button>
            )}
          </div>

          {config?.hasApiKey ? (
            <div className="space-y-2">
              <p className="text-sm text-green-600 flex items-center gap-2">
                <Check className="w-4 h-4" />
                API key configured
              </p>
              {config.endpoint && (
                <p className="text-sm text-slate-500">Endpoint: {config.endpoint}</p>
              )}
            </div>
          ) : (
            <div className="space-y-3">
              {config?.requiresEndpoint && (
                <Input
                  type="text"
                  placeholder={
                    config.activeProvider === LlmProvider.OLLAMA
                      ? 'http://localhost:11434'
                      : 'https://your-resource.openai.azure.com'
                  }
                  value={endpoint}
                  onChange={(e) => setEndpointInput(e.target.value)}
                  disabled={isSaving}
                  label="Endpoint"
                />
              )}
              {requiresApiKey(config?.activeProvider || LlmProvider.SYSTEM_DEFAULT) && (
                <Input
                  type="password"
                  placeholder={getPlaceholder(config?.activeProvider || LlmProvider.SYSTEM_DEFAULT)}
                  value={apiKey}
                  onChange={(e) => setApiKeyInput(e.target.value)}
                  disabled={isSaving}
                  label="API Key"
                />
              )}
              {config?.activeProvider === LlmProvider.OLLAMA && (
                <p className="text-sm text-slate-600">
                  Ollama is self-hosted and doesn't require an API key. Just provide your Ollama server endpoint.
                </p>
              )}
              <Button
                onClick={handleSave}
                disabled={
                  isSaving ||
                  (requiresApiKey(config?.activeProvider || LlmProvider.SYSTEM_DEFAULT) &&
                    !apiKey.trim()) ||
                  (config?.requiresEndpoint && !endpoint.trim())
                }
                className="w-full"
              >
                {isSaving ? (
                  <>
                    <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                    Validating...
                  </>
                ) : (
                  'Save & Validate'
                )}
              </Button>
            </div>
          )}
        </div>
      </div>
    </section>
  );
}
