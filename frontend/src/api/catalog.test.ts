import { describe, it, expect, vi, beforeEach } from 'vitest';
import { 
  getLanguages,
  getCourses,
  getCourse,
  getTutors,
  createCustomTutor,
} from './catalog';
import { CEFRLevel, CourseCategory, Difficulty, TutorGender, TutorPersonality, TeachingStyle, ConversationPhase } from '../types';
import type { Language, Course, CourseDetail, Tutor, TutorDetail, CreateTutorRequest } from '../types';

// Define the mock functions
const mockGet = vi.fn();
const mockPost = vi.fn();

// Mock the apiClient
vi.mock('./client', () => ({
  default: {
    get: mockGet,
    post: mockPost,
  }
}));

// Mock the FileReader for image handling
const mockFileReader = {
  onload: null,
  onerror: null,
  readAsDataURL: vi.fn(),
  result: 'data:image/png;base64,test-image-data'
};

global.FileReader = vi.fn(() => mockFileReader) as unknown as typeof FileReader;

describe('catalog API module', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // Reset mockFileReader properties
    mockFileReader.onload = null;
    mockFileReader.onerror = null;
    mockFileReader.readAsDataURL = vi.fn();
    mockFileReader.result = 'data:image/png;base64,test-image-data';
  });

  describe('getLanguages', () => {
    it('should fetch languages with default locale', async () => {
      const mockLanguages: Language[] = [
        {
          code: 'en',
          name: 'English',
          flagEmoji: '🇺🇸',
          nativeName: 'English',
          difficulty: 'BEGINNER' as Difficulty,
          description: 'English language',
          courseCount: 3,
        }
      ];
      
      mockGet.mockResolvedValue({ data: mockLanguages });
      
      const result = await getLanguages();
      
      expect(mockGet).toHaveBeenCalledWith('/catalog/languages', {
        params: { locale: 'en' },
      });
      expect(result).toEqual(mockLanguages);
    });

    it('should fetch languages with custom locale', async () => {
      const locale = 'es';
      const mockLanguages: Language[] = [
        {
          code: 'es',
          name: 'Spanish',
          flagEmoji: '🇪🇸',
          nativeName: 'Español',
          difficulty: 'BEGINNER' as Difficulty,
          description: 'Spanish language',
          courseCount: 2,
        }
      ];
      
      mockGet.mockResolvedValue({ data: mockLanguages });
      
      const result = await getLanguages(locale);
      
      expect(mockGet).toHaveBeenCalledWith('/catalog/languages', {
        params: { locale: 'es' },
      });
      expect(result).toEqual(mockLanguages);
    });
  });

  describe('getCourses', () => {
    it('should fetch courses for a language with default parameters', async () => {
      const languageCode = 'es';
      const mockCourses: Course[] = [
        {
          id: 'course1',
          languageCode: 'es',
          name: 'Spanish for Beginners',
          shortDescription: 'Learn basic Spanish',
          description: 'Learn basic Spanish',
          category: 'GENERAL' as CourseCategory,
          targetAudience: 'Beginners',
          startingLevel: 'A1' as CEFRLevel,
          targetLevel: 'A1' as CEFRLevel,
          estimatedWeeks: 10,
          displayOrder: 1,
          difficulty: 'BEGINNER' as Difficulty,
          userLevel: 'A1' as CEFRLevel,
        }
      ];
      
      mockGet.mockResolvedValue({ data: mockCourses });
      
      const result = await getCourses(languageCode);
      
      expect(mockGet).toHaveBeenCalledWith(
        `/catalog/languages/${languageCode}/courses`,
        { params: { sourceLanguage: 'en' } }
      );
      expect(result).toEqual(mockCourses);
    });

    it('should fetch courses with CEFR level and category filters', async () => {
      const languageCode = 'fr';
      const cefrLevel = 'A2';
      const category = 'TRAVEL';
      const locale = 'de';
      const mockCourses: Course[] = [
        {
          id: 'course2',
          languageCode: 'fr',
          name: 'French for Travel',
          shortDescription: 'Travel-specific French phrases',
          description: 'Travel-specific French phrases',
          category: 'TRAVEL' as CourseCategory,
          targetAudience: 'Travelers',
          startingLevel: 'A1' as CEFRLevel,
          targetLevel: 'A2' as CEFRLevel,
          estimatedWeeks: 5,
          displayOrder: 1,
          difficulty: 'INTERMEDIATE' as Difficulty,
          userLevel: 'A1' as CEFRLevel,
        }
      ];
      
      mockGet.mockResolvedValue({ data: mockCourses });
      
      const result = await getCourses(languageCode, locale, cefrLevel, category);
      
      expect(mockGet).toHaveBeenCalledWith(
        `/catalog/languages/${languageCode}/courses`,
        { 
          params: { 
            sourceLanguage: 'de',
            userLevel: 'A2',
            category: 'TRAVEL'
          } 
        }
      );
      expect(result).toEqual(mockCourses);
    });
  });

  describe('getCourse', () => {
    it('should fetch a specific course with default locale', async () => {
      const courseId = 'course1';
      const mockCourse: CourseDetail = {
        id: courseId,
        languageCode: 'es',
        name: 'Spanish for Beginners',
        shortDescription: 'Learn basic Spanish',
        description: 'Learn basic Spanish',
        category: 'GENERAL' as CourseCategory,
        targetAudience: 'Beginners',
        startingLevel: 'A1' as CEFRLevel,
        targetLevel: 'A1' as CEFRLevel,
        estimatedWeeks: 10,
        displayOrder: 1,
        difficulty: 'BEGINNER' as Difficulty,
        suggestedTutors: [],
        defaultPhase: 'CORRECTION' as ConversationPhase,
        topicSequence: null,
        learningGoals: [],
        tags: [],
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };
      
      mockGet.mockResolvedValue({ data: mockCourse });
      
      const result = await getCourse(courseId);
      
      expect(mockGet).toHaveBeenCalledWith(
        `/catalog/courses/${courseId}`,
        { params: { sourceLanguage: 'en' } }
      );
      expect(result).toEqual(mockCourse);
    });

    it('should fetch a specific course with custom locale', async () => {
      const courseId = 'course2';
      const locale = 'fr';
      const mockCourse: CourseDetail = {
        id: courseId,
        languageCode: 'fr',
        name: 'Français pour Débutants',
        shortDescription: 'Apprenez le français de base',
        description: 'Apprenez le français de base',
        category: 'GENERAL' as CourseCategory,
        targetAudience: 'Beginners',
        startingLevel: 'A1' as CEFRLevel,
        targetLevel: 'A1' as CEFRLevel,
        estimatedWeeks: 10,
        displayOrder: 1,
        difficulty: 'BEGINNER' as Difficulty,
        suggestedTutors: [],
        defaultPhase: 'CORRECTION' as ConversationPhase,
        topicSequence: null,
        learningGoals: [],
        tags: [],
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };
      
      mockGet.mockResolvedValue({ data: mockCourse });
      
      const result = await getCourse(courseId, locale);
      
      expect(mockGet).toHaveBeenCalledWith(
        `/catalog/courses/${courseId}`,
        { params: { sourceLanguage: 'fr' } }
      );
      expect(result).toEqual(mockCourse);
    });
  });

  describe('getTutors', () => {
    it('should fetch tutors for a language with default locale', async () => {
      const languageCode = 'es';
      const mockTutors: Tutor[] = [
        {
          id: 'tutor1',
          name: 'Maria',
          emoji: '👩',
          persona: 'A friendly Spanish tutor',
          domain: 'Spanish',
          personality: 'Encouraging' as TutorPersonality,
          teachingStyle: 'Guided' as TeachingStyle,
          description: 'A friendly Spanish tutor who encourages students to speak more',
          targetLanguageCode: 'es',
          culturalBackground: 'Spanish',
          age: 30,
          gender: 'Female' as TutorGender,
          imageUrl: null,
          displayOrder: 1,
          location: 'Spain',
        }
      ];
      
      mockGet.mockResolvedValue({ data: mockTutors });
      
      const result = await getTutors(languageCode);
      
      expect(mockGet).toHaveBeenCalledWith(
        `/catalog/languages/${languageCode}/tutors`,
        { params: { sourceLanguage: 'en' } }
      );
      expect(result).toEqual(mockTutors);
    });

    it('should fetch tutors for a language with custom locale', async () => {
      const languageCode = 'de';
      const locale = 'it';
      const mockTutors: Tutor[] = [
        {
          id: 'tutor2',
          name: 'Hans',
          emoji: '👨',
          persona: 'A strict German tutor',
          domain: 'German',
          personality: 'Strict' as TutorPersonality,
          teachingStyle: 'Directive' as TeachingStyle,
          description: 'A strict German tutor who focuses on grammar and accuracy',
          targetLanguageCode: 'de',
          culturalBackground: 'German',
          age: 40,
          gender: 'Male' as TutorGender,
          imageUrl: null,
          displayOrder: 1,
          location: 'Germany',
        }
      ];
      
      mockGet.mockResolvedValue({ data: mockTutors });
      
      const result = await getTutors(languageCode, locale);
      
      expect(mockGet).toHaveBeenCalledWith(
        `/catalog/languages/${languageCode}/tutors`,
        { params: { sourceLanguage: 'it' } }
      );
      expect(result).toEqual(mockTutors);
    });
  });

  describe('createCustomTutor', () => {
    it('should create a custom tutor', async () => {
      const request: CreateTutorRequest = {
        name: 'My Tutor',
        emoji: '📚',
        personaEnglish: 'A personalized Spanish tutor',
        domainEnglish: 'Spanish',
        descriptionEnglish: 'A personalized Spanish tutor',
        personality: 'Casual' as TutorPersonality,
        teachingStyle: 'Guided' as TeachingStyle,
        targetLanguageCode: 'es',
        culturalBackground: 'Spanish',
        gender: 'Neutral' as TutorGender,
        age: 25,
        location: 'Spain',
        isActive: true,
        displayOrder: 1,
      };
      
      const mockTutor: TutorDetail = {
        id: 'custom-tutor-123',
        name: 'My Tutor',
        emoji: '📚',
        persona: 'A personalized Spanish tutor',
        domain: 'Spanish',
        personality: 'Casual' as TutorPersonality,
        teachingStyle: 'Guided' as TeachingStyle,
        description: 'A personalized Spanish tutor',
        targetLanguageCode: 'es',
        culturalBackground: 'Spanish',
        age: 25,
        gender: 'Neutral' as TutorGender,
        imageUrl: null,
        displayOrder: 1,
        location: 'Spain',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };
      
      mockPost.mockResolvedValue({ data: mockTutor });
      
      const result = await createCustomTutor(request);
      
      expect(mockPost).toHaveBeenCalledWith('/catalog/tutors', request);
      expect(result).toEqual(mockTutor);
    });
  });
});