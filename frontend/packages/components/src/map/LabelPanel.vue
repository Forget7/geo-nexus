<template>
  <div class="label-panel">
    <div class="panel-header">
      <h3>🏷️ {{ t('label.title') }}</h3>
    </div>

    <div class="tabs">
      <button :class="{ active: tab === 'rules' }" @click="tab = 'rules'">{{ t('label.rules') }}</button>
      <button :class="{ active: tab === 'calculate' }" @click="tab = 'calculate'">{{ t('label.calculate') }}</button>
      <button :class="{ active: tab === 'export' }" @click="tab = 'export'">{{ t('label.export') }}</button>
    </div>

    <!-- 规则管理 -->
    <div v-if="tab === 'rules'" class="tab-content">
      <div class="section-actions">
        <button @click="showCreateRule = true" class="create-btn">+ {{ t('label.newRule') }}</button>
      </div>
      <div class="rule-list">
        <div v-for="rule in rules" :key="rule.id" class="rule-item">
          <div class="rule-info">
            <strong>{{ rule.name }}</strong>
            <span class="layer-type">{{ rule.layerType }}</span>
          </div>
          <div class="rule-actions">
            <button @click="deleteRule(rule.id)" class="del-btn">{{ t('label.delete') }}</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 标签计算 -->
    <div v-if="tab === 'calculate'" class="tab-content">
      <div class="param-section">
        <h4>{{ t('label.labelRequest') }}</h4>
        <textarea v-model="labelRequestJson" rows="8" :placeholder="t('label.requestHint')" class="json-input"></textarea>
        <button @click="calculateLabels" class="action-btn" :disabled="calculating">
          {{ calculating ? t('label.calculating') : t('label.runCalculation') }}
        </button>
      </div>

      <div v-if="placedLabels.length" class="result-section">
        <h4>{{ t('label.placedLabels') }} ({{ placedLabels.length }})</h4>
        <div class="label-list">
          <div v-for="label in placedLabels.slice(0, 50)" :key="label.id" class="label-item">
            <span class="label-text">{{ label.text }}</span>
            <span class="label-pos">[{{ label.x?.toFixed(2) }}, {{ label.y?.toFixed(2) }}]</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 导出 -->
    <div v-if="tab === 'export'" class="tab-content">
      <div class="param-section">
        <h4>{{ t('label.selectLabels') }}</h4>
        <textarea v-model="exportLabelsJson" rows="6" :placeholder="t('label.labelsHint')" class="json-input"></textarea>
        <div class="format-select">
          <label>{{ t('label.format') }}:</label>
          <select v-model="exportFormat">
            <option value="geojson">GeoJSON</option>
            <option value="kml">KML</option>
            <option value="csv">CSV</option>
          </select>
        </div>
        <button @click="exportLabels" class="action-btn">{{ t('label.export') }}</button>
        <div v-if="exportResult" class="export-result">
          <pre>{{ exportResult }}</pre>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

const t = (key: string) => key
const tab = ref('rules')
const rules = ref<any[]>([])
const placedLabels = ref<any[]>([])
const calculating = ref(false)
const labelRequestJson = ref('{"layerType":"...","features":[...]}')
const exportLabelsJson = ref('[]')
const exportFormat = ref('geojson')
const exportResult = ref('')
const showCreateRule = ref(false)

onMounted(() => fetchRules())

async function fetchRules() {
  try {
    const { data } = await apiClient.get('/labels/rules')
    rules.value = data
  } catch {}
}

async function calculateLabels() {
  calculating.value = true
  placedLabels.value = []
  try {
    const req = JSON.parse(labelRequestJson.value)
    const { data } = await apiClient.post('/labels/calculate', req)
    placedLabels.value = data
  } catch {}
  calculating.value = false
}

async function deleteRule(id: string) {
  if (!confirm(t('label.confirmDelete'))) return
  await apiClient.delete(`/labels/rules/${id}`)
  await fetchRules()
}

async function exportLabels() {
  try {
    const labels = JSON.parse(exportLabelsJson.value)
    const { data } = await apiClient.post('/labels/export', { labels, format: exportFormat.value })
    exportResult.value = typeof data === 'string' ? data : JSON.stringify(data, null, 2)
  } catch {}
}
</script>

<style scoped>
.label-panel { background: white; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.15); max-height: 80vh; overflow: hidden; display: flex; flex-direction: column; }
.panel-header { padding: 12px 16px; border-bottom: 1px solid #e2e8f0; }
.panel-header h3 { font-size: 15px; font-weight: 600; }
.tabs { display: flex; border-bottom: 1px solid #e2e8f0; }
.tabs button { flex: 1; padding: 10px; border: none; background: none; cursor: pointer; font-size: 13px; color: #64748b; border-bottom: 2px solid transparent; margin-bottom: -1px; }
.tabs button.active { color: #2563eb; border-bottom-color: #2563eb; font-weight: 600; }
.tab-content { flex: 1; overflow-y: auto; padding: 12px 16px; }
.section-actions { margin-bottom: 12px; }
.create-btn { padding: 8px 16px; background: #2563eb; color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 13px; }
.rule-list { display: flex; flex-direction: column; gap: 8px; }
.rule-item { display: flex; justify-content: space-between; align-items: center; padding: 10px; background: #f8fafc; border-radius: 8px; border: 1px solid #e2e8f0; }
.rule-info { display: flex; gap: 8px; align-items: center; }
.rule-info strong { font-size: 13px; }
.layer-type { padding: 2px 8px; background: #e0e7ff; color: #3730a3; border-radius: 4px; font-size: 11px; }
.rule-actions { display: flex; gap: 6px; }
.del-btn { padding: 4px 10px; border: 1px solid #fecaca; border-radius: 4px; background: white; color: #dc2626; cursor: pointer; font-size: 11px; }
.param-section { margin-bottom: 16px; }
.param-section h4 { font-size: 13px; font-weight: 600; margin-bottom: 8px; }
.json-input { width: 100%; padding: 10px; border: 1px solid #e2e8f0; border-radius: 6px; font-size: 11px; font-family: monospace; box-sizing: border-box; resize: vertical; }
.action-btn { width: 100%; padding: 10px; background: #2563eb; color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 13px; margin-top: 8px; }
.action-btn:disabled { opacity: 0.6; }
.result-section { margin-top: 16px; }
.result-section h4 { font-size: 13px; font-weight: 600; margin-bottom: 8px; }
.label-list { max-height: 200px; overflow-y: auto; border: 1px solid #e2e8f0; border-radius: 6px; }
.label-item { display: flex; justify-content: space-between; padding: 7px 10px; border-bottom: 1px solid #f1f5f9; font-size: 12px; }
.label-item:last-child { border-bottom: none; }
.label-text { font-weight: 500; color: #1e293b; }
.label-pos { color: #94a3b8; font-family: monospace; font-size: 11px; }
.format-select { display: flex; align-items: center; gap: 10px; margin-top: 10px; }
.format-select label { font-size: 13px; color: #64748b; }
.format-select select { padding: 8px; border: 1px solid #e2e8f0; border-radius: 6px; font-size: 13px; }
.export-result { margin-top: 12px; }
.export-result pre { max-height: 200px; overflow: auto; background: #1e293b; color: #e2e8f0; padding: 12px; border-radius: 6px; font-size: 11px; font-family: monospace; white-space: pre-wrap; word-break: break-all; }
</style>
