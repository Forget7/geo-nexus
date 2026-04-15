<template>
  <div class="elevation-panel">
    <div class="panel-header">
      <h3>🏔️ {{ t('elevation.title') }}</h3>
      <button class="close-btn" @click="$emit('close')">×</button>
    </div>

    <!-- 绘制模式切换 -->
    <div class="mode-toggle">
      <button :class="{ active: mode === 'draw' }" @click="startDraw">
        ✏️ {{ t('elevation.drawLine') }}
      </button>
      <button :class="{ active: mode === 'view' }" @click="mode = 'view'">
        👁️ {{ t('elevation.viewProfile') }}
      </button>
      <button @click="clearProfile" class="clear-btn">
        🗑️ {{ t('elevation.clear') }}
      </button>
    </div>

    <!-- 绘制提示 -->
    <div v-if="mode === 'draw'" class="draw-hint">
      <p>{{ t('elevation.drawHint') }}</p>
    </div>

    <!-- 剖面图 -->
    <div v-if="profileData" class="profile-result">
      <!-- 统计卡片 -->
      <div class="stats-grid">
        <div class="stat-card">
          <span class="stat-value">{{ formatNumber(profileData.totalDistance) }}</span>
          <span class="stat-label">{{ t('elevation.distance') }} (m)</span>
        </div>
        <div class="stat-card ascent">
          <span class="stat-value">↑ {{ formatNumber(profileData.totalAscent) }}</span>
          <span class="stat-label">{{ t('elevation.ascent') }} (m)</span>
        </div>
        <div class="stat-card descent">
          <span class="stat-value">↓ {{ formatNumber(profileData.totalDescent) }}</span>
          <span class="stat-label">{{ t('elevation.descent') }} (m)</span>
        </div>
        <div class="stat-card">
          <span class="stat-value">{{ formatNumber(profileData.minElevation) }}~{{ formatNumber(profileData.maxElevation) }}</span>
          <span class="stat-label">{{ t('elevation.elevationRange') }} (m)</span>
        </div>
      </div>

      <!-- ECharts 剖面图 -->
      <div class="chart-container">
        <div ref="chartRef" class="elevation-chart"></div>
      </div>

      <!-- 高程数据表 -->
      <div class="points-table">
        <h4>{{ t('elevation.elevationPoints') }}</h4>
        <div class="table-wrapper">
          <table>
            <thead>
              <tr>
                <th>#</th>
                <th>{{ t('elevation.lat') }}</th>
                <th>{{ t('elevation.lng') }}</th>
                <th>{{ t('elevation.elevation') }} (m)</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(pt, idx) in profileData.points" :key="idx">
                <td>{{ idx + 1 }}</td>
                <td>{{ pt.lat.toFixed(5) }}</td>
                <td>{{ pt.lng.toFixed(5) }}</td>
                <td>{{ formatNumber(pt.elevation) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="empty-state">
      <p>🗻 {{ t('elevation.emptyHint') }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'

const t = (key: string) => key
const emit = defineEmits(['close', 'elevationLoaded'])

const mode = ref<'draw' | 'view'>('draw')
const chartRef = ref<HTMLElement>()
const profileData = ref<any>(null)
const drawnCoordinates = ref<[number, number][]>([])

function startDraw() {
  mode.value = 'draw'
  drawnCoordinates.value = []
  profileData.value = null
}

function clearProfile() {
  drawnCoordinates.value = []
  profileData.value = null
  mode.value = 'draw'
}

function formatNumber(n: number) {
  return Math.round(n * 10) / 10
}

// 在地图上添加临时折线（通过 emit 通知父组件）
function addPoint(lng: number, lat: number) {
  drawnCoordinates.value.push([lng, lat])
  if (drawnCoordinates.value.length >= 2) {
    fetchProfile()
  }
}

async function fetchProfile() {
  try {
    const { data } = await apiClient.post('/elevation/profile', {
      coordinates: drawnCoordinates.value
    })
    profileData.value = data
    mode.value = 'view'
    await nextTick()
    renderChart()
    emit('elevationLoaded', data)
  } catch (e) {
    console.error('Failed to fetch elevation profile:', e)
  }
}

function renderChart() {
  if (!chartRef.value || !profileData.value) return
  const chart = echarts.init(chartRef.value)
  const distances = profileData.value.points.map((p: any) => p.distance)
  const elevations = profileData.value.points.map((p: any) => p.elevation)
  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) =>
        `${params[0].data[0].toFixed(0)}m: ${params[0].data[1].toFixed(1)}m`
    },
    grid: { left: '50px', right: '20px', top: '20px', bottom: '40px' },
    xAxis: {
      type: 'value',
      name: t('elevation.distanceM'),
      nameLocation: 'middle',
      nameGap: 25,
      axisLabel: { fontSize: 10 }
    },
    yAxis: {
      type: 'value',
      name: t('elevation.elevationM'),
      nameLocation: 'middle',
      nameGap: 35,
      axisLabel: { fontSize: 10 }
    },
    series: [{
      type: 'line',
      data: elevations.map((e: number, i: number) => [distances[i], e]),
      smooth: true,
      lineStyle: { color: '#2563eb', width: 2 },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(37,99,235,0.4)' },
          { offset: 1, color: 'rgba(37,99,235,0.05)' }
        ])
      },
      symbol: 'circle',
      symbolSize: 4,
      itemStyle: { color: '#2563eb' }
    }]
  }
  chart.setOption(option)
}

onMounted(() => {
  if (chartRef.value) {
    // Charts render on data load
  }
})

// 暴露 addPoint 给父组件调用
defineExpose({ addPoint })
</script>

<style scoped>
.elevation-panel {
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.15);
  max-height: 80vh;
  overflow-y: auto;
}
.panel-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 16px; border-bottom: 1px solid #e2e8f0;
}
.panel-header h3 { font-size: 15px; font-weight: 600; }
.close-btn { background: none; border: none; font-size: 20px; cursor: pointer; color: #64748b; }
.mode-toggle {
  display: flex; gap: 6px; padding: 10px 16px;
}
.mode-toggle button {
  flex: 1; padding: 7px; border: 1px solid #e2e8f0;
  border-radius: 6px; background: white; cursor: pointer; font-size: 12px;
}
.mode-toggle button.active { background: #eff6ff; border-color: #2563eb; color: #2563eb; }
.clear-btn { color: #dc2626 !important; border-color: #fecaca !important; }
.draw-hint {
  padding: 8px 16px;
  background: #fefce8; border: 1px solid #fef08a; margin: 0 16px 12px;
  border-radius: 6px; font-size: 12px; color: #854d0e;
}
.stats-grid {
  display: grid; grid-template-columns: 1fr 1fr; gap: 8px;
  padding: 12px 16px;
}
.stat-card {
  background: #f8fafc; border-radius: 8px; padding: 10px;
  text-align: center;
}
.stat-card.ascent { background: #f0fdf4; }
.stat-card.descent { background: #fef2f2; }
.stat-value { display: block; font-size: 16px; font-weight: 700; color: #1e293b; }
.stat-label { display: block; font-size: 10px; color: #64748b; margin-top: 2px; }
.chart-container { padding: 12px 16px; }
.elevation-chart { width: 100%; height: 200px; }
.points-table { padding: 12px 16px; }
.points-table h4 { font-size: 13px; margin-bottom: 8px; }
.table-wrapper { max-height: 200px; overflow-y: auto; border: 1px solid #e2e8f0; border-radius: 6px; }
table { width: 100%; border-collapse: collapse; font-size: 11px; }
th { background: #f8fafc; position: sticky; top: 0; padding: 6px 8px; text-align: left; font-weight: 600; }
td { padding: 5px 8px; border-top: 1px solid #f1f5f9; }
.empty-state { text-align: center; padding: 40px; color: #94a3b8; font-size: 14px; }
</style>
