import apiClient from './client';
import type { VocabularyItem, VocabularyContext, DueCountResponse } from '../types';

export interface VocabularyItemWithContexts extends VocabularyItem {
  contexts: VocabularyContext[];
}

export async function getVocabulary(
  userId: string,
  language?: string
): Promise<VocabularyItem[]> {
  const params = new URLSearchParams({ userId });
  if (language) {
    params.append('lang', language);
  }
  const response = await apiClient.get(`/vocabulary?${params.toString()}`);
  return response.data;
}

export async function getVocabularyItem(
  itemId: string
): Promise<VocabularyItemWithContexts> {
  const response = await apiClient.get(`/vocabulary/${itemId}`);
  return response.data;
}

export async function getDueVocabulary(
  lang: string,
  limit: number = 20
): Promise<VocabularyItem[]> {
  const params = new URLSearchParams({ lang, limit: limit.toString() });
  const response = await apiClient.get(`/vocabulary/due?${params.toString()}`);
  return response.data;
}

export async function getDueCount(lang: string): Promise<DueCountResponse> {
  const params = new URLSearchParams({ lang });
  const response = await apiClient.get(`/vocabulary/due/count?${params.toString()}`);
  return response.data;
}

export async function recordReview(
  itemId: string,
  success: boolean
): Promise<VocabularyItem> {
  const response = await apiClient.post(`/vocabulary/${itemId}/review`, { success });
  return response.data;
}
