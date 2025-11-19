import { useState, useEffect, useCallback } from 'react';
import { X } from 'lucide-react';
import { CEFRLevel, LanguageProficiencyType } from '../../types';
import type { Language } from '../../types';
import Button from '../ui/Button';
import { getLanguages } from '../../api/catalog';

interface AddLanguageModalProps {
  isOpen: boolean;
  onClose: () => void;
  onAdd: (languageCode: string, type: LanguageProficiencyType, cefrLevel?: CEFRLevel) => Promise<void>;
  onEdit?: (languageCode: string, level: CEFRLevel) => Promise<void>;
  editLanguageCode?: string;
  editCurrentLevel?: CEFRLevel;
  existingLanguageCodes?: string[];
}

export default function AddLanguageModal({
  isOpen,
  onClose,
  onAdd,
  onEdit,
  editLanguageCode,
  editCurrentLevel,
  existingLanguageCodes = [],
}: AddLanguageModalProps) {
  const [languages, setLanguages] = useState<Language[]>([]);
  const [selectedLanguageCode, setSelectedLanguageCode] = useState<string>('');
  const [selectedType, setSelectedType] = useState<LanguageProficiencyType>(LanguageProficiencyType.Learning);
  const [selectedLevel, setSelectedLevel] = useState<CEFRLevel>(CEFRLevel.None);
  const [isLoading, setIsLoading] = useState(false);
  const [isLoadingLanguages, setIsLoadingLanguages] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const isEditMode = !!editLanguageCode;

  const loadLanguages = useCallback(async () => {
    setIsLoadingLanguages(true);
    try {
      const data = await getLanguages();
      setLanguages(data);
      if (!isEditMode && data.length > 0) {
        // Find first language not in existing languages
        const availableLanguage = data.find(
          (lang) => !existingLanguageCodes.includes(lang.code)
        );
        if (availableLanguage) {
          setSelectedLanguageCode(availableLanguage.code);
        }
      }
    } catch {
      setError('Failed to load languages');
    } finally {
      setIsLoadingLanguages(false);
    }
  }, [isEditMode, existingLanguageCodes]);

  useEffect(() => {
    if (isOpen) {
      loadLanguages();
      if (isEditMode && editLanguageCode && editCurrentLevel) {
        setSelectedLanguageCode(editLanguageCode);
        setSelectedLevel(editCurrentLevel);
      } else {
        setSelectedLanguageCode('');
        setSelectedType(LanguageProficiencyType.Learning);
        setSelectedLevel(CEFRLevel.None);
      }
      setError(null);
    }
  }, [isOpen, isEditMode, editLanguageCode, editCurrentLevel, loadLanguages]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedLanguageCode) {
      setError('Please select a language');
      return;
    }
    if (selectedType === LanguageProficiencyType.Learning && !selectedLevel) {
      setError('Please select a proficiency level');
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      if (isEditMode && onEdit) {
        await onEdit(selectedLanguageCode, selectedLevel);
      } else {
        await onAdd(
          selectedLanguageCode,
          selectedType,
          selectedType === LanguageProficiencyType.Learning ? selectedLevel : undefined
        );
      }
      onClose();
    } catch {
      setError(
        isEditMode
          ? 'Failed to update language proficiency'
          : 'Failed to add language proficiency'
      );
    } finally {
      setIsLoading(false);
    }
  };

  if (!isOpen) return null;

  const availableLanguages = isEditMode
    ? languages.filter((lang) => lang.code === editLanguageCode)
    : languages.filter((lang) => !existingLanguageCodes.includes(lang.code));

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg max-w-md w-full p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-bold text-gray-900">
            {isEditMode ? 'Edit Language Level' : 'Add Language'}
          </h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 transition-colors"
            disabled={isLoading}
          >
            <X className="w-6 h-6" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
              {error}
            </div>
          )}

          <div>
            <label
              htmlFor="language"
              className="block text-sm font-medium text-gray-700 mb-1"
            >
              Language
            </label>
            <select
              id="language"
              value={selectedLanguageCode}
              onChange={(e) => setSelectedLanguageCode(e.target.value)}
              disabled={isEditMode || isLoadingLanguages || isLoading}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100 disabled:cursor-not-allowed"
              required
            >
              <option value="">
                {isLoadingLanguages
                  ? 'Loading languages...'
                  : 'Select a language'}
              </option>
              {availableLanguages.map((lang) => (
                <option key={lang.code} value={lang.code}>
                  {lang.nativeName} - {lang.name}
                </option>
              ))}
            </select>
          </div>

          {!isEditMode && (
            <div>
              <label
                htmlFor="type"
                className="block text-sm font-medium text-gray-700 mb-1"
              >
                Type
              </label>
              <select
                id="type"
                value={selectedType}
                onChange={(e) => setSelectedType(e.target.value as LanguageProficiencyType)}
                disabled={isLoading}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100 disabled:cursor-not-allowed"
                required
              >
                {Object.values(LanguageProficiencyType).map((type) => (
                  <option key={type} value={type}>
                    {type}
                  </option>
                ))}
              </select>
              <p className="text-xs text-gray-500 mt-1">
                Native: Native speaker, Learning: Currently learning this language
              </p>
            </div>
          )}

          {(isEditMode || selectedType === LanguageProficiencyType.Learning) && (
            <div>
              <label
                htmlFor="level"
                className="block text-sm font-medium text-gray-700 mb-1"
              >
                Proficiency Level
              </label>
              <select
                id="level"
                value={selectedLevel}
                onChange={(e) => setSelectedLevel(e.target.value as CEFRLevel)}
                disabled={isLoading}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100 disabled:cursor-not-allowed"
                required={selectedType === LanguageProficiencyType.Learning}
              >
                {Object.values(CEFRLevel).map((level) => (
                  <option key={level} value={level}>
                    {level}
                  </option>
                ))}
              </select>
              <p className="text-xs text-gray-500 mt-1">
                A1: Beginner, A2: Elementary, B1: Intermediate, B2: Upper
                Intermediate, C1: Advanced, C2: Proficient
              </p>
            </div>
          )}

          <div className="flex gap-3 pt-4">
            <Button
              type="button"
              variant="secondary"
              onClick={onClose}
              disabled={isLoading}
              className="flex-1"
            >
              Cancel
            </Button>
            <Button
              type="submit"
              variant="primary"
              isLoading={isLoading}
              disabled={
                isLoading ||
                isLoadingLanguages ||
                !selectedLanguageCode ||
                availableLanguages.length === 0
              }
              className="flex-1"
            >
              {isEditMode ? 'Update' : 'Add'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
