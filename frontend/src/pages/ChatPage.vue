<template>
  <div class="h-screen flex flex-col bg-gray-50">
    <header class="bg-gradient-to-r from-blue-500 to-cyan-500 text-white px-4 py-3 shadow-lg">
      <div class="flex items-center justify-between">
        <div class="flex items-center space-x-3">
          <div class="w-10 h-10 bg-white/20 rounded-full flex items-center justify-center">
            <HeartPulse class="w-6 h-6" />
          </div>
          <h1 class="text-xl font-bold">健康智助手</h1>
        </div>
        <div class="flex items-center space-x-2">
          <button @click="showMenu = !showMenu" class="p-2 hover:bg-white/20 rounded-full transition-colors">
            <MoreVertical class="w-5 h-5" />
          </button>
          <div class="w-8 h-8 bg-white/30 rounded-full flex items-center justify-center text-sm font-semibold">
            {{ userInitial }}
          </div>
        </div>
      </div>
    </header>

    <div v-if="showMenu" class="absolute top-16 right-4 bg-white rounded-lg shadow-xl py-2 z-10">
      <button @click="handleLogout" class="block w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-50">
        <span class="flex items-center space-x-2">
          <LogOut class="w-4 h-4" />
          <span>退出登录</span>
        </span>
      </button>
    </div>

    <div ref="messageContainer" class="flex-1 overflow-y-auto px-4 py-6 space-y-4">
      <div class="flex justify-center mb-4">
        <div class="px-4 py-2 bg-gray-200 rounded-full text-sm text-gray-600">
          {{ currentDate }}
        </div>
      </div>

      <div class="flex flex-col space-y-4">
        <div class="flex justify-start">
          <div class="max-w-[75%]">
            <div class="bg-white rounded-2xl rounded-tl-md px-4 py-3 shadow-sm">
              <p class="text-gray-800 leading-relaxed">{{ welcomeMessage }}</p>
            </div>
          </div>
        </div>

        <div
          v-for="(message, index) in messages"
          :key="index"
          :class="['flex', message.isUser ? 'justify-end' : 'justify-start']"
        >
          <div :class="['max-w-[75%]', message.isUser ? 'space-x-2' : 'space-x-2']">
            <div
              :class="[
                'rounded-2xl px-4 py-3 shadow-sm',
                message.isUser
                  ? 'bg-gradient-to-r from-blue-500 to-cyan-500 text-white rounded-tr-md'
                  : 'bg-white rounded-tl-md text-gray-800'
              ]"
            >
              <p class="leading-relaxed whitespace-pre-wrap">{{ message.content }}</p>
            </div>
            <div :class="['text-xs text-gray-400 mt-1', message.isUser ? 'text-right' : 'text-left']">
              {{ message.time }}
            </div>
          </div>
        </div>

        <div v-if="isLoading" class="flex justify-start">
          <div class="bg-white rounded-2xl rounded-tl-md px-4 py-3 shadow-sm">
            <div class="flex space-x-1">
              <span class="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style="animation-delay: 0ms"></span>
              <span class="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style="animation-delay: 150ms"></span>
              <span class="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style="animation-delay: 300ms"></span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="bg-white border-t border-gray-200 p-4">
      <div class="flex space-x-3 mb-4">
        <button
          @click="handleQuickAction('policy')"
          class="flex-1 flex items-center justify-center space-x-2 px-4 py-3 bg-blue-50 border-2 border-blue-500 text-blue-600 rounded-full hover:bg-blue-100 transition-colors"
        >
          <FileText class="w-5 h-5" />
          <span class="font-medium">保单查询</span>
        </button>
        <button
          @click="handleQuickAction('examination')"
          class="flex-1 flex items-center justify-center space-x-2 px-4 py-3 bg-cyan-50 border-2 border-cyan-500 text-cyan-600 rounded-full hover:bg-cyan-100 transition-colors"
        >
          <Stethoscope class="w-5 h-5" />
          <span class="font-medium">体检预约</span>
        </button>
      </div>

      <div class="flex items-center space-x-3">
        <button class="p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-full transition-colors">
          <Mic class="w-6 h-6" />
        </button>
        <div class="flex-1 relative">
          <input
            v-model="inputMessage"
            @keyup.enter="sendMessage"
            type="text"
            placeholder="请输入您的问题..."
            class="w-full px-4 py-3 bg-gray-100 rounded-full focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition-all"
          />
        </div>
        <button
          @click="sendMessage"
          :disabled="!inputMessage.trim() || isLoading"
          class="p-3 bg-gradient-to-r from-blue-500 to-cyan-500 text-white rounded-full hover:shadow-lg disabled:opacity-50 disabled:cursor-not-allowed transition-all"
        >
          <Send class="w-5 h-5" />
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import { useAuth } from '@/composables/useAuth';
import {
  HeartPulse,
  MoreVertical,
  LogOut,
  FileText,
  Stethoscope,
  Mic,
  Send
} from 'lucide-vue-next';

const router = useRouter();
const { logout, user } = useAuth();

const messageContainer = ref<HTMLElement | null>(null);
const showMenu = ref(false);
const inputMessage = ref('');
const isLoading = ref(false);
const messages = ref<Array<{ content: string; isUser: boolean; time: string }>>([]);

const welcomeMessage = '您好！我是健康智助手，很高兴为您服务。请问有什么可以帮您？';

const currentDate = computed(() => {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  const weekDays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'];
  return `${year}年${month}月${day}日 ${weekDays[now.getDay()]}`;
});

const userInitial = computed(() => {
  if (user.value) {
    return user.value.username.charAt(0).toUpperCase();
  }
  return '?';
});

function getCurrentTime() {
  const now = new Date();
  const hours = String(now.getHours()).padStart(2, '0');
  const minutes = String(now.getMinutes()).padStart(2, '0');
  return `${hours}:${minutes}`;
}

async function sendMessage() {
  if (!inputMessage.value.trim() || isLoading.value) return;

  const message = inputMessage.value.trim();
  inputMessage.value = '';

  messages.value.push({
    content: message,
    isUser: true,
    time: getCurrentTime()
  });

  isLoading.value = true;
  scrollToBottom();

  try {
    const response = await fetch('/api/smart-chat/send', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        message,
        userId: user.value?.username || 'user001'
      })
    });

    const data = await response.json();
    
    if (data.code === 200 && data.data) {
      messages.value.push({
        content: data.data.message,
        isUser: false,
        time: getCurrentTime()
      });
    } else {
      messages.value.push({
        content: '抱歉，我现在无法回答您的问题，请稍后再试。',
        isUser: false,
        time: getCurrentTime()
      });
    }
  } catch (error) {
    console.error('发送消息失败:', error);
    messages.value.push({
      content: '网络异常，请检查网络连接后重试。',
      isUser: false,
      time: getCurrentTime()
    });
  } finally {
    isLoading.value = false;
    scrollToBottom();
  }
}

function handleQuickAction(action: string) {
  let message = '';
  if (action === 'policy') {
    message = '我想查询我的保单';
  } else if (action === 'examination') {
    message = '我想预约体检';
  }
  
  inputMessage.value = message;
  sendMessage();
}

async function handleLogout() {
  showMenu.value = false;
  await logout();
  router.push('/login');
}

function scrollToBottom() {
  nextTick(() => {
    if (messageContainer.value) {
      messageContainer.value.scrollTop = messageContainer.value.scrollHeight;
    }
  });
}

onMounted(() => {
  scrollToBottom();
});
</script>
