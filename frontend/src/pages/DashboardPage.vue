<template>
  <div class="min-h-screen bg-slate-50">
    <nav class="bg-white shadow-sm border-b border-slate-200">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between h-16">
          <div class="flex items-center">
            <div class="flex items-center space-x-3">
              <div class="w-10 h-10 bg-gradient-to-br from-sky-500 to-teal-500 rounded-full flex items-center justify-center">
                <HeartPulse class="w-6 h-6 text-white" />
              </div>
              <span class="text-xl font-bold text-slate-800">健康助手</span>
            </div>
          </div>
          
          <div class="flex items-center space-x-4">
            <div class="flex items-center space-x-3">
              <div class="w-8 h-8 bg-slate-200 rounded-full flex items-center justify-center">
                <User class="w-5 h-5 text-slate-600" />
              </div>
              <span class="text-slate-700 font-medium">{{ user?.username }}</span>
            </div>
            <button
              @click="handleLogout"
              class="flex items-center space-x-2 px-4 py-2 text-slate-600 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
            >
              <LogOut class="w-5 h-5" />
              <span>退出登录</span>
            </button>
          </div>
        </div>
      </div>
    </nav>

    <main class="max-w-7xl mx-auto py-8 px-4 sm:px-6 lg:px-8">
      <div class="mb-8">
        <h1 class="text-3xl font-bold text-slate-800">欢迎回来，{{ user?.username }}！</h1>
        <p class="text-slate-500 mt-2">今天是个美好的日子，让我们一起保持健康。</p>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div
          @click="router.push('/policy')"
          class="bg-white rounded-xl shadow-sm border border-slate-200 p-6 hover:shadow-md transition-shadow cursor-pointer"
        >
          <div class="flex items-center justify-between mb-4">
            <div class="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center">
              <Shield class="w-6 h-6 text-blue-600" />
            </div>
          </div>
          <h3 class="text-lg font-semibold text-slate-800">保单查询</h3>
          <p class="text-slate-500 mt-1">查看您的保单信息</p>
        </div>

        <div
          @click="router.push('/examination/packages')"
          class="bg-white rounded-xl shadow-sm border border-slate-200 p-6 hover:shadow-md transition-shadow cursor-pointer"
        >
          <div class="flex items-center justify-between mb-4">
            <div class="w-12 h-12 bg-teal-100 rounded-lg flex items-center justify-center">
              <Stethoscope class="w-6 h-6 text-teal-600" />
            </div>
          </div>
          <h3 class="text-lg font-semibold text-slate-800">体检预约</h3>
          <p class="text-slate-500 mt-1">查看套餐、预约体检</p>
        </div>

        <div
          @click="router.push('/chat')"
          class="bg-white rounded-xl shadow-sm border border-slate-200 p-6 hover:shadow-md transition-shadow cursor-pointer"
        >
          <div class="flex items-center justify-between mb-4">
            <div class="w-12 h-12 bg-purple-100 rounded-lg flex items-center justify-center">
              <Bot class="w-6 h-6 text-purple-600" />
            </div>
          </div>
          <h3 class="text-lg font-semibold text-slate-800">AI助手</h3>
          <p class="text-slate-500 mt-1">智能问答与健康服务</p>
        </div>
      </div>

      <div class="mt-8 bg-white rounded-xl shadow-sm border border-slate-200 p-6">
        <h2 class="text-xl font-bold text-slate-800 mb-4">今日健康提示</h2>
        <div class="bg-gradient-to-r from-sky-50 to-teal-50 rounded-lg p-4">
          <p class="text-slate-700">
            💡 保持充足的睡眠，每天至少 7-8 小时，这对您的身心健康至关重要。
          </p>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import { useAuth } from '@/composables/useAuth';
import { User, LogOut, HeartPulse, Shield, Stethoscope, Bot } from 'lucide-vue-next';

const router = useRouter();
const { user, logout } = useAuth();

async function handleLogout() {
  await logout();
  router.push('/login');
}
</script>
