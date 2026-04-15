<template>
  <div class="admin-dashboard">
    <div class="admin-header">
      <h1>⚙️ {{ t('admin.title') }}</h1>
      <div class="admin-tabs">
        <button
          v-for="tab in tabs" :key="tab.id"
          :class="{ active: activeTab === tab.id }"
          @click="activeTab = tab.id"
        >{{ tab.icon }} {{ tab.label }}</button>
      </div>
    </div>

    <!-- 系统统计 -->
    <div v-if="activeTab === 'stats'" class="tab-content">
      <div class="stats-grid">
        <div class="stat-card blue">
          <div class="stat-icon">👥</div>
          <div class="stat-info">
            <span class="stat-num">{{ stats.totalUsers }}</span>
            <span class="stat-label">{{ t('admin.totalUsers') }}</span>
          </div>
        </div>
        <div class="stat-card green">
          <div class="stat-icon">🗂️</div>
          <div class="stat-info">
            <span class="stat-num">{{ stats.totalDatasets }}</span>
            <span class="stat-label">{{ t('admin.totalDatasets') }}</span>
          </div>
        </div>
        <div class="stat-card purple">
          <div class="stat-icon">💬</div>
          <div class="stat-info">
            <span class="stat-num">{{ stats.totalChatSessions }}</span>
            <span class="stat-label">{{ t('admin.totalSessions') }}</span>
          </div>
        </div>
        <div class="stat-card orange">
          <div class="stat-icon">💾</div>
          <div class="stat-info">
            <span class="stat-num">{{ stats.storageMB }}</span>
            <span class="stat-label">{{ t('admin.storageMB') }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 用户管理 -->
    <div v-if="activeTab === 'users'" class="tab-content">
      <table class="admin-table">
        <thead>
          <tr>
            <th>{{ t('admin.username') }}</th>
            <th>{{ t('admin.email') }}</th>
            <th>{{ t('admin.role') }}</th>
            <th>{{ t('admin.createdAt') }}</th>
            <th>{{ t('admin.actions') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in users" :key="user.id">
            <td>{{ user.username }}</td>
            <td>{{ user.email }}</td>
            <td>
              <select :value="user.role" @change="setRole(user.id, $event.target.value)" class="role-select">
                <option value="USER">USER</option>
                <option value="ADMIN">ADMIN</option>
                <option value="EDITOR">EDITOR</option>
              </select>
            </td>
            <td>{{ user.createdAt }}</td>
            <td>
              <button @click="disableUser(user.id)" class="danger-btn">
                {{ t('admin.disable') }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 数据集 -->
    <div v-if="activeTab === 'datasets'" class="tab-content">
      <table class="admin-table">
        <thead>
          <tr>
            <th>{{ t('admin.datasetName') }}</th>
            <th>{{ t('admin.type') }}</th>
            <th>{{ t('admin.size') }}</th>
            <th>{{ t('admin.crs') }}</th>
            <th>{{ t('admin.createdAt') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="ds in datasets" :key="ds.id">
            <td>{{ ds.name }}</td>
            <td><span class="type-badge">{{ ds.type }}</span></td>
            <td>{{ ds.sizeMB }} MB</td>
            <td>{{ ds.crs || '-' }}</td>
            <td>{{ ds.createdAt }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 审计日志 -->
    <div v-if="activeTab === 'logs'" class="tab-content">
      <div class="log-filters">
        <select v-model="logLevel" @change="fetchLogs" class="filter-select">
          <option value="">All Levels</option>
          <option value="CREATE">CREATE</option>
          <option value="UPDATE">UPDATE</option>
          <option value="DELETE">DELETE</option>
          <option value="READ">READ</option>
          <option value="LOGIN">LOGIN</option>
        </select>
      </div>
      <div class="log-list">
        <div v-for="log in logs" :key="log.id" :class="['log-entry', log.level?.toLowerCase()]">
          <span class="log-level">{{ log.level || '-' }}</span>
          <span class="log-time">{{ log.timestamp || '-' }}</span>
          <span class="log-user">{{ log.userId || '-' }}</span>
          <span class="log-action">{{ log.action || '-' }}</span>
          <span class="log-msg">{{ log.message || '-' }}</span>
        </div>
        <div v-if="logs.length === 0" class="empty-state">
          No audit logs found
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const activeTab = ref('stats')
const tabs = [
  { id: 'stats', label: '系统统计', icon: '📊' },
  { id: 'users', label: '用户管理', icon: '👥' },
  { id: 'datasets', label: '数据集', icon: '🗂️' },
  { id: 'logs', label: '审计日志', icon: '📋' }
]

const stats = reactive({ totalUsers: 0, totalDatasets: 0, totalChatSessions: 0, storageMB: 0 })
const users = ref<any[]>([])
const datasets = ref<any[]>([])
const logs = ref<any[]>([])
const logLevel = ref('')

// Simple API client - adjust based on your actual API setup
const apiClient = {
  async get(url: string) {
    const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api/v1'
    const response = await fetch(`${baseUrl}${url}`, {
      headers: { 'Content-Type': 'application/json' }
    })
    const result = await response.json()
    return { data: result.data }
  },
  async patch(url: string) {
    const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api/v1'
    await fetch(`${baseUrl}${url}`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' }
    })
  },
  async post(url: string) {
    const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api/v1'
    await fetch(`${baseUrl}${url}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' }
    })
  }
}

onMounted(() => {
  fetchStats()
  fetchUsers()
  fetchDatasets()
  fetchLogs()
})

async function fetchStats() {
  try {
    const { data } = await apiClient.get('/admin/stats')
    Object.assign(stats, data)
  } catch (e) {
    console.error('Failed to fetch stats:', e)
  }
}

async function fetchUsers() {
  try {
    const { data } = await apiClient.get('/admin/users?page=0&size=100')
    users.value = data || []
  } catch (e) {
    console.error('Failed to fetch users:', e)
  }
}

async function fetchDatasets() {
  try {
    const { data } = await apiClient.get('/admin/datasets')
    datasets.value = data || []
  } catch (e) {
    console.error('Failed to fetch datasets:', e)
  }
}

async function fetchLogs() {
  try {
    const params = new URLSearchParams({ page: '0', size: '100' })
    if (logLevel.value) params.set('level', logLevel.value)
    const { data } = await apiClient.get('/admin/audit-logs?' + params)
    logs.value = data || []
  } catch (e) {
    console.error('Failed to fetch logs:', e)
  }
}

async function setRole(userId: string, role: string) {
  try {
    await apiClient.patch(`/admin/users/${userId}/role?role=${role}`)
    await fetchUsers()
  } catch (e) {
    console.error('Failed to set role:', e)
  }
}

async function disableUser(userId: string) {
  if (!confirm('确定禁用该用户？')) return
  try {
    await apiClient.post(`/admin/users/${userId}/disable`)
    await fetchUsers()
  } catch (e) {
    console.error('Failed to disable user:', e)
  }
}
</script>

<style scoped>
.admin-dashboard { padding: 24px; background: #f8fafc; min-height: 100vh; }
.admin-header { margin-bottom: 24px; }
.admin-header h1 { font-size: 24px; margin-bottom: 16px; color: #1e293b; }
.admin-tabs { display: flex; gap: 4px; border-bottom: 2px solid #e2e8f0; }
.admin-tabs button {
  padding: 10px 20px; border: none; background: none; cursor: pointer;
  font-size: 14px; border-bottom: 2px solid transparent; margin-bottom: -2px;
  color: #64748b;
}
.admin-tabs button.active { color: #2563eb; border-bottom-color: #2563eb; font-weight: 600; }
.tab-content { margin-top: 20px; }
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
@media (max-width: 1024px) { .stats-grid { grid-template-columns: repeat(2, 1fr); } }
.stat-card {
  background: white; border-radius: 12px; padding: 20px;
  display: flex; align-items: center; gap: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.stat-icon { font-size: 32px; }
.stat-num { display: block; font-size: 28px; font-weight: 700; color: #1e293b; }
.stat-label { display: block; font-size: 13px; color: #64748b; margin-top: 4px; }
.stat-card.blue { border-left: 4px solid #2563eb; }
.stat-card.green { border-left: 4px solid #16a34a; }
.stat-card.purple { border-left: 4px solid #9333ea; }
.stat-card.orange { border-left: 4px solid #ea580c; }
.admin-table { width: 100%; background: white; border-radius: 12px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.admin-table th { background: #f8fafc; padding: 12px 16px; text-align: left; font-size: 12px; font-weight: 600; color: #64748b; text-transform: uppercase; }
.admin-table td { padding: 12px 16px; border-top: 1px solid #f1f5f9; font-size: 13px; color: #374151; }
.admin-table tr:hover td { background: #f8fafc; }
.role-select { padding: 4px 8px; border: 1px solid #e2e8f0; border-radius: 4px; font-size: 12px; }
.danger-btn { padding: 4px 12px; background: #fef2f2; color: #dc2626; border: 1px solid #fecaca; border-radius: 4px; cursor: pointer; font-size: 12px; }
.danger-btn:hover { background: #fee2e2; }
.type-badge { background: #eff6ff; color: #2563eb; padding: 2px 8px; border-radius: 10px; font-size: 11px; }
.log-filters { margin-bottom: 16px; }
.filter-select { padding: 8px 12px; border: 1px solid #e2e8f0; border-radius: 6px; font-size: 13px; }
.log-list { background: white; border-radius: 12px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.log-entry { display: flex; gap: 12px; padding: 10px 16px; border-bottom: 1px solid #f1f5f9; font-size: 12px; align-items: baseline; }
.log-entry:last-child { border-bottom: none; }
.log-level { font-weight: 700; width: 60px; flex-shrink: 0; color: #7c3aed; }
.log-time { width: 180px; color: #94a3b8; flex-shrink: 0; font-family: monospace; }
.log-user { width: 100px; color: #64748b; flex-shrink: 0; }
.log-action { width: 80px; color: #2563eb; flex-shrink: 0; font-weight: 500; }
.log-msg { color: #374151; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.empty-state { padding: 40px; text-align: center; color: #94a3b8; }
</style>
