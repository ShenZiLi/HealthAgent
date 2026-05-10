import type { LoginRequest as LoginRequestType, LoginResponse, RefreshTokenRequest as RefreshTokenRequestType, Result, UserInfo } from '@/types/auth';

const API_BASE = '/api/auth';

async function fetchRequest<T>(url: string, options: RequestInit = {}): Promise<Result<T>> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...options.headers as Record<string, string>,
  };

  const token = localStorage.getItem('accessToken');
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(url, {
    ...options,
    headers,
  });

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }

  const contentType = response.headers.get('content-type');
  if (!contentType || !contentType.includes('application/json')) {
    const text = await response.text();
    throw new Error(`Expected JSON response, but got: ${text || 'Empty response'}`);
  }

  const text = await response.text();
  if (!text) {
    throw new Error('Empty response body');
  }

  try {
    const data = JSON.parse(text);
    return data;
  } catch (e) {
    throw new Error(`Failed to parse JSON: ${text.substring(0, 100)}...`);
  }
}

export const authApi = {
  async login(request: LoginRequestType): Promise<Result<LoginResponse>> {
    return fetchRequest<LoginResponse>(`${API_BASE}/login`, {
      method: 'POST',
      body: JSON.stringify(request),
    });
  },

  async logout(refreshToken?: string): Promise<Result<void>> {
    return fetchRequest<void>(`${API_BASE}/logout`, {
      method: 'POST',
      body: refreshToken ? JSON.stringify({ refreshToken }) : undefined,
    });
  },

  async refreshToken(request: RefreshTokenRequestType): Promise<Result<LoginResponse>> {
    return fetchRequest<LoginResponse>(`${API_BASE}/refresh`, {
      method: 'POST',
      body: JSON.stringify(request),
    });
  },

  async getCurrentUser(): Promise<Result<UserInfo>> {
    return fetchRequest<UserInfo>(`${API_BASE}/current`, {
      method: 'GET',
    });
  },
};
