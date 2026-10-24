import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { ApiErrorResponse } from '../types';

/**
 * Local-development API client.
 * Uses Vite proxy (/api) to forward requests to Spring Boot on port 8080.
 *
 * AUTH CREDENTIAL STORAGE STRATEGY (Local Dev):
 * Tokens and active user metadata are stored in localStorage for persistence across browser reloads.
 * This is documented for local demonstration only. In production environments, HttpOnly/SameSite cookies
 * or an OAuth2 authorization server would be preferred to protect against XSS token extraction.
 */

const TOKEN_KEY = 'appointense_jwt_token';
const USER_KEY = 'appointense_user_profile';

export const getStoredToken = (): string | null => {
  return localStorage.getItem(TOKEN_KEY);
};

export const setStoredAuth = (token: string, user: { id: number; fullName: string; email: string; role: 'USER' | 'ADMIN' }) => {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(USER_KEY, JSON.stringify(user));
};

export const clearStoredAuth = () => {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
};

export const getStoredUser = () => {
  const data = localStorage.getItem(USER_KEY);
  if (!data) return null;
  try {
    return JSON.parse(data);
  } catch {
    return null;
  }
};

export const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Interceptor: Attach Bearer token to protected API requests
api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getStoredToken();
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Interceptor: Global 401 handling (token expired / revoked)
api.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiErrorResponse>) => {
    if (error.response?.status === 401) {
      clearStoredAuth();
      // Redirect to login if user was on a protected page
      if (window.location.pathname !== '/login' && window.location.pathname !== '/register') {
        window.location.href = '/login?session_expired=true';
      }
    }
    return Promise.reject(error);
  }
);
