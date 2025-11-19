import type { Course, Language } from '../../types';
import FlagIcon from '../ui/FlagIcon';

interface CourseCardProps {
  course: Course;
  onClick: () => void;
  language?: Language;
}

export default function CourseCard({ course, onClick, language }: CourseCardProps) {
  const difficultyColors = {
    Easy: 'bg-gradient-to-r from-emerald-500 to-green-600 text-white',
    Medium: 'bg-gradient-to-r from-amber-500 to-orange-600 text-white',
    Hard: 'bg-gradient-to-r from-red-500 to-rose-600 text-white',
  };

  const categoryColors = {
    General: 'bg-gradient-to-r from-blue-500 to-cyan-600 text-white',
    Business: 'bg-gradient-to-r from-purple-500 to-violet-600 text-white',
    Travel: 'bg-gradient-to-r from-pink-500 to-rose-600 text-white',
    Academic: 'bg-gradient-to-r from-indigo-500 to-blue-600 text-white',
    ExamPrep: 'bg-gradient-to-r from-orange-500 to-amber-600 text-white',
    Conversational: 'bg-gradient-to-r from-green-500 to-emerald-600 text-white',
    Grammar: 'bg-gradient-to-r from-amber-500 to-yellow-600 text-white',
    Hobby: 'bg-gradient-to-r from-purple-500 to-fuchsia-600 text-white',
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
        {course.isDraft && (
          <span className="inline-flex items-center rounded-full bg-amber-100 px-2.5 py-0.5 text-xs font-semibold text-amber-800">
            Draft
          </span>
        )}
      </div>

      <p className="mb-4 line-clamp-2 text-sm text-slate-600">
        {course.description || course.shortDescription}
      </p>

      <div className="mt-auto flex flex-wrap gap-2">
        {language && (
          <span className="inline-flex items-center gap-1 rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-700">
            <FlagIcon languageCode={language.code} size={1} />
            {language.nativeName.split('(')[0].trim()}
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
