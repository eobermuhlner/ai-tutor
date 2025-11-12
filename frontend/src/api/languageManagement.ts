import apiClient from './client';
import { Difficulty } from '../types';
import type { Language } from '../types';

export interface LanguageRequest {
  code: string;
  nameJson: string;           // JSON map: {"en": "Spanish", "es": "Español", ...}
  flagEmoji: string;
  nativeName: string;
  difficulty: Difficulty;
  descriptionJson: string;    // JSON map: {"en": "Spanish description", ...}
  isActive: boolean;
  displayOrder: number;
}

// Get all languages
export async function getAllLanguages(): Promise<Language[]> {
  const response = await apiClient.get<Language[]>('/languages');
  return response.data;
}

// Get a specific language by code
export async function getLanguage(code: string): Promise<Language> {
  const response = await apiClient.get<Language>(`/languages/${code}`);
  return response.data;
}

// Create a new language
export async function createLanguage(request: LanguageRequest): Promise<Language> {
  const response = await apiClient.post<Language>('/languages', request);
  return response.data;
}

// Update an existing language
export async function updateLanguage(code: string, request: LanguageRequest): Promise<Language> {
  const response = await apiClient.put<Language>(`/languages/${code}`, request);
  return response.data;
}

// Delete a language
export async function deleteLanguage(code: string): Promise<void> {
  await apiClient.delete(`/languages/${code}`);
}

// Activate a language
export async function activateLanguage(code: string): Promise<Language> {
  const response = await apiClient.post<Language>(`/languages/${code}/activate`);
  return response.data;
}

// Deactivate a language
export async function deactivateLanguage(code: string): Promise<Language> {
  const response = await apiClient.post<Language>(`/languages/${code}/deactivate`);
  return response.data;
}