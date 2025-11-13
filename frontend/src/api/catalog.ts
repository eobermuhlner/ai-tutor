import apiClient from './client';
import type { Language, Course, CourseDetail, Tutor, TutorDetail, CreateTutorRequest, CatalogImportResponse, CatalogValidationResponse } from '../types';

export async function getLanguages(
  locale: string = 'en'
): Promise<Language[]> {
  const response = await apiClient.get<Language[]>('/catalog/languages', {
    params: { locale },
  });
  return response.data;
}

export async function getCourses(
  languageCode: string,
  locale: string = 'en',
  cefrLevel?: string,
  category?: string
): Promise<Course[]> {
  const params: { [key: string]: string | undefined } = { locale };
  if (cefrLevel) params.cefrLevel = cefrLevel;
  if (category) params.category = category;
  
  const response = await apiClient.get<Course[]>(
    `/catalog/languages/${languageCode}/courses`,
    { params }
  );
  return response.data;
}

export async function getCourse(
  courseId: string,
  locale: string = 'en'
): Promise<CourseDetail> {
  const response = await apiClient.get<CourseDetail>(`/catalog/courses/${courseId}`, {
    params: { locale },
  });
  return response.data;
}

export async function getTutors(
  languageCode: string,
  locale: string = 'en'
): Promise<Tutor[]> {
  const response = await apiClient.get<Tutor[]>(
    `/catalog/languages/${languageCode}/tutors`,
    { params: { locale } }
  );
  return response.data;
}

export async function createCustomTutor(
  request: CreateTutorRequest
): Promise<TutorDetail> {
  const response = await apiClient.post<TutorDetail>('/catalog/tutors', request);
  return response.data;
}

export async function updateCustomTutor(
  tutorId: string,
  request: Partial<CreateTutorRequest>
): Promise<TutorDetail> {
  const response = await apiClient.put<TutorDetail>(`/catalog/tutors/${tutorId}`, request);
  return response.data;
}

export async function getTutorImage(tutorId: string): Promise<string | null> {
  try {
    const response = await apiClient.get<Blob>(
      `/images/tutor/${tutorId}/data`,
      { responseType: 'blob' }
    );

    if (response.data.size === 0) {
      console.log('🖼️ getTutorImage: Empty blob received');
      return null;
    }

    // Convert blob to data URL
    const reader = new FileReader();
    return new Promise((resolve, reject) => {
      reader.onload = () => {
        const dataUrl = reader.result as string;
        console.log('🖼️ getTutorImage: Converted to data URL', { tutorId, urlLength: dataUrl.length });
        resolve(dataUrl);
      };
      reader.onerror = () => {
        console.error('🖼️ getTutorImage: FileReader error');
        reject(reader.error);
      };
      reader.readAsDataURL(response.data);
    });
  } catch (error) {
    console.error('🖼️ getTutorImage: Failed to fetch image', { tutorId, error });
    return null;
  }
}

export async function getTutorImagePreview(
  languageCode: string,
  gender: string,
  age: number,
  location?: string,
  persona?: string
): Promise<string | null> {
  try {
    const params: Record<string, string | number> = {
      languageCode,
      gender,
      age,
    };
    if (location) params.location = location;
    if (persona) params.persona = persona;

    const response = await apiClient.get<Blob>(
      '/images/person/preview',
      {
        responseType: 'blob',
        params
      }
    );

    if (response.data.size === 0) {
      console.log('🖼️ getTutorImagePreview: Empty blob received');
      return null;
    }

    // Convert blob to data URL
    const reader = new FileReader();
    return new Promise((resolve, reject) => {
      reader.onload = () => {
        const dataUrl = reader.result as string;
        console.log('🖼️ getTutorImagePreview: Converted to data URL', {
          languageCode, gender, age, location, persona, urlLength: dataUrl.length
        });
        resolve(dataUrl);
      };
      reader.onerror = () => {
        console.error('🖼️ getTutorImagePreview: FileReader error');
        reject(reader.error);
      };
      reader.readAsDataURL(response.data);
    });
  } catch {
    console.log('🖼️ getTutorImagePreview: No image found (expected for preview)', {
      languageCode, gender, age, location, persona
    });
    return null;
  }
}

/**
 * Import a complete catalog (languages, tutors, courses) from a catalog.yml file.
 * Requires ADMIN role.
 */
export async function importCatalog(
  catalogFile: File,
  lessonFiles: File[] = []
): Promise<CatalogImportResponse> {
  const formData = new FormData();
  formData.append('catalogFile', catalogFile);
  lessonFiles.forEach(file => {
    formData.append('lessonFiles', file);
  });

  const response = await apiClient.post<CatalogImportResponse>(
    '/catalog/import',
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
 * Validate a catalog file before importing.
 * Requires ADMIN role.
 */
export async function validateCatalog(
  catalogFile: File,
  lessonFiles: File[] = []
): Promise<CatalogValidationResponse> {
  const formData = new FormData();
  formData.append('catalogFile', catalogFile);
  lessonFiles.forEach(file => {
    formData.append('lessonFiles', file);
  });

  const response = await apiClient.post<CatalogValidationResponse>(
    '/catalog/import/validate',
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }
  );
  return response.data;
}
