import apiClient from './client';
import { ConversationPhase } from '../types';

// Helper functions to convert between frontend and backend ConversationPhase formats
function frontendToBackendPhase(frontendPhase: ConversationPhase): string {
  const phaseMap: Record<ConversationPhase, string> = {
    [ConversationPhase.FREE]: 'Free',
    [ConversationPhase.CORRECTION]: 'Correction',
    [ConversationPhase.DRILL]: 'Drill',
    [ConversationPhase.AUTO]: 'Auto',
  };
  return phaseMap[frontendPhase];
}

function backendToFrontendPhase(backendPhase: string): ConversationPhase {
  const phaseMap: Record<string, ConversationPhase> = {
    'Free': ConversationPhase.FREE,
    'Correction': ConversationPhase.CORRECTION,
    'Drill': ConversationPhase.DRILL,
    'Auto': ConversationPhase.AUTO,
  };
  return phaseMap[backendPhase] || ConversationPhase.FREE;
}

// Course management API functions

export interface CreateCourseRequest {
  languageCode: string;
  nameJson: string;
  shortDescriptionJson: string;
  descriptionJson: string;
  category: string; // CourseCategory
  targetAudienceJson: string;
  startingLevel: string; // CEFRLevel
  targetLevel: string; // CEFRLevel
  estimatedWeeks?: number | null;
  suggestedTutorIdsJson?: string | null;
  defaultPhase: ConversationPhase | string; // Frontend enum or backend string
  topicSequenceJson?: string | null;
  learningGoalsJson: string;
  tagsJson?: string | null;
}

export interface UpdateCourseRequest {
  nameJson?: string;
  shortDescriptionJson?: string;
  descriptionJson?: string;
  category?: string; // CourseCategory
  targetAudienceJson?: string;
  startingLevel?: string; // CEFRLevel
  targetLevel?: string; // CEFRLevel
  estimatedWeeks?: number | null;
  suggestedTutorIdsJson?: string | null;
  defaultPhase?: ConversationPhase | string; // Frontend enum or backend string
  topicSequenceJson?: string | null;
  learningGoalsJson?: string;
  tagsJson?: string | null;
}

export interface CourseResponse {
  id: string;
  languageCode: string;
  nameJson: string;
  shortDescriptionJson: string;
  descriptionJson: string;
  category: string; // CourseCategory
  targetAudienceJson: string;
  startingLevel: string; // CEFRLevel
  targetLevel: string; // CEFRLevel
  estimatedWeeks?: number | null;
  suggestedTutorIdsJson?: string | null;
  defaultPhase: ConversationPhase; // Frontend enum type after conversion
  topicSequenceJson?: string | null;
  learningGoalsJson: string;
  isActive: boolean;
  displayOrder: number;
  tagsJson?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
  isDraft: boolean;
  publishedAt?: string | null;
  lastEditedBy?: string | null; // UUID as string
  version: number;
}

export async function getAllCourses(includeDrafts: boolean = false): Promise<CourseResponse[]> {
  const response = await apiClient.get<CourseResponse[]>('/courses', {
    params: { includeDrafts }
  });
  return response.data;
}

export async function createCourse(request: CreateCourseRequest): Promise<CourseResponse> {
  // Convert defaultPhase to backend format before sending if it's a frontend enum value
  let backendPhase = request.defaultPhase;
  if (Object.values(ConversationPhase).includes(request.defaultPhase as ConversationPhase)) {
    // It's a frontend enum value, convert to backend format
    backendPhase = frontendToBackendPhase(request.defaultPhase as ConversationPhase);
  }
  // If it's already a string, assume it's in backend format
  
  const requestWithConvertedPhase = {
    ...request,
    defaultPhase: backendPhase,
  };
  
  const response = await apiClient.post<CourseResponse>('/courses', requestWithConvertedPhase);
  // Convert the response back to frontend format
  const responseData = response.data;
  responseData.defaultPhase = backendToFrontendPhase(responseData.defaultPhase);
  return responseData;
}

export async function updateCourse(courseId: string, request: UpdateCourseRequest): Promise<CourseResponse> {
  // Convert defaultPhase to backend format before sending, if it exists
  const requestWithConvertedPhase: UpdateCourseRequest = { ...request };
  if (request.defaultPhase !== undefined) {
    // Check if it's a frontend enum value
    if (Object.values(ConversationPhase).includes(request.defaultPhase as ConversationPhase)) {
      // It's a frontend enum value, convert to backend format
      requestWithConvertedPhase.defaultPhase = frontendToBackendPhase(request.defaultPhase as ConversationPhase);
    }
    // If it's already in string format, assume it's already in backend format
  }
  
  const response = await apiClient.put<CourseResponse>(`/courses/${courseId}`, requestWithConvertedPhase);
  // Convert the response back to frontend format
  const responseData = response.data;
  responseData.defaultPhase = backendToFrontendPhase(responseData.defaultPhase);
  return responseData;
}

export async function publishCourse(courseId: string): Promise<CourseResponse> {
  const response = await apiClient.post<CourseResponse>(`/courses/${courseId}/publish`);
  return response.data;
}

export async function unpublishCourse(courseId: string): Promise<CourseResponse> {
  const response = await apiClient.post<CourseResponse>(`/courses/${courseId}/unpublish`);
  return response.data;
}

export async function getCourse(courseId: string): Promise<CourseResponse> {
  const response = await apiClient.get<CourseResponse>(`/courses/${courseId}`);
  const responseData = response.data;
  // Convert defaultPhase from backend format to frontend format
  responseData.defaultPhase = backendToFrontendPhase(responseData.defaultPhase);
  return responseData;
}

export async function deleteCourse(courseId: string): Promise<void> {
  await apiClient.delete(`/courses/${courseId}`);
}

// Course import types and functions

export interface CourseImportRequest {
  languageCode: string;
  courseName: string;
  courseDescription?: string;
  category?: string; // CourseCategory
  startingLevel?: string; // CEFRLevel
  targetLevel?: string; // CEFRLevel
}

export interface CourseImportResponse {
  courseId: string;
  courseName: string;
  lessonsImported: number;
  errors: string[];
  success: boolean;
}

/**
 * Import complete course from curriculum.yml and lesson markdown files.
 *
 * @param curriculumFile - curriculum.yml file
 * @param lessonFiles - Array of .md lesson files
 * @param metadata - Course metadata (language, name, description, etc.)
 * @returns Import result with course ID and status
 */
export async function importCourseFromFiles(
  curriculumFile: File,
  lessonFiles: File[],
  metadata: CourseImportRequest
): Promise<CourseImportResponse> {
  const formData = new FormData();

  // Add curriculum file
  formData.append('curriculumFile', curriculumFile);

  // Add lesson files
  lessonFiles.forEach(file => {
    formData.append('lessonFiles', file);
  });

  // Add metadata fields
  formData.append('languageCode', metadata.languageCode);
  formData.append('courseName', metadata.courseName);
  if (metadata.courseDescription) {
    formData.append('courseDescription', metadata.courseDescription);
  }
  if (metadata.category) {
    formData.append('category', metadata.category);
  }
  if (metadata.startingLevel) {
    formData.append('startingLevel', metadata.startingLevel);
  }
  if (metadata.targetLevel) {
    formData.append('targetLevel', metadata.targetLevel);
  }

  const response = await apiClient.post<CourseImportResponse>(
    '/courses/import/file',
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }
  );

  return response.data;
}

/**
 * Import lessons to existing course from markdown files.
 *
 * @param courseId - Existing course ID
 * @param lessonFiles - Array of .md lesson files
 * @returns Import result with number of lessons imported
 */
export async function importLessonsToExistingCourse(
  courseId: string,
  lessonFiles: File[]
): Promise<{ courseId: string; lessonsImported: number; errors: string[]; success: boolean }> {
  const formData = new FormData();

  lessonFiles.forEach(file => {
    formData.append('lessonFiles', file);
  });

  const response = await apiClient.post(
    `/courses/${courseId}/lessons/file`,
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }
  );

  return response.data;
}

/**
 * Validate import files without actually importing.
 *
 * @param curriculumFile - curriculum.yml file (optional)
 * @param lessonFiles - Array of .md lesson files
 * @returns Validation result with errors
 */
export async function validateImportFiles(
  curriculumFile: File | null,
  lessonFiles: File[]
): Promise<{ valid: boolean; errors: string[] }> {
  const formData = new FormData();

  if (curriculumFile) {
    formData.append('curriculumFile', curriculumFile);
  }

  lessonFiles.forEach(file => {
    formData.append('lessonFiles', file);
  });

  const response = await apiClient.post<{ valid: boolean; errors: string[] }>(
    '/courses/import/validate',
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }
  );

  return response.data;
}