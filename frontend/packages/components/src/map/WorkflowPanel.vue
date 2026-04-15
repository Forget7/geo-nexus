<template>
  <div class="workflow-panel">
    <div class="panel-header">
      <h3>⚙️ {{ t('wf.title') }}</h3>
      <button @click="showCreate = true" class="create-btn">+ {{ t('wf.newWorkflow') }}</button>
    </div>

    <!-- 工作流列表 -->
    <div class="wf-list">
      <div v-for="wf in workflows" :key="wf.id" class="wf-card">
        <div class="wf-info">
          <strong>{{ wf.name }}</strong>
          <p>{{ wf.description }}</p>
          <div class="wf-meta">
            <span class="step-count">{{ wf.steps?.length || 0 }} {{ t('wf.steps') }}</span>
            <span class="wf-status" :class="wf.status">{{ wf.status }}</span>
          </div>
        </div>
        <div class="wf-actions">
          <button @click="execute(wf.id)" class="run-btn">▶️ {{ t('wf.run') }}</button>
          <button @click="deleteWf(wf.id)" class="del-btn">🗑️</button>
        </div>
      </div>
    </div>

    <!-- 执行历史 -->
    <div class="exec-section">
      <h4>{{ t('wf.executionHistory') }}</h4>
      <div v-for="exec in executions" :key="exec.id" class="exec-item">
        <div class="exec-info">
          <span class="exec-id">#{{ exec.id.substring(0,8) }}</span>
          <span class="exec-wf">{{ exec.workflowName }}</span>
          <span :class="['exec-status', exec.status]">{{ exec.status }}</span>
        </div>
        <div class="exec-actions">
          <button v-if="exec.status === 'RUNNING'" @click="cancelExec(exec.id)">
            {{ t('wf.cancel') }}
          </button>
          <button v-if="exec.status === 'COMPLETED'" @click="viewResult(exec.id)">
            {{ t('wf.viewResult') }}
          </button>
        </div>
      </div>
    </div>

    <!-- 新建弹窗（简化） -->
    <div v-if="showCreate" class="modal-overlay" @click.self="showCreate = false">
      <div class="modal">
        <h3>{{ t('wf.createWorkflow') }}</h3>
        <input v-model="newWf.name" :placeholder="t('wf.namePlaceholder')" class="input" />
        <textarea v-model="newWf.description" :placeholder="t('wf.descPlaceholder')" class="input" rows="3"></textarea>
        <div class="modal-actions">
          <button @click="createWorkflow">{{ t('wf.create') }}</button>
          <button @click="showCreate = false" class="cancel-btn">{{ t('wf.cancel') }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

const t = (key: string) => key
const workflows = ref<any[]>([])
const executions = ref<any[]>([])
const showCreate = ref(false)
const newWf = ref({ name: '', description: '' })

onMounted(() => { fetchWorkflows(); fetchExecutions() })

async function fetchWorkflows() {
  try {
    const { data } = await apiClient.get('/workflows')
    workflows.value = data
  } catch {}
}

async function fetchExecutions() {
  try {
    const { data } = await apiClient.get('/workflows/executions?size=20')
    executions.value = data.content || data
  } catch {}
}

async function execute(id: string) {
  await apiClient.post(`/workflows/${id}/execute`, {})
  await fetchExecutions()
}

async function deleteWf(id: string) {
  if (!confirm(t('wf.confirmDelete'))) return
  await apiClient.delete(`/workflows/${id}`)
  await fetchWorkflows()
}

async function cancelExec(id: string) {
  await apiClient.post(`/workflows/executions/${id}/cancel`)
  await fetchExecutions()
}

function viewResult(id: string) {
  console.log('View result:', id)
}

async function createWorkflow() {
  await apiClient.post('/workflows', newWf.value)
  newWf.value = { name: '', description: '' }
  showCreate.value = false
  await fetchWorkflows()
}
</script>

<style scoped>
.workflow-panel { background: white; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.15); max-height: 80vh; overflow-y: auto; }
.panel-header { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; border-bottom: 1px solid #e2e8f0; }
.panel-header h3 { font-size: 15px; font-weight: 600; }
.create-btn { padding: 7px 14px; background: #2563eb; color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 12px; }
.wf-list { padding: 12px 16px; border-bottom: 1px solid #f1f5f9; }
.wf-card { display: flex; justify-content: space-between; align-items: center; padding: 10px; background: #f8fafc; border-radius: 8px; margin-bottom: 8px; }
.wf-info { flex: 1; }
.wf-info strong { font-size: 14px; }
.wf-info p { font-size: 12px; color: #64748b; margin: 4px 0; }
.wf-meta { display: flex; gap: 8px; font-size: 11px; }
.step-count { color: #94a3b8; }
.wf-status { padding: 2px 8px; border-radius: 10px; font-weight: 600; text-transform: uppercase; font-size: 10px; }
.wf-status.active { background: #dcfce7; color: #16a34a; }
.wf-status.inactive { background: #f1f5f9; color: #94a3b8; }
.wf-actions { display: flex; gap: 6px; }
.run-btn { padding: 6px 12px; background: #2563eb; color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 12px; }
.del-btn { padding: 6px 8px; background: none; border: 1px solid #fecaca; border-radius: 6px; cursor: pointer; font-size: 12px; }
.exec-section { padding: 12px 16px; }
.exec-section h4 { font-size: 13px; font-weight: 600; margin-bottom: 8px; }
.exec-item { display: flex; justify-content: space-between; align-items: center; padding: 8px; background: #f8fafc; border-radius: 6px; margin-bottom: 6px; }
.exec-info { display: flex; gap: 10px; align-items: center; font-size: 12px; }
.exec-id { color: #94a3b8; font-family: monospace; }
.exec-status { padding: 2px 8px; border-radius: 10px; font-weight: 600; font-size: 10px; }
.exec-status.RUNNING { background: #fef9c3; color: #a16207; }
.exec-status.COMPLETED { background: #dcfce7; color: #16a34a; }
.exec-status.FAILED { background: #fef2f2; color: #dc2626; }
.exec-status.CANCELLED { background: #f1f5f9; color: #94a3b8; }
.exec-actions button { padding: 4px 10px; border: 1px solid #e2e8f0; border-radius: 4px; background: white; cursor: pointer; font-size: 11px; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal { background: white; border-radius: 12px; padding: 24px; width: 400px; }
.modal h3 { font-size: 16px; margin-bottom: 16px; }
.input { width: 100%; padding: 10px; border: 1px solid #e2e8f0; border-radius: 6px; font-size: 13px; margin-bottom: 8px; }
.modal-actions { display: flex; gap: 8px; justify-content: flex-end; }
.modal-actions button { padding: 8px 16px; border-radius: 6px; cursor: pointer; font-size: 13px; border: none; }
.modal-actions button:first-child { background: #2563eb; color: white; }
.cancel-btn { background: #f1f5f9; color: #64748b; }
</style>
