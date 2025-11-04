import { describe, it, expect, vi, beforeEach } from 'vitest';
import { 
  getLanguages,
  getCourses,
  getCourse,
  getTutors,
  createCustomTutor,
  getTutorImage,
  getTutorImagePreview,
} from './catalog';
import apiClient from './client';
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

global.FileReader = vi.fn(() => mockFileReader);

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
          isTarget: true,
          isSource: true,
        }
      ];
      
      (mockApiClient.get as any).mockResolvedValue({ data: mockLanguages });
      
      const result = await getLanguages();
      
      expect(mockApiClient.get).toHaveBeenCalledWith('/catalog/languages', {
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
          isTarget: true,
          isSource: true,
        }
      ];
      
      (mockApiClient.get as any).mockResolvedValue({ data: mockLanguages });
      
      const result = await getLanguages(locale);
      
      expect(mockApiClient.get).toHaveBeenCalledWith('/catalog/languages', {
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
          title: 'Spanish for Beginners',
          description: 'Learn basic Spanish',
          languageCode: 'es',
          category: 'GENERAL',
          difficulty: 'BEGINNER',
          estimatedDuration: 10,
          estimatedDurationUnit: 'hours',
          targetLevel: 'A1',
          sourceLevel: 'A1',
        }
      ];
      
      (mockApiClient.get as any).mockResolvedValue({ data: mockCourses });
      
      const result = await getCourses(languageCode);
      
      expect(mockApiClient.get).toHaveBeenCalledWith(
        `/catalog/languages/${languageCode}/courses`,
        { params: { locale: 'en' } }
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
          title: 'French for Travel',
          description: 'Travel-specific French phrases',
          languageCode: 'fr',
          category: 'TRAVEL',
          difficulty: 'INTERMEDIATE',
          estimatedDuration: 5,
          estimatedDurationUnit: 'hours',
          targetLevel: 'A2',
          sourceLevel: 'A1',
        }
      ];
      
      (mockApiClient.get as any).mockResolvedValue({ data: mockCourses });
      
      const result = await getCourses(languageCode, locale, cefrLevel, category);
      
      expect(mockApiClient.get).toHaveBeenCalledWith(
        `/catalog/languages/${languageCode}/courses`,
        { 
          params: { 
            locale: 'de',
            cefrLevel: 'A2',
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
        title: 'Spanish for Beginners',
        description: 'Learn basic Spanish',
        languageCode: 'es',
        category: 'GENERAL',
        difficulty: 'BEGINNER',
        estimatedDuration: 10,
        estimatedDurationUnit: 'hours',
        targetLevel: 'A1',
        sourceLevel: 'A1',
        lessons: [],
      };
      
      (mockApiClient.get as any).mockResolvedValue({ data: mockCourse });
      
      const result = await getCourse(courseId);
      
      expect(mockApiClient.get).toHaveBeenCalledWith(
        `/catalog/courses/${courseId}`,
        { params: { locale: 'en' } }
      );
      expect(result).toEqual(mockCourse);
    });

    it('should fetch a specific course with custom locale', async () => {
      const courseId = 'course2';
      const locale = 'fr';
      const mockCourse: CourseDetail = {
        id: courseId,
        title: 'Français pour Débutants',
        description: 'Apprenez le français de base',
        languageCode: 'fr',
        category: 'GENERAL',
        difficulty: 'BEGINNER',
        estimatedDuration: 10,
        estimatedDurationUnit: 'hours',
        targetLevel: 'A1',
        sourceLevel: 'A1',
        lessons: [],
      };
      
      (mockApiClient.get as any).mockResolvedValue({ data: mockCourse });
      
      const result = await getCourse(courseId, locale);
      
      expect(mockApiClient.get).toHaveBeenCalledWith(
        `/catalog/courses/${courseId}`,
        { params: { locale: 'fr' } }
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
          languageCode: 'es',
          gender: 'Female',
          age: 30,
          personality: 'Encouraging',
          isGlobal: true,
        }
      ];
      
      (mockApiClient.get as any).mockResolvedValue({ data: mockTutors });
      
      const result = await getTutors(languageCode);
      
      expect(mockApiClient.get).toHaveBeenCalledWith(
        `/catalog/languages/${languageCode}/tutors`,
        { params: { locale: 'en' } }
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
          languageCode: 'de',
          gender: 'Male',
          age: 40,
          personality: 'Strict',
          isGlobal: true,
        }
      ];
      
      (mockApiClient.get as any).mockResolvedValue({ data: mockTutors });
      
      const result = await getTutors(languageCode, locale);
      
      expect(mockApiClient.get).toHaveBeenCalledWith(
        `/catalog/languages/${languageCode}/tutors`,
        { params: { locale: 'it' } }
      );
      expect(result).toEqual(mockTutors);
    });
  });

  describe('createCustomTutor', () => {
    it('should create a custom tutor', async () => {
      const request: CreateTutorRequest = {
        name: 'My Tutor',
        languageCode: 'es',
        gender: 'Neutral',
        age: 25,
        personality: 'Casual',
        isGlobal: false,
        description: 'A personalized Spanish tutor',
      };
      
      const mockTutor: TutorDetail = {
        id: 'custom-tutor-123',
        name: 'My Tutor',
        languageCode: 'es',
        gender: 'Neutral',
        age: 25,
        personality: 'Casual',
        isGlobal: false,
        description: 'A personalized Spanish tutor',
        createdAt: new Date().toISOString(),
        image: null,
      };
      
      (mockApiClient.post as any).mockResolvedValue({ data: mockTutor });
      
      const result = await createCustomTutor(request);
      
      expect(mockApiClient.post).toHaveBeenCalledWith('/catalog/tutors', request);
      expect(result).toEqual(mockTutor);
    });
  });
});