import { useState, useEffect, useCallback } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import Layout from '../components/layout/Layout';
import Button from '../components/ui/Button';
import EmojiPicker from '../components/ui/EmojiPicker';
import TutorImage from '../components/tutor/TutorImage';
import { getLanguages, getTutorImagePreview } from '../api/catalog';
import { createCustomTutor } from '../api/catalog';
import { TutorPersonality, TeachingStyle, TutorGender } from '../types';
import type { Language } from '../types';
import toast from 'react-hot-toast';

// Gender-based emoji suggestions
const GENDER_EMOJIS = {
  [TutorGender.Male]: ['👨‍🏫', '👨‍💼', '🧑‍🏫', '👨', '🧔', '👨‍🎓'],
  [TutorGender.Female]: ['👩‍🏫', '👩‍💼', '🧑‍🏫', '👩', '👩‍🎓', '👱‍♀️'],
  [TutorGender.Neutral]: ['🧑‍🏫', '🧑‍💼', '🧑', '👤', '🧑‍🎓', '👥'],
};

const getRandomEmoji = (gender: TutorGender): string => {
  const emojis = GENDER_EMOJIS[gender];
  return emojis[Math.floor(Math.random() * emojis.length)];
};

export default function CreateCustomTutorPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const courseId = searchParams.get('courseId'); // Get courseId from URL params
  const [languages, setLanguages] = useState<Language[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [showEmojiPicker, setShowEmojiPicker] = useState(false);
  const [userSelectedEmoji, setUserSelectedEmoji] = useState(false); // Track if user manually picked emoji
  const [previewImageUrl, setPreviewImageUrl] = useState<string | null>(null);
  const [isLoadingPreview, setIsLoadingPreview] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    emoji: '',
    personaEnglish: '',
    domainEnglish: '',
    descriptionEnglish: '',
    personality: TutorPersonality.Casual,
    teachingStyle: TeachingStyle.Reactive,
    targetLanguageCode: '',
    culturalBackground: '',
    location: '',
    age: 30,
    gender: TutorGender.Neutral,
  });

  const loadLanguages = useCallback(async () => {
    try {
      const languagesData = await getLanguages();
      setLanguages(languagesData);
      
      // Check for language parameter from URL after languages are loaded
      const languageParam = searchParams.get('language');
      let targetLanguageCode = '';
      
      if (languageParam) {
        // Check if the language from URL exists in available languages
        const matchingLanguage = languagesData.find(lang => lang.code === languageParam);
        if (matchingLanguage) {
          targetLanguageCode = languageParam;
        }
      }
      
      // If no language was set from URL parameter, use the first available language as fallback
      if (!targetLanguageCode && languagesData.length > 0) {
        targetLanguageCode = languagesData[0].code;
      }
      
      setFormData(prev => ({ ...prev, targetLanguageCode }));
    } catch (error) {
      toast.error('Failed to load languages');
      console.error('Error loading languages:', error);
    }
  }, [searchParams]);

  useEffect(() => {
    loadLanguages();
  }, [loadLanguages]);

  // Helper function to navigate back
  const navigateBack = () => {
    if (courseId) {
      navigate(`/courses/${courseId}`);
    } else {
      navigate('/languages');
    }
  };

  // Auto-select emoji when gender changes (only if user hasn't manually selected one)
  useEffect(() => {
    if (!userSelectedEmoji) {
      const randomEmoji = getRandomEmoji(formData.gender);
      setFormData(prev => ({ ...prev, emoji: randomEmoji }));
    }
  }, [formData.gender, userSelectedEmoji]);

  // Load preview image when relevant fields change
  useEffect(() => {
    const loadPreviewImage = async () => {
      // Only load if we have the required fields
      if (!formData.targetLanguageCode || !formData.gender) {
        setPreviewImageUrl(null);
        return;
      }

      setIsLoadingPreview(true);
      const imageUrl = await getTutorImagePreview(
        formData.targetLanguageCode,
        formData.gender,
        formData.age,
        formData.location || undefined,
        formData.personaEnglish || undefined
      );
      setPreviewImageUrl(imageUrl);
      setIsLoadingPreview(false);
    };

    loadPreviewImage();
  }, [formData.targetLanguageCode, formData.gender, formData.age, formData.location, formData.personaEnglish]);



  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!formData.name || !formData.emoji || !formData.personaEnglish ||
        !formData.domainEnglish || !formData.descriptionEnglish || !formData.targetLanguageCode) {
      toast.error('Please fill in all required fields');
      return;
    }

    setIsLoading(true);

    const request = {
      name: formData.name,
      emoji: formData.emoji,
      personaEnglish: formData.personaEnglish,
      domainEnglish: formData.domainEnglish,
      descriptionEnglish: formData.descriptionEnglish,
      personality: formData.personality,
      teachingStyle: formData.teachingStyle,
      targetLanguageCode: formData.targetLanguageCode,
      age: formData.age,
      gender: formData.gender,
      culturalBackground: formData.culturalBackground ? formData.culturalBackground.trim() : undefined,
      location: formData.location ? formData.location.trim() : undefined,
    };



    // Validate the request
    if (!request.targetLanguageCode) {
      toast.error('Please select a target language');
      setIsLoading(false);
      return;
    }

    try {
      await createCustomTutor(request);
      toast.success('Custom tutor created successfully!');
      navigateBack();
    } catch (error: unknown) {
      const errorMessage = (error as { response?: { data?: { message?: string } } }).response?.data?.message || (error as Error).message || 'Failed to create custom tutor';
      toast.error(errorMessage);
      console.error('Error creating tutor:', error);
      console.error('Request payload:', request);
      if ((error as { response?: { status?: number; data?: unknown } }).response) {
        console.error('Response status:', (error as { response: { status?: number } }).response.status);
        console.error('Response data:', (error as { response: { data?: unknown } }).response.data);
      }
    } finally {
      setIsLoading(false);
    }
  };

  const personalityOptions = [
    { value: TutorPersonality.Casual, label: 'Casual', description: 'Friendly, informal, and relaxed' },
    { value: TutorPersonality.Professional, label: 'Professional', description: 'Business-focused, formal but approachable' },
    { value: TutorPersonality.Encouraging, label: 'Encouraging', description: 'Positive reinforcement, patient' },
    { value: TutorPersonality.Strict, label: 'Strict', description: 'High standards, formal' },
    { value: TutorPersonality.Academic, label: 'Academic', description: 'Scholarly, detailed explanations' },
  ];

  const teachingStyleOptions = [
    { value: TeachingStyle.Reactive, label: 'Reactive', description: 'Responds to learner input, minimal guidance' },
    { value: TeachingStyle.Guided, label: 'Guided', description: 'Provides structured support and suggestions' },
    { value: TeachingStyle.Directive, label: 'Directive', description: 'Takes active lead, provides clear instructions' },
  ];

  return (
    <Layout>
      <div className="max-w-3xl mx-auto">
        <div className="mb-6">
          <Button
            variant="secondary"
            onClick={navigateBack}
            className="mb-4"
          >
            &larr; Back to Course
          </Button>
          <h1 className="text-3xl font-bold text-gray-900">Create Custom Tutor</h1>
          <p className="mt-2 text-gray-600">
            Design your own AI tutor with a unique personality and teaching style
          </p>
        </div>

        <form onSubmit={handleSubmit} className="bg-white rounded-lg shadow-sm border border-gray-200 p-6 space-y-6">
          {/* Basic Info */}
          <div className="space-y-4">
            <h2 className="text-xl font-semibold text-gray-900">Basic Information</h2>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Name <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  placeholder="e.g., Maria"
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Emoji <span className="text-red-500">*</span>
                </label>
                <div className="flex gap-2">
                  <input
                    type="text"
                    value={formData.emoji}
                    onChange={(e) => {
                      setFormData({ ...formData, emoji: e.target.value });
                      setUserSelectedEmoji(true);
                    }}
                    className="flex-1 px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                    placeholder="👩‍🏫"
                    required
                    maxLength={4}
                  />
                  <button
                    type="button"
                    onClick={() => setShowEmojiPicker(true)}
                    className="px-4 py-2 bg-gray-100 hover:bg-gray-200 border border-gray-300 rounded-md transition-colors text-2xl"
                    title="Pick emoji"
                  >
                    😀
                  </button>
                </div>
                <p className="mt-1 text-xs text-gray-500">Single emoji to represent your tutor</p>
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Target Language <span className="text-red-500">*</span>
              </label>
              <select
                value={formData.targetLanguageCode}
                onChange={(e) => setFormData({ ...formData, targetLanguageCode: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                required
              >
                {languages.map((lang) => (
                  <option key={lang.code} value={lang.code}>
                    {lang.flagEmoji} {lang.name}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Gender <span className="text-red-500">*</span>
              </label>
              <div className="grid grid-cols-3 gap-3">
                <button
                  type="button"
                  onClick={() => setFormData({ ...formData, gender: TutorGender.Male })}
                  className={`p-3 rounded-lg border-2 transition-all text-center ${
                    formData.gender === TutorGender.Male
                      ? 'border-blue-500 bg-blue-50'
                      : 'border-gray-200 hover:border-blue-300'
                  }`}
                >
                  <div className="text-2xl mb-1">👨</div>
                  <div className="font-medium text-gray-900 text-sm">Male</div>
                </button>
                <button
                  type="button"
                  onClick={() => setFormData({ ...formData, gender: TutorGender.Female })}
                  className={`p-3 rounded-lg border-2 transition-all text-center ${
                    formData.gender === TutorGender.Female
                      ? 'border-blue-500 bg-blue-50'
                      : 'border-gray-200 hover:border-blue-300'
                  }`}
                >
                  <div className="text-2xl mb-1">👩</div>
                  <div className="font-medium text-gray-900 text-sm">Female</div>
                </button>
                <button
                  type="button"
                  onClick={() => setFormData({ ...formData, gender: TutorGender.Neutral })}
                  className={`p-3 rounded-lg border-2 transition-all text-center ${
                    formData.gender === TutorGender.Neutral
                      ? 'border-blue-500 bg-blue-50'
                      : 'border-gray-200 hover:border-blue-300'
                  }`}
                >
                  <div className="text-2xl mb-1">🧑</div>
                  <div className="font-medium text-gray-900 text-sm">Neutral</div>
                </button>
              </div>
              <p className="mt-2 text-xs text-gray-500">
                Selecting a gender will suggest an appropriate emoji (you can still pick your own)
              </p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Age <span className="text-red-500">*</span>
                </label>
                <input
                  type="number"
                  value={formData.age}
                  onChange={(e) => setFormData({ ...formData, age: parseInt(e.target.value) || 30 })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  placeholder="30"
                  min="18"
                  max="100"
                  required
                />
                <p className="mt-1 text-xs text-gray-500">Tutor's age (18-100)</p>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Location (Optional)
                </label>
                <input
                  type="text"
                  value={formData.location}
                  onChange={(e) => setFormData({ ...formData, location: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  placeholder="e.g., Barcelona, Spain"
                  maxLength={128}
                />
                <p className="mt-1 text-xs text-gray-500">Where the tutor is from/based</p>
              </div>
            </div>
          </div>

          {/* Tutor Character */}
          <div className="space-y-4">
            <h2 className="text-xl font-semibold text-gray-900">Tutor Character</h2>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Persona <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={formData.personaEnglish}
                onChange={(e) => setFormData({ ...formData, personaEnglish: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                placeholder="e.g., Native Spanish teacher from Madrid"
                required
                maxLength={256}
              />
              <p className="mt-1 text-xs text-gray-500">Brief role or identity (max 256 characters)</p>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Domain/Expertise <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={formData.domainEnglish}
                onChange={(e) => setFormData({ ...formData, domainEnglish: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                placeholder="e.g., Spanish grammar and conversation"
                required
                maxLength={256}
              />
              <p className="mt-1 text-xs text-gray-500">Area of expertise (max 256 characters)</p>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Description <span className="text-red-500">*</span>
              </label>
              <textarea
                value={formData.descriptionEnglish}
                onChange={(e) => setFormData({ ...formData, descriptionEnglish: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                placeholder="Describe your tutor's background, teaching approach, and what makes them unique..."
                rows={4}
                required
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Cultural Background (Optional)
              </label>
              <input
                type="text"
                value={formData.culturalBackground}
                onChange={(e) => setFormData({ ...formData, culturalBackground: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                placeholder="e.g., Native speaker from Barcelona, Spain"
              />
            </div>
          </div>

          {/* Teaching Style */}
          <div className="space-y-4">
            <h2 className="text-xl font-semibold text-gray-900">Teaching Style</h2>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Personality <span className="text-red-500">*</span>
              </label>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                {personalityOptions.map((option) => (
                  <button
                    key={option.value}
                    type="button"
                    onClick={() => setFormData({ ...formData, personality: option.value })}
                    className={`text-left p-3 rounded-lg border-2 transition-all ${
                      formData.personality === option.value
                        ? 'border-blue-500 bg-blue-50'
                        : 'border-gray-200 hover:border-blue-300'
                    }`}
                  >
                    <div className="font-medium text-gray-900">{option.label}</div>
                    <div className="text-sm text-gray-600">{option.description}</div>
                  </button>
                ))}
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Teaching Approach <span className="text-red-500">*</span>
              </label>
              <div className="space-y-3">
                {teachingStyleOptions.map((option) => (
                  <button
                    key={option.value}
                    type="button"
                    onClick={() => setFormData({ ...formData, teachingStyle: option.value })}
                    className={`w-full text-left p-3 rounded-lg border-2 transition-all ${
                      formData.teachingStyle === option.value
                        ? 'border-blue-500 bg-blue-50'
                        : 'border-gray-200 hover:border-blue-300'
                    }`}
                  >
                    <div className="font-medium text-gray-900">{option.label}</div>
                    <div className="text-sm text-gray-600">{option.description}</div>
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* Preview - Shows how the tutor will appear */}
          <div className="bg-gradient-to-br from-slate-50 to-blue-50 rounded-lg p-6 border-2 border-blue-200">
            <h3 className="text-sm font-medium text-gray-700 mb-4 flex items-center gap-2">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
              </svg>
              Preview - How your tutor will appear in the tutor panel
            </h3>

            {/* Tutor Card Preview */}
            <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-4">
              <div className="flex items-start gap-4">
                {/* Tutor Image Preview */}
                {isLoadingPreview ? (
                  <div className="w-16 h-16 rounded-lg flex-shrink-0 bg-gray-200 flex items-center justify-center">
                    <span className="text-xs text-gray-400">...</span>
                  </div>
                ) : (
                  <div className="flex-shrink-0">
                    <TutorImage
                      tutorEmoji={formData.emoji || '❓'}
                      tutorName={formData.name || 'Tutor Preview'}
                      size="large"
                      rounded="lg"
                      previewImageUrl={previewImageUrl}
                    />
                  </div>
                )}

                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-2 flex-wrap">
                    <h4 className="font-semibold text-gray-900 text-lg">
                      {formData.name || 'Tutor Name'}
                    </h4>
                    {formData.age && (
                      <span className="text-xs px-2 py-0.5 rounded-full bg-gray-100 text-gray-700">
                        Age {formData.age}
                      </span>
                    )}
                  </div>

                  {formData.location && (
                    <p className="text-sm text-gray-600 mb-2 flex items-center gap-1">
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                      </svg>
                      {formData.location}
                    </p>
                  )}

                  <div className="flex items-center gap-2 mb-2 flex-wrap">
                    <span className="text-xs px-2 py-1 rounded-full bg-blue-100 text-blue-800 font-medium">
                      {formData.personality}
                    </span>
                    <span className="text-xs px-2 py-1 rounded-full bg-green-100 text-green-800 font-medium">
                      {formData.teachingStyle}
                    </span>
                  </div>

                  <p className="text-sm text-gray-700 font-medium mb-1">
                    {formData.personaEnglish || 'Persona will appear here...'}
                  </p>

                  <p className="text-sm text-gray-600">
                    {formData.descriptionEnglish || 'Description will appear here...'}
                  </p>

                  {formData.culturalBackground && (
                    <p className="text-xs text-gray-500 mt-2 italic">
                      {formData.culturalBackground}
                    </p>
                  )}
                </div>
              </div>
            </div>
          </div>

          {/* Actions */}
          <div className="flex gap-3 pt-4">
            <Button
              type="submit"
              isLoading={isLoading}
              className="flex-1"
            >
              Create Tutor
            </Button>
            <Button
              type="button"
              variant="secondary"
              onClick={navigateBack}
              disabled={isLoading}
            >
              Cancel
            </Button>
          </div>
        </form>
      </div>

      {/* Emoji Picker Modal */}
      {showEmojiPicker && (
        <EmojiPicker
          onSelect={(emoji) => {
            setFormData({ ...formData, emoji });
            setUserSelectedEmoji(true);
          }}
          onClose={() => setShowEmojiPicker(false)}
        />
      )}
    </Layout>
  );
}
