<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

// 引入孤儿组件
import StatisticsPanel from './StatisticsPanel.vue'
import OfflineDownloader from './OfflineDownloader.vue'
import MapSharePanel from './MapSharePanel.vue'
import IntelligentMapPanel from './IntelligentMapPanel.vue'
import AnomalyPanel from './AnomalyPanel.vue'

const { t } = useI18n()

const props = defineProps<{
  mode: '2d' | '3d'
  mapUrl?: string
}>()

const mapContainer = ref<HTMLElement | null>(null)
const mapInstance = ref<L.Map | null>(null)
const iframeUrl = ref('')
const isMapLoading = ref(false)

// 侧边面板状态
const showSidePanel = ref(false)
const activeSideTab = ref<'statistics' | 'offline' | 'share' | 'intelligent' | 'anomaly'>('statistics')

// Tile layers
const tileLayers = {
  osm: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
  satellite: 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
  dark: 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png',
  terrain: 'https://{s}.tile.opentopomap.org/{z}/{x}/{y}.png'
}

const currentTile = ref('osm')

// 当前可视范围（用于离线下载）
const mapBounds = computed(() => {
  if (!mapInstance.value) return null
  const bounds = mapInstance.value.getBounds()
  return {
    minLon: bounds.getWest(),
    minLat: bounds.getSouth(),
    maxLon: bounds.getEast(),
    maxLat: bounds.getNorth()
  }
})

// 初始化Leaflet地图
function initLeafletMap() {
  if (!mapContainer.value || mapInstance.value) return

  isMapLoading.value = true

  mapInstance.value = L.map(mapContainer.value).setView([35.0, 105.0], 5)

  L.tileLayer(tileLayers.osm, {
    attribution: '© OpenStreetMap contributors',
    maxZoom: 19
  }).on('load', () => {
    isMapLoading.value = false
  }).addTo(mapInstance.value)

  // 添加控件
  L.control.layers(null, null, { collapsed: false }).addTo(mapInstance.value)
  L.control.scale().addTo(mapInstance.value)
  L.control.mousePosition().addTo(mapInstance.value)

  // 如果初始化很快完成，确保 loading 状态被清除
  setTimeout(() => { isMapLoading.value = false }, 500)
}

// 切换底图
function switchTile(type: string) {
  if (!mapInstance.value) return

  isMapLoading.value = true
  currentTile.value = type
  // 移除旧图层，添加新图层
  mapInstance.value.eachLayer((layer) => {
    if (layer instanceof L.TileLayer) {
      mapInstance.value?.removeLayer(layer)
    }
  })

  L.tileLayer(tileLayers[type as keyof typeof tileLayers] || tileLayers.osm, {
    attribution: '© OpenStreetMap contributors',
    maxZoom: 19
  }).on('load', () => {
    isMapLoading.value = false
  }).addTo(mapInstance.value)
}

// 加载外部地图URL（iframe方式）
function loadExternalMap(url: string) {
  iframeUrl.value = url
}

// 切换侧边面板
function toggleSidePanel(tab?: 'statistics' | 'offline' | 'share' | 'intelligent') {
  if (tab) {
    activeSideTab.value = tab
    showSidePanel.value = true
  } else {
    showSidePanel.value = !showSidePanel.value
  }
}

// 监听props变化
watch(() => props.mapUrl, (newUrl) => {
  if (newUrl) {
    loadExternalMap(newUrl)
  }
})

onMounted(() => {
  if (props.mode === '2d') {
    setTimeout(initLeafletMap, 100)
  }
})

// 模式切换时重新初始化
watch(() => props.mode, (newMode) => {
  if (newMode === '2d') {
    setTimeout(initLeafletMap, 100)
  }
})

// 暴露地图实例和状态供父组件使用
defineExpose({
  getMapInstance: () => mapInstance.value,
  getBounds: () => mapBounds.value
})
</script>

<template>
  <div class="map-view">
    <!-- 2D Leaflet 地图 -->
    <div v-if="mode === '2d'" class="leaflet-container">
      <!-- Loading overlay -->
      <Transition name="fade">
        <div v-if="isMapLoading" class="map-loading-overlay">
          <div class="loading-spinner"></div>
          <span>{{ t('common.loading') }}</span>
        </div>
      </Transition>

      <div ref="mapContainer" class="map-element"></div>

      <!-- 底图切换 -->
      <div class="map-controls">
        <button
          v-for="(url, type) in tileLayers"
          :key="type"
          :class="{ active: currentTile === type }"
          @click="switchTile(type as string)"
        >
          {{ type }}
        </button>
      </div>

      <!-- 侧边面板开关 -->
      <div class="panel-toggles">
        <button
          class="panel-toggle-btn"
          @click="toggleSidePanel('statistics')"
          :title="t('map.mapView.statistics')"
        >
          📊
        </button>
        <button
          class="panel-toggle-btn"
          @click="toggleSidePanel('offline')"
          :title="t('map.mapView.offlineDownload')"
        >
          📥
        </button>
        <button
          class="panel-toggle-btn"
          @click="toggleSidePanel('share')"
          :title="t('map.mapView.shareMap')"
        >
          🔗
        </button>
        <button
          class="panel-toggle-btn"
          @click="toggleSidePanel('intelligent')"
          :title="t('map.mapView.intelligentMap')"
        >
          🤖
        </button>
      </div>

      <!-- 侧边面板 (带滑入动画) -->
      <Transition name="panel-slide">
        <div v-if="showSidePanel" class="side-panel">
          <div class="panel-header">
            <div class="panel-tabs">
              <button
                :class="{ active: activeSideTab === 'statistics' }"
                @click="activeSideTab = 'statistics'"
              >
                📊 {{ t('map.mapView.tabStats') }}
              </button>
              <button
                :class="{ active: activeSideTab === 'offline' }"
                @click="activeSideTab = 'offline'"
              >
                📥 {{ t('map.mapView.tabDownload') }}
              </button>
              <button
                :class="{ active: activeSideTab === 'share' }"
                @click="activeSideTab = 'share'"
              >
                🔗 {{ t('map.mapView.tabShare') }}
              </button>
              <button
                :class="{ active: activeSideTab === 'intelligent' }"
                @click="activeSideTab = 'intelligent'"
              >
                🤖 {{ t('map.mapView.tabIntelligent') }}
              </button>
              <button
                :class="{ active: activeSideTab === 'anomaly' }"
                @click="activeSideTab = 'anomaly'"
              >
                🔍 {{ t('anomaly.title') }}
              </button>
            </div>
            <button class="panel-close" @click="showSidePanel = false">×</button>
          </div>

          <div class="panel-content">
            <StatisticsPanel v-if="activeSideTab === 'statistics'" :layerCount="1" />
            <OfflineDownloader
              v-if="activeSideTab === 'offline'"
              :mapBounds="mapBounds"
              :tileSource="tileLayers[currentTile as keyof typeof tileLayers]"
            />
            <MapSharePanel v-if="activeSideTab === 'share'" />
            <IntelligentMapPanel
              v-if="activeSideTab === 'intelligent'"
              :centerLon="mapInstance?.getCenter()?.lng"
              :centerLat="mapInstance?.getCenter()?.lat"
              :zoom="mapInstance?.getZoom()"
            />
            <AnomalyPanel
              v-if="activeSideTab === 'anomaly'"
            />
          </div>
        </div>
      </Transition>
    </div>

    <!-- 3D Cesium 地图 -->
    <div v-else class="cesium-container">
      <iframe
        v-if="iframeUrl"
        :src="iframeUrl"
        class="cesium-iframe"
        title="3D Map"
      ></iframe>
      <!-- 空状态 -->
      <div v-else class="cesium-placeholder">
        <div class="empty-icon">🌍</div>
        <p>{{ t('map.mapView.loading3d') }}</p>
        <p class="hint">{{ t('map.mapView.hint3d') }}</p>
        <!-- 3D模式下也显示智能地图入口 -->
        <div class="cesium-panel-toggles">
          <button
            class="panel-toggle-btn large"
            @click="toggleSidePanel('intelligent')"
            :title="t('map.mapView.intelligentMap')"
          >
            🤖 {{ t('map.mapView.intelligentMap') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.map-view {
  width: 100%;
  height: 100%;
  position: relative;
}

.leaflet-container {
  width: 100%;
  height: 100%;
}

.map-element {
  width: 100%;
  height: 100%;
}

/* Loading overlay */
.map-loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.85);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  z-index: 1000;
  backdrop-filter: blur(2px);
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #e2e8f0;
  border-top-color: #2563eb;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}

.map-controls {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 1000;
  display: flex;
  gap: 4px;
  background: white;
  padding: 4px;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
}

.map-controls button {
  padding: 6px 12px;
  border: none;
  border-radius: 4px;
  background: #f1f5f9;
  cursor: pointer;
  font-size: 12px;
  text-transform: uppercase;
  transition: all 0.2s;
}

.map-controls button:hover {
  background: #e2e8f0;
}

.map-controls button.active {
  background: #2563eb;
  color: white;
}

/* 侧边面板 */
.side-panel {
  position: absolute;
  top: 10px;
  left: 10px;
  width: 320px;
  max-height: calc(100% - 20px);
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.15);
  z-index: 1001;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* Panel slide animation */
.panel-slide-enter-active,
.panel-slide-leave-active {
  transition: transform 0.3s ease, opacity 0.3s ease;
}
.panel-slide-enter-from,
.panel-slide-leave-to {
  transform: translateX(-100%);
  opacity: 0;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-bottom: 1px solid #e2e8f0;
  background: #f8fafc;
}

.panel-tabs {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.panel-tabs button {
  padding: 6px 10px;
  border: none;
  border-radius: 6px;
  background: transparent;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
  color: #64748b;
}

.panel-tabs button:hover {
  background: #e2e8f0;
}

.panel-tabs button.active {
  background: #2563eb;
  color: white;
}

.panel-close {
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  font-size: 20px;
  color: #64748b;
  cursor: pointer;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.panel-close:hover {
  background: #fee2e2;
  color: #dc2626;
}

.panel-content {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

/* 面板开关按钮 */
.panel-toggles {
  position: absolute;
  top: 10px;
  left: 10px;
  z-index: 1000;
  display: flex;
  gap: 4px;
}

.panel-toggle-btn {
  width: 36px;
  height: 36px;
  border: none;
  background: white;
  border-radius: 8px;
  font-size: 18px;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.panel-toggle-btn:hover {
  transform: scale(1.1);
  box-shadow: 0 4px 12px rgba(0,0,0,0.2);
}

.panel-toggle-btn:active {
  transform: scale(0.95);
}

.panel-toggle-btn.large {
  width: auto;
  padding: 0 16px;
  font-size: 14px;
}

/* Cesium 容器 */
.cesium-container {
  width: 100%;
  height: 100%;
  position: relative;
}

.cesium-iframe {
  width: 100%;
  height: 100%;
  border: none;
}

.cesium-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1e3a5f 0%, #0f1c2e 100%);
  color: white;
  text-align: center;
  padding: 2rem;
}

.empty-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

.cesium-placeholder p {
  margin: 0;
  font-size: 1.5rem;
}

.cesium-placeholder .hint {
  margin-top: 0.5rem;
  font-size: 0.875rem;
  opacity: 0.7;
  max-width: 300px;
}

.cesium-panel-toggles {
  margin-top: 1.5rem;
}
</style>
