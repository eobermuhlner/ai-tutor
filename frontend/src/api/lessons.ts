import apiClient from './client';
import type { LessonContent } from '../types';

/**
 * Get current lesson for a session
 */
export async function getCurrentLesson(sessionId: string): Promise<LessonContent> {
  const response = await apiClient.get<LessonContent>(`/lessons/sessions/${sessionId}/current`);
  return response.data;
}

/**
 * Get a specific lesson by courseId and lessonId
 */
export async function getLesson(courseId: string, lessonId: string): Promise<LessonContent> {
  const response = await apiClient.get<LessonContent>(`/lessons/courses/${courseId}/lessons/${lessonId}`);
  return response.data;
}

/**
 * Advance to the next lesson in the session
 */
export async function advanceLesson(sessionId: string): Promise<LessonContent> {
  const response = await apiClient.post<LessonContent>(`/lessons/sessions/${sessionId}/advance`);
  return response.data;
}
