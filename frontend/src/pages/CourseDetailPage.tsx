import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Layout from '../components/layout/Layout';
import Button from '../components/ui/Button';
import Spinner from '../components/ui/Spinner';
import TutorCard from '../components/catalog/TutorCard';
import { getCourse, getLanguages } from '../api/catalog';
import { createSessionFromCourse } from '../api/chat';
import { formatLanguageDisplay, getLanguageAriaLabel } from '../utils/languageDisplay';
import type { CourseDetail, Language } from '../types';
import toast from 'react-hot-toast';

export default function CourseDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [course, setCourse] = useState<CourseDetail | null>(null);
  const [languages, setLanguages] = useState<Language[]>([]);
  const [selectedTutorId, setSelectedTutorId] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isCreatingSession, setIsCreatingSession] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    if (id) {
      loadCourse();
    }
  }, [id]);

  const loadCourse = async () => {
    if (!id) return;

    setIsLoading(true);
    try {
      const [courseData, languagesData] = await Promise.all([
        getCourse(id),
        getLanguages(),
      ]);
      setCourse(courseData);
      setLanguages(languagesData);
      // Auto-select first tutor
      if (courseData.suggestedTutors.length > 0) {
        setSelectedTutorId(courseData.suggestedTutors[0].id);
      }
    } catch (error) {
      toast.error('Failed to load course details');
      console.error('Error loading course:', error);
      navigate('/languages');
    } finally {
      setIsLoading(false);
    }
  };

  const handleStartLearning = async () => {
    if (!id) return;

    if (!selectedTutorId) {
      toast.error('Please select a tutor');
      return;
    }

    setIsCreatingSession(true);
    try {
      const session = await createSessionFromCourse(id, selectedTutorId);
      toast.success('Session created! Starting chat...');
      navigate(`/chat/${session.id}`);
    } catch (error) {
      toast.error('Failed to create session');
      console.error('Error creating session:', error);
    } finally {
      setIsCreatingSession(false);
    }
  };

  if (isLoading) {
    return (
      <Layout>
        <div className="flex items-center justify-center py-12">
          <Spinner size="lg" />
        </div>
      </Layout>
    );
  }

  if (!course) {
    return (
      <Layout>
        <div className="text-center">
          <h1 className="text-2xl font-bold text-gray-900">Course not found</h1>
          <Button
            onClick={() => navigate('/languages')}
            className="mt-4"
            variant="secondary"
          >
            Back to Languages
          </Button>
        </div>
      </Layout>
    );
  }

  const categoryColors = {
    GENERAL: 'bg-blue-100 text-blue-800',
    BUSINESS: 'bg-purple-100 text-purple-800',
    TRAVEL: 'bg-pink-100 text-pink-800',
    ACADEMIC: 'bg-indigo-100 text-indigo-800',
    EXAM_PREP: 'bg-orange-100 text-orange-800',
  };

  const currentLanguage = languages.find((lang) => lang.code === course.languageCode);
  const languageDisplay = currentLanguage
    ? formatLanguageDisplay(currentLanguage)
    : course.languageCode.toUpperCase();
  const languageAriaLabel = currentLanguage
    ? getLanguageAriaLabel(currentLanguage)
    : course.languageCode;

  return (
    <Layout>
      <div className="mb-6">
        <button
          onClick={() => navigate(`/languages/${course.languageCode}/courses`)}
          className="mb-4 text-blue-600 hover:underline"
        >
          ← Back to Courses
        </button>

        <div className="rounded-lg border border-gray-200 bg-white p-8 shadow-sm">
          <div className="mb-6">
            <div className="mb-3 flex items-center gap-2">
              <span className="inline-flex items-center rounded-full bg-brand-100 px-4 py-1.5 text-sm font-semibold text-brand-700" aria-label={languageAriaLabel}>
                {languageDisplay}
              </span>
            </div>
            <h1 className="mb-2 text-3xl font-bold text-gray-900">
              {course.name}
            </h1>
            <p className="text-lg text-gray-600">{course.shortDescription}</p>
          </div>

          <div className="mb-6 flex flex-wrap gap-2">
            <span
              className={`inline-flex items-center rounded-full px-3 py-1 text-sm font-medium ${
                categoryColors[course.category]
              }`}
            >
              {course.category.replace('_', ' ')}
            </span>
            <span className="inline-flex items-center rounded-full bg-gray-100 px-3 py-1 text-sm font-medium text-gray-800">
              {course.startingLevel} → {course.targetLevel}
            </span>
            {course.estimatedWeeks && (
              <span className="inline-flex items-center rounded-full bg-green-100 px-3 py-1 text-sm font-medium text-green-800">
                ~{course.estimatedWeeks} weeks
              </span>
            )}
          </div>

          <div className="mb-6">
            <h2 className="mb-2 text-xl font-semibold text-gray-900">
              Description
            </h2>
            <p className="text-gray-700">{course.description}</p>
          </div>

          {course.learningGoals && course.learningGoals.length > 0 && (
            <div className="mb-6">
              <h2 className="mb-3 text-xl font-semibold text-gray-900">
                Learning Goals
              </h2>
              <ul className="grid grid-cols-1 gap-2 md:grid-cols-2">
                {course.learningGoals.map((goal, index) => (
                  <li
                    key={index}
                    className="flex items-center text-gray-700"
                  >
                    <span className="mr-2 text-blue-600">✓</span>
                    {goal}
                  </li>
                ))}
              </ul>
            </div>
          )}

          {course.suggestedTutors && course.suggestedTutors.length > 0 && (
            <div className="mb-6">
              <h2 className="mb-3 text-xl font-semibold text-gray-900">
                Choose Your Tutor
              </h2>
              <div className="space-y-3">
                {course.suggestedTutors.map((tutor) => (
                  <TutorCard
                    key={tutor.id}
                    tutor={tutor}
                    isSelected={selectedTutorId === tutor.id}
                    onClick={() => setSelectedTutorId(tutor.id)}
                  />
                ))}
                <button
                  onClick={() => navigate(`/tutors/create?language=${course.languageCode}&courseId=${course.id}`)}
                  className="w-full text-left rounded-lg border-2 border-dashed border-gray-300 p-4 transition-all hover:shadow-md hover:border-brand-400 hover:bg-brand-50"
                >
                  <div className="flex items-center gap-3">
                    <div className="flex-shrink-0">
                      <div className="w-16 h-16 rounded-lg bg-gradient-to-br from-brand-400 to-brand-600 flex items-center justify-center text-white text-2xl">
                        +
                      </div>
                    </div>
                    <div>
                      <h3 className="font-semibold text-gray-900">Create Custom Tutor</h3>
                      <p className="text-sm text-gray-600">Design your own personalized tutor for the {currentLanguage ? formatLanguageDisplay(currentLanguage) : course.languageCode} course</p>
                    </div>
                  </div>
                </button>
              </div>
            </div>
          )}

          <div className="flex gap-4">
            <Button
              onClick={handleStartLearning}
              isLoading={isCreatingSession}
              disabled={isCreatingSession || !selectedTutorId}
              size="lg"
            >
              Start Learning
            </Button>
            <Button
              onClick={() => navigate(`/languages/${course.languageCode}/courses`)}
              variant="secondary"
              size="lg"
            >
              Browse More Courses
            </Button>
          </div>
        </div>
      </div>
    </Layout>
  );
}
