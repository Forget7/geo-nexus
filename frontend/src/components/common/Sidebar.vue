<script setup lang="ts">
defineProps<{
  activeTab: 'chat' | 'map' | 'data'
}>()

const emit = defineEmits<{
  (e: 'switch', tab: 'chat' | 'map' | 'data'): void
}>()

const menuItems = [
  { id: 'chat' as const, icon: '💬', label: '对话' },
  { id: 'map' as const, icon: '🗺️', label: '地图' },
  { id: 'data' as const, icon: '📁', label: '数据' }
]
</script>

<template>
  <aside class="sidebar">
    <div class="logo">
      <span class="logo-icon">🌐</span>
      <span class="logo-text">GeoNexus</span>
    </div>
    
    <nav class="nav-menu">
      <button
        v-for="item in menuItems"
        :key="item.id"
        :class="['nav-item', { active: activeTab === item.id }]"
        @click="emit('switch', item.id)"
      >
        <span class="nav-icon">{{ item.icon }}</span>
        <span class="nav-label">{{ item.label }}</span>
      </button>
    </nav>
    
    <div class="sidebar-footer">
      <div class="status">
        <span class="status-dot"></span>
        <span class="status-text">API 连接正常</span>
      </div>
    </div>
  </aside>
</template>

<style scoped>
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
  font-size: 0.75rem;
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
  background: transparent;
  border: none;
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
  font-size: 0.625rem;
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
  font-size: 0.625rem;
  color: #64748b;
}

.status-dot {
  width: 8px;
  height: 8px;
  background: #10b981;
  border-radius: 50%;
  margin-bottom: 0.25rem;
}
</style>
