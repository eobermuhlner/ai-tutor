import apiClient from './client';

export interface LessonRequest {
  lessonId: string;
  title: string;
  content: string;
  displayOrder: number;
  minimumDays?: number | null;
  requiredTurns?: number | null;
}

export interface LessonResponse {
  id: string;
  courseId: string;
  lessonId: string;
  title: string;
  content: string;
  displayOrder: number;
  minimumDays?: number | null;
  requiredTurns?: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface ReorderLessonsRequest {
  lessons: LessonOrderUpdate[];
}

export interface LessonOrderUpdate {
  id: string;
  displayOrder: number;
}

/**
 * Get all lessons for a course
 */
export async function getLessons(courseId: string): Promise<LessonResponse[]> {
  const response = await apiClient.get<LessonResponse[]>(`/courses/${courseId}/lessons`);
  return response.data;
}

/**
 * Create a new lesson for a course
 */
export async function createLesson(courseId: string, lesson: LessonRequest): Promise<LessonResponse> {
  const response = await apiClient.post<LessonResponse>(`/courses/${courseId}/lessons`, lesson);
  return response.data;
}

/**
 * Update an existing lesson
 */
export async function updateLesson(
  courseId: string, 
  lessonId: string, 
  lesson: LessonRequest
): Promise<LessonResponse> {
  const response = await apiClient.put<LessonResponse>(`/courses/${courseId}/lessons/${lessonId}`, lesson);
  return response.data;
}

/**
 * Delete a lesson
 */
export async function deleteLesson(courseId: string, lessonId: string): Promise<void> {
  await apiClient.delete(`/courses/${courseId}/lessons/${lessonId}`);
}

/**
 * Reorder lessons
 */
export async function reorderLessons(courseId: string, request: ReorderLessonsRequest): Promise<void> {
  await apiClient.put(`/courses/${courseId}/lessons/reorder`, request);
}