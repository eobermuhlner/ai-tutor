import { CEFRLevel, CourseCategory } from '../../types';

interface FilterBarProps {
  selectedLevel?: CEFRLevel;
  selectedCategory?: CourseCategory;
  onLevelChange: (level?: CEFRLevel) => void;
  onCategoryChange: (category?: CourseCategory) => void;
}

export default function FilterBar({
  selectedLevel,
  selectedCategory,
  onLevelChange,
  onCategoryChange,
}: FilterBarProps) {
  const levels: (CEFRLevel | undefined)[] = [
    undefined,
    CEFRLevel.None,
    CEFRLevel.A1,
    CEFRLevel.A2,
    CEFRLevel.B1,
    CEFRLevel.B2,
    CEFRLevel.C1,
    CEFRLevel.C2,
  ];

  const categories: (CourseCategory | undefined)[] = [
    undefined,
    CourseCategory.GENERAL,
    CourseCategory.BUSINESS,
    CourseCategory.TRAVEL,
    CourseCategory.ACADEMIC,
    CourseCategory.EXAM_PREP,
  ];

  return (
    <div className="mb-6 flex flex-wrap gap-4">
      <div className="flex-1 min-w-[200px]">
        <label className="mb-2 block text-sm font-medium text-gray-700">
          Level
        </label>
        <select
          value={selectedLevel || ''}
          onChange={(e) =>
            onLevelChange(e.target.value ? (e.target.value as CEFRLevel) : undefined)
          }
          className="w-full rounded-lg border border-gray-300 px-4 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          <option value="">All Levels</option>
          {levels.slice(1).map((level) => (
            <option key={level} value={level}>
              {level}
            </option>
          ))}
        </select>
      </div>

      <div className="flex-1 min-w-[200px]">
        <label className="mb-2 block text-sm font-medium text-gray-700">
          Category
        </label>
        <select
          value={selectedCategory || ''}
          onChange={(e) =>
            onCategoryChange(
              e.target.value ? (e.target.value as CourseCategory) : undefined
            )
          }
          className="w-full rounded-lg border border-gray-300 px-4 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          <option value="">All Categories</option>
          {categories.slice(1).map((category) => (
            <option key={category} value={category}>
              {category?.replace('_', ' ')}
            </option>
          ))}
        </select>
      </div>
    </div>
  );
}
