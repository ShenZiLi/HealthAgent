<template>
  <div class="min-h-screen bg-slate-50">
    <nav class="bg-white shadow-sm border-b border-slate-200">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between h-16">
          <div class="flex items-center">
            <button
              @click="router.push('/dashboard')"
              class="flex items-center space-x-3 text-slate-700 hover:text-slate-900"
            >
              <ArrowLeft class="w-5 h-5" />
              <span class="text-lg font-semibold">返回</span>
            </button>
          </div>
          
          <div class="flex items-center space-x-4">
            <div class="flex items-center space-x-3">
              <div class="w-8 h-8 bg-slate-200 rounded-full flex items-center justify-center">
                <User class="w-5 h-5 text-slate-600" />
              </div>
              <span class="text-slate-700 font-medium">{{ user?.username }}</span>
            </div>
          </div>
        </div>
      </div>
    </nav>

    <main class="max-w-7xl mx-auto py-8 px-4 sm:px-6 lg:px-8">
      <div class="mb-8">
        <h1 class="text-3xl font-bold text-slate-800">保单查询</h1>
        <p class="text-slate-500 mt-2">查询您的保单信息</p>
      </div>

      <div class="bg-white rounded-xl shadow-sm border border-slate-200 p-6 mb-6">
        <div class="flex flex-col sm:flex-row gap-4">
          <div class="flex-1">
            <label class="block text-sm font-medium text-slate-700 mb-1">投保人姓名</label>
            <input
              v-model="queryForm.policyHolderName"
              type="text"
              placeholder="请输入投保人姓名"
              class="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-sky-500 focus:border-sky-500 outline-none transition-colors"
            />
          </div>
          <div class="flex-1">
            <label class="block text-sm font-medium text-slate-700 mb-1">身份证号</label>
            <input
              v-model="queryForm.idCardNo"
              type="text"
              placeholder="请输入身份证号"
              class="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-sky-500 focus:border-sky-500 outline-none transition-colors"
            />
          </div>
          <div class="flex-1">
            <label class="block text-sm font-medium text-slate-700 mb-1">保单状态</label>
            <select
              v-model="queryForm.status"
              class="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-sky-500 focus:border-sky-500 outline-none transition-colors"
            >
              <option value="">全部</option>
              <option value="active">有效</option>
              <option value="expired">已过期</option>
              <option value="cancelled">已取消</option>
            </select>
          </div>
          <div class="flex items-end gap-2">
            <button
              @click="handleQuery"
              :disabled="loading"
              class="px-6 py-2 bg-sky-600 text-white rounded-lg hover:bg-sky-700 disabled:bg-sky-300 transition-colors flex items-center space-x-2"
            >
              <Search v-if="!loading" class="w-4 h-4" />
              <Loader2 v-else class="w-4 h-4 animate-spin" />
              <span>{{ loading ? '查询中...' : '查询' }}</span>
            </button>
            <button
              @click="handleReset"
              class="px-6 py-2 bg-slate-100 text-slate-700 rounded-lg hover:bg-slate-200 transition-colors"
            >
              重置
            </button>
          </div>
        </div>
      </div>

      <div v-if="loading && policies.length === 0" class="bg-white rounded-xl shadow-sm border border-slate-200 p-12 text-center">
        <Loader2 class="w-8 h-8 animate-spin mx-auto text-sky-600" />
        <p class="text-slate-500 mt-4">查询中...</p>
      </div>

      <div v-else-if="policies.length === 0 && hasQueried" class="bg-white rounded-xl shadow-sm border border-slate-200 p-12 text-center">
        <FileText class="w-12 h-12 mx-auto text-slate-300" />
        <p class="text-slate-500 mt-4">未查询到保单信息</p>
      </div>

      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <div
          v-for="policy in policies"
          :key="policy.polNo"
          class="bg-white rounded-xl shadow-sm border border-slate-200 p-6 hover:shadow-md transition-shadow"
        >
          <div class="flex items-center justify-between mb-4">
            <span class="text-sm font-medium text-slate-500">{{ policy.polNo }}</span>
            <span
              :class="[
                'px-2 py-1 text-xs font-medium rounded-full',
                getStatusClass(policy.status)
              ]"
            >
              {{ getStatusText(policy.status) }}
            </span>
          </div>
          
          <h3 class="text-lg font-semibold text-slate-800 mb-3">{{ policy.productName }}</h3>
          
          <div class="space-y-2 text-sm">
            <div class="flex justify-between">
              <span class="text-slate-500">保险公司</span>
              <span class="text-slate-700 font-medium">{{ policy.insuranceCompany }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-slate-500">保险类型</span>
              <span class="text-slate-700">{{ policy.insuranceType }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-slate-500">保费金额</span>
              <span class="text-slate-700">¥{{ policy.premiumAmount?.toLocaleString() }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-slate-500">保额</span>
              <span class="text-sky-600 font-semibold">¥{{ policy.insuredAmount?.toLocaleString() }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-slate-500">投保人</span>
              <span class="text-slate-700">{{ policy.policyHolderName }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-slate-500">被保险人</span>
              <span class="text-slate-700">{{ policy.insuredName }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-slate-500">生效日期</span>
              <span class="text-slate-700">{{ formatDate(policy.effectiveDate) }}</span>
            </div>
            <div v-if="policy.expiryDate" class="flex justify-between">
              <span class="text-slate-500">到期日期</span>
              <span class="text-slate-700">{{ formatDate(policy.expiryDate) }}</span>
            </div>
            <div v-else class="flex justify-between">
              <span class="text-slate-500">保障期限</span>
              <span class="text-slate-700">终身</span>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuth } from '@/composables/useAuth';
import { policyApi } from '@/utils/policyApi';
import type { PolInfoEntity, PolicyQueryRequest } from '@/types/policy';
import { ArrowLeft, User, Search, Loader2, FileText } from 'lucide-vue-next';

const router = useRouter();
const { user } = useAuth();

const loading = ref(false);
const hasQueried = ref(false);
const policies = ref<PolInfoEntity[]>([]);

const queryForm = reactive<PolicyQueryRequest>({
  policyHolderName: '张三',
  idCardNo: '110101199001011234',
  status: '',
});

function getStatusClass(status: string): string {
  switch (status) {
    case 'active':
      return 'bg-green-100 text-green-700';
    case 'expired':
      return 'bg-orange-100 text-orange-700';
    case 'cancelled':
      return 'bg-red-100 text-red-700';
    default:
      return 'bg-slate-100 text-slate-700';
  }
}

function getStatusText(status: string): string {
  switch (status) {
    case 'active':
      return '有效';
    case 'expired':
      return '已过期';
    case 'cancelled':
      return '已取消';
    default:
      return status;
  }
}

function formatDate(dateStr: string): string {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' });
}

async function handleQuery() {
  loading.value = true;
  hasQueried.value = true;
  try {
    const result = await policyApi.queryPolicies(queryForm);
    if (result.code === 200) {
      policies.value = result.data || [];
    } else {
      policies.value = [];
    }
  } catch (e) {
    console.error('查询保单失败:', e);
    policies.value = [];
  } finally {
    loading.value = false;
  }
}

function handleReset() {
  queryForm.policyHolderName = '';
  queryForm.idCardNo = '';
  queryForm.status = '';
  policies.value = [];
  hasQueried.value = false;
}

onMounted(() => {
  handleQuery();
});
</script>
