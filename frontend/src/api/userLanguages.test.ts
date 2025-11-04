import { describe, it, expect, vi, beforeEach } from 'vitest';
import { 
  getLanguageProficiencies,
  addLanguageProficiency,
  updateLanguageProficiency,
  setPrimaryLanguage,
  removeLanguageProficiency,
  type LanguageProficiency
} from './userLanguages';
import apiClient from './client';
import { CEFRLevel, LanguageProficiencyType } from '../types';

// Mock the apiClient
vi.mock('./client');

const mockApiClient = apiClient as { 
  get: typeof vi.fn;
  post: typeof vi.fn;
  patch: typeof vi.fn;
  delete: typeof vi.fn;
};

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
          isPrimary: true,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        }
      ];
      
      (mockApiClient.get as any).mockResolvedValue({ data: mockProficiencies });
      
      const result = await getLanguageProficiencies(userId);
      
      expect(mockApiClient.get).toHaveBeenCalledWith(`/users/${userId}/languages`);
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
        isPrimary: false,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };
      
      (mockApiClient.post as any).mockResolvedValue({ data: mockProficiency });
      
      const result = await addLanguageProficiency(
        userId, 
        'es', 
        LanguageProficiencyType.Learning, 
        CEFRLevel.A2
      );
      
      expect(mockApiClient.post).toHaveBeenCalledWith(
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
        isPrimary: true,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };
      
      (mockApiClient.post as any).mockResolvedValue({ data: mockProficiency });
      
      const result = await addLanguageProficiency(
        userId, 
        'en', 
        LanguageProficiencyType.Native
      );
      
      expect(mockApiClient.post).toHaveBeenCalledWith(
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
        isPrimary: false,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };
      
      (mockApiClient.patch as any).mockResolvedValue({ data: mockUpdatedProficiency });
      
      const result = await updateLanguageProficiency(
        userId,
        languageCode,
        CEFRLevel.B2
      );
      
      expect(mockApiClient.patch).toHaveBeenCalledWith(
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
        isPrimary: true,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };
      
      (mockApiClient.post as any).mockResolvedValue({ data: mockProficiency });
      
      const result = await setPrimaryLanguage(userId, languageCode);
      
      expect(mockApiClient.post).toHaveBeenCalledWith(
        `/users/${userId}/languages/${languageCode}/primary`
      );
      expect(result).toEqual(mockProficiency);
    });
  });

  describe('removeLanguageProficiency', () => {
    it('should remove a language proficiency', async () => {
      const userId = 'user123';
      const languageCode = 'it';
      
      (mockApiClient.delete as any).mockResolvedValue({});
      
      await removeLanguageProficiency(userId, languageCode);
      
      expect(mockApiClient.delete).toHaveBeenCalledWith(
        `/users/${userId}/languages/${languageCode}`
      );
    });
  });
});