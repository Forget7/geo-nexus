<template>
  <div class="sat-panel">
    <div class="panel-header">
      <h3>🛰️ {{ t('sat.title') }}</h3>
      <button @click="showAdd = true" class="add-btn">+ {{ t('sat.addImage') }}</button>
    </div>

    <!-- 筛选 -->
    <div class="filter-bar">
      <select v-model="filterSatellite" @change="fetchImages" class="filter-select">
        <option value="">所有卫星</option>
        <option v-for="sat in satellites" :key="sat" :value="sat">{{ sat }}</option>
      </select>
    </div>

    <!-- 影像列表 -->
    <div class="image-list">
      <div v-for="img in images" :key="img.id" class="image-card">
        <div class="image-info">
          <div class="image-header">
            <strong>{{ img.name || img.id }}</strong>
            <span class="sat-tag">{{ img.satellite }}</span>
          </div>
          <div class="image-meta">
            <span>{{ formatDate(img.captureDate || img.uploadedAt) }}</span>
            <span>{{ img.cloudCover || 0 }}% {{ t('sat.cloud') }}</span>
            <span>{{ img.resolution }}m</span>
          </div>
        </div>
        <div class="image-actions">
          <button @click="viewBands(img.id)" class="view-btn">{{ t('sat.bands') }}</button>
          <button @click="deleteImage(img.id)" class="del-btn">{{ t('sat.delete') }}</button>
        </div>
      </div>
      <div v-if="!images.length" class="empty-state">
        <p>🛰️ {{ t('sat.noImages') }}</p>
      </div>
    </div>

    <!-- 添加弹窗 -->
    <div v-if="showAdd" class="modal-overlay" @click.self="showAdd = false">
      <div class="modal">
        <h3>{{ t('sat.addImage') }}</h3>
        <input v-model="newImg.name" :placeholder="t('sat.name')" class="input" />
        <input v-model="newImg.satellite" :placeholder="t('sat.satellite')" class="input" />
        <input v-model="newImg.captureDate" type="date" :placeholder="t('sat.date')" class="input" />
        <input v-model.number="newImg.cloudCover" type="number" :placeholder="t('sat.cloudCover')" class="input" />
        <input v-model.number="newImg.resolution" type="number" :placeholder="t('sat.resolution')" class="input" />
        <div class="modal-actions">
          <button @click="addImage">{{ t('sat.add') }}</button>
          <button @click="showAdd = false" class="cancel-btn">{{ t('sat.cancel') }}</button>
        </div>
      </div>
    </div>

    <!-- 波段信息弹窗 -->
    <div v-if="showBands" class="modal-overlay" @click.self="showBands = false">
      <div class="modal">
        <h3>{{ t('sat.bandInfo') }}</h3>
        <div class="band-list">
          <div v-for="(band, i) in bands" :key="i" class="band-item">
            <span class="band-name">{{ band.name }}</span>
            <span class="band-range">{{ band.wavelength }}</span>
          </div>
        </div>
        <button @click="showBands = false" class="close-btn">{{ t('sat.close') }}</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

const t = (key: string) => key
const images = ref<any[]>([])
const satellites = ref<any[]>([])
const filterSatellite = ref('')
const showAdd = ref(false)
const showBands = ref(false)
const bands = ref<any[]>([])
const newImg = ref<any>({})

onMounted(() => { fetchImages(); fetchSatellites() })

async function fetchImages() {
  try {
    const params: any = { limit: 50 }
    if (filterSatellite.value) params.satellite = filterSatellite.value
    const { data } = await apiClient.get('/satellite/images', { params })
    images.value = data
  } catch {}
}

async function fetchSatellites() {
  try {
    const { data } = await apiClient.get('/satellite/satellites')
    satellites.value = data
  } catch {}
}

async function addImage() {
  await apiClient.post('/satellite/images', newImg.value)
  newImg.value = {}
  showAdd.value = false
  await fetchImages()
}

async function deleteImage(id: string) {
  if (!confirm(t('sat.confirmDelete'))) return
  await apiClient.delete(`/satellite/images/${id}`)
  await fetchImages()
}

async function viewBands(id: string) {
  try {
    const { data } = await apiClient.get(`/satellite/images/${id}/bands`)
    bands.value = data
    showBands.value = true
  } catch {}
}

function formatDate(ts: number): string {
  if (!ts) return ''
  const d = new Date(ts)
  return d.toLocaleDateString()
}
</script>

<style scoped>
.sat-panel { background: white; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.15); max-height: 80vh; overflow: hidden; display: flex; flex-direction: column; }
.panel-header { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; border-bottom: 1px solid #e2e8f0; }
.panel-header h3 { font-size: 15px; font-weight: 600; }
.add-btn { padding: 7px 14px; background: #2563eb; color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 12px; }
.filter-bar { padding: 8px 16px; border-bottom: 1px solid #f1f5f9; }
.filter-select { padding: 6px 12px; border: 1px solid #e2e8f0; border-radius: 6px; font-size: 13px; width: 100%; }
.image-list { flex: 1; overflow-y: auto; padding: 12px 16px; display: flex; flex-direction: column; gap: 8px; }
.image-card { display: flex; justify-content: space-between; align-items: center; padding: 10px; background: #f8fafc; border-radius: 8px; border: 1px solid #e2e8f0; }
.image-info { flex: 1; }
.image-header { display: flex; gap: 8px; align-items: center; margin-bottom: 4px; }
.image-header strong { font-size: 13px; }
.sat-tag { padding: 2px 8px; background: #e0e7ff; color: #3730a3; border-radius: 4px; font-size: 11px; }
.image-meta { display: flex; gap: 10px; font-size: 11px; color: #94a3b8; }
.image-actions { display: flex; gap: 6px; }
.view-btn { padding: 5px 10px; border: 1px solid #e2e8f0; border-radius: 4px; background: white; cursor: pointer; font-size: 11px; }
.del-btn { padding: 5px 10px; border: 1px solid #fecaca; border-radius: 4px; background: white; color: #dc2626; cursor: pointer; font-size: 11px; }
.empty-state { text-align: center; padding: 40px; color: #94a3b8; font-size: 14px; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal { background: white; border-radius: 12px; padding: 24px; width: 420px; }
.modal h3 { font-size: 16px; margin-bottom: 16px; }
.input { width: 100%; padding: 10px; border: 1px solid #e2e8f0; border-radius: 6px; font-size: 13px; margin-bottom: 8px; box-sizing: border-box; }
.modal-actions { display: flex; gap: 8px; justify-content: flex-end; margin-top: 8px; }
.modal-actions button { padding: 8px 16px; border-radius: 6px; cursor: pointer; font-size: 13px; border: none; }
.modal-actions button:first-child { background: #2563eb; color: white; }
.cancel-btn { background: #f1f5f9; color: #64748b; }
.band-list { display: flex; flex-direction: column; gap: 6px; margin-bottom: 16px; max-height: 300px; overflow-y: auto; }
.band-item { display: flex; justify-content: space-between; padding: 8px; background: #f8fafc; border-radius: 6px; font-size: 13px; }
.band-name { font-weight: 600; color: #1e293b; }
.band-range { color: #64748b; font-size: 12px; }
.close-btn { width: 100%; padding: 8px; background: #f1f5f9; border: none; border-radius: 6px; cursor: pointer; font-size: 13px; }
</style>
