import { CEFRLevel } from '../../types';

interface SkillProgressPanelProps {
  grammar?: CEFRLevel | string | null;
  vocabulary?: CEFRLevel | string | null;
  fluency?: CEFRLevel | string | null;
  comprehension?: CEFRLevel | string | null;
  lastAssessedAt?: string | null;
  assessmentCount?: number;
}

export default function SkillProgressPanel({
  grammar,
  vocabulary,
  fluency,
  comprehension,
  lastAssessedAt,
  assessmentCount,
}: SkillProgressPanelProps) {
  const cefrLevels = ['None', 'A1', 'A2', 'B1', 'B2', 'C1', 'C2'];

  const getLevelProgress = (level: CEFRLevel | string | null | undefined): number => {
    if (!level || level === 'Unknown') return 0;
    const levelStr = typeof level === 'string' ? level : level;
    const index = cefrLevels.indexOf(levelStr);
    return index >= 0 ? ((index + 1) / cefrLevels.length) * 100 : 0;
  };

  const formatSkill = (skill: CEFRLevel | string | null | undefined): string => {
    if (!skill) return 'Not assessed';
    return typeof skill === 'string' ? skill : skill;
  };

  const formatDate = (dateString: string | null | undefined): string => {
    if (!dateString) return 'Never';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
  };

  const skills = [
    { label: 'Grammar', value: grammar, icon: '📝' },
    { label: 'Vocabulary', value: vocabulary, icon: '📚' },
    { label: 'Fluency', value: fluency, icon: '💬' },
    { label: 'Comprehension', value: comprehension, icon: '👂' },
  ];

  return (
    <div className="bg-white rounded-2xl shadow-soft border border-slate-200 p-6">
      <div className="flex items-center justify-between mb-6">
        <h3 className="text-lg font-semibold text-slate-900">Skill Progress</h3>
        {lastAssessedAt && (
          <div className="text-xs text-slate-500">
            Last assessed: {formatDate(lastAssessedAt)}
          </div>
        )}
      </div>

      <div className="space-y-4">
        {skills.map((skill) => {
          const progress = getLevelProgress(skill.value);
          const levelStr = formatSkill(skill.value);

          return (
            <div key={skill.label}>
              <div className="flex items-center justify-between mb-2">
                <div className="flex items-center gap-2">
                  <span className="text-lg">{skill.icon}</span>
                  <span className="text-sm font-medium text-slate-700">{skill.label}</span>
                </div>
                <span className="text-sm font-bold text-brand-600">{levelStr}</span>
              </div>
              <div className="w-full bg-slate-100 rounded-full h-2.5 overflow-hidden">
                <div
                  className="bg-gradient-to-r from-brand-500 to-brand-600 h-2.5 rounded-full transition-all duration-500"
                  style={{ width: `${progress}%` }}
                />
              </div>
            </div>
          );
        })}
      </div>

      {assessmentCount !== undefined && assessmentCount > 0 && (
        <div className="mt-6 pt-4 border-t border-slate-100 text-center">
          <p className="text-xs text-slate-500">
            {assessmentCount} assessment{assessmentCount !== 1 ? 's' : ''} completed
          </p>
        </div>
      )}
    </div>
  );
}
