import { ConversationPhase } from '../../types';

interface PhaseIndicatorProps {
  currentPhase: ConversationPhase;
  effectivePhase?: ConversationPhase | null;
  onPhaseChange: (phase: ConversationPhase) => void;
  disabled?: boolean;
}

const phaseConfig: Record<
  ConversationPhase,
  { label: string; shortLabel: string; className: string; description: string }
> = {
  FREE: {
    label: 'Free Conversation',
    shortLabel: 'Free',
    className: 'bg-gradient-to-r from-brand-400 to-brand-500 text-white shadow-md hover:shadow-lg',
    description: 'No error tracking',
  },
  CORRECTION: {
    label: 'Correction Mode',
    shortLabel: 'Correction',
    className: 'bg-gradient-to-r from-brand-500 to-brand-600 text-white shadow-md hover:shadow-lg',
    description: 'Errors tracked, shown on hover',
  },
  DRILL: {
    label: 'Practice Mode',
    shortLabel: 'Drill',
    className: 'bg-gradient-to-r from-brand-600 to-brand-700 text-white shadow-md hover:shadow-lg',
    description: 'Explicit error discussion',
  },
  AUTO: {
    label: 'Adaptive Mode',
    shortLabel: 'Auto',
    className: 'bg-gradient-to-r from-brand-500 to-brand-600 text-white shadow-md hover:shadow-lg',
    description: 'Automatic phase selection',
  },
};

export default function PhaseIndicator({
  currentPhase,
  effectivePhase,
  onPhaseChange,
  disabled,
}: PhaseIndicatorProps) {
  const currentConfig = phaseConfig[currentPhase] || phaseConfig.FREE;

  // Display label: show effective phase when in Auto mode
  const displayLabel = currentPhase === 'AUTO' && effectivePhase && effectivePhase !== 'AUTO'
    ? `${phaseConfig.AUTO.shortLabel} (${phaseConfig[effectivePhase].shortLabel})`
    : currentConfig.label;

  const displayTitle = currentPhase === 'AUTO' && effectivePhase && effectivePhase !== 'AUTO'
    ? `${currentConfig.description} - currently using ${phaseConfig[effectivePhase].shortLabel}`
    : currentConfig.description;

  return (
    <div className="relative inline-block">
      <select
        value={currentPhase}
        onChange={(e) => onPhaseChange(e.target.value as ConversationPhase)}
        disabled={disabled}
        className={`cursor-pointer rounded-full px-4 py-2 text-sm font-semibold transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-brand-500 focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-60 ${currentConfig.className} [&>option]:bg-white [&>option]:text-slate-900`}
        title={displayTitle}
        style={{
          minWidth: currentPhase === 'AUTO' && effectivePhase ? '180px' : 'auto'
        }}
      >
        {/* Show actual selection value with label */}
        <option value={currentPhase} className="bg-white text-slate-900">
          {displayLabel}
        </option>
        {/* Show other options */}
        {Object.entries(phaseConfig)
          .filter(([phase]) => phase !== currentPhase)
          .map(([phase, config]) => (
            <option key={phase} value={phase} className="bg-white text-slate-900">
              {config.label}
            </option>
          ))}
      </select>
    </div>
  );
}
