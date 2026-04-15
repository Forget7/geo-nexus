<template>
  <div class="geom-val-panel">
    <div class="panel-header">
      <h3>📐 {{ t('geomVal.title') }}</h3>
    </div>

    <div class="tabs">
      <button :class="{ active: tab === 'validate' }" @click="tab = 'validate'">{{ t('geomVal.validate') }}</button>
      <button :class="{ active: tab === 'repair' }" @click="tab = 'repair'">{{ t('geomVal.repair') }}</button>
      <button :class="{ active: tab === 'simple' }" @click="tab = 'simple'">{{ t('geomVal.simpleCheck') }}</button>
    </div>

    <!-- 验证 -->
    <div v-if="tab === 'validate'" class="tab-content">
      <div class="format-toggle">
        <button :class="{ active: inputFormat === 'geojson' }" @click="inputFormat = 'geojson'">GeoJSON</button>
        <button :class="{ active: inputFormat === 'wkt' }" @click="inputFormat = 'wkt'">WKT</button>
      </div>

      <div class="param-group">
        <label>{{ t('geomVal.geometry') }}</label>
        <textarea v-model="validateInput" :placeholder="inputFormat === 'geojson' ? t('geomVal.geojsonPlaceholder') : t('geomVal.wktPlaceholder')" class="input textarea" rows="5"></textarea>
      </div>

      <div class="btn-row">
        <button @click="quickValidate" class="action-btn">{{ t('geomVal.quickCheck') }}</button>
        <button @click="fullValidate" class="action-btn secondary">{{ t('geomVal.fullValidate') }}</button>
      </div>

      <div v-if="validateResult" class="result-box" :class="validateResult.valid ? 'success' : 'error'">
        <div class="result-icon">{{ validateResult.valid ? '✅' : '❌' }}</div>
        <div class="result-text">
          <strong>{{ validateResult.valid ? t('geomVal.valid') : t('geomVal.invalid') }}</strong>
          <p v-if="validateResult.message">{{ validateResult.message }}</p>
        </div>
      </div>
    </div>

    <!-- 修复 -->
    <div v-if="tab === 'repair'" class="tab-content">
      <div class="format-toggle">
        <button :class="{ active: inputFormat === 'geojson' }" @click="inputFormat = 'geojson'">GeoJSON</button>
        <button :class="{ active: inputFormat === 'wkt' }" @click="inputFormat = 'wkt'">WKT</button>
      </div>

      <div class="param-group">
        <label>{{ t('geomVal.inputGeometry') }}</label>
        <textarea v-model="repairInput" :placeholder="inputFormat === 'geojson' ? t('geomVal.geojsonPlaceholder') : t('geomVal.wktPlaceholder')" class="input textarea" rows="5"></textarea>
      </div>

      <button @click="makeValid" class="action-btn" :disabled="repairing">
        {{ repairing ? t('geomVal.repairing') : t('geomVal.repair') }}
      </button>

      <div v-if="repairResult" class="result-box" :class="repairResult.fixedValid ? 'success' : 'error'">
        <div class="result-icon">{{ repairResult.fixedValid ? '✅' : '⚠️' }}</div>
        <div class="result-text">
          <p>{{ t('geomVal.originalValid') }}: {{ repairResult.originalValid ? '✅' : '❌' }}</p>
          <p>{{ t('geomVal.fixedValid') }}: {{ repairResult.fixedValid ? '✅' : '❌' }}</p>
        </div>
        <div v-if="repairResult.geometry" class="fixed-geometry">
          <label>{{ t('geomVal.fixedResult') }}:</label>
          <pre>{{ JSON.stringify(repairResult.geometry, null, 2) }}</pre>
        </div>
      </div>
    </div>

    <!-- 简单性检查 -->
    <div v-if="tab === 'simple'" class="tab-content">
      <div class="format-toggle">
        <button :class="{ active: inputFormat === 'geojson' }" @click="inputFormat = 'geojson'">GeoJSON</button>
        <button :class="{ active: inputFormat === 'wkt' }" @click="inputFormat = 'wkt'">WKT</button>
      </div>

      <div class="param-group">
        <label>{{ t('geomVal.geometry') }}</label>
        <textarea v-model="simpleInput" :placeholder="inputFormat === 'geojson' ? t('geomVal.geojsonPlaceholder') : t('geomVal.wktPlaceholder')" class="input textarea" rows="5"></textarea>
      </div>

      <button @click="checkSimple" class="action-btn">{{ t('geomVal.check') }}</button>

      <div v-if="simpleResult !== null" class="result-box" :class="simpleResult ? 'success' : 'error'">
        <div class="result-icon">{{ simpleResult ? '✅' : '❌' }}</div>
        <div class="result-text">
          <strong>{{ simpleResult ? t('geomVal.isSimple') : t('geomVal.notSimple') }}</strong>
        </div>
      </div>
    </div>

    <!-- 预设示例 -->
    <div class="examples-section">
      <h4>{{ t('geomVal.examples') }}</h4>
      <div class="example-btns">
        <button v-for="ex in examples" :key="ex.label" @click="loadExample(ex)" class="example-btn">{{ ex.icon }} {{ ex.label }}</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const t = (key: string) => key
const tab = ref('validate')
const inputFormat = ref<'geojson' | 'wkt'>('geojson')
const validateInput = ref('')
const repairInput = ref('')
const simpleInput = ref('')
const validateResult = ref<any>(null)
const repairResult = ref<any>(null)
const simpleResult = ref<boolean | null>(null)
const repairing = ref(false)

const examples = [
  {
    label: 'Valid Polygon',
    icon: '✅',
    type: 'geojson',
    geojson: '{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,1],[0,0]]]}',
    wkt: 'POLYGON ((0 0, 1 0, 1 1, 0 1, 0 0))'
  },
  {
    label: 'Self-Intersecting',
    icon: '❌',
    type: 'geojson',
    geojson: '{"type":"Polygon","coordinates":[[[0,0],[2,2],[2,0],[0,2],[0,0]]]}',
    wkt: 'POLYGON ((0 0, 2 2, 2 0, 0 2, 0 0))'
  },
  {
    label: 'Simple Line',
    icon: '✅',
    type: 'geojson',
    geojson: '{"type":"LineString","coordinates":[[0,0],[1,1],[2,0]]}',
    wkt: 'LINESTRING (0 0, 1 1, 2 0)'
  }
]

function loadExample(ex: any) {
  inputFormat.value = ex.type as 'geojson' | 'wkt'
  if (tab.value === 'validate') {
    validateInput.value = ex[ex.type]
  } else if (tab.value === 'repair') {
    repairInput.value = ex[ex.type]
  } else {
    simpleInput.value = ex[ex.type]
  }
}

function buildPayload(geoStr: string) {
  if (inputFormat.value === 'wkt') {
    return { geometry: geoStr }
  }
  try {
    return { geometry: JSON.parse(geoStr) }
  } catch {
    return { geometry: geoStr }
  }
}

async function quickValidate() {
  if (!validateInput.value.trim()) return
  validateResult.value = null
  try {
    const { data } = await apiClient.post('/geometry/is-valid', buildPayload(validateInput.value))
    validateResult.value = data
  } catch {}
}

async function fullValidate() {
  if (!validateInput.value.trim()) return
  validateResult.value = null
  try {
    const { data } = await apiClient.post('/geometry/validate', buildPayload(validateInput.value))
    validateResult.value = data
  } catch {}
}

async function makeValid() {
  if (!repairInput.value.trim()) return
  repairResult.value = null
  repairing.value = true
  try {
    const { data } = await apiClient.post('/geometry/make-valid', buildPayload(repairInput.value))
    repairResult.value = data
  } catch {} finally {
    repairing.value = false
  }
}

async function checkSimple() {
  if (!simpleInput.value.trim()) return
  simpleResult.value = null
  try {
    const { data } = await apiClient.post('/geometry/is-simple', buildPayload(simpleInput.value))
    simpleResult.value = data.simple
  } catch {}
}
</script>

<style scoped>
.geom-val-panel { background: white; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.15); max-height: 80vh; overflow: hidden; display: flex; flex-direction: column; }
.panel-header { padding: 12px 16px; border-bottom: 1px solid #e2e8f0; }
.panel-header h3 { font-size: 15px; font-weight: 600; }
.tabs { display: flex; border-bottom: 1px solid #e2e8f0; }
.tabs button { flex: 1; padding: 10px; border: none; background: none; cursor: pointer; font-size: 13px; color: #64748b; border-bottom: 2px solid transparent; margin-bottom: -1px; }
.tabs button.active { color: #2563eb; border-bottom-color: #2563eb; font-weight: 600; }
.tab-content { flex: 1; overflow-y: auto; padding: 12px 16px; }
.format-toggle { display: flex; gap: 4px; margin-bottom: 10px; }
.format-toggle button { flex: 1; padding: 6px; border: 1px solid #e2e8f0; border-radius: 6px; background: white; cursor: pointer; font-size: 12px; color: #64748b; }
.format-toggle button.active { background: #2563eb; color: white; border-color: #2563eb; }
.param-group { margin-bottom: 10px; }
.param-group label { display: block; font-size: 12px; color: #64748b; margin-bottom: 4px; font-weight: 500; }
.input { width: 100%; padding: 9px; border: 1px solid #e2e8f0; border-radius: 6px; font-size: 13px; box-sizing: border-box; }
.input.textarea { resize: vertical; font-family: monospace; font-size: 12px; }
.btn-row { display: flex; gap: 8px; }
.action-btn { flex: 1; padding: 10px; background: #2563eb; color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 13px; margin-top: 6px; }
.action-btn:disabled { background: #93c5fd; cursor: not-allowed; }
.action-btn.secondary { background: #7c3aed; }
.result-box { display: flex; gap: 10px; padding: 12px; border-radius: 8px; margin-top: 12px; align-items: flex-start; }
.result-box.success { background: #f0fdf4; border: 1px solid #bbf7d0; }
.result-box.error { background: #fef2f2; border: 1px solid #fecaca; }
.result-icon { font-size: 20px; flex-shrink: 0; }
.result-text { font-size: 13px; }
.result-text strong { display: block; margin-bottom: 4px; }
.result-text p { margin: 0; font-size: 12px; color: #475569; }
.fixed-geometry { margin-top: 8px; width: 100%; }
.fixed-geometry label { font-size: 11px; color: #64748b; display: block; margin-bottom: 4px; }
.fixed-geometry pre { background: #f8fafc; padding: 8px; border-radius: 6px; font-size: 11px; overflow-x: auto; max-height: 150px; }
.examples-section { border-top: 1px solid #e2e8f0; padding: 10px 16px; }
.examples-section h4 { font-size: 11px; color: #94a3b8; margin-bottom: 6px; font-weight: 600; text-transform: uppercase; }
.example-btns { display: flex; flex-wrap: wrap; gap: 6px; }
.example-btn { padding: 5px 10px; border: 1px solid #e2e8f0; border-radius: 20px; background: white; cursor: pointer; font-size: 11px; color: #475569; }
.example-btn:hover { border-color: #2563eb; color: #2563eb; }
</style>
