import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ChevronLeft, ChevronRight, Save, CheckCircle, AlertCircle } from 'lucide-react';
import { getLanguages, getTutors, createCustomTutor, updateCustomTutor } from '../api/catalog';
import { createCourse, updateCourse, getCourse } from '../api/courseManagement';
import { getLessons } from '../api/lessonManagement';
import { useAuthStore } from '../store/authStore';
import { TutorPersonality, TeachingStyle, TutorGender } from '../types';
import type { Language, Tutor } from '../types';
import type { LessonResponse } from '../api/lessonManagement';
import Button from '../components/ui/Button';
import Input from '../components/ui/Input';
import Select from '../components/ui/Select';
import MultilingualTextArea from '../components/ui/MultilingualTextArea';
import TagInput from '../components/ui/TagInput';

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
  tags: string[];
  suggestedTutorIdsJson: string;
  defaultPhase: string;
}

const STEPS = [
  { id: 'basic', label: 'Basic Info', icon: '📝' },
  { id: 'levels', label: 'Levels & Goals', icon: '🎯' },
  { id: 'settings', label: 'Settings', icon: '⚙️' },
  { id: 'tutors', label: 'Tutors', icon: '👥' },
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
    languageCode: 'en-US',  // Default to English-USA
    nameJson: '{"en":"New Course"}',
    shortDescriptionJson: '{"en":"Short description"}',
    descriptionJson: '{"en":"Detailed course description"}',
    category: 'General',
    startingLevel: 'A1',
    targetLevel: 'A2',
    targetAudienceJson: '{"en":"Course target audience"}',
    learningGoalsJson: '{"en":["Goal 1","Goal 2"]}',
    estimatedWeeks: null,
    tags: ['tag1', 'tag2'],
    suggestedTutorIdsJson: '[]',
    defaultPhase: 'AUTO',
  });

  const [selectedTutors, setSelectedTutors] = useState<string[]>([]);
  const [lessons, setLessons] = useState<LessonResponse[]>([]);

  // State for Tutors step
  const [activeTutorTab, setActiveTutorTab] = useState<'select' | 'create'>('select');
  const [newTutorForm, setNewTutorForm] = useState<{
    name: string;
    emoji: string;
    personaEnglish: string;
    domainEnglish: string;
    descriptionEnglish: string;
    personality: TutorPersonality;
    teachingStyle: TeachingStyle;
    targetLanguageCode: string;
    culturalBackground: string;
    location: string;
    age: number;
    gender: TutorGender;
  }>({
    name: '',
    emoji: '👩‍🏫',
    personaEnglish: '',
    domainEnglish: '',
    descriptionEnglish: '',
    personality: TutorPersonality.Casual,
    teachingStyle: TeachingStyle.Reactive,
    targetLanguageCode: formData.languageCode, // Use the course language by default
    culturalBackground: '',
    location: '',
    age: 30,
    gender: TutorGender.Neutral,
  });

  // State for editing tutors
  const [editingTutor, setEditingTutor] = useState<Tutor | null>(null);
  const [editTutorForm, setEditTutorForm] = useState<{
    name: string;
    emoji: string;
    personaEnglish: string;
    domainEnglish: string;
    descriptionEnglish: string;
    personality: TutorPersonality;
    teachingStyle: TeachingStyle;
    targetLanguageCode: string;
    culturalBackground: string;
    location: string;
    age: number;
    gender: TutorGender;
  }>({
    name: '',
    emoji: '👩‍🏫',
    personaEnglish: '',
    domainEnglish: '',
    descriptionEnglish: '',
    personality: TutorPersonality.Casual,
    teachingStyle: TeachingStyle.Reactive,
    targetLanguageCode: formData.languageCode,
    culturalBackground: '',
    location: '',
    age: 30,
    gender: TutorGender.Neutral,
  });

  // Update new tutor form when course language changes
  useEffect(() => {
    setNewTutorForm(prev => ({
      ...prev,
      targetLanguageCode: formData.languageCode
    }));
  }, [formData.languageCode]);

  // Reload tutors when course language changes (for new courses)
  useEffect(() => {
    if (!courseId) { // Only reload for new courses, not when editing
      const loadTutorsForLanguage = async () => {
        try {
          const tutorsData = await getTutors(formData.languageCode, 'en');
          setTutors(tutorsData);
        } catch (error) {
          console.error('Failed to reload tutors for language:', formData.languageCode, error);
        }
      };
      
      loadTutorsForLanguage();
    }
  }, [formData.languageCode, courseId]);
  const [isCreatingTutor, setIsCreatingTutor] = useState(false);
  const [isUpdatingTutor, setIsUpdatingTutor] = useState(false);

  useEffect(() => {
    const loadInitialData = async () => {
      try {
        setLoading(true);
        const [langs] = await Promise.all([
          getLanguages('en'),
        ]);
        
        // Determine the target language for tutors
        let courseLanguage = formData.languageCode; // Default to current form value
        
        if (courseId) {
          // If editing existing course, load course data first to get language
          const courseData = await getCourse(courseId);
          courseLanguage = courseData.languageCode;
          setIsCreating(false);

          // Parse tags from JSON string to array
          let tagsArray: string[] = [];
          try {
            if (courseData.tagsJson) {
              const parsedTags = JSON.parse(courseData.tagsJson);
              if (Array.isArray(parsedTags)) {
                tagsArray = parsedTags.filter(tag => typeof tag === 'string').map(tag => tag.trim()).filter(tag => tag);
              }
            }
          } catch {
            tagsArray = [];
          }

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
            tags: tagsArray,
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
        
        // Get tutors for the course's target language
        const tutorsData = await getTutors(courseLanguage, 'en'); // Using 'en' as source language for translations
        
        setLanguages(langs);
        setTutors(tutorsData);

        if (courseId) {
          // Load existing course data for editing
          const courseData = await getCourse(courseId);
          setIsCreating(false);
          
          // Parse tags from JSON string to array
          let tagsArray: string[] = [];
          try {
            if (courseData.tagsJson) {
              const parsedTags = JSON.parse(courseData.tagsJson);
              if (Array.isArray(parsedTags)) {
                tagsArray = parsedTags.filter(tag => typeof tag === 'string').map(tag => tag.trim()).filter(tag => tag);
              }
            }
          } catch {
            tagsArray = [];
          }

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
            tags: tagsArray,
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

    loadInitialData();
  }, [courseId]); // Run once when component mounts, but re-run if courseId changes

  const handleInputChange = (field: keyof FormData, value: string | number | null | string[]) => {
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

  const getRandomEmoji = (gender: TutorGender): string => {
    const genderEmojis: Record<TutorGender, string[]> = {
      [TutorGender.Male]: ['👨‍🏫', '👨‍💼', '🧑‍🏫', '👨', '🧔', '👨‍🎓'],
      [TutorGender.Female]: ['👩‍🏫', '👩‍💼', '🧑‍🏫', '👩', '👩‍🎓', '👱‍♀️'],
      [TutorGender.Neutral]: ['🧑‍🏫', '🧑‍💼', '🧑', '👤', '🧑‍🎓', '👥'],
    };
    const emojis = genderEmojis[gender] || genderEmojis[TutorGender.Neutral];
    return emojis[Math.floor(Math.random() * emojis.length)];
  };

  const handleCreateTutor = async () => {
    if (!newTutorForm.name || !newTutorForm.emoji || !newTutorForm.personaEnglish ||
        !newTutorForm.domainEnglish || !newTutorForm.descriptionEnglish || !newTutorForm.targetLanguageCode) {
      setError('Please fill in all required fields');
      return;
    }

    setIsCreatingTutor(true);
    setError(null);

    try {
      const request = {
        name: newTutorForm.name,
        emoji: newTutorForm.emoji,
        personaEnglish: newTutorForm.personaEnglish,
        domainEnglish: newTutorForm.domainEnglish,
        descriptionEnglish: newTutorForm.descriptionEnglish,
        personality: newTutorForm.personality,
        teachingStyle: newTutorForm.teachingStyle,
        targetLanguageCode: newTutorForm.targetLanguageCode,
        age: newTutorForm.age,
        gender: newTutorForm.gender,
        culturalBackground: newTutorForm.culturalBackground ? newTutorForm.culturalBackground.trim() : undefined,
        location: newTutorForm.location ? newTutorForm.location.trim() : undefined,
      };

      const createdTutor = await createCustomTutor(request);
      
      // Add the newly created tutor to the selected tutors list
      setSelectedTutors(prev => [...prev, createdTutor.id]);
      
      // Add the new tutor to the tutors list so it appears in the UI
      setTutors(prev => [...prev, createdTutor]);
      
      // Reset the form
      setNewTutorForm({
        name: '',
        emoji: '👩‍🏫',
        personaEnglish: '',
        domainEnglish: '',
        descriptionEnglish: '',
        personality: TutorPersonality.Casual,
        teachingStyle: TeachingStyle.Reactive,
        targetLanguageCode: formData.languageCode, // Default to course language
        culturalBackground: '',
        location: '',
        age: 30,
        gender: TutorGender.Neutral,
      });
      
      // Switch to the select tab to show the new tutor
      setActiveTutorTab('select');
    } catch (error: unknown) {
      console.error('Error creating tutor:', error);
      const errorMessage = (error as { response?: { data?: { message?: string } } }).response?.data?.message || 
                           (error as Error).message || 
                           'Failed to create custom tutor';
      setError(errorMessage);
    } finally {
      setIsCreatingTutor(false);
    }
  };

  const handleEditTutor = (tutor: Tutor) => {
    // Set the tutor data in the edit form
    setEditTutorForm({
      name: tutor.name || '',
      emoji: tutor.emoji || '👩‍🏫',
      personaEnglish: tutor.persona || tutor.name || '',
      domainEnglish: tutor.domain || '',
      descriptionEnglish: tutor.description || '',
      personality: tutor.personality,
      teachingStyle: tutor.teachingStyle,
      targetLanguageCode: tutor.targetLanguageCode,
      culturalBackground: tutor.culturalBackground || '',
      location: tutor.location || '',
      age: tutor.age,
      gender: tutor.gender || TutorGender.Neutral,
    });
    setEditingTutor(tutor);
    // Switch to create tab to show the edit form
    setActiveTutorTab('create');
  };

  const handleUpdateTutor = async () => {
    if (!editingTutor) return;
    
    setIsUpdatingTutor(true);
    setError(null);

    try {
      const request = {
        name: editTutorForm.name,
        emoji: editTutorForm.emoji,
        personaEnglish: editTutorForm.personaEnglish,
        domainEnglish: editTutorForm.domainEnglish,
        descriptionEnglish: editTutorForm.descriptionEnglish,
        personality: editTutorForm.personality,
        teachingStyle: editTutorForm.teachingStyle,
        targetLanguageCode: editTutorForm.targetLanguageCode,
        age: editTutorForm.age,
        gender: editTutorForm.gender,
        culturalBackground: editTutorForm.culturalBackground || undefined,
        location: editTutorForm.location || undefined,
        isActive: true, // Keep active by default
        displayOrder: 0, // Default display order
      };

      const updatedTutor = await updateCustomTutor(editingTutor.id, request);
      
      // Update the tutor in the tutors list
      setTutors(prev => prev.map(t => t.id === updatedTutor.id ? updatedTutor : t));
      
      // Reset editing state
      setEditingTutor(null);
      setIsUpdatingTutor(false);
      setActiveTutorTab('select');
    } catch (error: unknown) {
      console.error('Error updating tutor:', error);
      const errorMessage = (error as { response?: { data?: { message?: string } } }).response?.data?.message || 
                           (error as Error).message || 
                           'Failed to update custom tutor';
      setError(errorMessage);
    } finally {
      setIsUpdatingTutor(false);
    }
  };

  const cancelEditTutor = () => {
    setEditingTutor(null);
    setIsUpdatingTutor(false);
    setActiveTutorTab('select');
  };

  const handleSubmit = async (publish: boolean) => {
    setLoading(true);
    setError(null);
    
    try {
      // Convert tags array to JSON string for the backend
      // Update the suggestedTutorIdsJson with selected tutors
      const updatedFormData = {
        ...formData,
        tagsJson: JSON.stringify(formData.tags),
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
      case 3: // Tutors
        return true; // All tutor fields are optional
      case 4: // Lessons
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
                    { value: 'General', label: 'General' },
                    { value: 'Business', label: 'Business' },
                    { value: 'Travel', label: 'Travel' },
                    { value: 'Academic', label: 'Academic' },
                    { value: 'ExamPrep', label: 'Exam Preparation' },
                    { value: 'Conversational', label: 'Conversational' },
                    { value: 'Grammar', label: 'Grammar' },
                    { value: 'Hobby', label: 'Hobby' },
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
                Tags
              </label>
              <TagInput
                value={formData.tags}
                onChange={(tags) => handleInputChange('tags', tags)}
                placeholder="Add a tag..."
                disabled={loading}
              />
              <p className="mt-1 text-sm text-slate-500">
                Add tags to categorize your course
              </p>
            </div>
          </div>
        )}

        {step === 3 && (
          <div className="space-y-6">
            <h2 className="text-xl font-semibold text-slate-900">Course Tutors</h2>
            <p className="text-slate-600">Select tutors for this course or create new ones.</p>

            <div className="border border-slate-200 rounded-lg">
              {/* Tabs for Select Tutors and Create Tutor */}
              <div className="border-b border-slate-200">
                <nav className="-mb-px flex">
                  <button
                    className={`py-2 px-4 text-sm font-medium border-b-2 ${
                      activeTutorTab === 'select'
                        ? 'border-brand-500 text-brand-600'
                        : 'border-transparent text-slate-500 hover:text-slate-700 hover:border-slate-300'
                    }`}
                    onClick={() => setActiveTutorTab('select')}
                  >
                    Select Tutors
                  </button>
                  <button
                    className={`py-2 px-4 text-sm font-medium border-b-2 ${
                      activeTutorTab === 'create'
                        ? 'border-brand-500 text-brand-600'
                        : 'border-transparent text-slate-500 hover:text-slate-700 hover:border-slate-300'
                    }`}
                    onClick={() => setActiveTutorTab('create')}
                  >
                    Create New Tutor
                  </button>
                </nav>
              </div>

              <div className="p-6">
                {activeTutorTab === 'select' && (
                  <div>
                    <h3 className="font-medium text-slate-900 mb-4">Select Tutors for this Course</h3>
                    <div className="mt-4 grid grid-cols-1 sm:grid-cols-2 gap-3">
                      {tutors.filter(t => t.targetLanguageCode === formData.languageCode).map(tutor => (
                        <div key={tutor.id} className="flex items-center p-3 border rounded-lg hover:bg-slate-50">
                          <input
                            type="checkbox"
                            checked={selectedTutors.includes(tutor.id)}
                            onChange={() => handleTutorChange(tutor.id)}
                            className="h-4 w-4 text-brand-600 rounded focus:ring-brand-500 mr-3"
                          />
                          <div className="flex-1 flex items-center">
                            <span className="text-2xl">{tutor.emoji}</span>
                            <div className="ml-2 flex-1">
                              <div className="font-medium text-slate-900">{tutor.name}</div>
                              <div className="text-sm text-slate-500">{tutor.domain}</div>
                            </div>
                          </div>
                          {/* Edit button only for user-created tutors (not for seed data global tutors) */}
                          <button
                            type="button"
                            onClick={() => handleEditTutor(tutor)}
                            className="ml-2 p-1.5 text-sm rounded-md text-slate-500 hover:bg-slate-100 hover:text-slate-700"
                            title="Edit tutor"
                          >
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                            </svg>
                          </button>
                        </div>
                      ))}
                    </div>
                    {tutors.filter(t => t.targetLanguageCode === formData.languageCode).length === 0 && (
                      <div className="text-center py-8 text-slate-500">
                        No tutors available for {languages.find(l => l.code === formData.languageCode)?.name}. 
                        Create one using the "Create New Tutor" tab.
                      </div>
                    )}
                  </div>
                )}
                
                {activeTutorTab === 'create' && (
                  <div className="space-y-4">
                    <h3 className="font-medium text-slate-900">{editingTutor ? 'Edit Tutor' : 'Create New Tutor'}</h3>
                    <div className="bg-slate-50 p-4 rounded-lg">
                      <p className="text-slate-600 mb-4">
                        {editingTutor 
                          ? `Editing tutor: ${editingTutor.name}` 
                          : 'Use this form to create a new tutor specifically for this language course.'}
                      </p>
                      
                      {/* Tutor Creation/Editing Form */}
                      <div className="space-y-4">
                        <div>
                          <label className="block text-sm font-medium text-slate-700 mb-1">
                            Tutor Name
                          </label>
                          <input
                            type="text"
                            value={editingTutor ? editTutorForm.name : newTutorForm.name}
                            onChange={(e) => {
                              if (editingTutor) {
                                setEditTutorForm({...editTutorForm, name: e.target.value});
                              } else {
                                setNewTutorForm({...newTutorForm, name: e.target.value});
                              }
                            }}
                            className="w-full px-3 py-2 border border-slate-300 rounded-md focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-brand-500"
                            placeholder="e.g., Maria"
                          />
                        </div>

                        <div>
                          <label className="block text-sm font-medium text-slate-700 mb-1">
                            Emoji
                          </label>
                          <div className="flex gap-2">
                            <input
                              type="text"
                              value={editingTutor ? editTutorForm.emoji : newTutorForm.emoji}
                              onChange={(e) => {
                                if (editingTutor) {
                                  setEditTutorForm({...editTutorForm, emoji: e.target.value});
                                } else {
                                  setNewTutorForm({...newTutorForm, emoji: e.target.value});
                                }
                              }}
                              className="flex-1 px-3 py-2 border border-slate-300 rounded-md focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-brand-500"
                              placeholder="👩‍🏫"
                              maxLength={4}
                            />
                            <button
                              type="button"
                              className="px-4 py-2 bg-slate-100 hover:bg-slate-200 border border-slate-300 rounded-md transition-colors text-xl"
                              onClick={() => {
                                if (editingTutor) {
                                  setEditTutorForm({...editTutorForm, emoji: getRandomEmoji(editTutorForm.gender)});
                                } else {
                                  setNewTutorForm({...newTutorForm, emoji: getRandomEmoji(newTutorForm.gender)});
                                }
                              }}>
                              🎲
                            </button>
                          </div>
                        </div>

                        <div>
                          <label className="block text-sm font-medium text-slate-700 mb-1">
                            Target Language
                          </label>
                          <select
                            value={editingTutor ? editTutorForm.targetLanguageCode : newTutorForm.targetLanguageCode}
                            onChange={(e) => {
                              if (editingTutor) {
                                setEditTutorForm({...editTutorForm, targetLanguageCode: e.target.value});
                              } else {
                                setNewTutorForm({...newTutorForm, targetLanguageCode: e.target.value});
                              }
                            }}
                            className="w-full px-3 py-2 border border-slate-300 rounded-md focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-brand-500"
                          >
                            {languages.map((lang) => (
                              <option key={lang.code} value={lang.code}>
                                {lang.flagEmoji} {lang.name}
                              </option>
                            ))}
                          </select>
                        </div>

                        <div>
                          <label className="block text-sm font-medium text-slate-700 mb-1">
                            Gender
                          </label>
                          <div className="grid grid-cols-3 gap-2">
                            <button
                              type="button"
                              onClick={() => {
                                if (editingTutor) {
                                  setEditTutorForm({...editTutorForm, gender: TutorGender.Neutral});
                                } else {
                                  setNewTutorForm({...newTutorForm, gender: TutorGender.Neutral});
                                }
                              }}
                              className={`p-2 rounded-lg border text-center ${
                                (editingTutor ? editTutorForm.gender : newTutorForm.gender) === TutorGender.Neutral
                                  ? 'border-brand-500 bg-brand-50'
                                  : 'border-slate-200 hover:border-brand-300'
                              }`}
                            >
                              <div className="text-xl">🧑</div>
                              <div className="text-xs">Neutral</div>
                            </button>
                            <button
                              type="button"
                              onClick={() => {
                                if (editingTutor) {
                                  setEditTutorForm({...editTutorForm, gender: TutorGender.Male});
                                } else {
                                  setNewTutorForm({...newTutorForm, gender: TutorGender.Male});
                                }
                              }}
                              className={`p-2 rounded-lg border text-center ${
                                (editingTutor ? editTutorForm.gender : newTutorForm.gender) === TutorGender.Male
                                  ? 'border-brand-500 bg-brand-50'
                                  : 'border-slate-200 hover:border-brand-300'
                              }`}
                            >
                              <div className="text-xl">👨</div>
                              <div className="text-xs">Male</div>
                            </button>
                            <button
                              type="button"
                              onClick={() => {
                                if (editingTutor) {
                                  setEditTutorForm({...editTutorForm, gender: TutorGender.Female});
                                } else {
                                  setNewTutorForm({...newTutorForm, gender: TutorGender.Female});
                                }
                              }}
                              className={`p-2 rounded-lg border text-center ${
                                (editingTutor ? editTutorForm.gender : newTutorForm.gender) === TutorGender.Female
                                  ? 'border-brand-500 bg-brand-50'
                                  : 'border-slate-200 hover:border-brand-300'
                              }`}
                            >
                              <div className="text-xl">👩</div>
                              <div className="text-xs">Female</div>
                            </button>
                          </div>
                        </div>

                        <div>
                          <label className="block text-sm font-medium text-slate-700 mb-1">
                            Age
                          </label>
                          <input
                            type="number"
                            value={editingTutor ? editTutorForm.age : newTutorForm.age}
                            onChange={(e) => {
                              if (editingTutor) {
                                setEditTutorForm({...editTutorForm, age: parseInt(e.target.value) || 30});
                              } else {
                                setNewTutorForm({...newTutorForm, age: parseInt(e.target.value) || 30});
                              }
                            }}
                            className="w-full px-3 py-2 border border-slate-300 rounded-md focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-brand-500"
                            min="18"
                            max="100"
                          />
                        </div>

                        <div>
                          <label className="block text-sm font-medium text-slate-700 mb-1">
                            Persona
                          </label>
                          <input
                            type="text"
                            value={editingTutor ? editTutorForm.personaEnglish : newTutorForm.personaEnglish}
                            onChange={(e) => {
                              if (editingTutor) {
                                setEditTutorForm({...editTutorForm, personaEnglish: e.target.value});
                              } else {
                                setNewTutorForm({...newTutorForm, personaEnglish: e.target.value});
                              }
                            }}
                            className="w-full px-3 py-2 border border-slate-300 rounded-md focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-brand-500"
                            placeholder="e.g., Native Spanish teacher from Madrid"
                          />
                        </div>

                        <div>
                          <label className="block text-sm font-medium text-slate-700 mb-1">
                            Domain/Expertise
                          </label>
                          <input
                            type="text"
                            value={editingTutor ? editTutorForm.domainEnglish : newTutorForm.domainEnglish}
                            onChange={(e) => {
                              if (editingTutor) {
                                setEditTutorForm({...editTutorForm, domainEnglish: e.target.value});
                              } else {
                                setNewTutorForm({...newTutorForm, domainEnglish: e.target.value});
                              }
                            }}
                            className="w-full px-3 py-2 border border-slate-300 rounded-md focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-brand-500"
                            placeholder="e.g., Spanish grammar and conversation"
                          />
                        </div>

                        <div>
                          <label className="block text-sm font-medium text-slate-700 mb-1">
                            Description
                          </label>
                          <textarea
                            value={editingTutor ? editTutorForm.descriptionEnglish : newTutorForm.descriptionEnglish}
                            onChange={(e) => {
                              if (editingTutor) {
                                setEditTutorForm({...editTutorForm, descriptionEnglish: e.target.value});
                              } else {
                                setNewTutorForm({...newTutorForm, descriptionEnglish: e.target.value});
                              }
                            }}
                            className="w-full px-3 py-2 border border-slate-300 rounded-md focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-brand-500"
                            placeholder="Describe your tutor's background, teaching approach, and what makes them unique..."
                            rows={3}
                          />
                        </div>

                        <div>
                          <label className="block text-sm font-medium text-slate-700 mb-1">
                            Personality
                          </label>
                          <select
                            value={editingTutor ? editTutorForm.personality : newTutorForm.personality}
                            onChange={(e) => {
                              if (editingTutor) {
                                setEditTutorForm({...editTutorForm, personality: e.target.value as TutorPersonality});
                              } else {
                                setNewTutorForm({...newTutorForm, personality: e.target.value as TutorPersonality});
                              }
                            }}
                            className="w-full px-3 py-2 border border-slate-300 rounded-md focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-brand-500"
                          >
                            <option value={TutorPersonality.Casual}>{TutorPersonality.Casual}</option>
                            <option value={TutorPersonality.Professional}>{TutorPersonality.Professional}</option>
                            <option value={TutorPersonality.Encouraging}>{TutorPersonality.Encouraging}</option>
                            <option value={TutorPersonality.Strict}>{TutorPersonality.Strict}</option>
                            <option value={TutorPersonality.Academic}>{TutorPersonality.Academic}</option>
                          </select>
                        </div>

                        <div>
                          <label className="block text-sm font-medium text-slate-700 mb-1">
                            Teaching Style
                          </label>
                          <select
                            value={editingTutor ? editTutorForm.teachingStyle : newTutorForm.teachingStyle}
                            onChange={(e) => {
                              if (editingTutor) {
                                setEditTutorForm({...editTutorForm, teachingStyle: e.target.value as TeachingStyle});
                              } else {
                                setNewTutorForm({...newTutorForm, teachingStyle: e.target.value as TeachingStyle});
                              }
                            }}
                            className="w-full px-3 py-2 border border-slate-300 rounded-md focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-brand-500"
                          >
                            <option value={TeachingStyle.Reactive}>{TeachingStyle.Reactive}</option>
                            <option value={TeachingStyle.Guided}>{TeachingStyle.Guided}</option>
                            <option value={TeachingStyle.Directive}>{TeachingStyle.Directive}</option>
                          </select>
                        </div>

                        <div className="pt-4 flex gap-3">
                          {editingTutor ? (
                            <>
                              <button
                                type="button"
                                onClick={handleUpdateTutor}
                                disabled={isUpdatingTutor}
                                className="flex-1 bg-brand-600 hover:bg-brand-700 text-white font-medium py-2 px-4 rounded-md disabled:opacity-50"
                              >
                                {isUpdatingTutor ? 'Saving...' : 'Update Tutor'}
                              </button>
                              <button
                                type="button"
                                onClick={cancelEditTutor}
                                disabled={isUpdatingTutor}
                                className="flex-1 bg-slate-200 hover:bg-slate-300 text-slate-800 font-medium py-2 px-4 rounded-md"
                              >
                                Cancel
                              </button>
                            </>
                          ) : (
                            <button
                              type="button"
                              onClick={handleCreateTutor}
                              disabled={isCreatingTutor}
                              className="w-full bg-brand-600 hover:bg-brand-700 text-white font-medium py-2 px-4 rounded-md disabled:opacity-50"
                            >
                              {isCreatingTutor ? 'Creating...' : 'Create Tutor'}
                            </button>
                          )}
                        </div>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            </div>
          </div>
        )}

        {step === 4 && (
          <div className="space-y-6">
            <h2 className="text-xl font-semibold text-slate-900">Course Lessons</h2>
            <p className="text-slate-600">Create and manage the lessons for your course.</p>
            
            <div className="bg-slate-50 border border-slate-200 rounded-lg p-6">
              {courseId ? (
                <LessonEditor 
                  courseId={courseId} 
                  lessons={lessons} 
                  onLessonsChange={setLessons} 
                />
              ) : (
                <div className="text-center py-12">
                  <p className="text-slate-500 mb-4">Please save the course first before adding lessons.</p>
                  <p className="text-sm text-slate-400">Lessons can be added after the course is created.</p>
                </div>
              )}
            </div>
          </div>
        )}

        {step === 5 && (
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
                  <div>{formData.tags.length > 0 ? formData.tags.join(', ') : 'None'}</div>
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