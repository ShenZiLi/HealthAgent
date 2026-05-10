<template>
  <div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-sky-500 via-cyan-500 to-teal-500 p-4">
    <div class="w-full max-w-md">
      <div class="bg-white rounded-2xl shadow-2xl p-8 transform transition-all duration-300 hover:scale-[1.02]">
        <div class="text-center mb-8">
          <div class="mx-auto w-16 h-16 bg-gradient-to-br from-sky-500 to-teal-500 rounded-full flex items-center justify-center mb-4">
            <HeartPulse class="w-8 h-8 text-white" />
          </div>
          <h1 class="text-3xl font-bold text-slate-800">健康助手</h1>
          <p class="text-slate-500 mt-2">请登录您的账户</p>
        </div>

        <form @submit.prevent="handleLogin" class="space-y-6">
          <div>
            <label class="block text-sm font-medium text-slate-700 mb-2">用户名</label>
            <div class="relative">
              <User class="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-slate-400" />
              <input
                v-model="username"
                type="text"
                required
                class="w-full pl-10 pr-4 py-3 border border-slate-200 rounded-lg focus:ring-2 focus:ring-sky-500 focus:border-transparent transition-all outline-none"
                placeholder="请输入用户名"
              />
            </div>
          </div>

          <div>
            <label class="block text-sm font-medium text-slate-700 mb-2">密码</label>
            <div class="relative">
              <Lock class="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-slate-400" />
              <input
                v-model="password"
                :type="showPassword ? 'text' : 'password'"
                required
                class="w-full pl-10 pr-12 py-3 border border-slate-200 rounded-lg focus:ring-2 focus:ring-sky-500 focus:border-transparent transition-all outline-none"
                placeholder="请输入密码"
              />
              <button
                type="button"
                @click="showPassword = !showPassword"
                class="absolute right-3 top-1/2 transform -translate-y-1/2 text-slate-400 hover:text-slate-600 transition-colors"
              >
                <Eye v-if="!showPassword" class="w-5 h-5" />
                <EyeOff v-else class="w-5 h-5" />
              </button>
            </div>
          </div>

          <div v-if="errorMessage" class="bg-red-50 border border-red-200 rounded-lg p-3">
            <p class="text-red-600 text-sm flex items-center">
              <AlertCircle class="w-4 h-4 mr-2" />
              {{ errorMessage }}
            </p>
          </div>

          <button
            type="submit"
            :disabled="loading"
            class="w-full py-3 bg-gradient-to-r from-sky-500 to-teal-500 text-white font-semibold rounded-lg hover:from-sky-600 hover:to-teal-600 focus:ring-4 focus:ring-sky-300 transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed shadow-lg hover:shadow-xl"
          >
            <span v-if="loading" class="flex items-center justify-center">
              <svg class="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              登录中...
            </span>
            <span v-else>登 录</span>
          </button>
        </form>

        <div class="mt-6 text-center text-sm text-slate-500">
          <p>默认账户: admin / admin123</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuth } from '@/composables/useAuth';
import { User, Lock, Eye, EyeOff, HeartPulse, AlertCircle } from 'lucide-vue-next';

const router = useRouter();
const { login, loading } = useAuth();

const username = ref('');
const password = ref('');
const showPassword = ref(false);
const errorMessage = ref('');

async function handleLogin() {
  errorMessage.value = '';
  try {
    const success = await login(username.value, password.value);
    if (success) {
      router.push('/dashboard');
    }
  } catch (e) {
    errorMessage.value = e instanceof Error ? e.message : '登录失败，请稍后重试';
  }
}
</script>
