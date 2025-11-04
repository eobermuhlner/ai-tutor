import { describe, it, expect } from 'vitest';
import { isQuotaExceededError, getErrorMessage } from './errorHandling';

describe('errorHandling utils', () => {
  describe('isQuotaExceededError', () => {
    it('returns false for null/undefined error', () => {
      expect(isQuotaExceededError(null)).toBe(false);
      expect(isQuotaExceededError(undefined)).toBe(false);
    });

    it('returns true for HTTP 429 status', () => {
      const error = {
        response: {
          status: 429,
        }
      };
      
      expect(isQuotaExceededError(error)).toBe(true);
    });

    it('returns true for OpenAI insufficient_quota type', () => {
      const error = {
        response: {
          status: 400,
          data: {
            error: {
              type: 'insufficient_quota'
            }
          }
        }
      };
      
      expect(isQuotaExceededError(error)).toBe(true);
    });

    it('returns true for OpenAI insufficient_quota code', () => {
      const error = {
        response: {
          status: 400,
          data: {
            error: {
              code: 'insufficient_quota'
            }
          }
        }
      };
      
      expect(isQuotaExceededError(error)).toBe(true);
    });

    it('returns true for error messages containing quota', () => {
      const error = {
        message: 'OpenAI API Error: quota exceeded'
      };
      
      expect(isQuotaExceededError(error)).toBe(true);
    });

    it('returns true for error messages containing rate limit', () => {
      const error = {
        message: 'Rate limit exceeded'
      };
      
      expect(isQuotaExceededError(error)).toBe(true);
    });

    it('returns false for non quota-related errors', () => {
      const error = {
        message: 'Something went wrong'
      };
      
      expect(isQuotaExceededError(error)).toBe(false);
    });

    it('returns true for quota in response data message', () => {
      const error = {
        response: {
          status: 400,
          data: {
            error: {
              message: 'Your quota has been exceeded'
            }
          }
        }
      };
      
      expect(isQuotaExceededError(error)).toBe(true);
    });
  });

  describe('getErrorMessage', () => {
    it('returns default message for null/undefined error', () => {
      expect(getErrorMessage(null)).toBe('An error occurred');
      expect(getErrorMessage(undefined)).toBe('An error occurred');
      expect(getErrorMessage(null, 'Custom default')).toBe('Custom default');
    });

    it('returns quota exceeded message for quota errors', () => {
      const quotaError = {
        response: {
          status: 429
        }
      };
      
      expect(getErrorMessage(quotaError)).toBe('API quota exceeded. The service has reached its usage limit. Please try again later or contact support.');
    });

    it('returns message from response data', () => {
      const error = {
        response: {
          data: {
            message: 'Validation error occurred'
          }
        }
      };
      
      expect(getErrorMessage(error)).toBe('Validation error occurred');
    });

    it('returns message from OpenAI error structure', () => {
      const error = {
        response: {
          data: {
            error: {
              message: 'OpenAI specific error'
            }
          }
        }
      };
      
      expect(getErrorMessage(error)).toBe('OpenAI specific error');
    });

    it('returns error message as fallback', () => {
      const error = {
        message: 'Generic error message'
      };
      
      expect(getErrorMessage(error)).toBe('Generic error message');
    });

    it('returns default message when no other messages found', () => {
      const error = {
        someOtherProp: 'value'
      };
      
      expect(getErrorMessage(error, 'Fallback message')).toBe('Fallback message');
    });
  });
});