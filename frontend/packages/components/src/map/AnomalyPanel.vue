<template>
  <div class="anomaly-panel">
    <div class="panel-header">
      <h3>🔍 {{ t('anomaly.title') }}</h3>
    </div>

    <!-- 检测方法选择 -->
    <div class="method-selector">
      <label>{{ t('anomaly.method') }}</label>
      <select v-model="method">
        <option value="IQR">IQR（四分位距）</option>
        <option value="LOF">LOF（局部密度）</option>
        <option value="DBSCAN">DBSCAN（聚类）</option>
        <option value="GRID">GRID（网格Z-score）</option>
      </select>
    </div>

    <!-- 方法参数 -->
    <div class="method-params">
      <template v-if="method === 'IQR'">
        <label>{{ t('anomaly.iqrMultiplier') }}: {{ iqrMultiplier }}</label>
        <input type="range" v-model.number="iqrMultiplier" min="1" max="3" step="0.1" />
      </template>
      <template v-if="method === 'LOF'">
        <label>{{ t('anomaly.kNeighbors') }}: {{ kNeighbors }}</label>
        <input type="range" v-model.number="kNeighbors" min="3" max="20" step="1" />
        <label>{{ t('anomaly.threshold') }}: {{ threshold }}</label>
        <input type="range" v-model.number="threshold" min="1.0" max="3.0" step="0.1" />
      </template>
      <template v-if="method === 'DBSCAN'">
        <label>{{ t('anomaly.epsKm') }}: {{ epsKm }} km</label>
        <input type="range" v-model.number="epsKm" min="0.1" max="5" step="0.1" />
        <label>{{ t('anomaly.minPts') }}: {{ minPts }}</label>
        <input type="range" v-model.number="minPts" min="2" max="10" step="1" />
      </template>
      <template v-if="method === 'GRID'">
        <label>{{ t('anomaly.cellSizeKm') }}: {{ cellSizeKm }} km</label>
        <input type="range" v-model.number="cellSizeKm" min="0.1" max="2" step="0.1" />
        <label>{{ t('anomaly.zScoreThreshold') }}: {{ zScoreThreshold }}</label>
        <input type="range" v-model.number="zScoreThreshold" min="1" max="4" step="0.1" />
      </template>
    </div>

    <!-- 数值字段选择 -->
    <div class="field-selector" v-if="method === 'IQR' || method === 'GRID'">
      <label>{{ t('anomaly.valueField') }}</label>
      <select v-model="valueField">
        <option v-for="f in availableFields" :key="f" :value="f">{{ f }}</option>
      </select>
    </div>

    <button class="detect-btn" @click="runDetection" :disabled="loading">
      {{ loading ? t('anomaly.detecting') : t('anomaly.run') }}
    </button>

    <!-- 结果展示 -->
    <div v-if="result" class="result-section">
      <div class="result-summary">
        <span class="total">总点数: {{ result.totalPoints }}</span>
        <span class="anomaly-count" :class="result.anomalyCount > 0 ? 'has-anomaly' : ''">
          🔴 异常点: {{ result.anomalyCount }}
        </span>
        <span class="method">方法: {{ result.method }}</span>
      </div>

      <div v-if="result.anomalyCount > 0" class="anomaly-list">
        <div v-for="(a, idx) in result.anomalies.slice(0, 10)" :key="idx" class="anomaly-item">
          <span class="severity" :class="a.severity?.toLowerCase()">{{ a.severity }}</span>
          <span class="score">得分: {{ a.anomalyScore?.toFixed(2) }}</span>
          <span class="type">{{ a.anomalyType }}</span>
        </div>
        <div v-if="result.anomalyCount > 10" class="more">
          还有 {{ result.anomalyCount - 10 }} 个异常点...
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'

defineOptions({
  name: 'GnAnomalyPanel'
})

const { t } = useI18n()

const props = defineProps<{
  apiBase?: string
}>()

const method = ref('IQR')
const iqrMultiplier = ref(1.5)
const kNeighbors = ref(5)
const threshold = ref(1.5)
const epsKm = ref(1.0)
const minPts = ref(3)
const cellSizeKm = ref(0.5)
const zScoreThreshold = ref(2.0)
const valueField = ref('value')
const loading = ref(false)
const result = ref<any>(null)

const availableFields = ['value', 'pm25', 'pm10', 'noise', 'temperature', 'humidity', 'pressure', 'population']

async function runDetection() {
  loading.value = true
  result.value = null
  try {
    const apiBase = props.apiBase || '/api/v1'
    const response = await fetch(`${apiBase}/anomaly/detect`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        method: method.value,
        valueField: valueField.value,
        iqrMultiplier: iqrMultiplier.value,
        kNeighbors: kNeighbors.value,
        threshold: threshold.value,
        epsKm: epsKm.value,
        minPts: minPts.value,
        cellSizeKm: cellSizeKm.value,
        zScoreThreshold: zScoreThreshold.value,
        points: generateDemoPoints()
      })
    })
    const data = await response.json()
    if (data.success) {
      result.value = data.data
    }
  } catch (err) {
    console.error('[AnomalyPanel] detection error:', err)
  } finally {
    loading.value = false
  }
}

function generateDemoPoints() {
  // 生成模拟环境监测数据点
  const points = []
  for (let i = 0; i < 50; i++) {
    const baseLat = 39.9 + (Math.random() - 0.5) * 2
    const baseLng = 116.4 + (Math.random() - 0.5) * 2
    // 随机生成pm25值，大部分正常，少数异常高值
    const pm25 = Math.random() < 0.1 ? 150 + Math.random() * 100 : 20 + Math.random() * 80
    points.push({
      lat: baseLat,
      lng: baseLng,
      properties: {
        value: pm25,
        pm25: pm25,
        name: `监测点${i + 1}`
      }
    })
  }
  return points
}
</script>

<style scoped>
.anomaly-panel {
  padding: 12px;
}

.panel-header h3 {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #333;
}

.method-selector,
.field-selector {
  margin-bottom: 12px;
}

.method-selector label,
.field-selector label,
.method-params label {
  display: block;
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.method-selector select,
.field-selector select {
  width: 100%;
  padding: 6px 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 13px;
}

.method-params {
  margin-bottom: 12px;
}

.method-params label {
  margin-top: 8px;
}

.method-params input[type="range"] {
  width: 100%;
  margin-top: 4px;
}

.detect-btn {
  width: 100%;
  padding: 8px 16px;
  background: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
}

.detect-btn:hover:not(:disabled) {
  background: #0056b3;
}

.detect-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.result-section {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #eee;
}

.result-summary {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  font-size: 12px;
  margin-bottom: 8px;
}

.result-summary .has-anomaly {
  color: #dc3545;
  font-weight: bold;
}

.anomaly-list {
  max-height: 200px;
  overflow-y: auto;
}

.anomaly-item {
  display: flex;
  gap: 6px;
  padding: 4px 0;
  font-size: 11px;
  border-bottom: 1px solid #f5f5f5;
}

.anomaly-item .severity {
  font-weight: bold;
  min-width: 50px;
}

.anomaly-item .severity.high {
  color: #dc3545;
}

.anomaly-item .severity.medium {
  color: #fd7e14;
}

.anomaly-item .severity.low {
  color: #ffc107;
}

.more {
  padding: 8px 0;
  font-size: 11px;
  color: #999;
  text-align: center;
}
</style>
