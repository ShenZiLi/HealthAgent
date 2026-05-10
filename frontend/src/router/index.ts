import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import LoginPage from '@/pages/LoginPage.vue'
import DashboardPage from '@/pages/DashboardPage.vue'
import ChatPage from '@/pages/ChatPage.vue'
import PolicyPage from '@/pages/PolicyPage.vue'
import { useAuth } from '@/composables/useAuth'

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
    path: '/chat',
    name: 'chat',
    component: ChatPage,
    meta: { requiresAuth: true },
  },
  {
    path: '/policy',
    name: 'policy',
    component: PolicyPage,
    meta: { requiresAuth: true },
  },
  {
    path: '/',
    redirect: '/chat',
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
    next('/chat')
  } else {
    next()
  }
})

export default router
