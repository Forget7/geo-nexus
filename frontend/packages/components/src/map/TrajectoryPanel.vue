<template>
  <div class="traj-panel">
    <div class="panel-header">
      <h3>📈 {{ translate('traj.title') }}</h3>
      <button @click="showCreate = true" class="create-btn">+ {{ translate('traj.create') }}</button>
    </div>

    <div class="tabs">
      <button :class="{ active: tab === 'list' }" @click="tab = 'list'">{{ translate('traj.list') }}</button>
      <button :class="{ active: tab === 'filter' }" @click="tab = 'filter'">{{ translate('traj.spatialFilter') }}</button>
    </div>

    <!-- 轨迹列表 -->
    <div v-if="tab === 'list'" class="tab-content">
      <div class="traj-list">
        <div v-for="traj in trajectories" :key="traj.id" class="traj-card" @click="selectTrajectory(traj)">
          <div class="traj-info">
            <strong>{{ traj.objectId || traj.id }}</strong>
            <div class="traj-meta">
              <span>{{ traj.pointCount || 0 }} {{ translate('traj.points') }}</span>
              <span>{{ traj.startTime }}</span>
              <span v-if="traj.totalDistance" class="dist">{{ (traj.totalDistance / 1000).toFixed(1) }}km</span>
            </div>
          </div>
          <div class="traj-actions">
            <button @click.stop="deleteTraj(traj.id)" class="del-btn">{{ translate('traj.delete') }}</button>
          </div>
        </div>
        <div v-if="!trajectories.length" class="empty-state">{{ translate('traj.noTrajectories') }}</div>
      </div>
    </div>

    <!-- 空间过滤 -->
    <div v-if="tab === 'filter'" class="tab-content">
      <div class="param-group">
        <label>{{ translate('traj.bbox') }}</label>
        <div class="bbox-inputs">
          <input v-model.number="bbox[0]" type="number" step="any" placeholder="minLon" class="input small" />
          <input v-model.number="bbox[1]" type="number" step="any" placeholder="minLat" class="input small" />
          <input v-model.number="bbox[2]" type="number" step="any" placeholder="maxLon" class="input small" />
          <input v-model.number="bbox[3]" type="number" step="any" placeholder="maxLat" class="input small" />
        </div>
      </div>
      <button @click="spatialFilter" class="action-btn">{{ translate('traj.filter') }}</button>
      <div v-if="filteredTrajs.length" class="result-list">
        <div v-for="t in filteredTrajs" :key="t.id" class="traj-item">{{ t.objectId }}</div>
      </div>
    </div>

    <!-- 创建弹窗 -->
    <div v-if="showCreate" class="modal-overlay" @click.self="showCreate = false">
      <div class="modal">
        <h3>{{ translate('traj.create') }}</h3>
        <input v-model="newTraj.objectId" :placeholder="translate('traj.objectId')" class="input" />
        <div class="modal-actions">
          <button @click="createTrajectory">{{ translate('traj.create') }}</button>
          <button @click="showCreate = false" class="cancel-btn">{{ translate('traj.cancel') }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

const translate = (key: string) => key
const tab = ref('list')
const trajectories = ref<any[]>([])
const filteredTrajs = ref<any[]>([])
const showCreate = ref(false)
const newTraj = ref<any>({})
const bbox = ref([0, 0, 0, 0])

onMounted(() => fetchTrajectories())

async function fetchTrajectories() {
  try {
    const { data } = await apiClient.get('/trajectories?limit=50')
    trajectories.value = data
  } catch {}
}

async function createTrajectory() {
  await apiClient.post('/trajectories', newTraj.value)
  newTraj.value = {}
  showCreate.value = false
  await fetchTrajectories()
}

async function deleteTraj(id: string) {
  if (!confirm(translate('traj.confirmDelete'))) return
  await apiClient.delete(`/trajectories/${id}`)
  await fetchTrajectories()
}

async function spatialFilter() {
  try {
    const { data } = await apiClient.get('/trajectories/spatial', {
      params: { bbox: bbox.value.join(',') }
    })
    filteredTrajs.value = data
  } catch {}
}

function selectTrajectory(t: any) {
  console.log('Selected trajectory:', t.id)
}
</script>

<style scoped>
.traj-panel { background: white; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.15); max-height: 80vh; overflow: hidden; display: flex; flex-direction: column; }
.panel-header { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; border-bottom: 1px solid #e2e8f0; }
.panel-header h3 { font-size: 15px; font-weight: 600; margin: 0; }
.create-btn { padding: 7px 14px; background: #2563eb; color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 12px; }
.tabs { display: flex; border-bottom: 1px solid #e2e8f0; }
.tabs button { flex: 1; padding: 10px; border: none; background: none; cursor: pointer; font-size: 13px; color: #64748b; border-bottom: 2px solid transparent; margin-bottom: -1px; }
.tabs button.active { color: #2563eb; border-bottom-color: #2563eb; font-weight: 600; }
.tab-content { flex: 1; overflow-y: auto; padding: 12px 16px; }
.traj-list { display: flex; flex-direction: column; gap: 8px; }
.traj-card { display: flex; justify-content: space-between; align-items: center; padding: 10px; background: #f8fafc; border-radius: 8px; border: 1px solid #e2e8f0; cursor: pointer; }
.traj-card:hover { background: #f1f5f9; }
.traj-info { flex: 1; }
.traj-info strong { font-size: 13px; }
.traj-meta { display: flex; gap: 8px; font-size: 11px; color: #94a3b8; margin-top: 4px; }
.dist { color: #16a34a; font-weight: 600; }
.traj-actions { display: flex; gap: 6px; }
.del-btn { padding: 5px 10px; border: 1px solid #fecaca; border-radius: 4px; background: white; color: #dc2626; cursor: pointer; font-size: 11px; }
.empty-state { text-align: center; padding: 30px; color: #94a3b8; font-size: 14px; }
.param-group { margin-bottom: 12px; }
.param-group label { display: block; font-size: 12px; color: #64748b; margin-bottom: 6px; font-weight: 500; }
.bbox-inputs { display: grid; grid-template-columns: 1fr 1fr; gap: 6px; }
.input { width: 100%; padding: 9px; border: 1px solid #e2e8f0; border-radius: 6px; font-size: 13px; box-sizing: border-box; }
.input.small { flex: 1; }
.action-btn { width: 100%; padding: 10px; background: #2563eb; color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 13px; }
.result-list { margin-top: 12px; display: flex; flex-direction: column; gap: 6px; max-height: 200px; overflow-y: auto; }
.traj-item { padding: 8px; background: #f8fafc; border-radius: 6px; font-size: 13px; border: 1px solid #e2e8f0; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal { background: white; border-radius: 12px; padding: 24px; width: 380px; }
.modal h3 { font-size: 16px; margin-bottom: 16px; }
.modal-actions { display: flex; gap: 8px; justify-content: flex-end; margin-top: 8px; }
.modal-actions button { padding: 8px 16px; border-radius: 6px; cursor: pointer; font-size: 13px; border: none; }
.modal-actions button:first-child { background: #2563eb; color: white; }
.cancel-btn { background: #f1f5f9; color: #64748b; }
</style>
