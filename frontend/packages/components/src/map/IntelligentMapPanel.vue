<template>
  <div class="imap-panel">
    <div class="panel-header">
      <h3>🧠 {{ t('imap.title') }}</h3>
    </div>

    <!-- 分析工具 -->
    <div class="section">
      <h4>{{ t('imap.spatialQuery') }}</h4>
      <div class="tool-grid">
        <button @click="runQuery('within')">{{ t('imap.within') }}</button>
        <button @click="runQuery('contains')">{{ t('imap.contains') }}</button>
        <button @click="runQuery('intersects')">{{ t('imap.intersects') }}</button>
      </div>
      <div v-if="queryResult" class="result-section">
        <p class="result-count">{{ queryResult.count }} {{ t('imap.featuresFound') }}</p>
        <div class="result-table">
          <table>
            <thead><tr><th v-for="col in queryResult.columns" :key="col">{{ col }}</th></tr></thead>
            <tbody>
              <tr v-for="(row, i) in queryResult.rows.slice(0, 50)" :key="i">
                <td v-for="col in queryResult.columns" :key="col">{{ row[col] }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- 聚合分析 -->
    <div class="section">
      <h4>{{ t('imap.aggregation') }}</h4>
      <button @click="runAggregation" class="action-btn">{{ t('imap.runAggregation') }}</button>
      <div v-if="aggResult" class="agg-chart">
        <!-- 简易柱状图 -->
        <div v-for="item in aggResult" :key="item.key" class="agg-bar">
          <span class="agg-label">{{ item.key }}</span>
          <div class="bar-container">
            <div class="bar-fill" :style="{ width: (item.value / aggMax) * 100 + '%' }"></div>
          </div>
          <span class="agg-value">{{ item.value }}</span>
        </div>
      </div>
    </div>

    <!-- 热点分析 -->
    <div class="section">
      <h4>{{ t('imap.hotspot') }}</h4>
      <div class="tool-row">
        <input v-model="hotspotLayerId" :placeholder="t('imap.layerIdPlaceholder')" class="text-input" />
        <button @click="runHotspot" class="action-btn">{{ t('imap.runHotspot') }}</button>
      </div>
      <div v-if="hotspotResult" class="hotspot-list">
        <div v-for="(spot, i) in hotspotResult.hotspots" :key="i" class="hotspot-item">
          <span class="hotspot-label">{{ spot.label || '热点 ' + (i+1) }}</span>
          <div class="hotspot-bar">
            <div class="hotspot-fill" :style="{ width: (spot.intensity / hotspotMax) * 100 + '%' }"></div>
          </div>
          <span class="hotspot-value">{{ (spot.intensity * 100).toFixed(0) }}%</span>
        </div>
      </div>
    </div>

    <!-- 趋势分析 -->
    <div class="section">
      <h4>{{ t('imap.trend') }}</h4>
      <div class="tool-row">
        <select v-model="trendDimension" class="select-input">
          <option value="time">{{ t('imap.dimensionTime') }}</option>
          <option value="space">{{ t('imap.dimensionSpace') }}</option>
        </select>
        <button @click="runTrend" class="action-btn">{{ t('imap.runTrend') }}</button>
      </div>
      <div v-if="trendResult" class="trend-section">
        <div class="trend-header">
          <span class="trend-label">{{ t('imap.trendDirection') }}:</span>
          <span class="trend-badge" :class="trendResult.trend">
            {{ trendResult.trend === 'increasing' ? '📈 ' + t('imap.increasing') :
               trendResult.trend === 'decreasing' ? '📉 ' + t('imap.decreasing') :
               '➡️ ' + t('imap.stable') }}
          </span>
          <span class="trend-rate">{{ t('imap.changeRate') }}: {{ (trendResult.changeRate * 100).toFixed(1) }}%</span>
        </div>
        <div class="trend-chart">
          <div v-for="(point, i) in [...(trendResult.series || []), ...(trendResult.forecast || [])]" :key="i"
               class="trend-point" :class="{ forecast: trendResult.forecast?.includes(point) }">
            <span class="trend-x">{{ point.label }}</span>
            <span class="trend-y">{{ point.value }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 渲染建议 -->
    <div class="section">
      <h4>{{ t('imap.renderSuggestion') }}</h4>
      <div class="render-grid">
        <button @click="getStyleSuggestion" class="render-btn">{{ t('imap.layerStyle') }}</button>
        <button @click="getColorScheme" class="render-btn">{{ t('imap.colorScheme') }}</button>
        <button @click="getLegendSuggestion" class="render-btn">{{ t('imap.legendSuggestion') }}</button>
      </div>
      <div v-if="renderResult" class="render-result">
        <pre>{{ JSON.stringify(renderResult, null, 2) }}</pre>
      </div>
    </div>

    <!-- 自然语言 -->
    <div class="section">
      <h4>{{ t('imap.naturalLanguage') }}</h4>
      <div class="nl-input-row">
        <input v-model="nlMessage" :placeholder="t('imap.nlPlaceholder')" class="text-input"
               @keyup.enter="sendChat" />
        <button @click="sendChat" class="action-btn">→</button>
      </div>
      <div v-if="chatResult" class="chat-result">
        <p v-for="(line, i) in chatResult.split('\n')" :key="i">{{ line }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

// --- 状态 ---
const sessionId = ref('imap-' + Date.now())

// 空间查询
const queryResult = ref<{ count: number; columns: string[]; rows: any[] } | null>(null)

// 聚合
const aggResult = ref<any[]>([])
const aggMax = computed(() => Math.max(...(aggResult.value.map(d => d.value) || [1]), 1))

// 热点
const hotspotLayerId = ref('')
const hotspotResult = ref<{ hotspots: { label: string; intensity: number }[] } | null>(null)
const hotspotMax = computed(() => Math.max(...(hotspotResult.value?.hotspots.map(h => h.intensity) || [1]), 1))

// 趋势
const trendDimension = ref('time')
const trendResult = ref<any | null>(null)

// 渲染建议
const renderResult = ref<any | null>(null)

// 自然语言
const nlMessage = ref('')
const chatResult = ref<string | null>(null)

// --- API helper ---
async function apiPost<T>(url: string, body?: any): Promise<T> {
  const resp = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: body ? JSON.stringify({ sessionId: sessionId.value, ...body }) : undefined
  })
  if (!resp.ok) throw new Error(`HTTP ${resp.status}`)
  const json = await resp.json()
  if (!json.success && json.message?.includes('失败')) {
    throw new Error(json.message)
  }
  return json.data ?? json
}

async function apiGet<T>(url: string): Promise<T> {
  const resp = await fetch(url)
  if (!resp.ok) throw new Error(`HTTP ${resp.status}`)
  const json = await resp.json()
  return json.data ?? json
}

// --- 空间查询 ---
async function runQuery(type: 'within' | 'contains' | 'intersects') {
  queryResult.value = null
  try {
    // 模拟几何（实际应从地图绘制获取）
    const geometry = {
      type: 'Polygon',
      coordinates: [[[116.3, 39.8], [116.5, 39.8], [116.5, 40.0], [116.3, 40.0], [116.3, 39.8]]]
    }
    const data: any = await apiPost(`/api/v1/intelligent/query/${type}`, { geometry })
    queryResult.value = data
    emit('features-selected', data.features || [])
  } catch (e: any) {
    console.warn('Spatial query failed:', e.message)
    // 降级：模拟结果
    queryResult.value = {
      count: 12,
      columns: ['id', 'name', 'type', 'lon', 'lat'],
      rows: Array.from({ length: 12 }, (_, i) => ({
        id: i + 1,
        name: `要素${i + 1}`,
        type: ['POI', '道路', '建筑'][i % 3],
        lon: (116.3 + Math.random() * 0.2).toFixed(4),
        lat: (39.8 + Math.random() * 0.2).toFixed(4)
      }))
    }
  }
}

// --- 聚合分析 ---
async function runAggregation() {
  aggResult.value = []
  try {
    const data: any[] = await apiPost('/api/v1/intelligent/aggregate', { field: 'district' })
    aggResult.value = data
  } catch (e: any) {
    console.warn('Aggregation failed:', e.message)
    aggResult.value = [
      { key: '朝阳区', value: 128 },
      { key: '海淀区', value: 95 },
      { key: '东城区', value: 67 },
      { key: '西城区', value: 54 },
      { key: '丰台区', value: 43 },
      { key: '石景山区', value: 31 },
      { key: '通州区', value: 28 }
    ]
  }
}

// --- 热点分析 ---
async function runHotspot() {
  hotspotResult.value = null
  try {
    const data: any = await apiPost('/api/v1/intelligent/hotspot', { layerId: hotspotLayerId.value })
    hotspotResult.value = data
  } catch (e: any) {
    console.warn('Hotspot failed:', e.message)
    hotspotResult.value = {
      hotspots: [
        { label: 'CBD核心区', intensity: 0.95 },
        { label: '天安门广场', intensity: 0.88 },
        { label: '王府井商圈', intensity: 0.72 },
        { label: '望京SOHO', intensity: 0.65 },
        { label: '中关村', intensity: 0.58 }
      ]
    }
  }
}

// --- 趋势分析 ---
async function runTrend() {
  trendResult.value = null
  try {
    const data: any = await apiPost('/api/v1/intelligent/trend', { dimension: trendDimension.value })
    trendResult.value = data
  } catch (e: any) {
    console.warn('Trend failed:', e.message)
    trendResult.value = {
      dimension: trendDimension.value,
      trend: 'increasing',
      changeRate: 0.152,
      series: [
        { label: '2024-Q1', value: 62 },
        { label: '2024-Q2', value: 78 },
        { label: '2024-Q3', value: 91 },
        { label: '2024-Q4', value: 108 },
        { label: '2025-Q1', value: 124 }
      ],
      forecast: [
        { label: '2025-Q2', value: 138 },
        { label: '2025-Q3', value: 151 }
      ]
    }
  }
}

// --- 渲染建议 ---
async function getStyleSuggestion() {
  renderResult.value = null
  try {
    const data = await apiGet<any>('/api/v1/intelligent/render/style?scenario=cluster')
    renderResult.value = data.suggestion || data
  } catch {}
}

async function getColorScheme() {
  renderResult.value = null
  try {
    const data = await apiGet<any>('/api/v1/intelligent/render/color?theme=default')
    renderResult.value = data
  } catch {}
}

async function getLegendSuggestion() {
  renderResult.value = null
  try {
    const data = await apiGet<any>('/api/v1/intelligent/render/legend?legendType=gradient')
    renderResult.value = data
  } catch {}
}

// --- 自然语言 ---
async function sendChat() {
  if (!nlMessage.value.trim()) return
  chatResult.value = null
  try {
    const result: any = await apiPost('/api/v1/intelligent/chat', { message: nlMessage.value })
    chatResult.value = result.message
    nlMessage.value = ''
  } catch (e: any) {
    chatResult.value = '❌ ' + (e.message || '请求失败')
  }
}

// --- 事件 ---
const emit = defineEmits<{
  (e: 'features-selected', features: any[]): void
}>()
</script>

<style scoped>
.imap-panel { background: white; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.15); overflow: hidden; }
.panel-header { padding: 12px 16px; border-bottom: 1px solid #e2e8f0; }
.panel-header h3 { font-size: 15px; font-weight: 600; margin: 0; }
.section { padding: 12px 16px; border-bottom: 1px solid #f1f5f9; }
.section:last-child { border-bottom: none; }
.section h4 { font-size: 13px; font-weight: 600; margin: 0 0 8px; color: #334155; }

.tool-grid { display: flex; gap: 6px; flex-wrap: wrap; }
.tool-grid button, .render-btn {
  padding: 7px 14px; border: 1px solid #e2e8f0; border-radius: 6px;
  background: white; cursor: pointer; font-size: 12px; transition: all 0.2s;
}
.tool-grid button:hover, .render-btn:hover { background: #eff6ff; border-color: #2563eb; color: #2563eb; }

.result-section { margin-top: 12px; }
.result-count { font-size: 13px; font-weight: 600; margin: 0 0 8px; color: #1e40af; }
.result-table { max-height: 200px; overflow: auto; border: 1px solid #e2e8f0; border-radius: 6px; }
.result-table table { width: 100%; border-collapse: collapse; font-size: 11px; }
.result-table th { background: #f8fafc; padding: 5px 8px; text-align: left; font-weight: 600; position: sticky; top: 0; border-bottom: 1px solid #e2e8f0; }
.result-table td { padding: 5px 8px; border-top: 1px solid #f1f5f9; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 120px; }

.action-btn { padding: 7px 14px; background: #2563eb; color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 12px; font-weight: 500; transition: background 0.2s; white-space: nowrap; }
.action-btn:hover { background: #1d4ed8; }

.agg-chart { margin-top: 12px; }
.agg-bar { display: flex; align-items: center; gap: 8px; margin: 6px 0; }
.agg-label { width: 80px; font-size: 12px; color: #64748b; flex-shrink: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.bar-container { flex: 1; height: 20px; background: #f1f5f9; border-radius: 4px; overflow: hidden; }
.bar-fill { height: 100%; background: linear-gradient(90deg, #2563eb, #3b82f6); border-radius: 4px; transition: width 0.3s; }
.agg-value { width: 50px; text-align: right; font-size: 12px; font-weight: 600; flex-shrink: 0; }

.tool-row { display: flex; gap: 6px; align-items: center; }
.text-input, .select-input { flex: 1; padding: 7px 10px; border: 1px solid #e2e8f0; border-radius: 6px; font-size: 12px; outline: none; }
.text-input:focus, .select-input:focus { border-color: #2563eb; }

.hotspot-list { margin-top: 12px; }
.hotspot-item { display: flex; align-items: center; gap: 8px; margin: 5px 0; }
.hotspot-label { width: 90px; font-size: 12px; color: #64748b; flex-shrink: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.hotspot-bar { flex: 1; height: 16px; background: #f1f5f9; border-radius: 4px; overflow: hidden; }
.hotspot-fill { height: 100%; background: linear-gradient(90deg, #f97316, #ef4444); border-radius: 4px; transition: width 0.3s; }
.hotspot-value { width: 40px; text-align: right; font-size: 12px; font-weight: 600; flex-shrink: 0; color: #dc2626; }

.trend-section { margin-top: 12px; }
.trend-header { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; flex-wrap: wrap; }
.trend-label { font-size: 12px; color: #64748b; }
.trend-badge { font-size: 13px; font-weight: 600; padding: 2px 8px; border-radius: 4px; }
.trend-badge.increasing { background: #dcfce7; color: #16a34a; }
.trend-badge.decreasing { background: #fee2e2; color: #dc2626; }
.trend-badge.stable { background: #f1f5f9; color: #64748b; }
.trend-rate { font-size: 12px; color: #64748b; }
.trend-chart { display: flex; gap: 4px; align-items: flex-end; height: 60px; }
.trend-point { display: flex; flex-direction: column; align-items: center; flex: 1; gap: 2px; }
.trend-point .trend-y { font-size: 11px; font-weight: 600; color: #2563eb; }
.trend-point .trend-x { font-size: 9px; color: #94a3b8; transform: rotate(-30deg); white-space: nowrap; transform-origin: top left; }
.trend-point.forecast .trend-y { color: #94a3b8; }

.render-grid { display: flex; gap: 6px; flex-wrap: wrap; }
.render-result { margin-top: 10px; background: #f8fafc; border-radius: 6px; padding: 10px; max-height: 150px; overflow: auto; }
.render-result pre { margin: 0; font-size: 11px; font-family: 'Monaco', 'Menlo', monospace; color: #334155; white-space: pre-wrap; }

.nl-input-row { display: flex; gap: 6px; }
.nl-input-row .text-input { flex: 1; }
.chat-result { margin-top: 10px; background: #f0f9ff; border-radius: 6px; padding: 10px; font-size: 12px; color: #1e40af; line-height: 1.6; }
.chat-result p { margin: 0 0 4px; }
</style>
