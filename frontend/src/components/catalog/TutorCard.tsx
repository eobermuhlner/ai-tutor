import type { Tutor } from '../../types';
import { getRoleLabel, getRoleColorClass } from '../../utils/tutorRoles';
import TutorImage from '../tutor/TutorImage';

interface TutorCardProps {
  tutor: Tutor;
  isSelected: boolean;
  onClick: () => void;
}

export default function TutorCard({ tutor, isSelected, onClick }: TutorCardProps) {
  console.log('🎓 TutorCard: Tutor data', {
    tutorName: tutor.name,
    tutorAge: tutor.age,
    tutorImageUrl: tutor.imageUrl,
    tutorEmoji: tutor.emoji
  });
  const personalityColors = {
    Casual: 'bg-slate-100 text-slate-700',
    Professional: 'bg-slate-100 text-slate-700',
    Encouraging: 'bg-slate-100 text-slate-700',
    Strict: 'bg-slate-100 text-slate-700',
    Academic: 'bg-slate-100 text-slate-700',
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      onClick();
    }
  };

  return (
    <div
      onClick={onClick}
      onKeyDown={handleKeyDown}
      tabIndex={0}
      role="button"
      className={`w-full text-left rounded-lg border-2 p-4 transition-all hover:shadow-md cursor-pointer ${
        isSelected
          ? 'border-blue-500 bg-blue-50 shadow-md'
          : 'border-gray-200 bg-white hover:border-blue-300'
      }`}
      aria-pressed={isSelected}
    >
      <div className="flex items-start gap-3">
        <div className="flex-shrink-0">
          <TutorImage
            tutorId={tutor.id}
            tutorEmoji={tutor.emoji}
            tutorName={tutor.name}
            size="large"
            rounded="lg"
          />
        </div>
        <div className="flex-1">
          <div className="flex items-center gap-2 mb-1 flex-wrap">
            <h3 className="font-semibold text-gray-900">{tutor.name}</h3>
            <span className="text-xs text-gray-500">
              Age {tutor.age}
            </span>
            <span
              className={`text-xs px-2 py-0.5 rounded-full ${
                personalityColors[tutor.personality]
              }`}
            >
              {tutor.personality}
            </span>
            <span
              className={`inline-flex items-center gap-1 px-2 py-0.5 text-xs font-medium rounded-full ${getRoleColorClass(
                tutor
              )}`}
            >
              {getRoleLabel(tutor)}
            </span>
          </div>
          <p className="text-sm text-gray-600 mb-2">{tutor.persona}</p>
          <p className="text-sm text-gray-500">{tutor.description}</p>
          <div className="flex items-center gap-2 mt-1">
            {tutor.culturalBackground && (
              <p className="text-xs text-gray-400 italic">
                {tutor.culturalBackground}
              </p>
            )}
            {tutor.location && (
              <div className="flex items-center text-xs text-gray-500">
                <svg xmlns="http://www.w3.org/2000/svg" className="h-3 w-3 mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                </svg>
                {tutor.location}
              </div>
            )}
          </div>
        </div>
        {isSelected && (
          <div className="flex-shrink-0">
            <div className="w-6 h-6 rounded-full bg-blue-500 flex items-center justify-center">
              <svg
                className="w-4 h-4 text-white"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M5 13l4 4L19 7"
                />
              </svg>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
