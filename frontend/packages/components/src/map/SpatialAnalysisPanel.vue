<template>
  <div class="gn-spatial-panel">
    <div class="panel-header">
      <h3>📊 {{ t('spatial.title') || '空间分析' }}</h3>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane :label="t('spatial.buffer')" name="buffer">
        <div class="tool-section">
          <label>{{ t('spatial.geometry') }} (GeoJSON)</label>
          <el-input
            v-model="bufferGeometry"
            type="textarea"
            rows="4"
            :placeholder="t('spatial.geometryHint')"
          />

          <label>{{ t('spatial.distanceKm') }}</label>
          <el-input-number v-model="bufferDistance" :min="0.1" :max="100" :step="0.1" />

          <el-button type="primary" :loading="loading" @click="executeBuffer">
            {{ t('spatial.execute') }}
          </el-button>
        </div>
      </el-tab-pane>

      <el-tab-pane :label="t('spatial.overlay')" name="overlay">
        <div class="tool-section">
          <label>{{ t('spatial.geometry1') }} (GeoJSON)</label>
          <el-input
            v-model="overlayGeom1"
            type="textarea"
            rows="3"
            :placeholder="t('spatial.geometryHint')"
          />

          <label>{{ t('spatial.geometry2') }} (GeoJSON)</label>
          <el-input
            v-model="overlayGeom2"
            type="textarea"
            rows="3"
            :placeholder="t('spatial.geometryHint')"
          />

          <label>{{ t('spatial.operation') }}</label>
          <el-select v-model="overlayOperation">
            <el-option label="Union" value="union" />
            <el-option label="Intersection" value="intersection" />
            <el-option label="Difference" value="difference" />
            <el-option label="Symmetric Difference" value="symDifference" />
          </el-select>

          <el-button type="primary" :loading="loading" @click="executeOverlay">
            {{ t('spatial.execute') }}
          </el-button>
        </div>
      </el-tab-pane>

      <el-tab-pane :label="t('spatial.filter')" name="filter">
        <div class="tool-section">
          <label>{{ t('spatial.bounds') }}</label>
          <div class="bounds-grid">
            <el-input-number v-model="bounds.south" placeholder="South" />
            <el-input-number v-model="bounds.west" placeholder="West" />
            <el-input-number v-model="bounds.north" placeholder="North" />
            <el-input-number v-model="bounds.east" placeholder="East" />
          </div>

          <label>{{ t('spatial.features') }} (GeoJSON FeatureCollection)</label>
          <el-input
            v-model="filterFeatures"
            type="textarea"
            rows="5"
            :placeholder="t('spatial.featuresHint')"
          />

          <el-button type="primary" :loading="loading" @click="executeFilter">
            {{ t('spatial.execute') }}
          </el-button>
        </div>
      </el-tab-pane>

      <el-tab-pane :label="t('spatial.join')" name="join">
        <div class="tool-section">
          <label>{{ t('spatial.features1') }}</label>
          <el-input
            v-model="joinFeatures1"
            type="textarea"
            rows="4"
            :placeholder="t('spatial.featuresHint')"
          />

          <label>{{ t('spatial.features2') }}</label>
          <el-input
            v-model="joinFeatures2"
            type="textarea"
            rows="4"
            :placeholder="t('spatial.featuresHint')"
          />

          <label>{{ t('spatial.predicate') }}</label>
          <el-select v-model="joinPredicate">
            <el-option label="Intersects" value="intersects" />
            <el-option label="Contains" value="contains" />
            <el-option label="Within" value="within" />
            <el-option label="Equals" value="equals" />
            <el-option label="Distance" value="distance" />
          </el-select>

          <el-button type="primary" :loading="loading" @click="executeJoin">
            {{ t('spatial.execute') }}
          </el-button>
        </div>
      </el-tab-pane>
    </el-tabs>

    <div v-if="result" class="result-section">
      <h4>{{ t('spatial.result') }}</h4>
      <pre>{{ JSON.stringify(result, null, 2) }}</pre>
    </div>

    <div v-if="error" class="error-section">
      <el-alert type="error" :title="error" show-icon :closable="false" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useI18n } from 'vue-i18n'
import { spatialApi } from '@/service/api'
import { ElMessage } from 'element-plus'

defineOptions({ name: 'GnSpatialAnalysisPanel' })

const { t } = useI18n()

const activeTab = ref('buffer')
const loading = ref(false)
const result = ref<any>(null)
const error = ref('')

// Buffer
const bufferGeometry = ref('')
const bufferDistance = ref(1)

// Overlay
const overlayGeom1 = ref('')
const overlayGeom2 = ref('')
const overlayOperation = ref('union')

// Filter
const bounds = reactive({ south: 0, west: 0, north: 0, east: 0 })
const filterFeatures = ref('')

// Join
const joinFeatures1 = ref('')
const joinFeatures2 = ref('')
const joinPredicate = ref('intersects')

async function executeBuffer() {
  error.value = ''
  result.value = null
  if (!bufferGeometry.value) {
    error.value = t('spatial.geometryRequired')
    return
  }

  loading.value = true
  try {
    const geometry = JSON.parse(bufferGeometry.value)
    const response = await spatialApi.buffer({ geometry, distanceKm: bufferDistance.value })
    if (response.data?.success) {
      result.value = response.data.data
    } else {
      error.value = response.data?.message || 'Buffer failed'
    }
  } catch (e: any) {
    error.value = e.message || 'Invalid geometry JSON'
  } finally {
    loading.value = false
  }
}

async function executeOverlay() {
  error.value = ''
  result.value = null
  if (!overlayGeom1.value || !overlayGeom2.value) {
    error.value = t('spatial.bothGeometriesRequired')
    return
  }

  loading.value = true
  try {
    const geometry1 = JSON.parse(overlayGeom1.value)
    const geometry2 = JSON.parse(overlayGeom2.value)
    const endpoint = overlayOperation.value === 'symDifference' ? 'sym-difference' : overlayOperation.value
    const response = await (spatialApi as any)[endpoint]({ geometry1, geometry2 })
    if (response.data?.success) {
      result.value = response.data.data
    } else {
      error.value = response.data?.message || 'Overlay failed'
    }
  } catch (e: any) {
    error.value = e.message || 'Invalid geometry JSON'
  } finally {
    loading.value = false
  }
}

async function executeFilter() {
  error.value = ''
  result.value = null
  if (!filterFeatures.value) {
    error.value = t('spatial.featuresRequired')
    return
  }

  loading.value = true
  try {
    const features = JSON.parse(filterFeatures.value)
    const response = await spatialApi.filterByBounds({
      features,
      ...bounds
    })
    if (response.data?.success) {
      result.value = { count: response.data.data.length, features: response.data.data }
    } else {
      error.value = response.data?.message || 'Filter failed'
    }
  } catch (e: any) {
    error.value = e.message || 'Invalid features JSON'
  } finally {
    loading.value = false
  }
}

async function executeJoin() {
  error.value = ''
  result.value = null
  if (!joinFeatures1.value || !joinFeatures2.value) {
    error.value = t('spatial.bothFeatureSetsRequired')
    return
  }

  loading.value = true
  try {
    const features1 = JSON.parse(joinFeatures1.value)
    const features2 = JSON.parse(joinFeatures2.value)
    const response = await spatialApi.spatialJoin({
      features1,
      features2,
      predicate: joinPredicate.value
    })
    if (response.data?.success) {
      result.value = { count: response.data.data.length, joined: response.data.data }
    } else {
      error.value = response.data?.message || 'Join failed'
    }
  } catch (e: any) {
    error.value = e.message || 'Invalid features JSON'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.gn-spatial-panel {
  padding: 16px;
}

.panel-header {
  margin-bottom: 16px;
}

.panel-header h3 {
  margin: 0;
  font-size: 16px;
}

.tool-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tool-section label {
  font-weight: 500;
  font-size: 13px;
}

.bounds-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.result-section {
  margin-top: 16px;
  padding: 12px;
  background: var(--bg-secondary, #f5f5f5);
  border-radius: 8px;
}

.result-section h4 {
  margin: 0 0 8px;
  font-size: 14px;
}

.result-section pre {
  margin: 0;
  font-size: 12px;
  overflow-x: auto;
}

.error-section {
  margin-top: 16px;
}
</style>
