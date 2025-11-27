import apiClient from './client';
import type { LessonContent } from '../types';

export interface CourseCurriculum {
  courseId: string;
  progressionMode: string; // Always "COMPLETION_BASED"
  lessons: LessonMetadata[];
}

export interface LessonMetadata {
  id: string;
  file: string;
  requiredTurns: number;
}

/**
 * Get current lesson for a session
 */
export async function getCurrentLesson(sessionId: string): Promise<LessonContent> {
  const response = await apiClient.get<LessonContent>(`/lessons/sessions/${sessionId}/current`);
  return response.data;
}

/**
 * Get course curriculum with lesson metadata
 */
export async function getCourseCurriculum(courseId: string): Promise<CourseCurriculum> {
  const response = await apiClient.get<CourseCurriculum>(`/lessons/courses/${courseId}/curriculum`);
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
