import { useState, useEffect } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { getCurrentLesson, getCourseCurriculum, type LessonMetadata } from '../../api/lessons';
import { navigateLesson, navigateToSpecificLesson } from '../../api/chat';
import Spinner from '../ui/Spinner';
import Button from '../ui/Button';
import Select from '../ui/Select';
import type { LessonContent } from '../../types';

interface LessonPanelProps {
  sessionId: string;
  courseId?: string; // Course ID is present for course-based sessions
}

/**
 * Remove frontmatter from markdown content
 * Frontmatter is the YAML/TOML section at the beginning of the file between --- markers
 */
function removeFrontmatter(markdown: string): string {
  // Match frontmatter at the start of the document
  // Pattern: starts with ---, followed by any content, ends with ---
  const frontmatterRegex = /^---\s*\n[\s\S]*?\n---\s*\n/;
  return markdown.replace(frontmatterRegex, '');
}

export default function LessonPanel({ sessionId, courseId }: LessonPanelProps) {
  const [lesson, setLesson] = useState<LessonContent | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isNavigating, setIsNavigating] = useState(false);
  const [showNavigation, setShowNavigation] = useState(false); // Only show if courseId is present
  const [canNavigateNext, setCanNavigateNext] = useState(true); // Assume true initially
  const [canNavigatePrevious, setCanNavigatePrevious] = useState(true); // Assume true initially
  const [lessons, setLessons] = useState<LessonMetadata[]>([]); // Store the list of all lessons
  const [selectedLesson, setSelectedLesson] = useState<string>(''); // Track selected lesson ID

  useEffect(() => {
    const loadLesson = async () => {
      try {
        setIsLoading(true);
        setError(null);
        setCanNavigateNext(true); // Reset navigation status when loading
        setCanNavigatePrevious(true);

        const lessonData = await getCurrentLesson(sessionId);
        setLesson(lessonData);

        // If we have a courseId, fetch the curriculum to determine navigation status
        if (courseId) {
          try {
            const curriculum = await getCourseCurriculum(courseId);
            // Set all lessons for the dropdown
            setLessons(curriculum.lessons);

            // Set selected lesson to the current one
            setSelectedLesson(lessonData.id);

            // Find current lesson's position in the curriculum
            const currentIndex = curriculum.lessons.findIndex(lesson => lesson.id === lessonData.id);

            if (currentIndex !== -1) {
              setCanNavigatePrevious(currentIndex > 0);
              setCanNavigateNext(currentIndex < curriculum.lessons.length - 1);
            }
          } catch (curriculumErr) {
            console.error('Failed to load curriculum for navigation status:', curriculumErr);
            // Fallback: keep initial assumption that both directions are possible
          }
        }
      } catch (err) {
        console.error('Failed to load lesson:', err);
        setError('Failed to load lesson. This session may not have an associated lesson.');
      } finally {
        setIsLoading(false);
      }
    };

    if (sessionId) {
      loadLesson();
    }
  }, [sessionId, courseId]);

  useEffect(() => {
    // Determine if navigation should be shown based on presence of courseId
    setShowNavigation(!!courseId);
  }, [courseId]);

  const handleNavigation = async (direction: 'NEXT' | 'PREVIOUS') => {
    if (!sessionId || !showNavigation || isNavigating) return;

    try {
      setIsNavigating(true);
      setError(null);

      await navigateLesson(sessionId, direction);

      // After successful navigation, reload the lesson content
      const lessonData = await getCurrentLesson(sessionId);
      setLesson(lessonData);

      // Update navigation status based on curriculum position
      if (courseId) {
        try {
          const curriculum = await getCourseCurriculum(courseId);
          // Set all lessons for the dropdown
          setLessons(curriculum.lessons);

          // Update the selected lesson
          setSelectedLesson(lessonData.id);

          // Find current lesson's position in the curriculum
          const currentIndex = curriculum.lessons.findIndex(lesson => lesson.id === lessonData.id);

          if (currentIndex !== -1) {
            setCanNavigatePrevious(currentIndex > 0);
            setCanNavigateNext(currentIndex < curriculum.lessons.length - 1);
          }
        } catch (curriculumErr) {
          console.error('Failed to load curriculum for navigation status after navigation:', curriculumErr);
          // Fallback: reset to enable both directions
          setCanNavigateNext(true);
          setCanNavigatePrevious(true);
        }
      } else {
        // If no courseId is available, reset the navigation states
        setCanNavigateNext(true);
        setCanNavigatePrevious(true);
        setLessons([]);
        setSelectedLesson('');
      }
    } catch (err: unknown) {
      console.error(`Failed to navigate ${direction.toLowerCase()} lesson:`, err);

      // Check if it's a 404 error (boundary case)
      const axiosError = err as { response?: { status: number } };
      if (axiosError.response?.status === 404) {
        // Update navigation status to reflect that we're at a boundary
        if (direction === 'NEXT') {
          setCanNavigateNext(false);
        } else {
          setCanNavigatePrevious(false);
        }

        setError(`No ${direction.toLowerCase()} lesson available. You've reached the ${direction === 'NEXT' ? 'end' : 'beginning'} of the course.`);
      } else {
        setError(`Failed to navigate to ${direction.toLowerCase()} lesson. Please try again.`);
      }
    } finally {
      setIsNavigating(false);
    }
  };

  const handleSpecificLessonNavigation = async (lessonId: string) => {
    if (!sessionId || !courseId || !lessonId || isNavigating) return;

    try {
      setIsNavigating(true);
      setError(null);

      await navigateToSpecificLesson(sessionId, lessonId);

      // After successful navigation, reload the lesson content
      const lessonData = await getCurrentLesson(sessionId);
      setLesson(lessonData);

      // Update the selected lesson state
      setSelectedLesson(lessonData.id);

      // Update navigation status based on curriculum position
      try {
        const curriculum = await getCourseCurriculum(courseId);
        // Set all lessons for the dropdown
        setLessons(curriculum.lessons);

        // Find current lesson's position in the curriculum
        const currentIndex = curriculum.lessons.findIndex(lesson => lesson.id === lessonData.id);

        if (currentIndex !== -1) {
          setCanNavigatePrevious(currentIndex > 0);
          setCanNavigateNext(currentIndex < curriculum.lessons.length - 1);
        }
      } catch (curriculumErr) {
        console.error('Failed to load curriculum for navigation status after specific lesson navigation:', curriculumErr);
        // Fallback: reset to enable both directions
        setCanNavigateNext(true);
        setCanNavigatePrevious(true);
      }
    } catch (err: unknown) {
      console.error(`Failed to navigate to specific lesson:`, err);
      setError('Failed to navigate to the selected lesson. Please try again.');
    } finally {
      setIsNavigating(false);
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-8">
        <Spinner size="md" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="text-sm text-slate-500 text-center py-8">
        {error}
      </div>
    );
  }

  if (!lesson) {
    return (
      <div className="text-sm text-slate-500 text-center py-8">
        No lesson found for this session.
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* Lesson Header */}
      <div className="border-b border-slate-200 pb-4">
        <h2 className="text-lg font-semibold text-slate-900">{lesson.title}</h2>
        {lesson.weekNumber && (
          <p className="text-sm text-slate-600 mt-1">Week {lesson.weekNumber}</p>
        )}
        {lesson.estimatedDuration && (
          <p className="text-sm text-slate-600">Duration: {lesson.estimatedDuration}</p>
        )}
      </div>

      {/* Navigation Controls - Only show for course-based sessions */}
      {showNavigation && (
        <div className="flex items-center justify-between pt-2 border-t border-slate-200 pt-4">
          <Button
            onClick={() => handleNavigation('PREVIOUS')}
            disabled={isNavigating || !canNavigatePrevious}
            variant="outline"
            size="sm"
            className="flex items-center gap-2"
          >
            {isNavigating ? (
              <>
                <Spinner size="sm" />
                <span>Navigating...</span>
              </>
            ) : (
              <>
                ← Previous
              </>
            )}
          </Button>

          {/* Lesson Selection Dropdown */}
          <div className="flex flex-col items-center gap-1">
            <Select
              value={selectedLesson}
              onChange={(value) => handleSpecificLessonNavigation(value as string)}
              disabled={isNavigating || lessons.length === 0}
              className="w-40 text-xs"
            >
              <option value="">Select Lesson</option>
              {lessons.map((lesson) => (
                <option key={lesson.id} value={lesson.id}>
                  {lesson.file}
                </option>
              ))}
            </Select>
            <span className="text-xs text-slate-500">Lesson</span>
          </div>

          <Button
            onClick={() => handleNavigation('NEXT')}
            disabled={isNavigating || !canNavigateNext}
            variant="outline"
            size="sm"
            className="flex items-center gap-2"
          >
            {isNavigating ? (
              <>
                <Spinner size="sm" />
                <span>Navigating...</span>
              </>
            ) : (
              <>
                Next →
              </>
            )}
          </Button>
        </div>
      )}

      {/* Markdown Content */}
      <div className="prose prose-sm max-w-none prose-slate">
        <ReactMarkdown
          remarkPlugins={[remarkGfm]}
          components={{
            // Customize heading styles
            h1: ({ ...props }) => <h1 className="text-xl font-bold text-slate-900 mt-6 mb-3" {...props} />,
            h2: ({ ...props }) => <h2 className="text-lg font-semibold text-slate-900 mt-5 mb-2" {...props} />,
            h3: ({ ...props }) => <h3 className="text-base font-semibold text-slate-800 mt-4 mb-2" {...props} />,
            // Customize list styles
            ul: ({ ...props }) => <ul className="list-disc list-inside space-y-1 text-slate-700" {...props} />,
            ol: ({ ...props }) => <ol className="list-decimal list-inside space-y-1 text-slate-700" {...props} />,
            // Customize paragraph styles
            p: ({ ...props }) => <p className="text-slate-700 leading-relaxed mb-3" {...props} />,
            code: (({ inline, ...props }: { inline?: boolean } & React.HTMLAttributes<HTMLElement>) => {
              return inline ? (
                <code className="px-1.5 py-0.5 bg-slate-100 text-slate-800 rounded text-sm font-mono" {...props} />
              ) : (
                <code className="block p-3 bg-slate-100 text-slate-800 rounded text-sm font-mono overflow-x-auto" {...props} />
              );
            }),
            // Customize blockquotes
            blockquote: ({ ...props }) =>
              <blockquote className="border-l-4 border-brand-500 pl-4 italic text-slate-600" {...props} />,
            // Customize links
            a: ({ ...props }) =>
              <a className="text-brand-600 hover:text-brand-700 underline" {...props} />,
            // Customize tables
            table: ({ ...props }) =>
              <div className="overflow-x-auto">
                <table className="min-w-full border-collapse border border-slate-300" {...props} />
              </div>,
            th: ({ ...props }) =>
              <th className="border border-slate-300 px-3 py-2 bg-slate-100 font-semibold text-left" {...props} />,
            td: ({ ...props }) =>
              <td className="border border-slate-300 px-3 py-2" {...props} />,
          }}
        >
          {removeFrontmatter(lesson.fullMarkdown)}
        </ReactMarkdown>
      </div>
    </div>
  );
}
