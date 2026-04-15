<template>
  <div class="notification-center">
    <!-- 触发按钮（带未读计数） -->
    <button class="notif-btn" @click="togglePanel" :class="{ active: isOpen }">
      🔔
      <span v-if="unreadCount > 0" class="badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
    </button>

    <!-- 面板 -->
    <div v-if="isOpen" class="notif-panel" ref="panelRef">
      <div class="panel-header">
        <h3>{{ t('notif.title') }}</h3>
        <div class="header-actions">
          <button @click="markAllRead" v-if="unreadCount > 0" class="text-btn">
            {{ t('notif.markAllRead') }}
          </button>
          <button @click="clearAll" class="text-btn danger">{{ t('notif.clearAll') }}</button>
        </div>
      </div>

      <!-- 标签切换 -->
      <div class="tab-bar">
        <button :class="{ active: tab === 'all' }" @click="tab = 'all'; fetchNotifs()">
          {{ t('notif.all') }}
        </button>
        <button :class="{ active: tab === 'unread' }" @click="tab = 'unread'; fetchNotifs()">
          {{ t('notif.unread') }}
        </button>
      </div>

      <!-- 通知列表 -->
      <div class="notif-list">
        <div
          v-for="notif in notifications"
          :key="notif.id"
          :class="['notif-item', { unread: !notif.read, [notif.type]: true }]"
          @click="handleNotifClick(notif)"
        >
          <div class="notif-icon">{{ getIcon(notif.type) }}</div>
          <div class="notif-body">
            <div class="notif-title">{{ notif.title }}</div>
            <div class="notif-msg">{{ notif.body }}</div>
            <div class="notif-time">{{ formatTime(notif.createdAt) }}</div>
          </div>
          <button
            v-if="!notif.read"
            @click.stop="markRead(notif.id)"
            class="read-dot"
            :title="t('notif.markRead')"
          >●</button>
        </div>

        <!-- 空状态 -->
        <div v-if="notifications.length === 0" class="empty-state">
          <p>🔔 {{ t('notif.empty') }}</p>
        </div>

        <!-- 分页 -->
        <div class="pagination" v-if="totalPages > 1">
          <button @click="prevPage" :disabled="page === 0">←</button>
          <span>{{ page + 1 }} / {{ totalPages }}</span>
          <button @click="nextPage" :disabled="page >= totalPages - 1">→</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'

const t = (key: string) => key
const isOpen = ref(false)
const tab = ref('all')
const notifications = ref<any[]>([])
const unreadCount = ref(0)
const page = ref(0)
const size = 20
const totalPages = computed(() => 1) // simplified

function togglePanel() {
  isOpen.value = !isOpen.value
  if (isOpen.value) fetchNotifs()
}

async function fetchNotifs() {
  const userId = 'current-user' // replace with real user
  try {
    const endpoint = tab.value === 'unread' ? '/notifications/unread' : '/notifications'
    const { data } = await apiClient.get(`${endpoint}?userId=${userId}&page=${page.value}&size=${size}`)
    notifications.value = data.content || []
    if (tab.value === 'all') {
      const cnt = await apiClient.get(`/notifications/unread/count?userId=${userId}`)
      unreadCount.value = cnt.data
    }
  } catch {}
}

async function markRead(id: string) {
  await apiClient.patch(`/notifications/${id}/read`)
  const n = notifications.value.find(n => n.id === id)
  if (n) n.read = true
  unreadCount.value = Math.max(0, unreadCount.value - 1)
}

async function markAllRead() {
  await apiClient.post('/notifications/read-all?userId=current-user')
  notifications.value.forEach(n => n.read = true)
  unreadCount.value = 0
}

async function clearAll() {
  if (!confirm(t('notif.confirmClear'))) return
  await apiClient.delete('/notifications/all?userId=current-user')
  notifications.value = []
}

function handleNotifClick(notif: any) {
  if (!notif.read) markRead(notif.id)
  if (notif.url) window.location.href = notif.url
}

function getIcon(type: string) {
  return { TASK_COMPLETE: '✅', ALERT: '🚨', SYSTEM: '⚙️', SHARE: '🔗', INFO: 'ℹ️' }[type] || '🔔'
}

function formatTime(iso: string) {
  if (!iso) return ''
  const d = new Date(iso)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  return d.toLocaleDateString()
}

function prevPage() { page.value--; fetchNotifs() }
function nextPage() { page.value++; fetchNotifs() }

onMounted(() => {
  document.addEventListener('click', (e) => {
    const panel = document.querySelector('.notification-center')
    if (panel && !panel.contains(e.target as Node)) {
      isOpen.value = false
    }
  })
})
</script>

<style scoped>
.notification-center { position: relative; }
.notif-btn {
  position: relative; background: none; border: none;
  font-size: 20px; cursor: pointer; padding: 6px;
}
.badge {
  position: absolute; top: -4px; right: -4px;
  background: #dc2626; color: white;
  border-radius: 10px; font-size: 10px; font-weight: 700;
  min-width: 16px; height: 16px; display: flex; align-items: center; justify-content: center;
  padding: 0 4px;
}
.notif-btn.active { background: #eff6ff; border-radius: 8px; }
.notif-panel {
  position: absolute; top: 40px; right: 0;
  width: 380px; background: white;
  border-radius: 12px; box-shadow: 0 8px 30px rgba(0,0,0,0.15);
  z-index: 1000; overflow: hidden;
}
.panel-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14px 16px; border-bottom: 1px solid #e2e8f0;
}
.panel-header h3 { font-size: 15px; font-weight: 600; }
.header-actions { display: flex; gap: 8px; }
.text-btn { background: none; border: none; cursor: pointer; font-size: 12px; color: #2563eb; }
.text-btn.danger { color: #dc2626; }
.tab-bar { display: flex; border-bottom: 1px solid #e2e8f0; }
.tab-bar button {
  flex: 1; padding: 10px; border: none; background: none;
  cursor: pointer; font-size: 13px; color: #64748b;
  border-bottom: 2px solid transparent; margin-bottom: -1px;
}
.tab-bar button.active { color: #2563eb; border-bottom-color: #2563eb; font-weight: 600; }
.notif-list { max-height: 500px; overflow-y: auto; }
.notif-item {
  display: flex; gap: 10px; padding: 12px 16px;
  border-bottom: 1px solid #f1f5f9; cursor: pointer;
  transition: background 0.15s;
}
.notif-item:hover { background: #f8fafc; }
.notif-item.unread { background: #eff6ff; }
.notif-item.unread:hover { background: #dbeafe; }
.notif-icon { font-size: 20px; flex-shrink: 0; padding-top: 2px; }
.notif-body { flex: 1; min-width: 0; }
.notif-title { font-size: 13px; font-weight: 600; color: #1e293b; }
.notif-msg { font-size: 12px; color: #64748b; margin-top: 2px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.notif-time { font-size: 11px; color: #94a3b8; margin-top: 4px; }
.read-dot { background: none; border: none; color: #2563eb; cursor: pointer; font-size: 10px; flex-shrink: 0; padding: 0 4px; }
.empty-state { text-align: center; padding: 40px; color: #94a3b8; font-size: 14px; }
.pagination { display: flex; justify-content: center; align-items: center; gap: 12px; padding: 12px; border-top: 1px solid #f1f5f9; font-size: 12px; }
.pagination button { padding: 4px 12px; border: 1px solid #e2e8f0; border-radius: 4px; background: white; cursor: pointer; }
.pagination button:disabled { opacity: 0.4; }
</style>
