/**
 * Vue Router 配置
 * 
 * 视图统一使用 packages/apps/web/src/views/ 目录（monorepo 正确入口）
 */

import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../../packages/apps/web/src/views/HomeView.vue')
  },
  {
    path: '/chat',
    name: 'Chat',
    component: () => import('../../packages/apps/web/src/views/ChatView.vue')
  },
  {
    path: '/map',
    name: 'Map',
    component: () => import('../../packages/apps/web/src/views/MapView.vue')
  },
  {
    path: '/map/share/:id',
    name: 'SharedMap',
    component: () => import('../views/SharedMapView.vue')
  },
  {
    path: '/data',
    name: 'Data',
    component: () => import('../../packages/apps/web/src/views/DataView.vue')
  },
  {
    path: '/tools',
    name: 'Tools',
    component: () => import('../../packages/apps/web/src/views/ToolsView.vue')
  },
  {
    path: '/plugins',
    name: 'PluginMarket',
    component: () => import('../../packages/apps/web/src/views/PluginMarketView.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
