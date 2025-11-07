import { describe, it, expect, vi, beforeEach } from 'vitest';
import { 
  getLanguageProficiencies,
  addLanguageProficiency,
  updateLanguageProficiency,
  setPrimaryLanguage,
  removeLanguageProficiency,
} from './userLanguages';
import type { LanguageProficiency } from '../types';
import { CEFRLevel, LanguageProficiencyType } from '../types';

// Mock the apiClient
const mockGet = vi.fn();
const mockPost = vi.fn();
const mockPatch = vi.fn();
const mockDelete = vi.fn();

vi.mock('./client', () => ({
  default: {
    get: mockGet,
    post: mockPost,
    patch: mockPatch,
    delete: mockDelete,
  }
}));

describe('userLanguages API module', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('getLanguageProficiencies', () => {
    it('should fetch user language proficiencies', async () => {
      const userId = 'user123';
      const mockProficiencies: LanguageProficiency[] = [
        {
          id: 'prof1',
          userId: userId,
          languageCode: 'en',
          proficiencyType: LanguageProficiencyType.Learning,
          cefrLevel: CEFRLevel.B1,
          isNative: false,
          isPrimary: true,
          selfAssessed: true,
          lastAssessedAt: new Date().toISOString(),
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        }
      ];
      
      mockGet.mockResolvedValue({ data: mockProficiencies });
      
      const result = await getLanguageProficiencies(userId);
      
      expect(mockGet).toHaveBeenCalledWith(`/users/${userId}/languages`);
      expect(result).toEqual(mockProficiencies);
    });
  });

  describe('addLanguageProficiency', () => {
    it('should add a new language proficiency', async () => {
      const userId = 'user123';
      const mockProficiency: LanguageProficiency = {
        id: 'newProf',
        userId: userId,
        languageCode: 'es',
        proficiencyType: LanguageProficiencyType.Learning,
        cefrLevel: CEFRLevel.A2,
        isNative: false,
        isPrimary: false,
        selfAssessed: true,
        lastAssessedAt: new Date().toISOString(),
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };
      
      mockPost.mockResolvedValue({ data: mockProficiency });
      
      const result = await addLanguageProficiency(
        userId, 
        'es', 
        LanguageProficiencyType.Learning, 
        CEFRLevel.A2
      );
      
      expect(mockPost).toHaveBeenCalledWith(
        `/users/${userId}/languages`,
        {
          languageCode: 'es',
          type: LanguageProficiencyType.Learning,
          cefrLevel: CEFRLevel.A2,
          isNative: false,
        }
      );
      expect(result).toEqual(mockProficiency);
    });

    it('should handle native language type', async () => {
      const userId = 'user123';
      const mockProficiency: LanguageProficiency = {
        id: 'newProf',
        userId: userId,
        languageCode: 'en',
        proficiencyType: LanguageProficiencyType.Native,
        cefrLevel: CEFRLevel.C2,
        isNative: true,
        isPrimary: true,
        selfAssessed: true,
        lastAssessedAt: new Date().toISOString(),
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };
      
      mockPost.mockResolvedValue({ data: mockProficiency });
      
      const result = await addLanguageProficiency(
        userId, 
        'en', 
        LanguageProficiencyType.Native
      );
      
      expect(mockPost).toHaveBeenCalledWith(
        `/users/${userId}/languages`,
        {
          languageCode: 'en',
          type: LanguageProficiencyType.Native,
          cefrLevel: undefined,
          isNative: true,
        }
      );
      expect(result).toEqual(mockProficiency);
    });
  });

  describe('updateLanguageProficiency', () => {
    it('should update language proficiency level', async () => {
      const userId = 'user123';
      const languageCode = 'fr';
      const mockUpdatedProficiency: LanguageProficiency = {
        id: 'prof1',
        userId: userId,
        languageCode: languageCode,
        proficiencyType: LanguageProficiencyType.Learning,
        cefrLevel: CEFRLevel.B2,
        isNative: false,
        isPrimary: false,
        selfAssessed: true,
        lastAssessedAt: new Date().toISOString(),
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };
      
      mockPatch.mockResolvedValue({ data: mockUpdatedProficiency });
      
      const result = await updateLanguageProficiency(
        userId,
        languageCode,
        CEFRLevel.B2
      );
      
      expect(mockPatch).toHaveBeenCalledWith(
        `/users/${userId}/languages/${languageCode}`,
        {
          cefrLevel: CEFRLevel.B2,
        }
      );
      expect(result).toEqual(mockUpdatedProficiency);
    });
  });

  describe('setPrimaryLanguage', () => {
    it('should set a language as primary', async () => {
      const userId = 'user123';
      const languageCode = 'de';
      const mockProficiency: LanguageProficiency = {
        id: 'prof1',
        userId: userId,
        languageCode: languageCode,
        proficiencyType: LanguageProficiencyType.Learning,
        cefrLevel: CEFRLevel.B1,
        isNative: false,
        isPrimary: true,
        selfAssessed: true,
        lastAssessedAt: new Date().toISOString(),
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };
      
      mockPost.mockResolvedValue({ data: mockProficiency });
      
      const result = await setPrimaryLanguage(userId, languageCode);
      
      expect(mockPost).toHaveBeenCalledWith(
        `/users/${userId}/languages/${languageCode}/primary`
      );
      expect(result).toEqual(mockProficiency);
    });
  });

  describe('removeLanguageProficiency', () => {
    it('should remove a language proficiency', async () => {
      const userId = 'user123';
      const languageCode = 'it';
      
      mockDelete.mockResolvedValue({});
      
      await removeLanguageProficiency(userId, languageCode);
      
      expect(mockDelete).toHaveBeenCalledWith(
        `/users/${userId}/languages/${languageCode}`
      );
    });
  });
});