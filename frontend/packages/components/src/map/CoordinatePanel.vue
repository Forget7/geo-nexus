<template>
  <div class="coord-panel">
    <div class="panel-header">
      <h3>📍 {{ t('coord.title') }}</h3>
    </div>

    <div class="tabs">
      <button :class="{ active: tab === 'transform' }" @click="tab = 'transform'">{{ t('coord.transform') }}</button>
      <button :class="{ active: tab === 'crs' }" @click="tab = 'crs'">{{ t('coord.crsSearch') }}</button>
      <button :class="{ active: tab === 'geodesy' }" @click="tab = 'geodesy'">{{ t('coord.geodesy') }}</button>
    </div>

    <!-- 坐标转换 -->
    <div v-if="tab === 'transform'" class="tab-content">
      <div class="param-group">
        <label>{{ t('coord.fromEpsg') }}</label>
        <input v-model="fromEpsg" placeholder="EPSG:4326" class="input" />
      </div>
      <div class="param-group">
        <label>{{ t('coord.toEpsg') }}</label>
        <input v-model="toEpsg" placeholder="EPSG:3857" class="input" />
      </div>
      <div class="coord-inputs">
        <input v-model.number="inX" type="number" step="any" :placeholder="t('coord.x')" class="input small" />
        <input v-model.number="inY" type="number" step="any" :placeholder="t('coord.y')" class="input small" />
      </div>
      <button @click="transform" class="action-btn">{{ t('coord.transform') }}</button>
      <div v-if="outCoords" class="result-box">
        <p class="result-label">{{ t('coord.result') }}:</p>
        <p class="result-val">X: {{ outCoords[0]?.toFixed(6) }}</p>
        <p class="result-val">Y: {{ outCoords[1]?.toFixed(6) }}</p>
      </div>
    </div>

    <!-- CRS 搜索 -->
    <div v-if="tab === 'crs'" class="tab-content">
      <div class="param-group">
        <input v-model="crsKeyword" :placeholder="t('coord.searchHint')" class="input" @keyup.enter="searchCRS" />
        <button @click="searchCRS" class="action-btn">{{ t('coord.search') }}</button>
      </div>
      <div class="crs-list">
        <div v-for="crs in crsResults" :key="crs.epsg" class="crs-item">
          <strong>{{ crs.epsg }}</strong>
          <span>{{ crs.name }}</span>
          <span class="crs-type">{{ crs.type }}</span>
        </div>
      </div>
    </div>

    <!-- 大地测量 -->
    <div v-if="tab === 'geodesy'" class="tab-content">
      <div class="param-group">
        <label>{{ t('coord.point1') }}</label>
        <div class="coord-inputs">
          <input v-model.number="p1lon" type="number" step="any" placeholder="Lon1" class="input small" />
          <input v-model.number="p1lat" type="number" step="any" placeholder="Lat1" class="input small" />
        </div>
      </div>
      <div class="param-group">
        <label>{{ t('coord.point2') }}</label>
        <div class="coord-inputs">
          <input v-model.number="p2lon" type="number" step="any" placeholder="Lon2" class="input small" />
          <input v-model.number="p2lat" type="number" step="any" placeholder="Lat2" class="input small" />
        </div>
      </div>
      <button @click="calcDistance" class="action-btn">{{ t('coord.distance') }}</button>
      <button @click="calcBearing" class="action-btn secondary">{{ t('coord.bearing') }}</button>
      <div v-if="geoResult" class="result-box">
        <p class="result-label">{{ geoResult.label }}:</p>
        <p class="result-val large">{{ geoResult.value }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const t = (key: string) => key
const tab = ref('transform')
const fromEpsg = ref('EPSG:4326')
const toEpsg = ref('EPSG:3857')
const inX = ref()
const inY = ref()
const outCoords = ref<any>(null)
const crsKeyword = ref('')
const crsResults = ref<any[]>([])
const p1lon = ref()
const p1lat = ref()
const p2lon = ref()
const p2lat = ref()
const geoResult = ref<any>(null)

async function transform() {
  try {
    const { data } = await apiClient.post('/coordinates/transform', {
      point: [inX.value, inY.value],
      fromEpsg: fromEpsg.value,
      toEpsg: toEpsg.value
    })
    outCoords.value = data
  } catch {}
}

async function searchCRS() {
  try {
    const { data } = await apiClient.get(`/coordinates/crs?keyword=${encodeURIComponent(crsKeyword.value)}`)
    crsResults.value = data
  } catch {}
}

async function calcDistance() {
  try {
    const { data } = await apiClient.get(`/coordinates/distance?lon1=${p1lon.value}&lat1=${p1lat.value}&lon2=${p2lon.value}&lat2=${p2lat.value}`)
    geoResult.value = { label: t('coord.distanceM'), value: data.toFixed(2) + ' m' }
  } catch {}
}

async function calcBearing() {
  try {
    const { data } = await apiClient.get(`/coordinates/bearing?lon1=${p1lon.value}&lat1=${p1lat.value}&lon2=${p2lon.value}&lat2=${p2lat.value}`)
    geoResult.value = { label: t('coord.bearingDeg'), value: data.toFixed(2) + '°' }
  } catch {}
}
</script>

<style scoped>
.coord-panel { background: white; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.15); max-height: 80vh; overflow: hidden; display: flex; flex-direction: column; }
.panel-header { padding: 12px 16px; border-bottom: 1px solid #e2e8f0; }
.panel-header h3 { font-size: 15px; font-weight: 600; }
.tabs { display: flex; border-bottom: 1px solid #e2e8f0; }
.tabs button { flex: 1; padding: 10px; border: none; background: none; cursor: pointer; font-size: 13px; color: #64748b; border-bottom: 2px solid transparent; margin-bottom: -1px; }
.tabs button.active { color: #2563eb; border-bottom-color: #2563eb; font-weight: 600; }
.tab-content { flex: 1; overflow-y: auto; padding: 12px 16px; }
.param-group { margin-bottom: 10px; }
.param-group label { display: block; font-size: 12px; color: #64748b; margin-bottom: 4px; font-weight: 500; }
.input { width: 100%; padding: 9px; border: 1px solid #e2e8f0; border-radius: 6px; font-size: 13px; box-sizing: border-box; }
.input.small { flex: 1; }
.coord-inputs { display: flex; gap: 8px; margin-bottom: 8px; }
.action-btn { width: 100%; padding: 10px; background: #2563eb; color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 13px; margin-top: 6px; }
.action-btn.secondary { background: #7c3aed; margin-top: 6px; }
.result-box { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 12px; margin-top: 12px; }
.result-label { font-size: 12px; color: #64748b; margin-bottom: 6px; }
.result-val { font-size: 14px; font-weight: 600; color: #1e293b; font-family: monospace; }
.result-val.large { font-size: 20px; }
.crs-list { display: flex; flex-direction: column; gap: 6px; max-height: 300px; overflow-y: auto; }
.crs-item { display: flex; gap: 8px; align-items: center; padding: 8px; background: #f8fafc; border-radius: 6px; border: 1px solid #e2e8f0; font-size: 12px; }
.crs-item strong { font-family: monospace; color: #2563eb; flex-shrink: 0; }
.crs-item span { color: #64748b; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.crs-type { padding: 2px 6px; background: #e0e7ff; color: #3730a3; border-radius: 4px; font-size: 10px; flex-shrink: 0; }
</style>
