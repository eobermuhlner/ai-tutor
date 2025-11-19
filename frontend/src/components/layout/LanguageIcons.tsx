import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { CEFRLevel } from '../../types';
import type { LanguageProficiency, Language } from '../../types';
import { getLanguageProficiencies } from '../../api/userLanguages';
import { getLanguages } from '../../api/catalog';
import Tooltip from '../ui/Tooltip';
import FlagIcon from '../ui/FlagIcon';

// Map CEFR levels to numeric values for sorting (higher is more proficient)
const cefrLevelValues: Record<CEFRLevel, number> = {
  [CEFRLevel.C2]: 6,
  [CEFRLevel.C1]: 5,
  [CEFRLevel.B2]: 4,
  [CEFRLevel.B1]: 3,
  [CEFRLevel.A2]: 2,
  [CEFRLevel.A1]: 1,
  [CEFRLevel.None]: 0,
};

// Function to get a language's name by code
function getLanguageName(languageCode: string, languages: Language[]): string {
  const language = languages.find(lang => lang.code === languageCode);
  return language ? language.name : languageCode.toUpperCase();
}

interface LanguageIconsProps {
  userId: string;
}

export default function LanguageIcons({ userId }: LanguageIconsProps) {
  const navigate = useNavigate();
  const [proficiencies, setProficiencies] = useState<LanguageProficiency[]>([]);
  const [languages, setLanguages] = useState<Language[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    async function fetchLanguagesAndProficiencies() {
      try {
        const [userProficiencies, allLanguages] = await Promise.all([
          getLanguageProficiencies(userId),
          getLanguages()
        ]);
        
        setProficiencies(userProficiencies);
        setLanguages(allLanguages);
      } catch (error) {
        console.error('Failed to load user language proficiencies:', error);
      } finally {
        setIsLoading(false);
      }
    }

    if (userId) {
      fetchLanguagesAndProficiencies();
    }
  }, [userId]);

  if (isLoading || !proficiencies.length) {
    return null;
  }

  // Sort proficiencies by proficiency level (descending) and then by isPrimary
  // Native speakers come first among same-level proficiencies
  const sortedProficiencies = [...proficiencies].sort((a, b) => {
    // Primary languages are shown first
    if (a.isPrimary && !b.isPrimary) return -1;
    if (!a.isPrimary && b.isPrimary) return 1;

    // If both or neither are primary, sort by CEFR level
    const levelA = cefrLevelValues[a.cefrLevel ?? CEFRLevel.None];
    const levelB = cefrLevelValues[b.cefrLevel ?? CEFRLevel.None];
    
    // If CEFR levels are the same, prioritize native speakers
    if (levelA === levelB) {
      if (a.isNative && !b.isNative) return -1;
      if (!a.isNative && b.isNative) return 1;
      return 0; // Equal priority
    }
    
    return levelB - levelA; // Descending order (higher level first)
  });

  return (
    <div className="flex items-center gap-1 ml-3">
      {sortedProficiencies.map((proficiency) => (
        <Tooltip
          key={proficiency.languageCode}
          title={`${getLanguageName(proficiency.languageCode, languages)} - ${proficiency.isNative ? 'Native Speaker' : `${proficiency.cefrLevel || 'No Level'} Proficiency`}`}
          position="bottom"
        >
          <div
            className={`relative flex items-center justify-center w-6 h-6 rounded-full overflow-hidden cursor-pointer ${
              proficiency.isPrimary
                ? 'ring-2 ring-blue-500 ring-offset-1'
                : 'ring-1 ring-slate-300'
            }`}
            onClick={() => navigate('/profile#language-proficiencies')}
          >
            <FlagIcon languageCode={proficiency.languageCode} size={1.2} />
            {proficiency.isPrimary && (
              <span className="absolute -top-1 -right-1 w-2 h-2 bg-blue-500 rounded-full border border-white"></span>
            )}
          </div>
        </Tooltip>
      ))}
    </div>
  );
}