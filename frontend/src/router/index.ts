import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import LoginPage from '@/pages/LoginPage.vue'
import DashboardPage from '@/pages/DashboardPage.vue'
import { useAuth } from '@/composables/useAuth'

// 定义路由配置
const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: LoginPage,
    meta: { requiresAuth: false },
  },
  {
    path: '/dashboard',
    name: 'dashboard',
    component: DashboardPage,
    meta: { requiresAuth: true },
  },
  {
    path: '/',
    redirect: '/dashboard',
  },
]

// 创建路由实例
const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(async (to, _from, next) => {
  const { isAuthenticated, checkAuth } = useAuth()
  
  if (!isAuthenticated.value) {
    await checkAuth()
  }

  if (to.meta.requiresAuth && !isAuthenticated.value) {
    next('/login')
  } else if (to.path === '/login' && isAuthenticated.value) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router
