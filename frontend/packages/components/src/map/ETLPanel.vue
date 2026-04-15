<template>
  <div class="etl-panel">
    <div class="panel-header">
      <h3>🔄 {{ t('etl.title') }}</h3>
      <div class="tabs">
        <button :class="{ active: tab === 'sources' }" @click="tab = 'sources'">{{ t('etl.sources') }}</button>
        <button :class="{ active: tab === 'rules' }" @click="tab = 'rules'">{{ t('etl.rules') }}</button>
        <button :class="{ active: tab === 'jobs' }" @click="tab = 'jobs'">{{ t('etl.jobs') }}</button>
      </div>
    </div>

    <!-- 数据源 -->
    <div v-if="tab === 'sources'" class="tab-content">
      <div class="section-actions">
        <button @click="showAddSource = true" class="create-btn">+ {{ t('etl.addSource') }}</button>
      </div>
      <div class="item-list">
        <div v-for="src in sources" :key="src.id" class="item-card">
          <div class="item-info">
            <strong>{{ src.name }}</strong>
            <span class="source-type">{{ src.sourceType }}</span>
          </div>
          <div class="item-actions">
            <button @click="preview(src.id)">{{ t('etl.preview') }}</button>
            <button @click="deleteSource(src.id)" class="del-btn">{{ t('etl.delete') }}</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 转换规则 -->
    <div v-if="tab === 'rules'" class="tab-content">
      <div class="section-actions">
        <button @click="showAddRule = true" class="create-btn">+ {{ t('etl.addRule') }}</button>
      </div>
      <div class="item-list">
        <div v-for="rule in rules" :key="rule.id" class="item-card">
          <div class="item-info">
            <strong>{{ rule.name }}</strong>
            <p class="rule-desc">{{ rule.description }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Jobs -->
    <div v-if="tab === 'jobs'" class="tab-content">
      <div class="section-actions">
        <button @click="showCreateJob = true" class="create-btn">+ {{ t('etl.createJob') }}</button>
      </div>
      <div class="item-list">
        <div v-for="job in jobs" :key="job.id" class="item-card job-card">
          <div class="job-info">
            <div class="job-header">
              <strong>{{ job.name }}</strong>
              <span :class="['job-status', job.status]">{{ job.status }}</span>
            </div>
            <p class="job-meta">
              <span>{{ t('etl.source') }}: {{ job.sourceId }}</span>
              <span v-if="job.startTime">{{ t('etl.started') }}: {{ formatTime(job.startTime) }}</span>
            </p>
          </div>
          <div class="job-actions">
            <button v-if="job.status === 'RUNNING' || job.status === 'PENDING'" @click="cancelJob(job.id)" class="cancel-btn">
              {{ t('etl.cancel') }}
            </button>
            <button @click="executeJob(job.id)" class="run-btn">▶️</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 预览弹窗 -->
    <div v-if="showPreview" class="modal-overlay" @click.self="showPreview = false">
      <div class="modal wide">
        <h3>{{ t('etl.dataPreview') }}</h3>
        <div class="preview-table">
          <table>
            <thead><tr><th v-for="col in previewData.columns" :key="col">{{ col }}</th></tr></thead>
            <tbody>
              <tr v-for="(row, i) in previewData.sampleData" :key="i">
                <td v-for="col in previewData.columns" :key="col">{{ row[col] }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <button @click="showPreview = false" class="close-btn">{{ t('etl.close') }}</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

const t = (key: string) => key
const tab = ref('sources')
const sources = ref<any[]>([])
const rules = ref<any[]>([])
const jobs = ref<any[]>([])
const showAddSource = ref(false)
const showAddRule = ref(false)
const showCreateJob = ref(false)
const showPreview = ref(false)
const previewData = ref<any>({ columns: [], sampleData: [] })

onMounted(() => { fetchSources(); fetchRules(); fetchJobs() })

async function fetchSources() {
  try {
    const { data } = await apiClient.get('/etl/datasources')
    sources.value = data
  } catch {}
}

async function fetchRules() {
  try {
    const { data } = await apiClient.get('/etl/rules')
    rules.value = data
  } catch {}
}

async function fetchJobs() {
  try {
    const { data } = await apiClient.get('/etl/jobs')
    jobs.value = data
  } catch {}
}

async function preview(id: string) {
  try {
    const { data } = await apiClient.get(`/etl/datasources/${id}/preview?limit=20`)
    previewData.value = data
    showPreview.value = true
  } catch {}
}

async function deleteSource(id: string) {
  if (!confirm(t('etl.confirmDelete'))) return
  await apiClient.delete(`/etl/datasources/${id}`)
  await fetchSources()
}

async function executeJob(id: string) {
  await apiClient.post(`/etl/jobs/${id}/execute`)
  await fetchJobs()
}

async function cancelJob(id: string) {
  await apiClient.post(`/etl/jobs/${id}/cancel`)
  await fetchJobs()
}

function formatTime(ts: number) {
  return ts ? new Date(ts).toLocaleString() : '-'
}
</script>

<style scoped>
.etl-panel { background: white; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.15); max-height: 80vh; overflow: hidden; display: flex; flex-direction: column; }
.panel-header { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; border-bottom: 1px solid #e2e8f0; }
.panel-header h3 { font-size: 15px; font-weight: 600; }
.tabs { display: flex; gap: 4px; }
.tabs button { padding: 5px 12px; border: 1px solid #e2e8f0; border-radius: 6px; background: white; cursor: pointer; font-size: 12px; }
.tabs button.active { background: #2563eb; color: white; border-color: #2563eb; }
.tab-content { flex: 1; overflow-y: auto; padding: 12px 16px; }
.section-actions { margin-bottom: 12px; }
.create-btn { padding: 7px 14px; background: #2563eb; color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 12px; }
.item-list { display: flex; flex-direction: column; gap: 8px; }
.item-card { display: flex; justify-content: space-between; align-items: center; padding: 10px; background: #f8fafc; border-radius: 8px; border: 1px solid #e2e8f0; }
.item-info { flex: 1; }
.item-info strong { font-size: 13px; }
.source-type { margin-left: 8px; padding: 2px 8px; background: #e0e7ff; color: #3730a3; border-radius: 4px; font-size: 11px; }
.rule-desc { font-size: 12px; color: #64748b; margin: 4px 0 0; }
.item-actions { display: flex; gap: 6px; }
.item-actions button { padding: 5px 10px; border: 1px solid #e2e8f0; border-radius: 4px; background: white; cursor: pointer; font-size: 11px; }
.del-btn { color: #dc2626; border-color: #fecaca; }
.job-card { flex-direction: column; align-items: flex-start; }
.job-info { width: 100%; }
.job-header { display: flex; justify-content: space-between; align-items: center; }
.job-meta { display: flex; gap: 12px; font-size: 11px; color: #94a3b8; margin: 4px 0 0; }
.job-status { padding: 2px 8px; border-radius: 10px; font-weight: 600; font-size: 10px; text-transform: uppercase; }
.job-status.PENDING { background: #fef9c3; color: #a16207; }
.job-status.RUNNING { background: #dbeafe; color: #2563eb; }
.job-status.COMPLETED { background: #dcfce7; color: #16a34a; }
.job-status.FAILED { background: #fef2f2; color: #dc2626; }
.job-status.CREATED { background: #f1f5f9; color: #64748b; }
.job-status.SCHEDULED { background: #f0fdf4; color: #15803d; }
.job-actions { display: flex; gap: 6px; margin-top: 8px; width: 100%; justify-content: flex-end; }
.run-btn { padding: 5px 12px; background: #2563eb; color: white; border: none; border-radius: 4px; cursor: pointer; font-size: 11px; }
.cancel-btn { padding: 5px 10px; border: 1px solid #fecaca; border-radius: 4px; background: white; color: #dc2626; cursor: pointer; font-size: 11px; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal { background: white; border-radius: 12px; padding: 24px; width: 500px; }
.modal.wide { width: 800px; max-height: 80vh; overflow-y: auto; }
.modal h3 { font-size: 16px; margin-bottom: 16px; }
.preview-table { max-height: 400px; overflow: auto; border: 1px solid #e2e8f0; border-radius: 6px; margin-bottom: 12px; }
.preview-table table { width: 100%; border-collapse: collapse; font-size: 11px; }
.preview-table th { background: #f8fafc; padding: 6px 10px; text-align: left; font-weight: 600; position: sticky; top: 0; border-bottom: 1px solid #e2e8f0; }
.preview-table td { padding: 6px 10px; border-top: 1px solid #f1f5f9; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 200px; }
.close-btn { padding: 8px 16px; background: #f1f5f9; border: none; border-radius: 6px; cursor: pointer; font-size: 13px; }
</style>
