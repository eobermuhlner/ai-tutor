import React, { useState, useEffect } from 'react';
import { Plus, Trash2, Edit3, Save, X } from 'lucide-react';
import type { LessonResponse, LessonRequest } from '../../api/lessonManagement';
import { 
  createLesson, 
  updateLesson, 
  deleteLesson 
} from '../../api/lessonManagement';
import Button from '../ui/Button';
import Input from '../ui/Input';
import Textarea from '../ui/Textarea';
interface LessonEditorProps {
  courseId: string;
  lessons: LessonResponse[];
  onLessonsChange: (lessons: LessonResponse[]) => void;
}

interface InternalLesson {
  id: string;
  courseId: string;
  lessonId: string;
  title: string;
  content: string;
  displayOrder: number;
  requiredTurns?: number | null;
  createdAt: string;
  updatedAt: string;
  isEditing?: boolean;
}

// For temporary lessons that don't exist on the backend yet
interface TemporaryLesson {
  id: string;
  courseId: string;
  lessonId: string;
  title: string;
  content: string;
  displayOrder: number;
  requiredTurns?: number | null;
  createdAt: string;
  updatedAt: string;
  isEditing: boolean;
}

const LessonEditor: React.FC<LessonEditorProps> = ({ 
  courseId, 
  lessons, 
  onLessonsChange, 
}) => {
  const [internalLessons, setInternalLessons] = useState<Array<InternalLesson | TemporaryLesson>>([]);
  const [activeLessonId, setActiveLessonId] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  
  // Initialize internal lessons when external lessons change
  useEffect(() => {
    setInternalLessons(lessons.map(lesson => ({ ...lesson, isEditing: false })));
  }, [lessons]);

  const handleAddLesson = async () => {
    // For new courses without a courseId yet, create a temporary lesson
    if (!courseId) {
      const tempLesson: TemporaryLesson = {
        id: `temp-${Date.now()}`,
        courseId: courseId || '', // Use actual courseId or empty string
        lessonId: `week-${internalLessons.length + 1}-new-topic`,
        title: 'New Lesson',
        content: `# New Lesson\n\nThis is a new lesson. Add your content here.`,
        displayOrder: internalLessons.length,
        requiredTurns: 5,
        createdAt: new Date().toISOString(), // Set current date for temporary lesson
        updatedAt: new Date().toISOString(),
        isEditing: true
      };
      
      setInternalLessons([...internalLessons, tempLesson]);
      setActiveLessonId(tempLesson.id);
      // Don't update parent with temporary lessons - only send real lessons
      return;
    }

    const newLessonRequest: LessonRequest = {
      lessonId: `week-${lessons.length + 1}-new-topic`,
      title: 'New Lesson',
      content: `# New Lesson\n\nThis is a new lesson. Add your content here.`,
      displayOrder: lessons.length,
      requiredTurns: 5,
    };
    
    try {
      setLoading(true);
      const newLesson = await createLesson(courseId, newLessonRequest);
      setInternalLessons([...internalLessons, { ...newLesson, isEditing: true }]);
      setActiveLessonId(newLesson.id);
      onLessonsChange([...lessons, newLesson]);
    } catch (error) {
      console.error('Failed to create lesson:', error);
      // TODO: Show error to user
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteLesson = async (lessonId: string, lessonBackendId: string) => {
    if (!window.confirm('Are you sure you want to delete this lesson? This action cannot be undone.')) {
      return;
    }
    
    // If it's a temporary lesson (no courseId yet), just update local state
    if ((lessonId.startsWith('temp-') || !courseId) && !lessonBackendId.startsWith('temp-')) {
      const updatedLessons = internalLessons.filter(lesson => lesson.id !== lessonId);
      setInternalLessons(updatedLessons);
      if (activeLessonId === lessonId) {
        setActiveLessonId(updatedLessons.length > 0 ? updatedLessons[0].id : null);
      }
      // Only update parent if we're dealing with real lessons
      if (!lessonId.startsWith('temp-')) {
        onLessonsChange(updatedLessons.filter(lesson => !lesson.id.startsWith('temp-')));
      }
      return;
    }
    
    try {
      setLoading(true);
      await deleteLesson(courseId, lessonBackendId);
      const updatedLessons = internalLessons.filter(lesson => lesson.id !== lessonId);
      setInternalLessons(updatedLessons);
      if (activeLessonId === lessonId) {
        setActiveLessonId(updatedLessons.length > 0 ? updatedLessons[0].id : null);
      }
      // Only update parent if we're dealing with real lessons
      if (!lessonId.startsWith('temp-')) {
        onLessonsChange(updatedLessons.filter(lesson => !lesson.id.startsWith('temp-')));
      }
    } catch (error) {
      console.error('Failed to delete lesson:', error);
      // TODO: Show error to user
    } finally {
      setLoading(false);
    }
  };

  const handleEditLesson = (lessonId: string) => {
    setInternalLessons(internalLessons.map(lesson => 
      lesson.id === lessonId 
        ? { ...lesson, isEditing: true } 
        : { ...lesson, isEditing: false }
    ));
    setActiveLessonId(lessonId);
  };

  const handleSaveLesson = async (lessonId: string) => {
    const lessonToSave = internalLessons.find(l => l.id === lessonId);
    if (!lessonToSave) return;

    // If it's a temporary lesson (new course) and no courseId available yet, just update the local state
    if (lessonToSave.id.startsWith('temp-') && !courseId) {
      const newInternalLessons = internalLessons.map(lesson => 
        lesson.id === lessonId 
          ? { ...lesson, isEditing: false } 
          : lesson
      );
      setInternalLessons(newInternalLessons);
      // For temporary lessons, don't update the parent
      if (!lessonId.startsWith('temp-')) {
        onLessonsChange(newInternalLessons.filter(lesson => !lesson.id.startsWith('temp-')));
      }
      return;
    }
    
    // Convert InternalLesson to LessonRequest (excluding backend-only fields)
    const lessonRequest: LessonRequest = {
      lessonId: lessonToSave.lessonId,
      title: lessonToSave.title,
      content: lessonToSave.content,
      displayOrder: lessonToSave.displayOrder,
      requiredTurns: lessonToSave.requiredTurns,
    };
    
    try {
      setLoading(true);
      const updatedLesson = await updateLesson(courseId, lessonToSave.lessonId, lessonRequest);
      const newInternalLessons = internalLessons.map(lesson => 
        lesson.id === lessonId 
          ? { ...updatedLesson, isEditing: false } 
          : lesson
      );
      setInternalLessons(newInternalLessons);
      onLessonsChange(newInternalLessons.filter(lesson => !lesson.id.startsWith('temp-')));
    } catch (error) {
      console.error('Failed to update lesson:', error);
      // TODO: Show error to user
    } finally {
      setLoading(false);
    }
  };

  const handleCancelEdit = (lessonId: string) => {
    setInternalLessons(internalLessons.map(lesson => 
      lesson.id === lessonId 
        ? { ...lesson, isEditing: false } 
        : lesson
    ));
  };

  const handleFieldChange = (lessonId: string, field: keyof InternalLesson, value: string | number | null) => {
    setInternalLessons(internalLessons.map(lesson => 
      lesson.id === lessonId 
        ? { ...lesson, [field]: value } 
        : lesson
    ));
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h3 className="text-lg font-medium text-slate-800">Manage Course Lessons</h3>
        <Button onClick={handleAddLesson} variant="primary" disabled={loading}>
          <Plus className="w-4 h-4 mr-2" />
          Add Lesson
        </Button>
      </div>
      
      {internalLessons.length === 0 ? (
        <div className="text-center py-8 bg-slate-50 rounded-lg border border-slate-200">
          <p className="text-slate-500">No lessons added yet. Click "Add Lesson" to create your first lesson.</p>
        </div>
      ) : (
        <div className="space-y-4">
          {internalLessons.map((lesson, index) => (
            <div 
              key={lesson.id} 
              className={`border rounded-lg overflow-hidden transition-all ${
                activeLessonId === lesson.id 
                  ? 'border-brand-500 shadow-md' 
                  : 'border-slate-200'
              }`}
            >
              <div className="flex items-center justify-between bg-slate-50 p-3 border-b">
                <div className="flex items-center space-x-3">
                  <span className="font-medium text-slate-700">
                    {index + 1}. {lesson.title}
                  </span>
                  <span className="text-xs bg-slate-200 text-slate-700 px-2 py-1 rounded">
                    {lesson.lessonId}
                  </span>
                </div>
                <div className="flex space-x-2">
                  {!lesson.isEditing ? (
                    <>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => handleEditLesson(lesson.id)}
                        title="Edit lesson"
                        disabled={loading}
                      >
                        <Edit3 className="w-4 h-4 text-slate-600" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => handleDeleteLesson(lesson.id, lesson.id)}
                        title="Delete lesson"
                        disabled={loading}
                      >
                        <Trash2 className="w-4 h-4 text-red-600" />
                      </Button>
                    </>
                  ) : (
                    <>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => handleSaveLesson(lesson.id)}
                        title="Save lesson"
                        disabled={loading}
                      >
                        <Save className="w-4 h-4 text-green-600" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => handleCancelEdit(lesson.id)}
                        title="Cancel"
                        disabled={loading}
                      >
                        <X className="w-4 h-4 text-slate-600" />
                      </Button>
                    </>
                  )}
                </div>
              </div>
              
              {lesson.isEditing && (
                <div className="p-4 bg-white">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
                    <div>
                      <label className="block text-sm font-medium text-slate-700 mb-1">
                        Lesson ID
                      </label>
                      <Input
                        value={lesson.lessonId}
                        onChange={(e) => handleFieldChange(lesson.id, 'lessonId', e.target.value)}
                        placeholder="e.g., week-01-greetings"
                        disabled={loading}
                      />
                      <p className="mt-1 text-xs text-slate-500">
                        Unique identifier for the lesson (e.g., week-01-topic)
                      </p>
                    </div>
                    
                    <div>
                      <label className="block text-sm font-medium text-slate-700 mb-1">
                        Title
                      </label>
                      <Input
                        value={lesson.title}
                        onChange={(e) => handleFieldChange(lesson.id, 'title', e.target.value)}
                        placeholder="Lesson title"
                        disabled={loading}
                      />
                    </div>
                  </div>
                  
                  <div className="mb-4">
                    <label className="block text-sm font-medium text-slate-700 mb-1">
                      Required Turns
                    </label>
                    <Input
                      type="number"
                      value={lesson.requiredTurns || ''}
                      onChange={(e) => handleFieldChange(lesson.id, 'requiredTurns',
                        e.target.value ? parseInt(e.target.value) : null)}
                      min="1"
                      disabled={loading}
                    />
                    <p className="mt-1 text-xs text-slate-500">
                      Number of conversation turns required before advancing to next lesson
                    </p>
                  </div>
                  
                  <div className="mb-4">
                    <label className="block text-sm font-medium text-slate-700 mb-1">
                      Content
                    </label>
                    <Textarea
                      value={lesson.content}
                      onChange={(e) => handleFieldChange(lesson.id, 'content', e.target.value)}
                      placeholder="Enter lesson content in markdown format..."
                      rows={10}
                      disabled={loading}
                    />
                  </div>
                </div>
              )}
              
              {!lesson.isEditing && (
                <div className="p-4 bg-white">
                  <div className="prose prose-sm max-w-none">
                    <div 
                      className="text-sm text-slate-700 whitespace-pre-wrap"
                    >
                      {lesson.content.substring(0, 200)}{lesson.content.length > 200 ? '...' : ''}
                    </div>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default LessonEditor;