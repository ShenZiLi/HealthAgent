import { reactive, computed } from 'vue';
import { authApi } from '@/utils/api';
import type { UserInfo } from '@/types/auth';

interface AuthState {
  isAuthenticated: boolean;
  user: UserInfo | null;
  accessToken: string | null;
  refreshToken: string | null;
  loading: boolean;
}

const state = reactive<AuthState>({
  isAuthenticated: false,
  user: null,
  accessToken: null,
  refreshToken: null,
  loading: false,
});

export function useAuth() {
  const isAuthenticated = computed(() => state.isAuthenticated);
  const user = computed(() => state.user);
  const loading = computed(() => state.loading);

  function saveTokens(accessToken: string, refreshToken: string) {
    state.accessToken = accessToken;
    state.refreshToken = refreshToken;
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
  }

  function clearTokens() {
    state.accessToken = null;
    state.refreshToken = null;
    state.user = null;
    state.isAuthenticated = false;
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
  }

  async function login(username: string, password: string): Promise<boolean> {
    state.loading = true;
    try {
      const result = await authApi.login({ username, password });
      if (result.code === 200 && result.data) {
        saveTokens(result.data.token, result.data.refreshToken);
        state.user = result.data.user;
        state.isAuthenticated = true;
        return true;
      }
      throw new Error(result.message || '登录失败');
    } finally {
      state.loading = false;
    }
  }

  async function logout(): Promise<void> {
    try {
      if (state.refreshToken) {
        await authApi.logout(state.refreshToken);
      }
    } catch (e) {
      console.error('登出请求失败:', e);
    } finally {
      clearTokens();
    }
  }

  async function refreshAccessToken(): Promise<boolean> {
    if (!state.refreshToken) return false;
    
    try {
      const result = await authApi.refreshToken({ refreshToken: state.refreshToken });
      if (result.code === 200 && result.data) {
        saveTokens(result.data.token, result.data.refreshToken);
        state.user = result.data.user;
        state.isAuthenticated = true;
        return true;
      }
    } catch (e) {
      console.error('刷新 token 失败:', e);
    }
    
    clearTokens();
    return false;
  }

  async function checkAuth(): Promise<boolean> {
    const savedAccessToken = localStorage.getItem('accessToken');
    const savedRefreshToken = localStorage.getItem('refreshToken');
    
    if (!savedAccessToken || !savedRefreshToken) {
      clearTokens();
      return false;
    }

    state.accessToken = savedAccessToken;
    state.refreshToken = savedRefreshToken;

    try {
      const result = await authApi.getCurrentUser();
      if (result.code === 200 && result.data) {
        state.user = result.data;
        state.isAuthenticated = true;
        return true;
      }
    } catch (e) {
      console.error('检查认证状态失败:', e);
    }

    // 如果直接检查失败，尝试刷新 token
    const refreshed = await refreshAccessToken();
    return refreshed;
  }

  // 初始化时检查
  checkAuth();

  return {
    isAuthenticated,
    user,
    loading,
    login,
    logout,
    refreshAccessToken,
    checkAuth,
  };
}
