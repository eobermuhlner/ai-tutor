import { useState, useRef, useEffect } from 'react';
import { ConversationPhase } from '../../types';

interface PhaseSelectorProps {
  currentPhase: ConversationPhase;
  effectivePhase?: ConversationPhase | null;
  onPhaseChange: (phase: ConversationPhase) => void;
  disabled?: boolean;
}

const phaseConfig: Record<
  ConversationPhase,
  { label: string; description: string; color: string }
> = {
  FREE: {
    label: 'Free Conversation',
    description: 'Natural conversation, corrections are shown on hover',
    color: 'bg-slate-100 text-slate-700',
  },
  CORRECTION: {
    label: 'Correction Mode',
    description: 'Natural conversation with minimal corrections',
    color: 'bg-slate-100 text-slate-700',
  },
  DRILL: {
    label: 'Practice Mode',
    description: 'Focused practice with explicit corrections',
    color: 'bg-slate-100 text-slate-700',
  },
  AUTO: {
    label: 'Auto',
    description: 'Tutor adapts mode automatically',
    color: 'bg-slate-100 text-slate-700',
  },
};

export default function PhaseSelector({
  currentPhase,
  effectivePhase,
  onPhaseChange,
  disabled,
}: PhaseSelectorProps) {
  const [isOpen, setIsOpen] = useState(false);
  const popoverRef = useRef<HTMLDivElement>(null);
  const buttonRef = useRef<HTMLButtonElement>(null);

  // Determine what to display
  const isAutoMode = currentPhase === ConversationPhase.AUTO;
  const displayConfig = phaseConfig[currentPhase] || phaseConfig.FREE;
  const effectiveConfig = effectivePhase && effectivePhase !== ConversationPhase.AUTO
    ? phaseConfig[effectivePhase]
    : null;

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

  const handlePhaseSelect = (phase: ConversationPhase) => {
    onPhaseChange(phase);
    setIsOpen(false);
  };

  return (
    <div className="relative">
      {/* Status Badge with Settings Icon */}
      <button
        ref={buttonRef}
        onClick={() => !disabled && setIsOpen(!isOpen)}
        disabled={disabled}
        className={`group flex items-center gap-2 rounded-full px-3 py-1.5 text-sm font-medium transition-all disabled:cursor-not-allowed disabled:opacity-60 ${displayConfig.color} hover:shadow-md`}
        title={displayConfig.description}
      >
        <span>
          {displayConfig.label}
          {isAutoMode && effectiveConfig && (
            <span className="opacity-60"> → {effectiveConfig.label}</span>
          )}
        </span>

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
            <h3 className="text-sm font-semibold text-slate-900">Learning Mode</h3>
            <p className="text-xs text-slate-500 mt-0.5">
              {isAutoMode
                ? 'Tutor is adapting to your needs automatically'
                : 'You have manually selected a mode'
              }
            </p>
          </div>

          <div className="py-2">
            {/* Auto Mode Option */}
            <button
              onClick={() => handlePhaseSelect(ConversationPhase.AUTO)}
              className={`w-full px-4 py-3 text-left transition-colors hover:bg-slate-50 ${
                currentPhase === ConversationPhase.AUTO ? 'bg-brand-50' : ''
              }`}
            >
              <div className="flex items-start gap-3">
                <div className="flex-shrink-0 mt-0.5">
                  {currentPhase === ConversationPhase.AUTO ? (
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
                    <svg className="w-4 h-4 text-brand-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
                    </svg>
                    <span className="text-sm font-medium text-slate-900">Adaptive Mode</span>
                    <span className="text-xs text-brand-600 font-medium">Recommended</span>
                  </div>
                  <p className="text-xs text-slate-500 mt-1">
                    {phaseConfig.AUTO.description}
                    {isAutoMode && effectivePhase && effectivePhase !== ConversationPhase.AUTO && (
                      <span className="block mt-1 text-brand-600">
                        Currently using: {phaseConfig[effectivePhase].label}
                      </span>
                    )}
                  </p>
                </div>
              </div>
            </button>

            <div className="px-4 py-2">
              <div className="text-xs font-medium text-slate-400 uppercase tracking-wide">
                Manual Override
              </div>
            </div>

            {/* Manual Mode Options */}
            {[ConversationPhase.FREE, ConversationPhase.CORRECTION, ConversationPhase.DRILL].map((phase) => (
              <button
                key={phase}
                onClick={() => handlePhaseSelect(phase)}
                className={`w-full px-4 py-3 text-left transition-colors hover:bg-slate-50 ${
                  currentPhase === phase ? 'bg-amber-50' : ''
                }`}
              >
                <div className="flex items-start gap-3">
                  <div className="flex-shrink-0 mt-0.5">
                    {currentPhase === phase ? (
                      <svg className="w-5 h-5 text-amber-600" fill="currentColor" viewBox="0 0 20 20">
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
                    <div className="text-sm font-medium text-slate-900">
                      {phaseConfig[phase].label}
                    </div>
                    <p className="text-xs text-slate-500 mt-1">
                      {phaseConfig[phase].description}
                    </p>
                  </div>
                </div>
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
