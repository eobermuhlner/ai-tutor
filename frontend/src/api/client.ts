import axios, { type AxiosError } from 'axios';
import toast from 'react-hot-toast';
import * as storage from '../utils/storage';
import { API_BASE_URL } from '../utils/constants';
import { isQuotaExceededError } from '../utils/errorHandling';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Queue to hold failed requests during token refresh
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (value?: unknown) => void;
  reject: (reason?: unknown) => void;
}> = [];
let reconnectToastId: string | undefined;

const processQueue = (
  error: AxiosError | null,
  token: string | null = null
) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

// Request interceptor: add access token
apiClient.interceptors.request.use((config) => {
  const token = storage.getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor: handle 401 and refresh token
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config;

    if (!originalRequest) {
      return Promise.reject(error);
    }

    // Type assertion for retry flag
    const requestWithRetry = originalRequest as typeof originalRequest & {
      _retry?: boolean;
    };

    // If 401 and not already retrying
    if (error.response?.status === 401 && !requestWithRetry._retry) {
      if (isRefreshing) {
        // Wait for token refresh to complete
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then((token) => {
            if (originalRequest.headers) {
              originalRequest.headers.Authorization = `Bearer ${token}`;
            }
            return apiClient(originalRequest);
          })
          .catch((err) => Promise.reject(err));
      }

      requestWithRetry._retry = true;
      isRefreshing = true;

      const refreshToken = storage.getRefreshToken();
      if (!refreshToken) {
        storage.clearTokens();
        toast.error('Session expired. Please log in again.');
        window.location.href = '/login';
        return Promise.reject(error);
      }

      // Show reconnecting toast (only once for multiple queued requests)
      if (!reconnectToastId) {
        reconnectToastId = toast.loading('Reconnecting...');
      }

      try {
        // Make the refresh token request directly without importing auth API to avoid circular dependency
        const refreshResponse = await axios.post(`${API_BASE_URL}/auth/refresh`, {
          refreshToken,
        }, {
          headers: { 'Content-Type': 'application/json' }
        });
        const { accessToken, refreshToken: newRefreshToken } = refreshResponse.data;
        storage.setTokens(accessToken, newRefreshToken);
        if (originalRequest.headers) {
          originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        }
        processQueue(null, accessToken);

        // Dismiss reconnecting toast and show success
        if (reconnectToastId) {
          toast.success('Reconnected', { id: reconnectToastId });
          reconnectToastId = undefined;
        }

        return apiClient(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError as AxiosError, null);
        storage.clearTokens();

        // Dismiss reconnecting toast and show error
        if (reconnectToastId) {
          toast.error('Session expired. Please log in again.', { id: reconnectToastId });
          reconnectToastId = undefined;
        }

        window.location.href = '/login';
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    // Handle quota exceeded errors (HTTP 429 or OpenAI quota errors)
    if (isQuotaExceededError(error)) {
      toast.error(
        'API quota exceeded. The service has reached its usage limit. Please try again later or contact support.',
        { duration: 6000 }
      );
    }

    return Promise.reject(error);
  }
);

export default apiClient;
