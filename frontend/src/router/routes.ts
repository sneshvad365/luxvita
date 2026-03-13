import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path:      '/',
    component: () => import('layouts/MainLayout.vue'),
    meta:      { requiresAuth: true },
    children: [
      { path: '',        redirect: '/today' },
      { path: 'today',   component: () => import('pages/TodayPage.vue') },
      { path: 'log',     component: () => import('pages/LogPage.vue') },
      { path: 'trends',  component: () => import('pages/TrendsPage.vue') },
      { path: 'profile', component: () => import('pages/ProfilePage.vue') },
    ],
  },
  { path: '/login',    component: () => import('pages/LoginPage.vue') },
  { path: '/register', component: () => import('pages/RegisterPage.vue') },
  { path: '/:catchAll(.*)*', redirect: '/today' },
]

export default routes
