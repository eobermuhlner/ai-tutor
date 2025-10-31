import { useState } from 'react';
import { CEFRLevel } from '../../types';

interface SkillBreakdownBadgeProps {
  overall: CEFRLevel;
  grammar?: CEFRLevel | string | null;
  vocabulary?: CEFRLevel | string | null;
  fluency?: CEFRLevel | string | null;
  comprehension?: CEFRLevel | string | null;
}

export default function SkillBreakdownBadge({
  overall,
  grammar,
  vocabulary,
  fluency,
  comprehension,
}: SkillBreakdownBadgeProps) {
  const [isExpanded, setIsExpanded] = useState(false);

  // Check if any skills are available
  const hasSkills = grammar || vocabulary || fluency || comprehension;

  if (!hasSkills) {
    return (
      <span className="text-sm font-medium text-slate-600">
        {overall}
      </span>
    );
  }

  const formatSkill = (skill: CEFRLevel | string | null | undefined): string => {
    if (!skill) return 'Unknown';
    return typeof skill === 'string' ? skill : skill;
  };

  return (
    <div className="relative inline-block">
      <button
        onClick={() => setIsExpanded(!isExpanded)}
        className="group flex items-center gap-1.5 text-sm font-medium text-slate-600 hover:text-brand-600 transition-colors"
      >
        <span>{overall}</span>
        <svg
          className={`w-3.5 h-3.5 transition-transform ${isExpanded ? 'rotate-180' : ''}`}
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
        </svg>
        {hasSkills && (
          <svg 
            className="w-3 h-3 text-slate-400" 
            fill="none" 
            stroke="currentColor" 
            viewBox="0 0 24 24"
            aria-label="Info"
          >
            <title>Skill levels updated manually - not in real-time</title>
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        )}
      </button>

      {isExpanded && (
        <div className="absolute top-full left-0 mt-2 w-64 bg-white rounded-xl shadow-2xl border border-slate-200 p-4 z-50">
          <div className="flex items-center justify-between mb-3">
            <h3 className="text-xs font-semibold text-slate-700 uppercase tracking-wide">
              Skill Breakdown
            </h3>
            <svg 
              className="w-4 h-4 text-slate-500" 
              fill="none" 
              stroke="currentColor" 
              viewBox="0 0 24 24"
            >
              <title>Updated manually - not in real-time</title>
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <div className="space-y-2">
            {grammar && (
              <div className="flex items-center justify-between">
                <span className="text-sm text-slate-600">Grammar</span>
                <span className="text-sm font-bold text-slate-900">{formatSkill(grammar)}</span>
              </div>
            )}
            {vocabulary && (
              <div className="flex items-center justify-between">
                <span className="text-sm text-slate-600">Vocabulary</span>
                <span className="text-sm font-bold text-slate-900">{formatSkill(vocabulary)}</span>
              </div>
            )}
            {fluency && (
              <div className="flex items-center justify-between">
                <span className="text-sm text-slate-600">Fluency</span>
                <span className="text-sm font-bold text-slate-900">{formatSkill(fluency)}</span>
              </div>
            )}
            {comprehension && (
              <div className="flex items-center justify-between">
                <span className="text-sm text-slate-600">Comprehension</span>
                <span className="text-sm font-bold text-slate-900">{formatSkill(comprehension)}</span>
              </div>
            )}
          </div>
          <div className="mt-3 pt-3 border-t border-slate-100">
            <p className="text-xs text-slate-500">
              Detailed skills updated manually, not in real-time
            </p>
          </div>
        </div>
      )}
    </div>
  );
}
