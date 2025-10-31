import type { Language } from '../../types';
import { getLanguageAriaLabel } from '../../utils/languageDisplay';

interface LanguageCardProps {
  language: Language;
  onClick: () => void;
}

export default function LanguageCard({ language, onClick }: LanguageCardProps) {
  return (
    <button
      onClick={onClick}
      className="group flex flex-col items-center rounded-2xl border-2 border-slate-200 bg-white p-6 shadow-soft transition-all hover:border-brand-500 hover:shadow-soft-lg hover:-translate-y-1"
      aria-label={getLanguageAriaLabel(language)}
    >
      <div className="mb-4 flex h-16 w-16 items-center justify-center text-5xl">
        {language.flagEmoji}
      </div>
      <h3 className="text-lg font-semibold text-slate-900 group-hover:text-brand-600 transition-colors">
        {language.nativeName}
      </h3>
      <p className="text-sm text-slate-600 mt-1">
        {language.name}
      </p>
    </button>
  );
}
