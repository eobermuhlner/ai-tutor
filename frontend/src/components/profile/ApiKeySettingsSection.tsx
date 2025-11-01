import { useState, useEffect } from 'react';
import { Key, Check, Loader2 } from 'lucide-react';
import Button from '../ui/Button';
import Input from '../ui/Input';
import toast from 'react-hot-toast';
import {
  getApiKeyConfiguration,
  setOpenAiKey,
  setAzureOpenAiKey,
  setAnthropicKey as setAnthropicApiKey,
  removeApiKey,
  setPreferredProvider,
  LlmProvider,
} from '../../api/apiKeys';
import type { ApiKeyConfiguration } from '../../api/apiKeys';

export default function ApiKeySettingsSection() {
  console.log('ApiKeySettingsSection rendering');
  const [config, setConfig] = useState<ApiKeyConfiguration | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // Form state for each provider
  const [openaiKey, setOpenaiKey] = useState('');
  const [azureKey, setAzureKey] = useState('');
  const [azureEndpoint, setAzureEndpoint] = useState('');
  const [anthropicKey, setAnthropicKey] = useState('');

  // Submission state
  const [isSavingOpenai, setIsSavingOpenai] = useState(false);
  const [isSavingAzure, setIsSavingAzure] = useState(false);
  const [isSavingAnthropic, setIsSavingAnthropic] = useState(false);

  useEffect(() => {
    loadConfiguration();
  }, []);

  const loadConfiguration = async () => {
    try {
      const data = await getApiKeyConfiguration();
      setConfig(data);
      if (data.azureOpenaiEndpoint) {
        setAzureEndpoint(data.azureOpenaiEndpoint);
      }
    } catch (error) {
      console.error('API Key config load error:', error);
      toast.error('Failed to load API key configuration');
      // Continue anyway - don't stay in loading state forever
      setConfig({
        openaiConfigured: false,
        azureOpenaiConfigured: false,
        anthropicConfigured: false,
        preferredProvider: null,
        azureOpenaiEndpoint: null,
      });
    } finally {
      setIsLoading(false);
    }
  };

  const handleSaveOpenAi = async () => {
    if (!openaiKey.trim()) {
      toast.error('Please enter an API key');
      return;
    }

    setIsSavingOpenai(true);
    try {
      await setOpenAiKey(openaiKey);
      toast.success('OpenAI API key saved and validated successfully');
      setOpenaiKey('');
      await loadConfiguration();
    } catch (error: any) {
      const message = error.response?.data?.error || 'Failed to save OpenAI API key';
      toast.error(message);
    } finally {
      setIsSavingOpenai(false);
    }
  };

  const handleSaveAzure = async () => {
    if (!azureKey.trim() || !azureEndpoint.trim()) {
      toast.error('Please enter both API key and endpoint');
      return;
    }

    setIsSavingAzure(true);
    try {
      await setAzureOpenAiKey(azureKey, azureEndpoint);
      toast.success('Azure OpenAI credentials saved and validated successfully');
      setAzureKey('');
      await loadConfiguration();
    } catch (error: any) {
      const message = error.response?.data?.error || 'Failed to save Azure OpenAI credentials';
      toast.error(message);
    } finally {
      setIsSavingAzure(false);
    }
  };

  const handleSaveAnthropic = async () => {
    if (!anthropicKey.trim()) {
      toast.error('Please enter an API key');
      return;
    }

    setIsSavingAnthropic(true);
    try {
      await setAnthropicApiKey(anthropicKey);
      toast.success('Anthropic API key saved and validated successfully');
      setAnthropicKey('');
      await loadConfiguration();
    } catch (error: any) {
      const message = error.response?.data?.error || 'Failed to save Anthropic API key';
      toast.error(message);
    } finally {
      setIsSavingAnthropic(false);
    }
  };

  const handleRemove = async (provider: LlmProvider) => {
    if (!confirm(`Are you sure you want to remove your ${provider} API key?`)) {
      return;
    }

    try {
      await removeApiKey(provider);
      toast.success('API key removed successfully');
      await loadConfiguration();
    } catch (error) {
      toast.error('Failed to remove API key');
    }
  };

  const handleSetPreferred = async (provider: LlmProvider) => {
    try {
      await setPreferredProvider(provider);
      toast.success(`Preferred provider set to ${provider}`);
      await loadConfiguration();
    } catch (error: any) {
      const message = error.response?.data?.error || 'Failed to set preferred provider';
      toast.error(message);
    }
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
        <p className="text-sm text-slate-600">
          Configure your own API keys for LLM providers. If not configured, the system will use default keys.
          Your API keys are encrypted and never shared.
        </p>

        {/* OpenAI */}
        <div className="border border-slate-200 rounded-lg p-4">
          <div className="flex items-center justify-between mb-3">
            <h3 className="font-semibold text-slate-900 flex items-center gap-2">
              OpenAI
              {config?.openaiConfigured && (
                <Check className="w-4 h-4 text-green-600" />
              )}
            </h3>
            {config?.openaiConfigured && (
              <Button
                variant="outline"
                size="sm"
                onClick={() => handleRemove(LlmProvider.OPENAI)}
              >
                Remove
              </Button>
            )}
          </div>

          {config?.openaiConfigured ? (
            <div className="flex items-center justify-between">
              <p className="text-sm text-green-600 flex items-center gap-2">
                <Check className="w-4 h-4" />
                API key configured
              </p>
              {config.preferredProvider !== LlmProvider.OPENAI && (
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handleSetPreferred(LlmProvider.OPENAI)}
                >
                  Set as Preferred
                </Button>
              )}
              {config.preferredProvider === LlmProvider.OPENAI && (
                <span className="text-sm text-brand-600 font-medium">Preferred Provider</span>
              )}
            </div>
          ) : (
            <div className="space-y-3">
              <Input
                type="password"
                placeholder="sk-..."
                value={openaiKey}
                onChange={(e) => setOpenaiKey(e.target.value)}
                disabled={isSavingOpenai}
              />
              <Button
                onClick={handleSaveOpenAi}
                disabled={isSavingOpenai || !openaiKey.trim()}
                className="w-full"
              >
                {isSavingOpenai ? (
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

        {/* Azure OpenAI */}
        <div className="border border-slate-200 rounded-lg p-4">
          <div className="flex items-center justify-between mb-3">
            <h3 className="font-semibold text-slate-900 flex items-center gap-2">
              Azure OpenAI
              {config?.azureOpenaiConfigured && (
                <Check className="w-4 h-4 text-green-600" />
              )}
            </h3>
            {config?.azureOpenaiConfigured && (
              <Button
                variant="outline"
                size="sm"
                onClick={() => handleRemove(LlmProvider.AZURE_OPENAI)}
              >
                Remove
              </Button>
            )}
          </div>

          {config?.azureOpenaiConfigured ? (
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <p className="text-sm text-green-600 flex items-center gap-2">
                  <Check className="w-4 h-4" />
                  API key configured
                </p>
                {config.preferredProvider !== LlmProvider.AZURE_OPENAI && (
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handleSetPreferred(LlmProvider.AZURE_OPENAI)}
                  >
                    Set as Preferred
                  </Button>
                )}
                {config.preferredProvider === LlmProvider.AZURE_OPENAI && (
                  <span className="text-sm text-brand-600 font-medium">Preferred Provider</span>
                )}
              </div>
              {config.azureOpenaiEndpoint && (
                <p className="text-sm text-slate-500">
                  Endpoint: {config.azureOpenaiEndpoint}
                </p>
              )}
            </div>
          ) : (
            <div className="space-y-3">
              <Input
                type="text"
                placeholder="https://your-resource.openai.azure.com"
                value={azureEndpoint}
                onChange={(e) => setAzureEndpoint(e.target.value)}
                disabled={isSavingAzure}
                label="Endpoint"
              />
              <Input
                type="password"
                placeholder="API Key"
                value={azureKey}
                onChange={(e) => setAzureKey(e.target.value)}
                disabled={isSavingAzure}
                label="API Key"
              />
              <Button
                onClick={handleSaveAzure}
                disabled={isSavingAzure || !azureKey.trim() || !azureEndpoint.trim()}
                className="w-full"
              >
                {isSavingAzure ? (
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

        {/* Anthropic */}
        <div className="border border-slate-200 rounded-lg p-4">
          <div className="flex items-center justify-between mb-3">
            <h3 className="font-semibold text-slate-900 flex items-center gap-2">
              Anthropic (Claude)
              {config?.anthropicConfigured && (
                <Check className="w-4 h-4 text-green-600" />
              )}
            </h3>
            {config?.anthropicConfigured && (
              <Button
                variant="outline"
                size="sm"
                onClick={() => handleRemove(LlmProvider.ANTHROPIC)}
              >
                Remove
              </Button>
            )}
          </div>

          {config?.anthropicConfigured ? (
            <div className="flex items-center justify-between">
              <p className="text-sm text-green-600 flex items-center gap-2">
                <Check className="w-4 h-4" />
                API key configured
              </p>
              {config.preferredProvider !== LlmProvider.ANTHROPIC && (
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handleSetPreferred(LlmProvider.ANTHROPIC)}
                >
                  Set as Preferred
                </Button>
              )}
              {config.preferredProvider === LlmProvider.ANTHROPIC && (
                <span className="text-sm text-brand-600 font-medium">Preferred Provider</span>
              )}
            </div>
          ) : (
            <div className="space-y-3">
              <Input
                type="password"
                placeholder="sk-ant-..."
                value={anthropicKey}
                onChange={(e) => setAnthropicKey(e.target.value)}
                disabled={isSavingAnthropic}
              />
              <Button
                onClick={handleSaveAnthropic}
                disabled={isSavingAnthropic || !anthropicKey.trim()}
                className="w-full"
              >
                {isSavingAnthropic ? (
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

        {/* Use System Default */}
        {config && (config.openaiConfigured || config.azureOpenaiConfigured || config.anthropicConfigured) && (
          <div className="border border-slate-200 rounded-lg p-4">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="font-semibold text-slate-900">System Default</h3>
                <p className="text-sm text-slate-600 mt-1">
                  Use the server's configured LLM provider
                </p>
              </div>
              {config.preferredProvider !== LlmProvider.SYSTEM_DEFAULT && (
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handleSetPreferred(LlmProvider.SYSTEM_DEFAULT)}
                >
                  Set as Preferred
                </Button>
              )}
              {config.preferredProvider === LlmProvider.SYSTEM_DEFAULT && (
                <span className="text-sm text-brand-600 font-medium">Preferred Provider</span>
              )}
            </div>
          </div>
        )}
      </div>
    </section>
  );
}
