import { useState, useRef, useEffect } from 'react';
import { TeachingStyle } from '../../types';

interface TeachingStyleSelectorProps {
  currentStyle: TeachingStyle;
  onStyleChange: (style: TeachingStyle) => void;
  disabled?: boolean;
}

const styleConfig: Record<
  TeachingStyle,
  { label: string; description: string; icon: string; color: string }
> = {
  [TeachingStyle.Reactive]: {
    label: 'Reactive',
    description: 'Follows your conversational lead',
    icon: '🎧',
    color: 'bg-slate-100 text-slate-700',
  },
  [TeachingStyle.Guided]: {
    label: 'Guided',
    description: 'Strategic prompts and questions',
    icon: '🧭',
    color: 'bg-slate-100 text-slate-700',
  },
  [TeachingStyle.Directive]: {
    label: 'Directive',
    description: 'Explicit instruction and lessons',
    icon: '📚',
    color: 'bg-slate-100 text-slate-700',
  },
};

export default function TeachingStyleSelector({
  currentStyle,
  onStyleChange,
  disabled,
}: TeachingStyleSelectorProps) {
  const [isOpen, setIsOpen] = useState(false);
  const popoverRef = useRef<HTMLDivElement>(null);
  const buttonRef = useRef<HTMLButtonElement>(null);

  const displayConfig = styleConfig[currentStyle];

  // Close popover when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        isOpen &&
        popoverRef.current &&
        buttonRef.current &&
        !popoverRef.current.contains(event.target as Node) &&
        !buttonRef.current.contains(event.target as Node)
      ) {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [isOpen]);

  const handleStyleSelect = (style: TeachingStyle) => {
    onStyleChange(style);
    setIsOpen(false);
  };

  return (
    <div className="relative">
      {/* Status Badge */}
      <button
        ref={buttonRef}
        onClick={() => !disabled && setIsOpen(!isOpen)}
        disabled={disabled}
        className={`group flex items-center gap-2 rounded-full px-3 py-1.5 text-sm font-medium transition-all disabled:cursor-not-allowed disabled:opacity-60 ${displayConfig.color} hover:shadow-md`}
        title={displayConfig.description}
      >
        <span>{displayConfig.label}</span>

        {/* Settings/gear icon */}
        <svg
          className="w-3.5 h-3.5 opacity-50 group-hover:opacity-100 transition-opacity"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"
          />
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
          />
        </svg>
      </button>

      {/* Popover Menu */}
      {isOpen && (
        <div
          ref={popoverRef}
          className="absolute left-0 top-full mt-2 w-80 rounded-xl bg-white shadow-2xl border border-slate-200 py-2 z-50"
        >
          <div className="px-4 py-2 border-b border-slate-100">
            <h3 className="text-sm font-semibold text-slate-900">Teaching Style</h3>
            <p className="text-xs text-slate-500 mt-0.5">
              How your tutor approaches lessons
            </p>
          </div>

          <div className="py-2">
            {Object.values(TeachingStyle).map((style) => {
              const config = styleConfig[style];
              return (
                <button
                  key={style}
                  onClick={() => handleStyleSelect(style)}
                  className={`w-full px-4 py-3 text-left transition-colors hover:bg-slate-50 ${
                    currentStyle === style ? 'bg-brand-50' : ''
                  }`}
                >
                  <div className="flex items-start gap-3">
                    <div className="flex-shrink-0 mt-0.5">
                      {currentStyle === style ? (
                        <svg className="w-5 h-5 text-brand-600" fill="currentColor" viewBox="0 0 20 20">
                          <path
                            fillRule="evenodd"
                            d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"
                            clipRule="evenodd"
                          />
                        </svg>
                      ) : (
                        <div className="w-5 h-5 rounded-full border-2 border-slate-300" />
                      )}
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2">
                        <span className="text-sm font-medium text-slate-900">{config.label}</span>
                      </div>
                      <p className="text-xs text-slate-500 mt-1">
                        {config.description}
                      </p>
                    </div>
                  </div>
                </button>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}
