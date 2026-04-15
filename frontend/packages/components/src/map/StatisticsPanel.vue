<template>
  <div class="gn-statisticspanel">
    <div class="panel-section">
      <h4>{{ t('map.stats.heatmap') }}</h4>
      <button @click="generateHeatmap" :disabled="heatmapLoading">
        {{ heatmapLoading ? t('map.stats.generating') : t('map.stats.generateHeatmap') }}
      </button>
      <p class="hint">{{ t('map.stats.heatmapHint') }}</p>
    </div>
    <div class="panel-section">
      <h4>{{ t('map.stats.summary') }}</h4>
      <div class="stat-item">
        <span>{{ t('map.stats.layers') }}:</span>
        <span>{{ layerCount }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import type { Viewer } from 'cesium'

defineOptions({
  name: 'GnStatisticsPanel'
})

const { t } = useI18n()

const props = defineProps<{
  cesiumViewer?: Viewer
  layerCount?: number
}>()

const heatmapLoading = ref(false)
const layerCount = ref(props.layerCount ?? 0)

function generateHeatmap() {
  heatmapLoading.value = true
  try {
    const viewer = props.cesiumViewer || (window as any).cesiumViewer
    if (!viewer) {
      console.warn('[StatisticsPanel] No Cesium viewer available for heatmap')
      return
    }
    // Dynamically import to avoid circular deps
    import('../../../packages/hooks/src/useCesiumAdvanced').then(({ useCesiumAdvanced }) => {
      const advanced = useCesiumAdvanced(viewer)
      // Generate random demo points around a default location
      const points = Array.from({ length: 100 }, () => ({
        lon: 116.3 + (Math.random() - 0.5) * 2,
        lat: 39.9 + (Math.random() - 0.5) * 2,
        intensity: Math.random()
      }))
      advanced.createHeatmap('demo-heatmap-' + Date.now(), points, {
        radius: 30,
        minIntensity: 0,
        maxIntensity: 1
      })
    })
  } finally {
    heatmapLoading.value = false
  }
}
</script>

<style scoped>
.gn-statisticspanel {
  width: 100%;
  height: 100%;
  padding: 12px;
  box-sizing: border-box;
}

.panel-section {
  margin-bottom: 16px;
}

.panel-section h4 {
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

.panel-section button {
  padding: 6px 14px;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.2s;
}

.panel-section button:hover:not(:disabled) {
  background: #1d4ed8;
}

.panel-section button:disabled {
  background: #93c5fd;
  cursor: not-allowed;
}

.hint {
  margin: 6px 0 0;
  font-size: 12px;
  color: #64748b;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  padding: 4px 0;
  color: #475569;
}
</style>
