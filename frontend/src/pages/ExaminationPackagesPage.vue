<template>
  <div class="min-h-screen bg-gradient-to-b from-blue-50 to-slate-50">
    <nav class="bg-blue-600 shadow-sm">
      <div class="max-w-3xl mx-auto px-4">
        <div class="flex justify-between h-14 items-center">
          <button @click="router.push('/dashboard')" class="flex items-center text-white hover:text-blue-100">
            <ArrowLeft class="w-5 h-5 mr-1" />
            <span>返回</span>
          </button>
          <h1 class="text-white text-lg font-semibold">体检预约</h1>
          <button @click="router.push('/examination/bookings')" class="text-blue-100 hover:text-white">
            <Clock class="w-5 h-5" />
          </button>
        </div>
      </div>
    </nav>

    <main class="max-w-3xl mx-auto py-6 px-4 pb-24">
      <div v-if="loading" class="flex justify-center py-12">
        <Loader2 class="w-8 h-8 animate-spin text-blue-600" />
      </div>

      <div v-else-if="packages.length === 0" class="text-center py-12 text-slate-500">
        暂无体检套餐
      </div>

      <div v-else class="space-y-4">
        <div
          v-for="pkg in packages"
          :key="pkg.id"
          @click="goToDetail(pkg)"
          class="bg-white rounded-2xl shadow-sm p-5 cursor-pointer hover:shadow-md transition-shadow"
        >
          <div class="flex justify-between items-start">
            <div class="flex-1">
              <h3 class="text-lg font-semibold text-slate-800">{{ pkg.packageName }}</h3>
              <p class="text-sm text-slate-500 mt-1">{{ pkg.packageDesc }}</p>
              <div class="flex gap-2 mt-3">
                <span class="inline-block px-3 py-1 bg-blue-50 text-blue-600 text-xs rounded-full">
                  {{ pkg.duration ? `约${pkg.duration}分钟` : '-' }}
                </span>
              </div>
            </div>
            <div class="text-right ml-4">
              <div class="text-xl font-bold text-orange-500">
                ¥{{ pkg.price }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- 底部预约记录按钮 -->
    <div class="fixed bottom-6 left-0 right-0 flex justify-center z-10">
      <button
        @click="router.push('/examination/bookings')"
        class="bg-blue-600 text-white px-8 py-3 rounded-full shadow-lg hover:bg-blue-700 transition-colors flex items-center space-x-2"
      >
        <Clock class="w-5 h-5" />
        <span>预约记录</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { examinationApi } from '@/utils/examinationApi';
import type { ExaminationPackage } from '@/types/examination';
import { ArrowLeft, Loader2, Clock } from 'lucide-vue-next';

const router = useRouter();
const loading = ref(false);
const packages = ref<ExaminationPackage[]>([]);

async function loadPackages() {
  loading.value = true;
  try {
    const result = await examinationApi.getPackages();
    if (result.code === 200) {
      packages.value = result.data || [];
    }
  } catch (e) {
    console.error('加载套餐失败:', e);
  } finally {
    loading.value = false;
  }
}

function goToDetail(pkg: ExaminationPackage) {
  router.push({ path: '/examination/detail', query: { id: pkg.id } });
}

onMounted(() => {
  loadPackages();
});
</script>
