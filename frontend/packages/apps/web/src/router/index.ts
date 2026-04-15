import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('@/views/HomeView.vue')
  },
  {
    path: '/map',
    name: 'Map',
    component: () => import('@/views/MapView.vue')
  },
  {
    path: '/chat',
    name: 'Chat',
    component: () => import('@/views/ChatView.vue')
  },
  {
    path: '/data',
    name: 'Data',
    component: () => import('@/views/DataView.vue')
  },
  {
    path: '/tools',
    name: 'Tools',
    component: () => import('@/views/ToolsView.vue')
  },
  {
    path: '/templates',
    name: 'TemplateMarket',
    component: () => import('@/views/TemplateMarketView.vue')
  },
  {
    path: '/stories/new',
    name: 'StoryEditor',
    component: () => import('@/views/StoryEditorView.vue')
  },
  {
    path: '/stories/edit/:id',
    name: 'StoryEditorEdit',
    component: () => import('@/views/StoryEditorView.vue')
  },
  {
    path: '/stories/shared/:token',
    name: 'StoryReader',
    component: () => import('@/views/StoryReaderView.vue')
  },
  {
    path: '/collab/:docId',
    name: 'Collaboration',
    component: () => import('@geonexus/components').then(m => m.CollaborationPanel)
  },
  {
    path: '/admin',
    name: 'AdminDashboard',
    component: () => import('@/views/AdminDashboardView.vue'),
    meta: { requiresAdmin: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
