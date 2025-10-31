import type { Correction } from '../../types';
import { ErrorSeverity, ErrorType } from '../../types';

interface CorrectionTooltipProps {
  correction: Correction;
}

const errorTypeLabels: Record<ErrorType, string> = {
  [ErrorType.GRAMMAR]: 'Grammar',
  [ErrorType.SPELLING]: 'Spelling',
  [ErrorType.VOCABULARY]: 'Vocabulary',
  [ErrorType.WORD_ORDER]: 'Word Order',
  [ErrorType.VERB_FORM]: 'Verb Form',
  [ErrorType.ARTICLE]: 'Article',
  [ErrorType.PREPOSITION]: 'Preposition',
  [ErrorType.PUNCTUATION]: 'Punctuation',
  [ErrorType.OTHER]: 'Other',
};

const severityColors: Record<ErrorSeverity, string> = {
  [ErrorSeverity.CRITICAL]: 'bg-red-500',
  [ErrorSeverity.HIGH]: 'bg-orange-500',
  [ErrorSeverity.MEDIUM]: 'bg-yellow-500',
  [ErrorSeverity.LOW]: 'bg-blue-500',
};

export function CorrectionTooltip({ correction }: CorrectionTooltipProps) {
  return (
    <div className="bg-white rounded-lg shadow-xl border border-slate-200 p-4 max-w-sm">
      <div className="space-y-3">
        {/* Error Type Badge */}
        <div className="flex items-center gap-2">
          <span className="inline-flex items-center px-2 py-1 rounded text-xs font-medium bg-blue-100 text-blue-800">
            {errorTypeLabels[correction.errorType]}
          </span>
          <div className="flex items-center gap-1">
            <span
              className={`w-2 h-2 rounded-full ${severityColors[correction.severity]}`}
              aria-label={`Severity: ${correction.severity.toLowerCase()}`}
            />
            <span className="text-xs text-slate-600">
              {correction.severity.toLowerCase()}
            </span>
          </div>
        </div>

        {/* Original Text */}
        <div>
          <span className="text-xs text-slate-500 font-medium">
            Original:
          </span>
          <p className="text-sm text-slate-700 line-through mt-1">
            {correction.originalText}
          </p>
        </div>

        {/* Corrected Text */}
        <div>
          <span className="text-xs text-slate-500 font-medium">
            Corrected:
          </span>
          <p className="text-sm text-green-600 font-medium mt-1">
            {correction.correctedText}
          </p>
        </div>

        {/* Explanation */}
        {correction.explanation && (
          <div>
            <span className="text-xs text-slate-500 font-medium">
              Explanation:
            </span>
            <p className="text-sm text-slate-700 mt-1">
              {correction.explanation}
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
