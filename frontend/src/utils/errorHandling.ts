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
export function isQuotaExceededError(error: any): boolean {
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
  const errorMessage = axiosError.response?.data?.error?.message ||
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
export function getErrorMessage(error: any, defaultMessage: string = 'An error occurred'): string {
  if (!error) return defaultMessage;

  // Check for quota errors first
  if (isQuotaExceededError(error)) {
    return 'API quota exceeded. The service has reached its usage limit. Please try again later or contact support.';
  }

  const axiosError = error as AxiosError<any>;

  // Try to extract message from response
  const responseData = axiosError.response?.data;
  if (responseData) {
    // Check for standard message field
    if (responseData.message) {
      return responseData.message;
    }

    // Check for OpenAI error structure
    if (responseData.error?.message) {
      return responseData.error.message;
    }
  }

  // Fall back to error message or default
  return error.message || defaultMessage;
}
