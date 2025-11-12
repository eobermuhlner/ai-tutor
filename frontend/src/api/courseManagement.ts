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