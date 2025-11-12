import { useState, useEffect, useRef } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ChevronLeft, Save, Smile } from 'lucide-react';
import { getLanguage, createLanguage, updateLanguage, type LanguageRequest } from '../api/languageManagement';
import { useAuthStore } from '../store/authStore';
import { Difficulty } from '../types';
import Button from '../components/ui/Button';
import Input from '../components/ui/Input';
import Select from '../components/ui/Select';
import MultilingualTextArea from '../components/ui/MultilingualTextArea';
import Layout from '../components/layout/Layout';
import FlagEmojiKeyboard from '../components/ui/FlagEmojiKeyboard';

interface FormData {
  code: string;
  nameJson: string;           // JSON map: {"en": "Spanish", "es": "Español", ...}
  flagEmoji: string;
  nativeName: string;
  difficulty: Difficulty;
  descriptionJson: string;    // JSON map: {"en": "Spanish description", ...}
  isActive: boolean;
  displayOrder: number;
}

export default function LanguageEditorPage() {
  const navigate = useNavigate();
  const { code } = useParams<{ code: string }>();
  const { user } = useAuthStore();
  const flagInputRef = useRef<HTMLInputElement>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isCreating, setIsCreating] = useState(true);
  const [showFlagKeyboard, setShowFlagKeyboard] = useState(false);
  const [isLoaded, setIsLoaded] = useState(false);
  
  const [formData, setFormData] = useState<FormData>({
    code: '',
    nameJson: '{"en":"New Language"}',
    flagEmoji: '🌐',
    nativeName: '',
    difficulty: Difficulty.Easy,
    descriptionJson: '{"en":"Language description"}',
    isActive: true,
    displayOrder: 0,
  });

  useEffect(() => {
    if (code) {
      // Editing existing language
      setIsCreating(false);
      loadLanguageData();
    } else {
      // Creating new language - mark as loaded immediately
      setIsLoaded(true);
    }
  }, [code]); // eslint-disable-line react-hooks/exhaustive-deps

  const loadLanguageData = async () => {
    try {
      setLoading(true);
      const language = await getLanguage(code!);
      
      setFormData({
        code: language.code,
        nameJson: language.nameJson || `{"en":"${language.name || language.nativeName}"}`,
        flagEmoji: language.flagEmoji,
        nativeName: language.nativeName,
        difficulty: language.difficulty,
        descriptionJson: language.descriptionJson || `{"en":"${language.description || ""}"}`,
        isActive: language.isActive ?? true,
        displayOrder: language.displayOrder || 0,
      });
      setIsLoaded(true);
    } catch (err) {
      console.error('Failed to load language:', err);
      setError('Failed to load language data. Please try again.');
      setIsLoaded(true); // Mark as loaded even if there was an error so we don't keep trying
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (field: keyof FormData, value: string | number | boolean | Difficulty) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }));
  };

  const handleInsertEmoji = (emoji: string) => {
    setFormData(prev => ({
      ...prev,
      flagEmoji: emoji
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const languageRequest: LanguageRequest = {
        code: formData.code,
        nameJson: formData.nameJson,
        flagEmoji: formData.flagEmoji,
        nativeName: formData.nativeName,
        difficulty: formData.difficulty,
        descriptionJson: formData.descriptionJson,
        isActive: formData.isActive,
        displayOrder: formData.displayOrder,
      };

      if (isCreating) {
        await createLanguage(languageRequest);
      } else {
        await updateLanguage(code!, languageRequest);
      }

      navigate('/languages/manage');
    } catch (err) {
      console.error('Failed to save language:', err);
      setError('Failed to save language. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  if (!user || (!user.roles.includes('EDITOR') && !user.roles.includes('ADMIN'))) {
    return (
      <Layout>
        <div className="max-w-4xl mx-auto p-6">
          <div className="bg-red-50 border border-red-200 rounded-lg p-4">
            <h2 className="text-lg font-semibold text-red-800">Access Denied</h2>
            <p className="text-red-600">You must be an editor or admin to manage languages.</p>
          </div>
        </div>
      </Layout>
    );
  }

  if (loading && !isCreating) {
    return (
      <Layout>
        <div className="max-w-4xl mx-auto p-6">
          <div className="flex justify-center items-center h-64">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-brand-600"></div>
          </div>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="max-w-4xl mx-auto p-6">
        <div className="mb-8">
          <div className="flex items-center gap-4">
            <Button
              variant="ghost"
              size="sm"
              onClick={() => navigate('/languages/manage')}
              className="p-2"
            >
              <ChevronLeft className="w-5 h-5" />
            </Button>
            <div>
              <h1 className="text-3xl font-bold text-slate-900">
                {isCreating ? 'Create New Language' : 'Edit Language'}
              </h1>
              <p className="text-slate-600 mt-2">
                {isCreating 
                  ? 'Define a new language for use in courses' 
                  : `Update language settings for ${formData.nativeName} (${formData.code})`}
              </p>
            </div>
          </div>
        </div>

        {error && (
          <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-6">
            <p className="text-red-600">{error}</p>
          </div>
        )}

        {!isLoaded ? (
          <div className="flex justify-center items-center h-64">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-brand-600"></div>
          </div>
        ) : (
        <form onSubmit={handleSubmit} className="bg-white rounded-xl shadow-soft border border-slate-200 p-8">
          <div className="space-y-6">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-2">
                Language Code *
              </label>
              <Input
                type="text"
                value={formData.code}
                onChange={(e) => handleInputChange('code', e.target.value)}
                placeholder="e.g., en-US, es-ES, fr"
                disabled={!isCreating} // Don't allow changing code for existing languages
                required
              />
              <p className="mt-1 text-sm text-slate-500">
                ISO 639-1 or BCP 47 language tag (e.g., en, en-US, es, es-ES)
              </p>
            </div>

            <div className="relative">
              <label className="block text-sm font-medium text-slate-700 mb-2">
                Flag Emoji *
              </label>
              <div className="flex items-center gap-2">
                <Input
                  type="text"
                  value={formData.flagEmoji}
                  onChange={(e) => handleInputChange('flagEmoji', e.target.value)}
                  placeholder="e.g., 🇺🇸, 🇪🇸, 🇫🇷"
                  required
                  className="flex-1"
                  ref={flagInputRef}
                />
                <button
                  type="button"
                  onClick={() => setShowFlagKeyboard(!showFlagKeyboard)}
                  className="p-2 text-slate-600 hover:bg-slate-100 rounded-lg transition-colors"
                  title="Open flag emoji picker"
                  aria-label="Open flag emoji picker"
                >
                  <Smile className="w-5 h-5" />
                </button>
              </div>
              
              <FlagEmojiKeyboard
                inputRef={flagInputRef}
                onEmojiInsert={handleInsertEmoji}
                isOpen={showFlagKeyboard}
                onClose={() => setShowFlagKeyboard(false)}
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-2">
                Native Name *
              </label>
              <Input
                type="text"
                value={formData.nativeName}
                onChange={(e) => handleInputChange('nativeName', e.target.value)}
                placeholder="e.g., English, Español, Français"
                required
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-2">
                Difficulty *
              </label>
              <Select
                value={formData.difficulty}
                onChange={(value) => handleInputChange('difficulty', value as Difficulty)}
                options={[
                  { value: Difficulty.Easy, label: 'Easy' },
                  { value: Difficulty.Medium, label: 'Medium' },
                  { value: Difficulty.Hard, label: 'Hard' },
                ]}
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-2">
                Display Order
              </label>
              <Input
                type="number"
                value={formData.displayOrder}
                onChange={(e) => handleInputChange('displayOrder', parseInt(e.target.value) || 0)}
                placeholder="0"
                min="0"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-2">
                Multilingual Name *
              </label>
              <MultilingualTextArea
                value={formData.nameJson}
                onChange={(value) => handleInputChange('nameJson', value)}
                placeholder='Enter language name in different languages'
                rows={3}
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-2">
                Multilingual Description
              </label>
              <MultilingualTextArea
                value={formData.descriptionJson}
                onChange={(value) => handleInputChange('descriptionJson', value)}
                placeholder='Enter language description in different languages'
                rows={3}
              />
            </div>

            <div className="flex items-center">
              <input
                type="checkbox"
                id="isActive"
                checked={formData.isActive}
                onChange={(e) => handleInputChange('isActive', e.target.checked)}
                className="h-4 w-4 text-brand-600 focus:ring-brand-500 border-slate-300 rounded"
              />
              <label htmlFor="isActive" className="ml-2 block text-sm text-slate-700">
                Active Language
              </label>
            </div>
          </div>

          <div className="mt-8 flex justify-end gap-3">
            <Button
              type="button"
              variant="outline"
              onClick={() => navigate('/languages/manage')}
              disabled={loading}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              variant="primary"
              disabled={loading}
              className="flex items-center gap-2"
            >
              {loading ? (
                <>
                  <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
                  {isCreating ? 'Creating...' : 'Updating...'}
                </>
              ) : (
                <>
                  <Save className="w-4 h-4" />
                  {isCreating ? 'Create Language' : 'Update Language'}
                </>
              )}
            </Button>
          </div>
        </form>
        )}
      </div>
    </Layout>
  );
}