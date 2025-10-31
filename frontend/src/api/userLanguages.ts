import apiClient from './client';
import { CEFRLevel, LanguageProficiencyType } from '../types';
import type { LanguageProficiency } from '../types';

export async function getLanguageProficiencies(
  userId: string
): Promise<LanguageProficiency[]> {
  const response = await apiClient.get<LanguageProficiency[]>(
    `/users/${userId}/languages`
  );
  return response.data;
}

export async function addLanguageProficiency(
  userId: string,
  languageCode: string,
  type: LanguageProficiencyType,
  cefrLevel?: CEFRLevel
): Promise<LanguageProficiency> {
  const response = await apiClient.post<LanguageProficiency>(
    `/users/${userId}/languages`,
    {
      languageCode,
      type,
      cefrLevel,
      isNative: type === LanguageProficiencyType.Native,
    }
  );
  return response.data;
}

export async function updateLanguageProficiency(
  userId: string,
  languageCode: string,
  cefrLevel: CEFRLevel
): Promise<LanguageProficiency> {
  const response = await apiClient.patch<LanguageProficiency>(
    `/users/${userId}/languages/${languageCode}`,
    {
      cefrLevel,
    }
  );
  return response.data;
}

export async function setPrimaryLanguage(
  userId: string,
  languageCode: string
): Promise<LanguageProficiency> {
  const response = await apiClient.post<LanguageProficiency>(
    `/users/${userId}/languages/${languageCode}/primary`
  );
  return response.data;
}

export async function removeLanguageProficiency(
  userId: string,
  languageCode: string
): Promise<void> {
  await apiClient.delete(`/users/${userId}/languages/${languageCode}`);
}
