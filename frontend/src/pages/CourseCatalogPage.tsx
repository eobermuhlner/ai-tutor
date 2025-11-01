import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Layout from '../components/layout/Layout';
import CourseCard from '../components/catalog/CourseCard';
import FilterBar from '../components/catalog/FilterBar';
import Spinner from '../components/ui/Spinner';
import EmptyState from '../components/ui/EmptyState';
import { getCourses, getLanguages } from '../api/catalog';
import { formatLanguageDisplay, getLanguageAriaLabel } from '../utils/languageDisplay';
import type { Course, CEFRLevel, CourseCategory, Language } from '../types';
import toast from 'react-hot-toast';

export default function CourseCatalogPage() {
  const { code } = useParams<{ code: string }>();
  const [courses, setCourses] = useState<Course[]>([]);
  const [languages, setLanguages] = useState<Language[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [selectedLevel, setSelectedLevel] = useState<CEFRLevel>();
  const [selectedCategory, setSelectedCategory] = useState<CourseCategory>();
  const navigate = useNavigate();

  const loadData = useCallback(async () => {
    if (!code) return;

    setIsLoading(true);
    try {
      const [coursesData, languagesData] = await Promise.all([
        getCourses(code, 'en', selectedLevel, selectedCategory),
        getLanguages(),
      ]);
      setCourses(coursesData);
      setLanguages(languagesData);
    } catch (error) {
      toast.error('Failed to load courses');
      console.error('Error loading courses:', error);
    } finally {
      setIsLoading(false);
    }
  }, [code, selectedLevel, selectedCategory]);

  useEffect(() => {
    if (code) {
      loadData();
    }
  }, [code, loadData]);

  const currentLanguage = languages.find((lang) => lang.code === code);

  // Build display text - wait for language data to load before showing flag
  let displayText: string;
  let languageAriaLabel: string | undefined;

  if (!currentLanguage && languages.length === 0) {
    // Still loading languages
    displayText = 'Loading...';
    languageAriaLabel = 'Loading';
  } else if (currentLanguage) {
    // Language found - show with flag emoji
    displayText = `${formatLanguageDisplay(currentLanguage)} Courses`;
    languageAriaLabel = getLanguageAriaLabel(currentLanguage);
  } else {
    // Languages loaded but current not found - fallback
    displayText = `${code?.toUpperCase()} Courses`;
    languageAriaLabel = code;
  }

  const handleCourseClick = (courseId: string) => {
    navigate(`/courses/${courseId}`);
  };

  return (
    <Layout>
      <div className="mb-8 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-4xl font-bold text-slate-900" aria-label={`Courses for ${languageAriaLabel}`}>
            {displayText}
          </h1>
          <p className="mt-2 text-slate-600">
            Browse and enroll in courses tailored to your level
          </p>
        </div>
        <button
          onClick={() => navigate('/languages')}
          className="text-brand-600 hover:text-brand-700 font-medium transition-colors flex items-center gap-1 self-start sm:self-auto"
        >
          ← Back to Languages
        </button>
      </div>

      <FilterBar
        selectedLevel={selectedLevel}
        selectedCategory={selectedCategory}
        onLevelChange={setSelectedLevel}
        onCategoryChange={setSelectedCategory}
      />

      {isLoading ? (
        <div className="flex items-center justify-center py-12">
          <Spinner size="lg" />
        </div>
      ) : courses.length === 0 ? (
        <EmptyState
          title="No courses found"
          message="There are no courses matching your criteria. Try adjusting your filters."
          action={{
            label: 'Clear Filters',
            onClick: () => {
              setSelectedLevel(undefined);
              setSelectedCategory(undefined);
            },
          }}
        />
      ) : (
        <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
          {courses.map((course) => (
            <CourseCard
              key={course.id}
              course={course}
              onClick={() => handleCourseClick(course.id)}
              language={currentLanguage}
            />
          ))}
        </div>
      )}
    </Layout>
  );
}
