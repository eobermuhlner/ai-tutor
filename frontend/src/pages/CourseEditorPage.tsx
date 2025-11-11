import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ChevronLeft, ChevronRight, Save, CheckCircle, AlertCircle } from 'lucide-react';
import { getLanguages, getTutors } from '../api/catalog';
import { createCourse, updateCourse, getCourse } from '../api/courseManagement';
import { getLessons } from '../api/lessonManagement';
import { useAuthStore } from '../store/authStore';
import type { Language, Tutor } from '../types';
import type { LessonResponse } from '../api/lessonManagement';
import Button from '../components/ui/Button';
import Input from '../components/ui/Input';
import Select from '../components/ui/Select';
import MultilingualTextArea from '../components/ui/MultilingualTextArea';
import { Tabs, TabsList, TabsTrigger } from '../components/ui/Tabs';
import LessonEditor from '../components/course/LessonEditor';

interface FormData {
  // Step 1: Basic Info
  languageCode: string;
  nameJson: string;
  shortDescriptionJson: string;
  descriptionJson: string;
  category: string;
  // Step 2: Levels & Goals
  startingLevel: string;
  targetLevel: string;
  targetAudienceJson: string;
  learningGoalsJson: string;
  // Step 3: Settings
  estimatedWeeks: number | null;
  tagsJson: string;
  suggestedTutorIdsJson: string;
  defaultPhase: string;
}

const STEPS = [
  { id: 'basic', label: 'Basic Info', icon: '📝' },
  { id: 'levels', label: 'Levels & Goals', icon: '🎯' },
  { id: 'settings', label: 'Settings', icon: '⚙️' },
  { id: 'lessons', label: 'Lessons', icon: '📚' },
  { id: 'review', label: 'Review', icon: '✅' },
];

export default function CourseEditorPage() {
  const navigate = useNavigate();
  const { courseId } = useParams<{ courseId: string }>();
  const { user } = useAuthStore();
  const [step, setStep] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [languages, setLanguages] = useState<Language[]>([]);
  const [tutors, setTutors] = useState<Tutor[]>([]);
  const [isCreating, setIsCreating] = useState(true);
  const [initialDataLoaded, setInitialDataLoaded] = useState(false);

  const [formData, setFormData] = useState<FormData>({
    languageCode: '',
    nameJson: '{"en":"New Course"}',
    shortDescriptionJson: '{"en":"Short description"}',
    descriptionJson: '{"en":"Detailed course description"}',
    category: 'GENERAL',
    startingLevel: 'A1',
    targetLevel: 'A2',
    targetAudienceJson: '{"en":"Course target audience"}',
    learningGoalsJson: '{"en":["Goal 1","Goal 2"]}',
    estimatedWeeks: null,
    tagsJson: '["tag1","tag2"]',
    suggestedTutorIdsJson: '[]',
    defaultPhase: 'AUTO',
  });

  const [selectedTutors, setSelectedTutors] = useState<string[]>([]);
  const [lessons, setLessons] = useState<LessonResponse[]>([]);

  useEffect(() => {
    loadInitialData();
  }, []);

  const loadInitialData = async () => {
    try {
      setLoading(true);
      const [langs, tutorsData] = await Promise.all([
        getLanguages('en'),
        getTutors('en', 'en') // For now, using 'en' as placeholder
      ]);
      
      setLanguages(langs);
      setTutors(tutorsData);

      if (courseId) {
        // Load existing course data for editing
        const courseData = await getCourse(courseId);
        setIsCreating(false);
        
        setFormData({
          languageCode: courseData.languageCode,
          nameJson: courseData.nameJson,
          shortDescriptionJson: courseData.shortDescriptionJson,
          descriptionJson: courseData.descriptionJson,
          category: courseData.category,
          startingLevel: courseData.startingLevel,
          targetLevel: courseData.targetLevel,
          targetAudienceJson: courseData.targetAudienceJson,
          learningGoalsJson: courseData.learningGoalsJson,
          estimatedWeeks: courseData.estimatedWeeks ?? null,
          tagsJson: courseData.tagsJson || '[]',
          suggestedTutorIdsJson: courseData.suggestedTutorIdsJson || '[]',
          defaultPhase: courseData.defaultPhase,
        });
        
        // Parse selected tutors from JSON
        try {
          const tutorIds = JSON.parse(courseData.suggestedTutorIdsJson || '[]');
          setSelectedTutors(tutorIds);
        } catch {
          setSelectedTutors([]);
        }
        
        // Load existing lessons for the course
        try {
          const courseLessons = await getLessons(courseId);
          setLessons(courseLessons);
        } catch (err) {
          console.error('Failed to load lessons:', err);
          // Don't set an error state for lessons as it's not critical for course editing
        }
      }
      setInitialDataLoaded(true);
    } catch (err) {
      console.error('Failed to load initial data:', err);
      setError('Failed to load initial data. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (field: keyof FormData, value: any) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }));
  };

  const handleTutorChange = (tutorId: string) => {
    setSelectedTutors(prev => 
      prev.includes(tutorId) 
        ? prev.filter(id => id !== tutorId) 
        : [...prev, tutorId]
    );
  };

  const handleLanguageChangeByValue = (languageCode: string) => {
    // When language changes, we need to update the JSON fields to match
    const nameJson = JSON.stringify({ [languageCode.split('-')[0]]: "New Course", "en": "New Course" });
    const shortDescJson = JSON.stringify({ [languageCode.split('-')[0]]: "Short description", "en": "Short description" });
    const descJson = JSON.stringify({ [languageCode.split('-')[0]]: "Detailed course description", "en": "Detailed course description" });
    const targetAudienceJson = JSON.stringify({ [languageCode.split('-')[0]]: "Course target audience", "en": "Course target audience" });
    
    setFormData(prev => ({
      ...prev,
      languageCode,
      nameJson,
      shortDescriptionJson: shortDescJson,
      descriptionJson: descJson,
      targetAudienceJson
    }));
  };

  const handleSubmit = async (publish: boolean) => {
    setLoading(true);
    setError(null);
    
    try {
      // Update the suggestedTutorIdsJson with selected tutors
      const updatedFormData = {
        ...formData,
        suggestedTutorIdsJson: JSON.stringify(selectedTutors),
      };

      let result;
      if (isCreating) {
        result = await createCourse(updatedFormData);
      } else {
        result = await updateCourse(courseId!, updatedFormData);
      }

      if (publish && result.isDraft) {
        // If we want to publish, call publish endpoint
        // For now, just navigate to management page, publishing will be handled separately
      }

      // After course is saved/updated, also save lessons if needed
      if (result.id) {
        // For now, just navigate - lesson saving would happen separately
        // In a real implementation, you'd save the lessons to the backend
      }

      navigate('/courses/manage');
    } catch (err) {
      console.error('Failed to save course:', err);
      setError('Failed to save course. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const nextStep = () => {
    if (step < STEPS.length - 1) {
      setStep(step + 1);
    }
  };

  const prevStep = () => {
    if (step > 0) {
      setStep(step - 1);
    }
  };

  const isStepValid = (stepIndex: number) => {
    switch (stepIndex) {
      case 0: // Basic Info
        return formData.languageCode && 
               formData.nameJson && 
               formData.category;
      case 1: // Levels & Goals
        return formData.startingLevel && 
               formData.targetLevel && 
               formData.targetAudienceJson;
      case 2: // Settings
        return true; // All fields are optional in settings
      case 3: // Lessons
        return true; // All fields are optional in lessons (lessons step is informational)
      default:
        return true;
    }
  };

  if (!user || !user.roles.includes('EDITOR') && !user.roles.includes('ADMIN')) {
    return (
      <div className="max-w-4xl mx-auto p-6">
        <div className="bg-red-50 border border-red-200 rounded-lg p-4">
          <h2 className="text-lg font-semibold text-red-800">Access Denied</h2>
          <p className="text-red-600">You must be an editor or admin to manage courses.</p>
        </div>
      </div>
    );
  }

  if (!initialDataLoaded) {
    return (
      <div className="max-w-4xl mx-auto p-6">
        <div className="flex justify-center items-center h-64">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-brand-600"></div>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto p-6">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-slate-900">
          {isCreating ? 'Create New Course' : 'Edit Course'}
        </h1>
        <p className="text-slate-600 mt-2">
          {isCreating ? 'Fill out the form to create a new course' : 'Update your course information'}
        </p>
      </div>

      {/* Stepper */}
      <div className="mb-8">
        <div className="flex items-center justify-between mb-6">
          {STEPS.map((s, index) => (
            <div key={s.id} className="flex items-center">
              <div
                className={`w-10 h-10 rounded-full flex items-center justify-center text-sm font-semibold ${
                  step === index
                    ? 'bg-brand-600 text-white'
                    : step > index
                    ? 'bg-green-100 text-green-800'
                    : 'bg-slate-100 text-slate-500'
                }`}
              >
                {step > index ? <CheckCircle className="w-5 h-5" /> : s.icon}
              </div>
              {index < STEPS.length - 1 && (
                <div
                  className={`flex-1 h-1 mx-2 ${
                    step > index ? 'bg-green-100' : 'bg-slate-100'
                  }`}
                ></div>
              )}
            </div>
          ))}
        </div>

        <div className="flex justify-between text-sm text-slate-500">
          {STEPS.map((s, index) => (
            <div
              key={s.id}
              className={`${
                step === index ? 'text-brand-600 font-medium' : 'text-slate-500'
              }`}
            >
              {s.label}
            </div>
          ))}
        </div>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-6">
          <div className="flex items-center">
            <AlertCircle className="w-5 h-5 text-red-500 mr-2" />
            <span className="text-red-700">{error}</span>
          </div>
        </div>
      )}

      {/* Step Content */}
      <div className="bg-white rounded-xl shadow-soft border border-slate-200 p-8 mb-6">
        {step === 0 && (
          <div className="space-y-6">
            <h2 className="text-xl font-semibold text-slate-900">Basic Information</h2>
            <p className="text-slate-600">Fill in the basic details for your course.</p>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-2">
                  Language *
                </label>
                <Select
                  value={formData.languageCode}
                  onChange={(value) => handleLanguageChangeByValue(value)}
                  options={languages.map(lang => ({
                    value: lang.code,
                    label: `${lang.name} ${lang.flagEmoji}`
                  }))}
                  disabled={loading}
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-700 mb-2">
                  Category *
                </label>
                <Select
                  value={formData.category}
                  onChange={(value) => handleInputChange('category', value)}
                  options={[
                    { value: 'GENERAL', label: 'General' },
                    { value: 'BUSINESS', label: 'Business' },
                    { value: 'TRAVEL', label: 'Travel' },
                    { value: 'ACADEMIC', label: 'Academic' },
                    { value: 'EXAM_PREP', label: 'Exam Preparation' },
                  ]}
                  disabled={loading}
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-2">
                Course Name (Multilingual) *
              </label>
              <MultilingualTextArea
                value={formData.nameJson}
                onChange={(value) => handleInputChange('nameJson', value)}
                placeholder='Enter course name in different languages'
                rows={3}
                disabled={loading}
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-2">
                Short Description (Multilingual) *
              </label>
              <MultilingualTextArea
                value={formData.shortDescriptionJson}
                onChange={(value) => handleInputChange('shortDescriptionJson', value)}
                placeholder='Enter short description in different languages'
                rows={3}
                disabled={loading}
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-2">
                Full Description (Multilingual) *
              </label>
              <MultilingualTextArea
                value={formData.descriptionJson}
                onChange={(value) => handleInputChange('descriptionJson', value)}
                placeholder='Enter full description in different languages'
                rows={5}
                disabled={loading}
              />
            </div>
          </div>
        )}

        {step === 1 && (
          <div className="space-y-6">
            <h2 className="text-xl font-semibold text-slate-900">Levels & Goals</h2>
            <p className="text-slate-600">Define the learning levels and goals for your course.</p>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-2">
                  Starting Level *
                </label>
                <Select
                  value={formData.startingLevel}
                  onChange={(value) => handleInputChange('startingLevel', value)}
                  options={[
                    { value: 'None', label: 'None' },
                    { value: 'A1', label: 'A1 (Beginner)' },
                    { value: 'A2', label: 'A2 (Elementary)' },
                    { value: 'B1', label: 'B1 (Intermediate)' },
                    { value: 'B2', label: 'B2 (Upper-Intermediate)' },
                    { value: 'C1', label: 'C1 (Advanced)' },
                    { value: 'C2', label: 'C2 (Proficient)' },
                  ]}
                  disabled={loading}
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-700 mb-2">
                  Target Level *
                </label>
                <Select
                  value={formData.targetLevel}
                  onChange={(value) => handleInputChange('targetLevel', value)}
                  options={[
                    { value: 'None', label: 'None' },
                    { value: 'A1', label: 'A1 (Beginner)' },
                    { value: 'A2', label: 'A2 (Elementary)' },
                    { value: 'B1', label: 'B1 (Intermediate)' },
                    { value: 'B2', label: 'B2 (Upper-Intermediate)' },
                    { value: 'C1', label: 'C1 (Advanced)' },
                    { value: 'C2', label: 'C2 (Proficient)' },
                  ]}
                  disabled={loading}
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-2">
                Target Audience (Multilingual) *
              </label>
              <MultilingualTextArea
                value={formData.targetAudienceJson}
                onChange={(value) => handleInputChange('targetAudienceJson', value)}
                placeholder='Enter target audience in different languages'
                rows={3}
                disabled={loading}
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-2">
                Learning Goals (Multilingual) *
              </label>
              <MultilingualTextArea
                value={formData.learningGoalsJson}
                onChange={(value) => handleInputChange('learningGoalsJson', value)}
                placeholder='Enter learning goals in different languages'
                rows={5}
                disabled={loading}
              />
              <p className="mt-1 text-sm text-slate-500">
                Define learning goals in multiple languages
              </p>
            </div>
          </div>
        )}

        {step === 2 && (
          <div className="space-y-6">
            <h2 className="text-xl font-semibold text-slate-900">Settings</h2>
            <p className="text-slate-600">Configure additional settings for your course.</p>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-2">
                  Estimated Weeks
                </label>
                <Input
                  type="number"
                  value={formData.estimatedWeeks || ''}
                  onChange={(e) => handleInputChange('estimatedWeeks', e.target.value ? parseInt(e.target.value) : null)}
                  placeholder="Enter estimated weeks"
                  disabled={loading}
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-700 mb-2">
                  Default Conversation Phase
                </label>
                <Select
                  value={formData.defaultPhase}
                  onChange={(value) => handleInputChange('defaultPhase', value)}
                  options={[
                    { value: 'FREE', label: 'Free' },
                    { value: 'CORRECTION', label: 'Correction' },
                    { value: 'DRILL', label: 'Drill' },
                    { value: 'AUTO', label: 'Auto' },
                  ]}
                  disabled={loading}
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-2">
                Tags (JSON Array)
              </label>
              <Input
                value={formData.tagsJson}
                onChange={(e) => handleInputChange('tagsJson', e.target.value)}
                placeholder='["tag1", "tag2"]'
                disabled={loading}
              />
              <p className="mt-1 text-sm text-slate-500">
                JSON array of tags for categorization
              </p>
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-2">
                Suggested Tutors
              </label>
              <Tabs value={formData.languageCode} onValueChange={handleLanguageChangeByValue}>
                <TabsList>
                  {languages.filter(l => l.code.startsWith(formData.languageCode.split('-')[0])).map(lang => (
                    <TabsTrigger key={lang.code} value={lang.code}>
                      {lang.name}
                    </TabsTrigger>
                  ))}
                </TabsList>
              </Tabs>
              <div className="mt-4 grid grid-cols-1 sm:grid-cols-2 gap-3">
                {tutors.filter(t => t.targetLanguageCode === formData.languageCode).map(tutor => (
                  <label key={tutor.id} className="flex items-center p-3 border rounded-lg hover:bg-slate-50 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={selectedTutors.includes(tutor.id)}
                      onChange={() => handleTutorChange(tutor.id)}
                      className="h-4 w-4 text-brand-600 rounded focus:ring-brand-500"
                    />
                    <div className="ml-3 flex items-center">
                      <span className="text-2xl">{tutor.emoji}</span>
                      <div className="ml-2">
                        <div className="font-medium text-slate-900">{tutor.name}</div>
                        <div className="text-sm text-slate-500">{tutor.domain}</div>
                      </div>
                    </div>
                  </label>
                ))}
              </div>
            </div>
          </div>
        )}

        {step === 3 && (
          <div className="space-y-6">
            <h2 className="text-xl font-semibold text-slate-900">Course Lessons</h2>
            <p className="text-slate-600">Create and manage the lessons for your course.</p>
            
            <div className="bg-slate-50 border border-slate-200 rounded-lg p-6">
              <LessonEditor 
                courseId={courseId || ''} 
                lessons={lessons} 
                onLessonsChange={setLessons} 
              />
            </div>
          </div>
        )}

        {step === 4 && (
          <div className="space-y-6">
            <h2 className="text-xl font-semibold text-slate-900">Review & Save</h2>
            <p className="text-slate-600">Review all course details before saving.</p>

            <div className="space-y-4">
              <div className="border-b pb-4">
                <h3 className="font-medium text-slate-900">Basic Information</h3>
                <div className="grid grid-cols-2 gap-2 mt-2 text-sm">
                  <div className="text-slate-500">Language:</div>
                  <div>{languages.find(l => l.code === formData.languageCode)?.name}</div>
                  
                  <div className="text-slate-500">Category:</div>
                  <div>{formData.category}</div>
                  
                  <div className="text-slate-500">Name:</div>
                  <div>{formData.nameJson}</div>
                </div>
              </div>

              <div className="border-b pb-4">
                <h3 className="font-medium text-slate-900">Levels & Goals</h3>
                <div className="grid grid-cols-2 gap-2 mt-2 text-sm">
                  <div className="text-slate-500">Starting Level:</div>
                  <div>{formData.startingLevel}</div>
                  
                  <div className="text-slate-500">Target Level:</div>
                  <div>{formData.targetLevel}</div>
                  
                  <div className="text-slate-500">Audience:</div>
                  <div>{formData.targetAudienceJson}</div>
                </div>
              </div>

              <div className="border-b pb-4">
                <h3 className="font-medium text-slate-900">Settings</h3>
                <div className="grid grid-cols-2 gap-2 mt-2 text-sm">
                  <div className="text-slate-500">Estimated Weeks:</div>
                  <div>{formData.estimatedWeeks || 'N/A'}</div>
                  
                  <div className="text-slate-500">Default Phase:</div>
                  <div>{formData.defaultPhase}</div>
                  
                  <div className="text-slate-500">Tags:</div>
                  <div>{formData.tagsJson}</div>
                </div>
              </div>

              <div>
                <h3 className="font-medium text-slate-900">Suggested Tutors</h3>
                <div className="mt-2 text-sm">
                  {selectedTutors.length > 0 
                    ? selectedTutors.map(id => tutors.find(t => t.id === id)?.name).join(', ')
                    : 'No tutors selected'}
                </div>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Navigation and Action Buttons */}
      <div className="flex justify-between">
        <Button
          variant="outline"
          onClick={prevStep}
          disabled={step === 0 || loading}
        >
          <ChevronLeft className="w-4 h-4 mr-2" />
          Previous
        </Button>

        <div className="flex gap-3">
          <Button
            variant="outline"
            onClick={() => navigate('/courses/manage')}
          >
            Cancel
          </Button>
          {step < STEPS.length - 1 ? (
            <Button
              className="ml-auto"
              onClick={nextStep}
              disabled={!isStepValid(step) || loading}
            >
              Next
              <ChevronRight className="w-4 h-4 ml-2" />
            </Button>
          ) : (
            <div className="flex gap-3 ml-auto">
              <Button
                variant="outline"
                onClick={() => handleSubmit(false)}
                disabled={loading}
              >
                <Save className="w-4 h-4 mr-2" />
                Save as Draft
              </Button>
              <Button
                onClick={() => handleSubmit(true)}
                disabled={loading}
              >
                <CheckCircle className="w-4 h-4 mr-2" />
                Save & Publish
              </Button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}