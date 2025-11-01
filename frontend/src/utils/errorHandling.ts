import type { AxiosError } from 'axios';

interface OpenAIErrorResponse {
  error?: {
    message?: string;
    type?: string;
    code?: string;
  };
}

/**
 * Checks if an error is an OpenAI quota exceeded error
 */
export function isQuotaExceededError(error: unknown): boolean {
  if (!error) return false;

  const axiosError = error as AxiosError<OpenAIErrorResponse>;

  // Check for HTTP 429 status
  if (axiosError.response?.status === 429) {
    return true;
  }

  // Check for OpenAI quota error in response body
  const responseData = axiosError.response?.data;
  if (responseData && typeof responseData === 'object') {
    const errorType = responseData.error?.type;
    const errorCode = responseData.error?.code;

    if (errorType === 'insufficient_quota' || errorCode === 'insufficient_quota') {
      return true;
    }
  }

  // Check error message for quota-related keywords
  let errorMessage = '';
  if (responseData && typeof responseData === 'object' && 'error' in responseData && 
      responseData.error && typeof responseData.error === 'object' && 'message' in responseData.error) {
    errorMessage = (responseData.error as { message?: string }).message || '';
  }
  
  errorMessage = errorMessage || 
                 axiosError.message || 
                 String(error);

  if (errorMessage.toLowerCase().includes('quota') ||
      errorMessage.toLowerCase().includes('rate limit')) {
    return true;
  }

  return false;
}

/**
 * Extracts a user-friendly error message from an API error
 */
export function getErrorMessage(error: unknown, defaultMessage: string = 'An error occurred'): string {
  if (!error) return defaultMessage;

  // Check for quota errors first
  if (isQuotaExceededError(error)) {
    return 'API quota exceeded. The service has reached its usage limit. Please try again later or contact support.';
  }

  const axiosError = error as AxiosError<Record<string, unknown> | OpenAIErrorResponse>;

  // Try to extract message from response
  const responseData = axiosError.response?.data;
  if (responseData && typeof responseData === 'object') {
    // Check for standard message field
    if ('message' in responseData && typeof responseData.message === 'string') {
      return responseData.message;
    }

    // Check for OpenAI error structure
    if ('error' in responseData && typeof responseData.error === 'object' && responseData.error !== null) {
      const errorObj = responseData.error as { message?: string };
      if ('message' in errorObj && typeof errorObj.message === 'string') {
        return errorObj.message;
      }
    }
  }

  // Fall back to error message or default
  return (error as { message?: string }).message || defaultMessage;
}
