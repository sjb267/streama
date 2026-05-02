import { createRouter, createWebHistory } from 'vue-router'
import { useAdminAuthStore } from '@/stores/adminAuth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { guestOnly: true, title: 'Streama 管理后台登录' },
    },
    {
      path: '/',
      component: () => import('@/views/AdminLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          redirect: '/home',
        },
        {
          path: 'home',
          name: 'home',
          meta: { title: '控制台', requiresAuth: true },
          component: () => import('@/views/AdminHomeView.vue'),
        },
        {
          path: 'content/category',
          name: 'category-manage',
          meta: { title: '分类管理', requiresAuth: true },
          component: () => import('@/views/CategoryManageView.vue'),
        },
        {
          path: 'content/videos',
          name: 'video-manage',
          meta: { title: '稿件管理', requiresAuth: true },
          component: () => import('@/views/VideoAuditView.vue'),
        },
        {
          path: 'content/videos/:videoId',
          name: 'video-audit-detail',
          meta: {
            title: '稿件审核',
            requiresAuth: true,
            activeMenu: '/content/videos',
          },
          component: () => import('@/views/VideoAuditDetailView.vue'),
        },
      ],
    },
  ],
})

router.beforeEach((to) => {
  const authStore = useAdminAuthStore()
  authStore.restore()

  if (to.meta?.guestOnly && authStore.isLoggedIn) {
    return { path: '/home' }
  }

  if (to.meta?.requiresAuth && !authStore.isLoggedIn) {
    return {
      path: '/login',
      query: {
        redirect: to.fullPath,
      },
    }
  }

  return true
})

export default router
