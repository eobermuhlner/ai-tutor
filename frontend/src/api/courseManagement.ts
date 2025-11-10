import apiClient from './client';

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
  defaultPhase: string; // ConversationPhase
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
  defaultPhase?: string; // ConversationPhase
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
  defaultPhase: string; // ConversationPhase
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
  const response = await apiClient.post<CourseResponse>('/courses', request);
  return response.data;
}

export async function updateCourse(courseId: string, request: UpdateCourseRequest): Promise<CourseResponse> {
  const response = await apiClient.put<CourseResponse>(`/courses/${courseId}`, request);
  return response.data;
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
  return response.data;
}

export async function deleteCourse(courseId: string): Promise<void> {
  await apiClient.delete(`/courses/${courseId}`);
}