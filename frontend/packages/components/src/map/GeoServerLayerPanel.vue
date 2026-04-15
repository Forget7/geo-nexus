<template>
  <div class="gs-layer-panel">
    <div class="panel-header">
      <h3>🗺️ {{ t('gsLayer.title') }}</h3>
    </div>

    <div class="tabs">
      <button :class="{ active: tab === 'store' }" @click="tab = 'store'">{{ t('gsLayer.createStore') }}</button>
      <button :class="{ active: tab === 'publish' }" @click="tab = 'publish'">{{ t('gsLayer.publish') }}</button>
      <button :class="{ active: tab === 'info' }" @click="tab = 'info'">{{ t('gsLayer.layerInfo') }}</button>
    </div>

    <!-- 创建数据存储 -->
    <div v-if="tab === 'store'" class="tab-content">
      <h4 class="section-title">{{ t('gsLayer.postgisStore') }}</h4>
      <div class="param-group">
        <label>{{ t('gsLayer.storeName') }}</label>
        <input v-model="postgisStore.storeName" :placeholder="t('gsLayer.storeNamePlaceholder')" class="input" />
      </div>
      <div class="param-group">
        <label>{{ t('gsLayer.host') }}</label>
        <input v-model="postgisStore.host" placeholder="localhost" class="input" />
      </div>
      <div class="param-group">
        <label>{{ t('gsLayer.database') }}</label>
        <input v-model="postgisStore.database" placeholder="geonexus" class="input" />
      </div>
      <div class="param-row">
        <div class="param-group">
          <label>{{ t('gsLayer.port') }}</label>
          <input v-model.number="postgisStore.port" type="number" placeholder="5432" class="input" />
        </div>
        <div class="param-group">
          <label>{{ t('gsLayer.user') }}</label>
          <input v-model="postgisStore.user" placeholder="postgres" class="input" />
        </div>
      </div>
      <div class="param-group">
        <label>{{ t('gsLayer.password') }}</label>
        <input v-model="postgisStore.password" type="password" placeholder="••••••••" class="input" />
      </div>
      <button @click="createPostGISStore" class="action-btn" :disabled="creating">
        {{ creating ? t('gsLayer.creating') : t('gsLayer.create') }}
      </button>
      <div v-if="storeResult" class="result-box" :class="storeResult.success ? 'success' : 'error'">
        {{ storeResult.message }}
      </div>
    </div>

    <!-- 发布图层 -->
    <div v-if="tab === 'publish'" class="tab-content">
      <h4 class="section-title">{{ t('gsLayer.publishFeatureType') }}</h4>
      <div class="param-group">
        <label>{{ t('gsLayer.storeName') }}</label>
        <input v-model="publish.storeName" :placeholder="t('gsLayer.storeNamePlaceholder')" class="input" />
      </div>
      <div class="param-group">
        <label>{{ t('gsLayer.featureName') }}</label>
        <input v-model="publish.featureName" :placeholder="t('gsLayer.featureNamePlaceholder')" class="input" />
      </div>
      <div class="param-group">
        <label>{{ t('gsLayer.srs') }}</label>
        <input v-model="publish.srs" placeholder="EPSG:4326" class="input" />
      </div>
      <div class="param-group">
        <label>{{ t('gsLayer.bbox') }} ({{ t('gsLayer.optional') }})</label>
        <div class="bbox-inputs">
          <input v-model.number="publish.bbox[0]" type="number" step="any" placeholder="minX" class="input small" />
          <input v-model.number="publish.bbox[1]" type="number" step="any" placeholder="minY" class="input small" />
          <input v-model.number="publish.bbox[2]" type="number" step="any" placeholder="maxX" class="input small" />
          <input v-model.number="publish.bbox[3]" type="number" step="any" placeholder="maxY" class="input small" />
        </div>
      </div>
      <button @click="publishFeatureType" class="action-btn" :disabled="publishing">
        {{ publishing ? t('gsLayer.publishing') : t('gsLayer.publish') }}
      </button>
      <div v-if="publishResult" class="result-box" :class="publishResult.success ? 'success' : 'error'">
        {{ publishResult.message }}
      </div>
    </div>

    <!-- 图层信息 -->
    <div v-if="tab === 'info'" class="tab-content">
      <h4 class="section-title">{{ t('gsLayer.layerInfo') }}</h4>
      <div class="param-group">
        <label>{{ t('gsLayer.layerName') }}</label>
        <input v-model="infoLayerName" :placeholder="t('gsLayer.layerNamePlaceholder')" class="input" />
        <button @click="fetchLayerInfo" class="action-btn secondary" :disabled="loadingInfo">{{ t('gsLayer.query') }}</button>
      </div>
      <div v-if="layerInfo" class="info-display">
        <div v-for="(value, key) in layerInfo" :key="key" class="info-row">
          <span class="info-key">{{ key }}:</span>
          <span class="info-val">{{ typeof value === 'object' ? JSON.stringify(value) : value }}</span>
        </div>
      </div>
      <div v-if="infoError" class="result-box error">{{ infoError }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'

const t = (key: string) => key
const tab = ref('store')
const creating = ref(false)
const publishing = ref(false)
const loadingInfo = ref(false)
const storeResult = ref<any>(null)
const publishResult = ref<any>(null)
const layerInfo = ref<any>(null)
const infoError = ref('')
const infoLayerName = ref('')

const postgisStore = reactive({
  storeName: '',
  host: 'localhost',
  port: 5432,
  database: 'geonexus',
  user: 'postgres',
  password: ''
})

const publish = reactive({
  storeName: '',
  featureName: '',
  srs: 'EPSG:4326',
  bbox: [null, null, null, null]
})

async function createPostGISStore() {
  if (!postgisStore.storeName || !postgisStore.database || !postgisStore.host || !postgisStore.user) {
    storeResult.value = { success: false, message: t('gsLayer.fillRequired') }
    return
  }
  creating.value = true
  storeResult.value = null
  try {
    const { data } = await apiClient.post('/geoserver/layers/stores/postgis', {
      storeName: postgisStore.storeName,
      database: postgisStore.database,
      host: postgisStore.host,
      port: postgisStore.port,
      user: postgisStore.user,
      password: postgisStore.password
    })
    storeResult.value = data
  } catch (e: any) {
    storeResult.value = { success: false, message: e.response?.data?.error || t('gsLayer.createFailed') }
  } finally {
    creating.value = false
  }
}

async function publishFeatureType() {
  if (!publish.storeName || !publish.featureName) {
    publishResult.value = { success: false, message: t('gsLayer.fillRequired') }
    return
  }
  publishing.value = true
  publishResult.value = null
  try {
    const request: any = {
      storeName: publish.storeName,
      featureName: publish.featureName,
      srs: publish.srs
    }
    if (publish.bbox[0] != null && publish.bbox[1] != null && publish.bbox[2] != null && publish.bbox[3] != null) {
      request.bbox = publish.bbox
    }
    const { data } = await apiClient.post('/geoserver/layers/publish', request)
    publishResult.value = data
  } catch (e: any) {
    publishResult.value = { success: false, message: e.response?.data?.error || t('gsLayer.publishFailed') }
  } finally {
    publishing.value = false
  }
}

async function fetchLayerInfo() {
  if (!infoLayerName.value) {
    infoError.value = t('gsLayer.enterLayerName')
    return
  }
  loadingInfo.value = true
  layerInfo.value = null
  infoError.value = ''
  try {
    const { data } = await apiClient.get(`/geoserver/layers/${encodeURIComponent(infoLayerName.value)}/info`)
    layerInfo.value = data
  } catch {
    infoError.value = t('gsLayer.layerNotFound')
  } finally {
    loadingInfo.value = false
  }
}
</script>

<style scoped>
.gs-layer-panel { background: white; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.15); max-height: 80vh; overflow: hidden; display: flex; flex-direction: column; }
.panel-header { padding: 12px 16px; border-bottom: 1px solid #e2e8f0; }
.panel-header h3 { font-size: 15px; font-weight: 600; }
.tabs { display: flex; border-bottom: 1px solid #e2e8f0; }
.tabs button { flex: 1; padding: 10px; border: none; background: none; cursor: pointer; font-size: 13px; color: #64748b; border-bottom: 2px solid transparent; margin-bottom: -1px; }
.tabs button.active { color: #2563eb; border-bottom-color: #2563eb; font-weight: 600; }
.tab-content { flex: 1; overflow-y: auto; padding: 12px 16px; }
.section-title { font-size: 12px; color: #64748b; margin-bottom: 10px; font-weight: 600; }
.param-group { margin-bottom: 10px; }
.param-group label { display: block; font-size: 12px; color: #64748b; margin-bottom: 4px; font-weight: 500; }
.param-row { display: flex; gap: 8px; }
.param-row .param-group { flex: 1; }
.input { width: 100%; padding: 9px; border: 1px solid #e2e8f0; border-radius: 6px; font-size: 13px; box-sizing: border-box; }
.input.small { flex: 1; }
.bbox-inputs { display: flex; gap: 6px; }
.action-btn { width: 100%; padding: 10px; background: #2563eb; color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 13px; margin-top: 6px; }
.action-btn:disabled { background: #93c5fd; cursor: not-allowed; }
.action-btn.secondary { background: #7c3aed; margin-top: 6px; }
.action-btn.secondary:disabled { background: #c4b5fd; cursor: not-allowed; }
.result-box { padding: 10px; border-radius: 6px; font-size: 13px; margin-top: 10px; }
.result-box.success { background: #dcfce7; color: #16a34a; border: 1px solid #bbf7d0; }
.result-box.error { background: #fef2f2; color: #dc2626; border: 1px solid #fecaca; }
.info-display { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 12px; margin-top: 10px; }
.info-row { display: flex; gap: 8px; padding: 4px 0; font-size: 12px; border-bottom: 1px solid #f1f5f9; }
.info-row:last-child { border-bottom: none; }
.info-key { font-weight: 600; color: #475569; flex-shrink: 0; }
.info-val { color: #1e293b; word-break: break-all; font-family: monospace; font-size: 11px; }
</style>
