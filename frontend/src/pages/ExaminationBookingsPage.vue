<template>
  <div class="min-h-screen bg-gradient-to-b from-blue-50 to-slate-50">
    <nav class="bg-blue-600 shadow-sm">
      <div class="max-w-3xl mx-auto px-4">
        <div class="flex justify-between h-14 items-center">
          <button @click="router.back()" class="flex items-center text-white hover:text-blue-100">
            <ArrowLeft class="w-5 h-5 mr-1" />
            <span>返回</span>
          </button>
          <h1 class="text-white text-lg font-semibold">预约记录</h1>
          <div class="w-10"></div>
        </div>
      </div>
    </nav>

    <main class="max-w-3xl mx-auto py-4 px-4 pb-8">
      <div v-if="loading" class="flex justify-center py-12">
        <Loader2 class="w-8 h-8 animate-spin text-blue-600" />
      </div>

      <div v-else-if="bookings.length === 0" class="text-center py-16">
        <div class="w-20 h-20 bg-slate-100 rounded-full flex items-center justify-center mx-auto mb-4">
          <ClipboardList class="w-10 h-10 text-slate-400" />
        </div>
        <p class="text-slate-500">暂无预约记录</p>
        <button
          @click="router.push('/examination/packages')"
          class="mt-4 px-6 py-2 bg-blue-600 text-white rounded-full text-sm hover:bg-blue-700 transition-colors"
        >
          去预约
        </button>
      </div>

      <div v-else class="space-y-4">
        <div
          v-for="b in bookings"
          :key="b.bookingNo"
          class="bg-white rounded-2xl shadow-sm overflow-hidden"
        >
          <!-- 头部 -->
          <div class="px-5 py-3 flex justify-between items-center border-b border-slate-50">
            <h3 class="font-semibold text-slate-800">{{ b.packageName }}</h3>
            <span
              :class="[
                'px-3 py-1 text-xs rounded-full font-medium',
                getStatusClass(b.status)
              ]"
            >
              {{ getStatusText(b.status) }}
            </span>
          </div>

          <!-- 内容 -->
          <div class="px-5 py-4 space-y-3">
            <div class="flex justify-between text-sm">
              <span class="text-slate-500">预约医院</span>
              <span class="text-slate-700 font-medium">{{ b.hospitalName }}</span>
            </div>
            <div class="flex justify-between text-sm">
              <span class="text-slate-500">预约日期</span>
              <span class="text-slate-700">{{ b.scheduleDate }} {{ b.scheduleTime }}</span>
            </div>
            <div class="flex justify-between text-sm">
              <span class="text-slate-500">预约人</span>
              <span class="text-slate-700">{{ b.bookerName }}</span>
            </div>
            <div class="flex justify-between text-sm">
              <span class="text-slate-500">联系电话</span>
              <span class="text-slate-700">{{ b.bookerPhone }}</span>
            </div>
            <div class="flex justify-between text-sm">
              <span class="text-slate-500">预约编号</span>
              <span class="text-slate-600 text-xs font-mono">{{ b.bookingNo }}</span>
            </div>
          </div>

          <!-- 底部操作 -->
          <div v-if="b.status === 'confirmed'" class="px-5 pb-4 pt-2">
            <button
              @click="handleCancel(b.bookingNo)"
              class="w-full py-2.5 border-2 border-red-400 text-red-500 rounded-xl text-sm font-medium hover:bg-red-50 transition-colors"
            >
              取消预约
            </button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuth } from '@/composables/useAuth';
import { examinationApi } from '@/utils/examinationApi';
import type { ExaminationBooking } from '@/types/examination';
import { ArrowLeft, Loader2, ClipboardList } from 'lucide-vue-next';

const router = useRouter();
const { user } = useAuth();

const loading = ref(false);
const bookings = ref<ExaminationBooking[]>([]);

function getStatusClass(status: string): string {
  switch (status) {
    case 'confirmed':
      return 'bg-blue-100 text-blue-600';
    case 'cancelled':
      return 'bg-red-100 text-red-600';
    case 'completed':
      return 'bg-green-100 text-green-600';
    default:
      return 'bg-slate-100 text-slate-600';
  }
}

function getStatusText(status: string): string {
  switch (status) {
    case 'confirmed':
      return '待确认';
    case 'cancelled':
      return '已取消';
    case 'completed':
      return '已完成';
    default:
      return status;
  }
}

async function loadBookings() {
  loading.value = true;
  try {
    const result = await examinationApi.getUserBookings(user.value?.username || '');
    if (result.code === 200) {
      bookings.value = result.data || [];
    }
  } catch (e) {
    console.error('加载预约记录失败:', e);
  } finally {
    loading.value = false;
  }
}

async function handleCancel(bookingNo: string) {
  if (!confirm('确定要取消该预约吗？')) return;
  try {
    const result = await examinationApi.cancelBooking(bookingNo, user.value?.username || '');
    if (result.code === 200) {
      await loadBookings();
    }
  } catch (e: any) {
    alert(e.message || '取消失败');
  }
}

onMounted(() => {
  loadBookings();
});
</script>
