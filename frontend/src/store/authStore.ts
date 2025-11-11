import { create } from 'zustand';
import * as authApi from '../api/auth';
import * as storage from '../utils/storage';
import type { User } from '../types';

interface AuthState {
  user: User | null;
  isLoading: boolean;
  isEditor: boolean;
  canManageCourses: boolean;
  login: (email: string, password: string) => Promise<User>;
  logout: () => Promise<void>;
  loadUser: () => Promise<void>;
  refreshUser: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set, get) => ({
  user: null,
  isLoading: true,

  get isEditor() {
    const state = get();
    return state.user?.roles.includes('EDITOR') || false;
  },

  get canManageCourses() {
    const state = get();
    return (state.user?.roles.includes('EDITOR') || state.user?.roles.includes('ADMIN')) || false;
  },

  login: async (email: string, password: string) => {
    const { accessToken, refreshToken, user } = await authApi.login(
      email,
      password
    );
    storage.setTokens(accessToken, refreshToken);
    set({ user });
    return user;
  },

  logout: async () => {
    try {
      await authApi.logout();
    } catch {
      // Logout anyway even if API call fails
    }
    storage.clearTokens();
    set({ user: null });
  },

  loadUser: async () => {
    const token = storage.getAccessToken();
    if (token) {
      try {
        const userData = await authApi.getMe();
        set({ user: userData, isLoading: false });
      } catch {
        storage.clearTokens();
        set({ user: null, isLoading: false });
      }
    } else {
      set({ isLoading: false });
    }
  },

  refreshUser: async () => {
    const token = storage.getAccessToken();
    if (token) {
      try {
        const userData = await authApi.getMe();
        set({ user: userData });
      } catch {
        storage.clearTokens();
        set({ user: null });
      }
    }
  },
}));
