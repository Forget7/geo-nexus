<template>
  <div class="geo-panel">
    <div class="panel-header">
      <h3>🌍 {{ t('geo.title') }}</h3>
    </div>

    <div class="tabs">
      <button :class="{ active: tab === 'geocode' }" @click="tab = 'geocode'">{{ t('geo.geocode') }}</button>
      <button :class="{ active: tab === 'route' }" @click="tab = 'route'">{{ t('geo.route') }}</button>
      <button :class="{ active: tab === 'service' }" @click="tab = 'service'">{{ t('geo.serviceArea') }}</button>
    </div>

    <!-- 地理编码 -->
    <div v-if="tab === 'geocode'" class="tab-content">
      <div class="input-section">
        <input v-model="address" :placeholder="t('geo.addressHint')" class="input" @keyup.enter="geocode" />
        <button @click="geocode" class="action-btn">{{ t('geo.geocode') }}</button>
      </div>
      <div v-if="geocodeResult" class="result-card">
        <div class="result-item">
          <label>{{ t('geo.address') }}:</label>
          <span>{{ geocodeResult.formattedAddress }}</span>
        </div>
        <div class="result-item">
          <label>{{ t('geo.coordinates') }}:</label>
          <span class="coords">{{ geocodeResult.lon?.toFixed(6) }}, {{ geocodeResult.lat?.toFixed(6) }}</span>
        </div>
      </div>

      <div class="divider">{{ t('geo.reverseGeocode') }}</div>
      <div class="input-section">
        <div class="coord-inputs">
          <input v-model.number="revLon" type="number" step="0.000001" :placeholder="t('geo.longitude')" class="input small" />
          <input v-model.number="revLat" type="number" step="0.000001" :placeholder="t('geo.latitude')" class="input small" />
        </div>
        <button @click="reverseGeocode" class="action-btn">{{ t('geo.reverse') }}</button>
      </div>
      <div v-if="revResult" class="result-card">
        <div class="result-item">
          <label>{{ t('geo.address') }}:</label>
          <span>{{ revResult.formattedAddress }}</span>
        </div>
      </div>
    </div>

    <!-- 路线规划 -->
    <div v-if="tab === 'route'" class="tab-content">
      <div class="input-section">
        <h4>{{ t('geo.stops') }}</h4>
        <div v-for="(stop, i) in routeStops" :key="i" class="stop-row">
          <input v-model="stop.name" :placeholder="t('geo.stopName')" class="input" />
          <input v-model.number="stop.lon" type="number" step="0.000001" :placeholder="t('geo.lon')" class="input small" />
          <input v-model.number="stop.lat" type="number" step="0.000001" :placeholder="t('geo.lat')" class="input small" />
          <button @click="removeStop(i)" v-if="routeStops.length > 1" class="rm-btn">✕</button>
        </div>
        <button @click="addStop" class="add-btn">+ {{ t('geo.addStop') }}</button>
        <div class="mode-select">
          <label>{{ t('geo.mode') }}:</label>
          <select v-model="routeMode">
            <option value="driving">🚗 {{ t('geo.driving') }}</option>
            <option value="walking">🚶 {{ t('geo.walking') }}</option>
            <option value="cycling">🚴 {{ t('geo.cycling') }}</option>
          </select>
        </div>
        <button @click="calculateRoute" class="action-btn">{{ t('geo.calculateRoute') }}</button>
      </div>
      <div v-if="routeResult" class="result-card">
        <div class="result-item">
          <label>{{ t('geo.distance') }}:</label>
          <span>{{ (routeResult.distance / 1000).toFixed(2) }} km</span>
        </div>
        <div class="result-item">
          <label>{{ t('geo.duration') }}:</label>
          <span>{{ Math.round(routeResult.duration / 60) }} min</span>
        </div>
      </div>
    </div>

    <!-- 服务区 -->
    <div v-if="tab === 'service'" class="tab-content">
      <div class="input-section">
        <div class="coord-row">
          <input v-model.number="serviceCenter.lon" type="number" step="0.000001" :placeholder="t('geo.lon')" class="input" />
          <input v-model.number="serviceCenter.lat" type="number" step="0.000001" :placeholder="t('geo.lat')" class="input" />
        </div>
        <input v-model.number="serviceRadius" type="number" :placeholder="t('geo.radiusKm')" class="input" />
        <div class="mode-select">
          <label>{{ t('geo.mode') }}:</label>
          <select v-model="serviceMode">
            <option value="driving">🚗 {{ t('geo.driving') }}</option>
            <option value="walking">🚶 {{ t('geo.walking') }}</option>
          </select>
        </div>
        <button @click="calculateServiceArea" class="action-btn">{{ t('geo.analyze') }}</button>
      </div>
      <div v-if="serviceResult" class="result-card">
        <div class="result-item">
          <label>{{ t('geo.intervals') }}:</label>
          <span>{{ serviceResult.intervals?.length || 0 }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const t = (key: string) => key
const tab = ref('geocode')
const address = ref('')
const geocodeResult = ref<any>(null)
const revLon = ref()
const revLat = ref()
const revResult = ref<any>(null)
const routeStops = ref([{ name: '', lon: null, lat: null }])
const routeMode = ref('driving')
const routeResult = ref<any>(null)
const serviceCenter = ref({ lon: null, lat: null })
const serviceRadius = ref(5)
const serviceMode = ref('driving')
const serviceResult = ref<any>(null)

async function geocode() {
  if (!address.value) return
  try {
    const { data } = await apiClient.get(`/geo/geocode?address=${encodeURIComponent(address.value)}`)
    geocodeResult.value = data
  } catch {}
}

async function reverseGeocode() {
  if (!revLon.value || !revLat.value) return
  try {
    const { data } = await apiClient.get(`/geo/reverse-geocode?lon=${revLon.value}&lat=${revLat.value}`)
    revResult.value = data
  } catch {}
}

function addStop() {
  routeStops.value.push({ name: '', lon: null, lat: null })
}

function removeStop(i: number) {
  routeStops.value.splice(i, 1)
}

async function calculateRoute() {
  try {
    const { data } = await apiClient.post('/geo/route/multi-stop', {
      stops: routeStops.value.filter(s => s.lon && s.lat),
      mode: routeMode.value
    })
    routeResult.value = data
  } catch {}
}

async function calculateServiceArea() {
  try {
    const { data } = await apiClient.post('/geo/service-area', {
      center: serviceCenter.value,
      radius: serviceRadius.value,
      mode: serviceMode.value,
      intervals: 3
    })
    serviceResult.value = data
  } catch {}
}
</script>

<style scoped>
.geo-panel { background: white; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.15); max-height: 80vh; overflow: hidden; display: flex; flex-direction: column; }
.panel-header { padding: 12px 16px; border-bottom: 1px solid #e2e8f0; }
.panel-header h3 { font-size: 15px; font-weight: 600; }
.tabs { display: flex; border-bottom: 1px solid #e2e8f0; }
.tabs button { flex: 1; padding: 10px; border: none; background: none; cursor: pointer; font-size: 13px; color: #64748b; border-bottom: 2px solid transparent; margin-bottom: -1px; }
.tabs button.active { color: #2563eb; border-bottom-color: #2563eb; font-weight: 600; }
.tab-content { flex: 1; overflow-y: auto; padding: 12px 16px; }
.input-section { display: flex; flex-direction: column; gap: 8px; }
.input { width: 100%; padding: 9px; border: 1px solid #e2e8f0; border-radius: 6px; font-size: 13px; box-sizing: border-box; }
.input.small { flex: 1; }
.coord-inputs { display: flex; gap: 8px; }
.coord-row { display: flex; gap: 8px; }
.mode-select { display: flex; align-items: center; gap: 10px; }
.mode-select label { font-size: 13px; color: #64748b; white-space: nowrap; }
.mode-select select { flex: 1; padding: 8px; border: 1px solid #e2e8f0; border-radius: 6px; font-size: 13px; }
.action-btn { width: 100%; padding: 10px; background: #2563eb; color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 13px; }
.stop-row { display: flex; gap: 8px; align-items: center; }
.stop-row .input { flex: 1; }
.stop-row .input.small { flex: 0 0 100px; }
.rm-btn { padding: 6px 8px; border: 1px solid #fecaca; border-radius: 4px; background: white; color: #dc2626; cursor: pointer; flex-shrink: 0; }
.add-btn { width: 100%; padding: 8px; border: 1px dashed #e2e8f0; border-radius: 6px; background: none; cursor: pointer; font-size: 12px; color: #64748b; }
.divider { font-size: 12px; color: #94a3b8; text-align: center; margin: 12px 0; }
.result-card { background: #f8fafc; border-radius: 8px; padding: 12px; border: 1px solid #e2e8f0; margin-top: 8px; }
.result-item { display: flex; gap: 8px; margin-bottom: 6px; font-size: 13px; }
.result-item:last-child { margin-bottom: 0; }
.result-item label { color: #64748b; flex-shrink: 0; }
.result-item span { color: #1e293b; font-weight: 500; }
.coords { font-family: monospace; font-size: 12px; }
</style>
