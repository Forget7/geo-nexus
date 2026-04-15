<template>
  <div class="alert-panel">
    <div class="panel-header">
      <h3>🚨 {{ t('alert.title') }}</h3>
      <div class="tabs">
        <button :class="{ active: tab === 'rules' }" @click="tab = 'rules'">
          {{ t('alert.rules') }}
        </button>
        <button :class="{ active: tab === 'history' }" @click="tab = 'history'">
          {{ t('alert.history') }}
          <span v-if="unackCount > 0" class="badge">{{ unackCount }}</span>
        </button>
        <button :class="{ active: tab === 'active' }" @click="tab = 'active'">
          {{ t('alert.active') }}
          <span v-if="activeCount > 0" class="badge warning">{{ activeCount }}</span>
        </button>
      </div>
    </div>

    <!-- 规则列表 -->
    <div v-if="tab === 'rules'" class="rules-section">
      <div class="section-actions">
        <button @click="showCreateRule = true" class="create-btn">+ {{ t('alert.newRule') }}</button>
      </div>
      <div v-if="rules.length === 0" class="empty-state">
        {{ t('alert.noRules') }}
      </div>
      <div class="rule-list">
        <div v-for="rule in rules" :key="rule.id" class="rule-item">
          <div class="rule-info">
            <span class="rule-name">{{ rule.name }}</span>
            <span :class="['rule-status', rule.enabled ? 'on' : 'off']">
              {{ rule.enabled ? 'ON' : 'OFF' }}
            </span>
          </div>
          <p class="rule-desc">{{ rule.description }}</p>
          <div class="rule-meta">
            <span class="rule-category">{{ rule.category }}</span>
            <span :class="['rule-severity', rule.severity]">{{ rule.severity }}</span>
          </div>
          <div class="rule-actions">
            <button @click="toggleRule(rule)">
              {{ rule.enabled ? t('alert.disable') : t('alert.enable') }}
            </button>
            <button @click="deleteRule(rule.id)" class="danger-btn">{{ t('alert.delete') }}</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 告警历史 -->
    <div v-if="tab === 'history'" class="history-section">
      <div class="history-filters">
        <select v-model="filterLevel" @change="fetchHistory" class="filter-select">
          <option value="">全部级别</option>
          <option value="info">INFO</option>
          <option value="warning">WARNING</option>
          <option value="critical">CRITICAL</option>
        </select>
        <select v-model="filterStatus" @change="fetchHistory" class="filter-select">
          <option value="">全部状态</option>
          <option value="triggered">触发</option>
          <option value="resolved">已解决</option>
        </select>
      </div>
      <div v-if="history.length === 0" class="empty-state">
        {{ t('alert.noHistory') }}
      </div>
      <div class="history-list">
        <div v-for="alert in history" :key="alert.id" :class="['alert-item', alert.severity?.toLowerCase()]">
          <div class="alert-icon">
            {{ alert.severity === 'CRITICAL' ? '🚨' : alert.severity === 'warning' ? '⚠️' : 'ℹ️' }}
          </div>
          <div class="alert-body">
            <div class="alert-title">{{ alert.message }}</div>
            <div class="alert-meta">
              <span>{{ formatTime(alert.triggeredAt) }}</span>
              <span v-if="alert.ruleId">规则: {{ alert.ruleId }}</span>
              <span v-if="alert.resolvedBy">处理人: {{ alert.resolvedBy }}</span>
            </div>
          </div>
          <button v-if="alert.status === 'triggered'" @click="acknowledge(alert.id)" class="ack-btn">
            {{ t('alert.acknowledge') }}
          </button>
          <span v-else class="ack-badge">✅</span>
        </div>
      </div>
    </div>

    <!-- 活跃告警 -->
    <div v-if="tab === 'active'" class="active-section">
      <div v-if="activeAlerts.length === 0" class="empty-state">
        {{ t('alert.noActive') }}
      </div>
      <div class="history-list">
        <div v-for="alert in activeAlerts" :key="alert.id" :class="['alert-item', alert.severity?.toLowerCase()]">
          <div class="alert-icon">
            {{ alert.severity === 'critical' ? '🚨' : alert.severity === 'warning' ? '⚠️' : 'ℹ️' }}
          </div>
          <div class="alert-body">
            <div class="alert-title">{{ alert.message }}</div>
            <div class="alert-meta">
              <span>{{ formatTime(alert.triggeredAt) }}</span>
              <span>{{ alert.ruleName }}</span>
            </div>
          </div>
          <button @click="resolveAlert(alert.id)" class="resolve-btn">
            {{ t('alert.resolve') }}
          </button>
        </div>
      </div>
    </div>

    <!-- 新建规则弹窗 -->
    <div v-if="showCreateRule" class="modal-overlay" @click.self="showCreateRule = false">
      <div class="modal-content">
        <h4>{{ t('alert.newRule') }}</h4>
        <form @submit.prevent="createRule">
          <div class="form-group">
            <label>{{ t('alert.ruleName') }}</label>
            <input v-model="newRule.name" type="text" required />
          </div>
          <div class="form-group">
            <label>{{ t('alert.description') }}</label>
            <input v-model="newRule.description" type="text" />
          </div>
          <div class="form-group">
            <label>{{ t('alert.category') }}</label>
            <select v-model="newRule.category">
              <option value="traffic">交通</option>
              <option value="security">安全</option>
              <option value="data">数据</option>
              <option value="system">系统</option>
            </select>
          </div>
          <div class="form-group">
            <label>{{ t('alert.severity') }}</label>
            <select v-model="newRule.severity">
              <option value="info">INFO</option>
              <option value="warning">WARNING</option>
              <option value="critical">CRITICAL</option>
            </select>
          </div>
          <div class="form-actions">
            <button type="button" @click="showCreateRule = false">{{ t('common.cancel') }}</button>
            <button type="submit" class="primary-btn">{{ t('common.confirm') }}</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'

const t = (key: string) => key
const tab = ref('rules')
const rules = ref<any[]>([])
const history = ref<any[]>([])
const activeAlerts = ref<any[]>([])
const unackCount = ref(0)
const activeCount = ref(0)
const filterLevel = ref('')
const filterStatus = ref('')
const showCreateRule = ref(false)

const newRule = ref({
  name: '',
  description: '',
  category: 'traffic',
  severity: 'warning'
})

onMounted(() => {
  fetchRules()
  fetchHistory()
  fetchActiveAlerts()
})

async function fetchRules() {
  try {
    const response = await fetch('/api/v1/alerts/rules/all')
    const json = await response.json()
    if (json.success && json.data) {
      rules.value = json.data
    }
  } catch (e) {
    console.error('Failed to fetch rules:', e)
  }
}

async function fetchHistory() {
  try {
    const params = new URLSearchParams({ limit: '50' })
    if (filterLevel.value) params.set('category', filterLevel.value)
    const response = await fetch('/api/v1/alerts/history?' + params)
    const json = await response.json()
    if (json.success && json.data) {
      history.value = json.data
      unackCount.value = json.data.filter((a: any) => a.status === 'triggered').length
    }
  } catch (e) {
    console.error('Failed to fetch history:', e)
  }
}

async function fetchActiveAlerts() {
  try {
    const response = await fetch('/api/v1/alerts/active')
    const json = await response.json()
    if (json.success && json.data) {
      activeAlerts.value = json.data
      activeCount.value = json.data.length
    }
  } catch (e) {
    console.error('Failed to fetch active alerts:', e)
  }
}

async function toggleRule(rule: any) {
  const action = rule.enabled ? 'disable' : 'enable'
  try {
    await fetch(`/api/v1/alerts/rules/${rule.id}/${action}`, { method: 'POST' })
    await fetchRules()
  } catch (e) {
    console.error('Failed to toggle rule:', e)
  }
}

async function deleteRule(id: string) {
  if (!confirm(t('alert.confirmDelete'))) return
  try {
    await fetch(`/api/v1/alerts/rules/${id}`, { method: 'DELETE' })
    await fetchRules()
  } catch (e) {
    console.error('Failed to delete rule:', e)
  }
}

async function createRule() {
  try {
    await fetch('/api/v1/alerts/rules', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        ...newRule.value,
        enabled: true,
        condition: {},
        conditionType: 'custom'
      })
    })
    showCreateRule.value = false
    newRule.value = { name: '', description: '', category: 'traffic', severity: 'warning' }
    await fetchRules()
  } catch (e) {
    console.error('Failed to create rule:', e)
  }
}

async function acknowledge(id: string) {
  try {
    await fetch(`/api/v1/alerts/history/${id}/acknowledge`, { method: 'POST' })
    await fetchHistory()
  } catch (e) {
    console.error('Failed to acknowledge:', e)
  }
}

async function resolveAlert(id: string) {
  try {
    await fetch(`/api/v1/alerts/history/${id}/resolve`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ resolvedBy: 'user', comment: '' })
    })
    await fetchHistory()
    await fetchActiveAlerts()
  } catch (e) {
    console.error('Failed to resolve:', e)
  }
}

function formatTime(timestamp: number) {
  if (!timestamp) return '-'
  return new Date(timestamp).toLocaleString()
}
</script>

<style scoped>
.alert-panel { background: white; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.15); max-height: 80vh; overflow: hidden; display: flex; flex-direction: column; }
.panel-header { padding: 12px 16px; border-bottom: 1px solid #e2e8f0; display: flex; justify-content: space-between; align-items: center; }
.panel-header h3 { font-size: 15px; font-weight: 600; margin: 0; }
.tabs { display: flex; gap: 8px; }
.tabs button { padding: 6px 12px; border: 1px solid #e2e8f0; border-radius: 6px; background: white; cursor: pointer; font-size: 12px; position: relative; }
.tabs button.active { background: #eff6ff; border-color: #2563eb; color: #2563eb; }
.badge { position: absolute; top: -6px; right: -6px; background: #dc2626; color: white; border-radius: 10px; font-size: 10px; min-width: 16px; height: 16px; display: flex; align-items: center; justify-content: center; }
.badge.warning { background: #d97706; }
.empty-state { padding: 24px; text-align: center; color: #94a3b8; font-size: 13px; }
.rules-section, .history-section, .active-section { flex: 1; overflow-y: auto; padding: 12px 16px; }
.section-actions { margin-bottom: 12px; }
.create-btn { padding: 8px 16px; background: #2563eb; color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 13px; }
.rule-list { display: flex; flex-direction: column; gap: 8px; }
.rule-item { padding: 12px; background: #f8fafc; border-radius: 8px; border: 1px solid #e2e8f0; }
.rule-info { display: flex; justify-content: space-between; margin-bottom: 4px; }
.rule-name { font-weight: 600; font-size: 14px; }
.rule-status { font-size: 11px; padding: 2px 8px; border-radius: 10px; font-weight: 600; }
.rule-status.on { background: #dcfce7; color: #16a34a; }
.rule-status.off { background: #f1f5f9; color: #94a3b8; }
.rule-desc { font-size: 12px; color: #64748b; margin-bottom: 8px; }
.rule-meta { display: flex; gap: 8px; margin-bottom: 8px; }
.rule-category { font-size: 11px; padding: 2px 6px; background: #e2e8f0; border-radius: 4px; }
.rule-severity { font-size: 11px; padding: 2px 6px; border-radius: 4px; font-weight: 600; }
.rule-severity.info { background: #dbeafe; color: #2563eb; }
.rule-severity.warning { background: #fef3c7; color: #d97706; }
.rule-severity.critical { background: #fee2e2; color: #dc2626; }
.rule-actions { display: flex; gap: 6px; }
.rule-actions button { padding: 4px 10px; border: 1px solid #e2e8f0; border-radius: 4px; background: white; cursor: pointer; font-size: 11px; }
.rule-actions .danger-btn { color: #dc2626; border-color: #fecaca; }
.history-filters { margin-bottom: 12px; display: flex; gap: 8px; }
.filter-select { padding: 6px 12px; border: 1px solid #e2e8f0; border-radius: 6px; font-size: 13px; }
.history-list { display: flex; flex-direction: column; gap: 6px; }
.alert-item { display: flex; gap: 10px; padding: 10px; background: #f8fafc; border-radius: 8px; align-items: center; }
.alert-item.critical, .alert-item.CRITICAL { background: #fef2f2; border-left: 3px solid #dc2626; }
.alert-item.warning, .alert-item.WARNING { background: #fffbeb; border-left: 3px solid #d97706; }
.alert-item.info, .alert-item.INFO { background: #eff6ff; border-left: 3px solid #2563eb; }
.alert-icon { font-size: 20px; flex-shrink: 0; }
.alert-body { flex: 1; min-width: 0; }
.alert-title { font-size: 13px; font-weight: 500; }
.alert-meta { display: flex; gap: 8px; font-size: 11px; color: #94a3b8; margin-top: 2px; }
.ack-btn { padding: 4px 10px; border: 1px solid #16a34a; border-radius: 4px; background: none; color: #16a34a; cursor: pointer; font-size: 11px; }
.resolve-btn { padding: 4px 10px; border: 1px solid #2563eb; border-radius: 4px; background: none; color: #2563eb; cursor: pointer; font-size: 11px; }
.ack-badge { font-size: 16px; }
.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal-content { background: white; padding: 24px; border-radius: 12px; width: 400px; max-width: 90%; }
.modal-content h4 { margin: 0 0 16px; font-size: 16px; }
.form-group { margin-bottom: 12px; }
.form-group label { display: block; margin-bottom: 4px; font-size: 12px; color: #64748b; }
.form-group input, .form-group select { width: 100%; padding: 8px 12px; border: 1px solid #e2e8f0; border-radius: 6px; font-size: 13px; box-sizing: border-box; }
.form-actions { display: flex; gap: 8px; justify-content: flex-end; margin-top: 16px; }
.form-actions button { padding: 8px 16px; border: 1px solid #e2e8f0; border-radius: 6px; background: white; cursor: pointer; font-size: 13px; }
.form-actions .primary-btn { background: #2563eb; color: white; border: none; }
</style>
