import apiClient from './client';
import type { User } from '../types';

export interface UpdateSubscriptionPlanRequest {
  subscriptionPlan: 'FREE' | 'FREE_BYOK' | 'SUBSCRIPTION_10';
}

export interface UpdateUserRequest {
  email?: string;
  firstName?: string;
  lastName?: string;
  enabled?: boolean;
  locked?: boolean;
  roles?: string[];
}

export interface UsersPageResponse {
  users: User[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
}

export interface GetUsersParams {
  page?: number;
  size?: number;
  search?: string;
  role?: 'USER' | 'ADMIN';
  subscriptionPlan?: 'FREE' | 'FREE_BYOK' | 'SUBSCRIPTION_10';
  enabled?: boolean;
  locked?: boolean;
}

export async function updateUserSubscriptionPlan(
  userId: string,
  subscriptionPlan: 'FREE' | 'FREE_BYOK' | 'SUBSCRIPTION_10'
): Promise<User> {
  const response = await apiClient.patch<User>(
    `/admin/users/${userId}/subscription-plan`,
    { subscriptionPlan }
  );
  return response.data;
}

export async function getUsers(params?: GetUsersParams): Promise<UsersPageResponse> {
  const response = await apiClient.get<UsersPageResponse>('/admin/users', { params });
  return response.data;
}

export async function getUser(userId: string): Promise<User> {
  const response = await apiClient.get<User>(`/admin/users/${userId}`);
  return response.data;
}

export async function updateUser(userId: string, data: UpdateUserRequest): Promise<User> {
  const response = await apiClient.patch<User>(`/admin/users/${userId}`, data);
  return response.data;
}

export async function forceLogoutUser(userId: string): Promise<{ message: string }> {
  const response = await apiClient.post<{ message: string }>(`/admin/users/${userId}/force-logout`);
  return response.data;
}

export async function resetUserPassword(userId: string): Promise<{ message: string }> {
  const response = await apiClient.post<{ message: string }>(`/admin/users/${userId}/reset-password`);
  return response.data;
}
