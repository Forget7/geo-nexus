<template>
  <div class="gn-gis-panel">
    <div class="panel-header">
      <h3>🗺️ {{ t('gisProcessing.title') || 'GIS批处理' }}</h3>
    </div>

    <el-tabs v-model="activeTab">
      <!-- 批量缓冲区 -->
      <el-tab-pane :label="t('gisProcessing.batchBuffer')" name="batchBuffer">
        <div class="tool-section">
          <label>{{ t('gisProcessing.geometries') }} (GeoJSON Features)</label>
          <el-input
            v-model="bufferGeometries"
            type="textarea"
            rows="6"
            :placeholder="t('gisProcessing.geometriesHint')"
          />
          <label>{{ t('gisProcessing.distanceKm') }}</label>
          <el-input-number v-model="bufferDistance" :min="0.1" :max="1000" :step="0.1" />
          <el-button type="primary" :loading="loading" @click="executeBatchBuffer">
            {{ t('gisProcessing.execute') }}
          </el-button>
        </div>
      </el-tab-pane>

      <!-- 批量距离计算 -->
      <el-tab-pane :label="t('gisProcessing.batchDistance')" name="batchDistance">
        <div class="tool-section">
          <label>{{ t('gisProcessing.points1') }}</label>
          <el-input
            v-model="points1"
            type="textarea"
            rows="4"
            :placeholder="t('gisProcessing.pointsHint')"
          />
          <label>{{ t('gisProcessing.points2') }}</label>
          <el-input
            v-model="points2"
            type="textarea"
            rows="4"
            :placeholder="t('gisProcessing.pointsHint')"
          />
          <el-button type="primary" :loading="loading" @click="executeBatchDistance">
            {{ t('gisProcessing.execute') }}
          </el-button>
        </div>
      </el-tab-pane>

      <!-- 网格聚合 -->
      <el-tab-pane :label="t('gisProcessing.gridAggregation')" name="gridAggregation">
        <div class="tool-section">
          <label>{{ t('gisProcessing.points') }} (GeoJSON Features)</label>
          <el-input
            v-model="aggPoints"
            type="textarea"
            rows="6"
            :placeholder="t('gisProcessing.pointsHint')"
          />
          <label>{{ t('gisProcessing.gridSizeDegrees') }}</label>
          <el-input-number v-model="gridSize" :min="0.0001" :max="1" :step="0.001" :precision="4" />
          <el-button type="primary" :loading="loading" @click="executeGridAggregation">
            {{ t('gisProcessing.execute') }}
          </el-button>
        </div>
      </el-tab-pane>

      <!-- 空间连接 -->
      <el-tab-pane :label="t('gisProcessing.spatialJoin')" name="spatialJoin">
        <div class="tool-section">
          <label>{{ t('gisProcessing.points') }} (GeoJSON Features)</label>
          <el-input
            v-model="joinPoints"
            type="textarea"
            rows="4"
            :placeholder="t('gisProcessing.pointsHint')"
          />
          <label>{{ t('gisProcessing.polygons') }} (GeoJSON Features)</label>
          <el-input
            v-model="joinPolygons"
            type="textarea"
            rows="4"
            :placeholder="t('gisProcessing.polygonsHint')"
          />
          <el-button type="primary" :loading="loading" @click="executeSpatialJoin">
            {{ t('gisProcessing.execute') }}
          </el-button>
        </div>
      </el-tab-pane>

      <!-- 路径简化 -->
      <el-tab-pane :label="t('gisProcessing.simplifyPath')" name="simplifyPath">
        <div class="tool-section">
          <label>{{ t('gisProcessing.coordinates') }}</label>
          <el-input
            v-model="simplifyCoords"
            type="textarea"
            rows="5"
            :placeholder="t('gisProcessing.coordsHint')"
          />
          <label>{{ t('gisProcessing.tolerance') }}</label>
          <el-input-number v-model="tolerance" :min="0.00001" :max="1" :step="0.001" :precision="5" />
          <el-button type="primary" :loading="loading" @click="executeSimplifyPath">
            {{ t('gisProcessing.execute') }}
          </el-button>
        </div>
      </el-tab-pane>
    </el-tabs>

    <div v-if="result" class="result-section">
      <h4>{{ t('gisProcessing.result') }}</h4>
      <pre>{{ JSON.stringify(result, null, 2) }}</pre>
    </div>

    <div v-if="error" class="error-section">
      <el-alert type="error" :title="error" show-icon :closable="false" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { gisProcessingApi } from '@/service/api'
import { ElMessage } from 'element-plus'

defineOptions({ name: 'GnGISProcessingPanel' })

const { t } = useI18n()

const activeTab = ref('batchBuffer')
const loading = ref(false)
const result = ref<any>(null)
const error = ref('')

// Buffer
const bufferGeometries = ref('')
const bufferDistance = ref(1.0)

// Distance
const points1 = ref('')
const points2 = ref('')

// Grid
const aggPoints = ref('')
const gridSize = ref(0.01)

// Spatial Join
const joinPoints = ref('')
const joinPolygons = ref('')

// Simplify
const simplifyCoords = ref('')
const tolerance = ref(0.001)

async function executeBatchBuffer() {
  error.value = ''
  result.value = null
  if (!bufferGeometries.value) {
    error.value = t('gisProcessing.geometriesRequired')
    return
  }
  loading.value = true
  try {
    const geometries = JSON.parse(bufferGeometries.value)
    const response = await gisProcessingApi.batchBuffer({ geometries, distanceKm: bufferDistance.value })
    if (response.data?.success) {
      result.value = response.data.data
    } else {
      error.value = response.data?.message || 'Batch buffer failed'
    }
  } catch (e: any) {
    error.value = e.message || t('gisProcessing.parseError')
  } finally {
    loading.value = false
  }
}

async function executeBatchDistance() {
  error.value = ''
  result.value = null
  if (!points1.value || !points2.value) {
    error.value = t('gisProcessing.bothPointsRequired')
    return
  }
  loading.value = true
  try {
    const pts1 = JSON.parse(points1.value)
    const pts2 = JSON.parse(points2.value)
    const response = await gisProcessingApi.batchDistance({ points1: pts1, points2: pts2 })
    if (response.data?.success) {
      result.value = response.data.data
    } else {
      error.value = response.data?.message || 'Batch distance failed'
    }
  } catch (e: any) {
    error.value = e.message || t('gisProcessing.parseError')
  } finally {
    loading.value = false
  }
}

async function executeGridAggregation() {
  error.value = ''
  result.value = null
  if (!aggPoints.value) {
    error.value = t('gisProcessing.pointsRequired')
    return
  }
  loading.value = true
  try {
    const points = JSON.parse(aggPoints.value)
    const response = await gisProcessingApi.gridAggregation({ points, gridSizeDegrees: gridSize.value })
    if (response.data?.success) {
      result.value = response.data.data
    } else {
      error.value = response.data?.message || 'Grid aggregation failed'
    }
  } catch (e: any) {
    error.value = e.message || t('gisProcessing.parseError')
  } finally {
    loading.value = false
  }
}

async function executeSpatialJoin() {
  error.value = ''
  result.value = null
  if (!joinPoints.value || !joinPolygons.value) {
    error.value = t('gisProcessing.pointsAndPolygonsRequired')
    return
  }
  loading.value = true
  try {
    const pts = JSON.parse(joinPoints.value)
    const polys = JSON.parse(joinPolygons.value)
    const response = await gisProcessingApi.spatialJoin({ points: pts, polygons: polys })
    if (response.data?.success) {
      result.value = response.data.data
    } else {
      error.value = response.data?.message || 'Spatial join failed'
    }
  } catch (e: any) {
    error.value = e.message || t('gisProcessing.parseError')
  } finally {
    loading.value = false
  }
}

async function executeSimplifyPath() {
  error.value = ''
  result.value = null
  if (!simplifyCoords.value) {
    error.value = t('gisProcessing.coordinatesRequired')
    return
  }
  loading.value = true
  try {
    const coords = JSON.parse(simplifyCoords.value)
    const response = await gisProcessingApi.simplifyPath({ coordinates: coords, tolerance: tolerance.value })
    if (response.data?.success) {
      result.value = response.data.data
    } else {
      error.value = response.data?.message || 'Simplify path failed'
    }
  } catch (e: any) {
    error.value = e.message || t('gisProcessing.parseError')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.gn-gis-panel {
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
