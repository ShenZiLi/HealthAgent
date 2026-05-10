<template>
  <div class="min-h-screen bg-slate-50">
    <nav class="bg-blue-600 shadow-sm">
      <div class="max-w-3xl mx-auto px-4">
        <div class="flex justify-between h-14 items-center">
          <button @click="router.back()" class="flex items-center text-white hover:text-blue-100">
            <ArrowLeft class="w-5 h-5 mr-1" />
            <span>返回</span>
          </button>
          <h1 class="text-white text-lg font-semibold">套餐详情</h1>
          <div class="w-10"></div>
        </div>
      </div>
    </nav>

    <main class="max-w-3xl mx-auto py-4 px-4 pb-32">
      <div v-if="loading" class="flex justify-center py-12">
        <Loader2 class="w-8 h-8 animate-spin text-blue-600" />
      </div>

      <template v-else>
        <!-- 套餐信息 -->
        <div class="bg-white rounded-2xl shadow-sm p-5 mb-4">
          <h2 class="text-xl font-bold text-slate-800 mb-2">{{ currentPackage?.packageName }}</h2>
          <p class="text-sm text-slate-500">{{ currentPackage?.packageDesc }}</p>
          <div class="mt-4 pt-4 border-t border-slate-100">
            <div class="flex justify-between items-center">
              <span class="text-slate-500 text-sm">套餐价格</span>
              <span class="text-2xl font-bold text-orange-500">¥{{ currentPackage?.price }}</span>
            </div>
            <div class="flex justify-between items-center mt-3">
              <span class="text-slate-500 text-sm">预计时长</span>
              <span class="text-slate-700">约{{ currentPackage?.duration }}分钟</span>
            </div>
          </div>
        </div>

        <!-- 检查项目 -->
        <div class="bg-white rounded-2xl shadow-sm p-5 mb-4">
          <h3 class="font-semibold text-slate-700 mb-3">检查项目</h3>
          <p class="text-sm text-slate-500 leading-relaxed">{{ getPackageItems(currentPackage) }}</p>
        </div>

        <!-- 预约信息 -->
        <div class="bg-white rounded-2xl shadow-sm p-5">
          <h3 class="font-semibold text-slate-700 mb-4">预约信息</h3>

          <!-- 选择医院 -->
          <div class="mb-4">
            <label class="block text-sm text-slate-600 mb-1">选择医院</label>
            <select
              v-model="selectedHospitalCode"
              class="w-full px-4 py-2.5 border border-slate-200 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
            >
              <option value="">请选择医院</option>
              <option v-for="h in hospitals" :key="h.hospitalCode" :value="h.hospitalCode">
                {{ h.hospitalName }} - {{ h.department }}
              </option>
            </select>
          </div>

          <!-- 选择日期 -->
          <div class="mb-4">
            <label class="block text-sm text-slate-600 mb-1">预约日期</label>
            <input
              type="date"
              v-model="selectedDate"
              :min="minDate"
              :max="maxDate"
              :disabled="!selectedHospitalCode"
              class="w-full px-4 py-2.5 border border-slate-200 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none disabled:bg-slate-50 disabled:text-slate-400"
            />
          </div>

          <!-- 预约人信息 -->
          <div class="mb-4">
            <label class="block text-sm text-slate-600 mb-1">预约人姓名</label>
            <input
              v-model="bookerName"
              type="text"
              placeholder="请输入姓名"
              class="w-full px-4 py-2.5 border border-slate-200 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 outline-none"
            />
          </div>

          <div class="mb-4">
            <label class="block text-sm text-slate-600 mb-1">联系电话</label>
            <input
              v-model="bookerPhone"
              type="tel"
              placeholder="请输入手机号"
              class="w-full px-4 py-2.5 border border-slate-200 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 outline-none"
            />
          </div>

          <div class="mb-4">
            <label class="block text-sm text-slate-600 mb-1">身份证号</label>
            <input
              v-model="idCardNo"
              type="text"
              placeholder="请输入身份证号"
              class="w-full px-4 py-2.5 border border-slate-200 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 outline-none"
            />
          </div>

          <button
            @click="handleBook"
            :disabled="!canBook || booking"
            class="w-full py-3 rounded-lg text-sm font-medium transition-colors"
            :class="canBook && !booking
              ? 'bg-blue-600 text-white hover:bg-blue-700'
              : 'bg-slate-200 text-slate-400 cursor-not-allowed'"
          >
            <span v-if="booking">预约中...</span>
            <span v-else>确认预约</span>
          </button>
        </div>
      </template>
    </main>

    <!-- 预约成功弹窗 -->
    <div v-if="bookingResult" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-2xl p-6 max-w-sm w-full text-center">
        <div class="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
          <CheckCircle class="w-8 h-8 text-green-600" />
        </div>
        <h3 class="text-lg font-bold text-slate-800 mb-2">预约成功</h3>
        <p class="text-sm text-slate-500 mb-4">预约编号：{{ bookingResult.bookingNo }}</p>
        <button
          @click="goToBookings"
          class="w-full py-2.5 bg-blue-600 text-white rounded-lg text-sm font-medium hover:bg-blue-700 transition-colors"
        >
          查看预约记录
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useAuth } from '@/composables/useAuth';
import { examinationApi } from '@/utils/examinationApi';
import type { ExaminationPackage, ExaminationHospital, ExaminationBooking } from '@/types/examination';
import { ArrowLeft, Loader2, CheckCircle } from 'lucide-vue-next';

const router = useRouter();
const route = useRoute();
const { user } = useAuth();

const loading = ref(false);
const booking = ref(false);
const bookingResult = ref<ExaminationBooking | null>(null);

const currentPackage = ref<ExaminationPackage | null>(null);
const hospitals = ref<ExaminationHospital[]>([]);

const selectedHospitalCode = ref('');
const selectedDate = ref('');
const bookerName = ref(user.value?.realName || '');
const bookerPhone = ref('');
const idCardNo = ref(user.value?.idCardNo || '');

const minDate = computed(() => {
  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  return tomorrow.toISOString().split('T')[0];
});

const maxDate = computed(() => {
  const threeMonths = new Date();
  threeMonths.setMonth(threeMonths.getMonth() + 3);
  return threeMonths.toISOString().split('T')[0];
});

const canBook = computed(() => {
  return selectedHospitalCode.value &&
    selectedDate.value &&
    bookerName.value &&
    bookerPhone.value;
});

const packageItemsMap: Record<string, string> = {
  '基础体检套餐': '内科、外科、血常规、尿常规、肝功能、肾功能、胸片、心电图',
  '全身体检套餐': '基础体检项目+彩超、CT、肿瘤标志物、甲状腺功能、血糖血脂全套',
  '入职体检套餐': '身高、体重、视力、听力、血常规、肝功能、胸片、心电图',
  '老年体检套餐': '全身体检项目+骨密度、颈动脉彩超、眼底检查、前列腺/乳腺彩超',
  '女性专项体检套餐': '基础体检+妇科检查、乳腺彩超、宫颈TCT、HPV检查',
};

function getPackageItems(pkg: ExaminationPackage | null): string {
  if (!pkg) return '';
  return packageItemsMap[pkg.packageName] || pkg.packageDesc || '-';
}

async function loadHospitals() {
  const result = await examinationApi.getHospitals();
  if (result.code === 200) {
    hospitals.value = result.data || [];
  }
}

async function handleBook() {
  if (!canBook.value) return;
  booking.value = true;
  try {
    const hospital = hospitals.value.find(h => h.hospitalCode === selectedHospitalCode.value);
    const result = await examinationApi.bookExamination({
      userId: user.value?.username || '',
      bookerName: bookerName.value,
      bookerPhone: bookerPhone.value,
      idCardNo: idCardNo.value,
      notes: '',
      hospitalId: hospital?.id,
      packageId: currentPackage.value?.id,
      scheduleDate: selectedDate.value,
    });
    if (result.code === 200 && result.data) {
      bookingResult.value = result.data;
    }
  } catch (e: any) {
    alert(e.message || '预约失败');
  } finally {
    booking.value = false;
  }
}

function goToBookings() {
  router.push('/examination/bookings');
}

onMounted(async () => {
  const pkgId = Number(route.query.id);
  if (!pkgId) {
    router.back();
    return;
  }

  loading.value = true;
  try {
    const pkgResult = await examinationApi.getPackages();
    if (pkgResult.code === 200) {
      currentPackage.value = pkgResult.data?.find(p => p.id === pkgId) || null;
    }
    await loadHospitals();
  } finally {
    loading.value = false;
  }
});
</script>
