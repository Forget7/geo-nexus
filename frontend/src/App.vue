<script setup lang="ts">
import { RouterView, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ref } from 'vue'
import LanguageSwitcher from '../packages/components/src/common/LanguageSwitcher.vue'
import AppInstaller from '../packages/components/src/common/AppInstaller.vue'

const route = useRoute()
const { t } = useI18n()

const isOffline = ref(!navigator.onLine)
window.addEventListener('online', () => isOffline.value = false)
window.addEventListener('offline', () => isOffline.value = true)

const navItems = [
  { path: '/', icon: '🏠', labelKey: 'nav.home' },
  { path: '/chat', icon: '💬', labelKey: 'nav.chat' },
  { path: '/map', icon: '🗺️', labelKey: 'nav.map' },
  { path: '/data', icon: '📁', labelKey: 'nav.data' },
  { path: '/tools', icon: '🛠️', labelKey: 'nav.tools' }
]
</script>

<template>
  <div class="app-container">
    <!-- 离线横幅 -->
    <Transition name="offline-banner">
      <div v-if="isOffline" class="offline-banner">
        📡 您当前处于离线状态，部分功能可能受限
      </div>
    </Transition>

    <!-- 侧边栏 -->
    <aside class="sidebar">
      <div class="logo">
        <span class="logo-icon">🌐</span>
        <span class="logo-text">GeoNexus</span>
      </div>
      
      <nav class="nav-menu">
        <router-link 
          v-for="item in navItems" 
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: route.path === item.path }"
        >
          <span class="nav-icon">{{ item.icon }}</span>
          <span class="nav-label">{{ t(item.labelKey) }}</span>
        </router-link>
      </nav>
      
      <div class="sidebar-footer">
        <LanguageSwitcher />
        <div class="status">
          <span class="status-dot"></span>
          <span class="status-text">系统正常</span>
        </div>
      </div>
    </aside>
    
    <!-- 主内容 -->
    <main class="main-content">
      <RouterView />
    </main>

    <!-- 应用安装器 -->
    <AppInstaller />
  </div>
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
  color: #1e293b;
  background: #f8fafc;
}

a {
  text-decoration: none;
  color: inherit;
}
</style>

<style scoped>
.app-container {
  display: flex;
  height: 100vh;
}

.sidebar {
  width: 80px;
  background: #1e293b;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 1rem 0;
}

.logo {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 2rem;
}

.logo-icon {
  font-size: 1.75rem;
}

.logo-text {
  font-size: 0.65rem;
  font-weight: 600;
  color: #94a3b8;
  margin-top: 0.25rem;
}

.nav-menu {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  flex: 1;
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0.75rem 0.5rem;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  width: 64px;
}

.nav-item:hover {
  background: #334155;
}

.nav-item.active {
  background: #2563eb;
}

.nav-icon {
  font-size: 1.5rem;
}

.nav-label {
  font-size: 0.6rem;
  color: #94a3b8;
  margin-top: 0.25rem;
}

.nav-item.active .nav-label {
  color: white;
}

.sidebar-footer {
  margin-top: auto;
}

.status {
  display: flex;
  flex-direction: column;
  align-items: center;
  font-size: 0.6rem;
  color: #64748b;
}

.status-dot {
  width: 8px;
  height: 8px;
  background: #10b981;
  border-radius: 50%;
  margin-bottom: 0.25rem;
}

.main-content {
  flex: 1;
  overflow: auto;
}

/* 离线横幅 */
.offline-banner {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  background: #f59e0b;
  color: white;
  text-align: center;
  padding: 8px;
  z-index: 9999;
  font-size: 14px;
}

.offline-banner-enter-active,
.offline-banner-leave-active {
  transition: transform 0.3s;
}

.offline-banner-enter-from,
.offline-banner-leave-to {
  transform: translateY(-100%);
}
</style>
