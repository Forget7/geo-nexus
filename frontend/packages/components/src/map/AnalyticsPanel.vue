<template>
  <div class="analytics-panel">
    <div class="panel-header">
      <h3>📊 {{ t('analytics.title') }}</h3>
    </div>
    <div class="stats-grid">
      <div class="stat-card">
        <span class="stat-val">{{ stats.events || 0 }}</span>
        <span class="stat-label">{{ t('analytics.events') }}</span>
      </div>
      <div class="stat-card warning">
        <span class="stat-val">{{ stats.anomalies || 0 }}</span>
        <span class="stat-label">{{ t('analytics.anomalies') }}</span>
      </div>
    </div>
    <div class="section">
      <h4>{{ t('analytics.realtime') }}</h4>
      <div class="metric-list">
        <div v-for="(val, key) in realtimeMetrics" :key="key" class="metric-item">
          <span class="metric-key">{{ key }}</span>
          <span class="metric-val">{{ typeof val === 'object' ? val.value : val }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'

defineOptions({
  name: 'GnAnalyticsPanel'
})

const { t } = useI18n()
const stats = ref<any>({})
const realtimeMetrics = ref<any>({})

onMounted(async () => {
  try {
    const { data: s } = await apiClient.get('/analytics/events?limit=100')
    stats.value = { events: Array.isArray(s) ? s.length : 0 }
    const { data: r } = await apiClient.get('/anomalies?limit=100')
    stats.value.anomalies = Array.isArray(r) ? r.length : 0
    const { data: m } = await apiClient.get('/analytics/metrics?streamId=default')
    realtimeMetrics.value = m || {}
  } catch (e) {
    console.warn('[AnalyticsPanel] failed to load', e)
  }
})
</script>

<style scoped>
.analytics-panel { background: white; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.15); }
.panel-header { padding: 12px 16px; border-bottom: 1px solid #e2e8f0; }
.panel-header h3 { font-size: 15px; font-weight: 600; margin: 0; }
.stats-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; padding: 12px 16px; }
.stat-card { background: #f8fafc; border-radius: 8px; padding: 12px; text-align: center; border: 1px solid #e2e8f0; }
.stat-val { display: block; font-size: 24px; font-weight: 700; color: #1e293b; }
.stat-label { font-size: 11px; color: #94a3b8; }
.stat-card.warning .stat-val { color: #f59e0b; }
.section { padding: 12px 16px; border-top: 1px solid #f1f5f9; }
.section h4 { font-size: 12px; font-weight: 600; color: #64748b; margin-bottom: 8px; }
.metric-list { display: flex; flex-direction: column; gap: 6px; }
.metric-item { display: flex; justify-content: space-between; padding: 8px; background: #f8fafc; border-radius: 6px; font-size: 13px; }
.metric-key { color: #64748b; }
.metric-val { font-weight: 600; color: #1e293b; }
</style>
