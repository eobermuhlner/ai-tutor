import { useState, useEffect } from 'react';
import { Star, Trash2, Edit2 } from 'lucide-react';
import { CEFRLevel } from '../../types';
import type { LanguageProficiency, Language } from '../../types';
import Button from '../ui/Button';
import FlagIcon from '../ui/FlagIcon';
import { getLanguages } from '../../api/catalog';
import { getLanguageAriaLabel } from '../../utils/languageDisplay';

interface LanguageProficiencyListProps {
  proficiencies: LanguageProficiency[];
  onSetPrimary: (languageCode: string) => void;
  onEdit: (languageCode: string, currentLevel: CEFRLevel) => void;
  onRemove: (languageCode: string) => void;
  isLoading?: boolean;
}

export default function LanguageProficiencyList({
  proficiencies,
  onSetPrimary,
  onEdit,
  onRemove,
  isLoading = false,
}: LanguageProficiencyListProps) {
  const [languages, setLanguages] = useState<Language[]>([]);

  useEffect(() => {
    loadLanguages();
  }, []);

  const loadLanguages = async () => {
    try {
      const data = await getLanguages();
      setLanguages(data);
    } catch (error) {
      console.error('Failed to load languages:', error);
    }
  };

  const getLanguage = (code: string) => {
    return languages.find((lang) => lang.code === code);
  };

  const getLanguageLabel = (code: string) => {
    const language = getLanguage(code);
    return language ? getLanguageAriaLabel(language) : code;
  };

  if (proficiencies.length === 0) {
    return (
      <div className="text-center py-8 text-gray-500">
        No language proficiencies added yet. Add your first language to get started!
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {proficiencies.map((proficiency) => (
        <div
          key={proficiency.languageCode}
          className="flex items-center justify-between p-4 border border-gray-200 rounded-lg hover:border-gray-300 transition-colors"
          role="article"
          aria-label={getLanguageLabel(proficiency.languageCode)}
        >
          <div className="flex items-center gap-3">
            <div
              className={`flex items-center justify-center w-10 h-10 rounded-full ${
                proficiency.isPrimary ? 'bg-blue-100' : 'bg-gray-100'
              }`}
              aria-hidden="true"
            >
              {proficiency.isPrimary && (
                <Star className="w-5 h-5 text-blue-600 fill-blue-600" />
              )}
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="font-medium text-gray-900 inline-flex items-center gap-1.5">
                  <FlagIcon languageCode={proficiency.languageCode} size={1.2} />
                  {getLanguage(proficiency.languageCode)?.nativeName.split('(')[0].trim() || proficiency.languageCode}
                </h3>
                {proficiency.isPrimary && (
                  <span className="text-xs bg-blue-100 text-blue-700 px-2 py-0.5 rounded-full">
                    Primary
                  </span>
                )}
                {proficiency.isNative && (
                  <span className="text-xs bg-green-100 text-green-700 px-2 py-0.5 rounded-full">
                    Native
                  </span>
                )}
              </div>
              <p className="text-sm text-gray-500">
                {proficiency.cefrLevel ? `Level: ${proficiency.cefrLevel}` : 'Native Speaker'}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            {!proficiency.isPrimary && (
              <Button
                variant="secondary"
                size="sm"
                onClick={() => onSetPrimary(proficiency.languageCode)}
                disabled={isLoading}
                title="Set as primary language"
              >
                <Star className="w-4 h-4" />
              </Button>
            )}
            {!proficiency.isNative && (
              <Button
                variant="secondary"
                size="sm"
                onClick={() => onEdit(proficiency.languageCode, proficiency.cefrLevel || CEFRLevel.A1)}
                disabled={isLoading}
                title="Edit level"
              >
                <Edit2 className="w-4 h-4" />
              </Button>
            )}
            <Button
              variant="danger"
              size="sm"
              onClick={() => onRemove(proficiency.languageCode)}
              disabled={isLoading}
              title="Remove language"
            >
              <Trash2 className="w-4 h-4" />
            </Button>
          </div>
        </div>
      ))}
    </div>
  );
}
