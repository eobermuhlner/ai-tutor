import { formatCompactLanguageDisplay } from '../../utils/languageDisplay';
import type { Course, Language } from '../../types';

interface CourseCardProps {
  course: Course;
  onClick: () => void;
  language?: Language;
}

export default function CourseCard({ course, onClick, language }: CourseCardProps) {
  const difficultyColors = {
    BEGINNER: 'bg-gradient-to-r from-emerald-500 to-green-600 text-white',
    INTERMEDIATE: 'bg-gradient-to-r from-amber-500 to-orange-600 text-white',
    ADVANCED: 'bg-gradient-to-r from-red-500 to-rose-600 text-white',
  };

  const categoryColors = {
    GENERAL: 'bg-gradient-to-r from-blue-500 to-cyan-600 text-white',
    BUSINESS: 'bg-gradient-to-r from-purple-500 to-violet-600 text-white',
    TRAVEL: 'bg-gradient-to-r from-pink-500 to-rose-600 text-white',
    ACADEMIC: 'bg-gradient-to-r from-indigo-500 to-blue-600 text-white',
    EXAM_PREP: 'bg-gradient-to-r from-orange-500 to-amber-600 text-white',
  };

  return (
    <button
      onClick={onClick}
      className="group flex flex-col rounded-2xl border-2 border-slate-200 bg-white p-6 text-left shadow-soft transition-all hover:border-brand-500 hover:shadow-soft-lg hover:-translate-y-1"
    >
      <div className="mb-3 flex items-start justify-between">
        <h3 className="text-lg font-semibold text-slate-900 group-hover:text-brand-600 transition-colors">
          {course.name}
        </h3>
      </div>

      <p className="mb-4 line-clamp-2 text-sm text-slate-600">
        {course.description || course.shortDescription}
      </p>

      <div className="mt-auto flex flex-wrap gap-2">
        {language && (
          <span className="inline-flex items-center rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-700">
            {formatCompactLanguageDisplay(language)}
          </span>
        )}
        {course.difficulty && (
          <span
            className={`inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold shadow-sm ${
              difficultyColors[course.difficulty as keyof typeof difficultyColors]
            }`}
          >
            {course.difficulty}
          </span>
        )}
        <span
          className={`inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold shadow-sm ${
            categoryColors[course.category]
          }`}
        >
          {course.category.replace('_', ' ')}
        </span>
        {course.userLevel && (
          <span className="inline-flex items-center rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-700">
            {course.userLevel}
          </span>
        )}
      </div>
    </button>
  );
}
