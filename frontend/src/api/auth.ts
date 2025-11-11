import apiClient from './client';
import type { User } from '../types';

interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

interface RefreshTokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export async function register(
  username: string,
  email: string,
  password: string
): Promise<User> {
  const response = await apiClient.post<User>('/auth/register', {
    username,
    email,
    password,
  });
  return response.data;
}

export async function login(
  email: string,
  password: string
): Promise<LoginResponse> {
  const response = await apiClient.post<LoginResponse>('/auth/login', {
    username: email, // Backend expects 'username' field (can be username or email)
    password,
  });
  return response.data;
}

export async function refreshToken(
  refreshToken: string
): Promise<RefreshTokenResponse> {
  const response = await apiClient.post<RefreshTokenResponse>(
    '/auth/refresh',
    {
      refreshToken,
    }
  );
  return response.data;
}

export async function getMe(): Promise<User> {
  const response = await apiClient.get<User>('/auth/me');
  return response.data;
}

export async function logout(): Promise<void> {
  await apiClient.post('/auth/logout');
}

export async function changePassword(
  currentPassword: string,
  newPassword: string
): Promise<void> {
  await apiClient.post('/auth/password', {
    currentPassword,
    newPassword,
  });
}

export async function changeEmail(newEmail: string): Promise<User> {
  const response = await apiClient.post<User>('/auth/email', {
    newEmail,
  });
  return response.data;
}

export async function updateProfile(firstName: string | null, lastName: string | null): Promise<User> {
  const response = await apiClient.post<User>('/auth/profile', {
    firstName,
    lastName,
  });
  return response.data;
}

export async function updatePronunciationPreference(pronunciationPreference: string): Promise<User> {
  const response = await apiClient.post<User>('/auth/pronunciation-preference', {
    pronunciationPreference,
  });
  return response.data;
}

// Email verification
export async function sendVerificationEmail(): Promise<{ message: string }> {
  const response = await apiClient.post<{ message: string }>('/auth/verify-email/send');
  return response.data;
}

export async function resendVerificationEmail(): Promise<{ message: string }> {
  const response = await apiClient.post<{ message: string }>('/auth/verify-email/resend');
  return response.data;
}

export async function verifyEmail(token: string): Promise<{ message: string }> {
  const response = await apiClient.post<{ message: string }>('/auth/verify-email', {
    token,
  });
  return response.data;
}

// Password reset
export async function forgotPassword(email: string): Promise<{ message: string }> {
  const response = await apiClient.post<{ message: string }>('/auth/password/forgot', {
    email,
  });
  return response.data;
}

export async function resetPassword(token: string, newPassword: string): Promise<{ message: string }> {
  const response = await apiClient.post<{ message: string }>('/auth/password/reset', {
    token,
    newPassword,
  });
  return response.data;
}
