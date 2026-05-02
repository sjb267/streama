import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('../views/HomeView.vue'),
    },
    {
      path: '/creator',
      name: 'creator',
      component: () => import('../views/CreatorCenterView.vue'),
    },
    {
      path: '/user-center',
      name: 'user-center',
      component: () => import('../views/UserCenterView.vue'),
    },
    {
      path: '/video/:videoId',
      name: 'video-play',
      component: () => import('../views/VideoPlayView.vue'),
    },
  ],
})

export default router
