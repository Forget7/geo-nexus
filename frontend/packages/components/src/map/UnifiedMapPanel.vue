<script setup lang="ts">
/**
 * UnifiedMapPanel - 统一地图面板
 * 支持2D(OpenLayers)和3D(Cesium)无缝切换
 */

import { ref, shallowRef, onMounted, computed, watch } from 'vue'
import { useOpenLayers } from './OpenLayersMap'
import { EffectManager } from '@geonex/hooks/useCesium'
import { 
  MapSyncManager, 
  UnifiedMapState, 
  CoordinateConverter 
} from '@geonex/hooks/useMapSync'
import type { Viewer } from 'cesium'
import * as Cesium from 'cesium'

// Props
const props = withDefaults(defineProps<{
  defaultMode?: '2d' | '3d'
  showControls?: boolean
  showLayerPanel?: boolean
  showEffectPanel?: boolean
}>(), {
  defaultMode: '2d',
  showControls: true,
  showLayerPanel: true,
  showEffectPanel: true
})

// Emits
const emit = defineEmits<{
  (e: 'viewChange', mode: '2d' | '3d'): void
  (e: 'featureSelect', feature: any): void
  (e: 'layerAdd', layer: any): void
  (e: 'layerRemove', layerId: string): void
}>()

// ==================== 状态 ====================

// 当前模式
const mapMode = ref<'2d' | '3d'>(props.defaultMode)

// OpenLayers
const olContainerId = 'ol-map-container'
const olMap = useOpenLayers(olContainerId)

// Cesium
const cesiumContainerId = 'cesium-map-container'
const cesiumContainerRef = ref<HTMLDivElement | null>(null)
const cesiumViewer = shallowRef<Viewer | null>(null)
const cesiumEffectManager = shallowRef<EffectManager | null>(null)

// 同步管理器
const syncManager = shallowRef<MapSyncManager | null>(null)
const unifiedState = shallowRef<UnifiedMapState | null>(null)

// UI状态
const showPanel = ref(true)
const activeTab = ref<'layers' | 'effects' | 'settings'>('layers')
const isLoading = ref(true)

// 图层列表
const layers = ref([
  { id: 'base', name: '底图', type: 'base', visible: true, opacity: 1 },
  { id: 'landmarks', name: '地标', type: 'vector', visible: true, opacity: 1 },
  { id: 'routes', name: '路线', type: 'vector', visible: true, opacity: 1 }
])

// 特效列表
const effects = ref([
  { id: 'none', name: '无', active: true },
  { id: 'rain', name: '雨', active: false },
  { id: 'snow', name: '雪', active: false },
  { id: 'bloom', name: '泛光', active: false }
])

// 底图选项
const baseLayers = [
  { id: 'osm', name: 'OpenStreetMap', icon: '🗺️' },
  { id: 'satellite', name: '卫星影像', icon: '🛰️' },
  { id: 'dark', name: '暗色地图', icon: '🌙' },
  { id: 'wms', name: 'GeoServer WMS', icon: '🗃️' }
]

const activeBaseLayer = ref('osm')

// 视图信息
const viewInfo = ref({
  center: [116.4, 39.9] as [number, number],
  zoom: 10,
  pitch: 0,
  heading: 0
})

// ==================== 生命周期 ====================

onMounted(async () => {
  // 等待地图初始化
  await new Promise(resolve => setTimeout(resolve, 500))
  
  // 初始化同步管理
  syncManager.value = new MapSyncManager({ twoWay: true })
  unifiedState.value = new UnifiedMapState()
  
  isLoading.value = false
})

// ==================== 模式切换 ====================

async function switchMode(mode: '2d' | '3d') {
  if (mode === mapMode.value) return
  
  isLoading.value = true
  
  // 保存当前视图状态
  if (mapMode.value === '2d') {
    const olView = olMap.getViewState()
    viewInfo.value = {
      center: olView.center,
      zoom: olView.zoom,
      pitch: 0,
      heading: olView.rotation * 180 / Math.PI
    }
  } else if (cesiumViewer.value) {
    const camera = cesiumViewer.value.camera
    const pos = CoordinateConverter.cesiumToLonLatHeight(camera.position)
    viewInfo.value = {
      center: [pos[0], pos[1]],
      zoom: Math.max(0, 20 - pos[2] / 1000),
      pitch: Cesium.Math.toDegrees(camera.pitch),
      heading: Cesium.Math.toDegrees(camera.heading)
    }
  }
  
  // 切换模式
  mapMode.value = mode

  // 绑定/更新同步
  if (mode === '3d') {
    const cv = await ensureCesiumViewer()
    if (cv) {
      syncManager.value?.bindCesium(cv)
    }
  } else if (olMap.map.value) {
    syncManager.value?.bindOpenLayers(olMap.map.value)
  }
  
  emit('viewChange', mode)
  
  // 恢复视图状态
  setTimeout(() => {
    if (mode === '3d' && cesiumViewer.value) {
      cesiumViewer.value.camera.flyTo({
        destination: Cesium.Cartesian3.fromDegrees(
          viewInfo.value.center[0],
          viewInfo.value.center[1],
          (20 - viewInfo.value.zoom) * 1500
        ),
        orientation: {
          heading: Cesium.Math.toRadians(viewInfo.value.heading),
          pitch: Cesium.Math.toRadians(viewInfo.value.pitch),
          roll: 0
        }
      })
    } else if (mode === '2d') {
      olMap.flyTo(viewInfo.value.center, viewInfo.value.zoom)
    }
    
    isLoading.value = false
  }, 100)
}

// ==================== Cesium初始化 ====================

async function initCesiumViewer(container: HTMLDivElement) {
  const CesiumModule = (await import('cesium')).default
  await import('cesium/Build/Cesium/Widgets/widgets.css')

  const viewer = new CesiumModule.Viewer(container, {
    terrain: CesiumModule.Terrain.fromWorldTerrain(),
    baseLayerPicker: false,
    geocoder: false,
    homeButton: true,
    sceneModePicker: true,
    navigationHelpButton: false,
    animation: false,
    timeline: false,
    fullscreenButton: false,
    vrButton: false,
    infoBox: true,
    selectionIndicator: true,
    skyBox: new CesiumModule.SkyBox({ show: false }),
    skyAtmosphere: new CesiumModule.SkyAtmosphere(),
  })

  cesiumViewer.value = viewer
  cesiumEffectManager.value = new EffectManager(viewer)
  ;(window as any).cesiumViewer = viewer

  onCesiumReady(viewer)
  return viewer
}

let cesiumViewerInitPromise: Promise<Viewer | null> | null = null

async function ensureCesiumViewer() {
  const container = cesiumContainerRef.value
  if (!container || cesiumViewer.value) return cesiumViewer.value

  if (!cesiumViewerInitPromise) {
    cesiumViewerInitPromise = initCesiumViewer(container)
  }
  return cesiumViewerInitPromise
}

// ==================== Cesium事件 ====================

function onCesiumReady(viewer: Viewer) {
  cesiumViewer.value = viewer
  cesiumEffectManager.value = new EffectManager(viewer)
  
  if (mapMode.value === '3d') {
    syncManager.value?.bindCesium(viewer)
  }
  
  // 监听相机变化
  viewer.camera.changed.addEventListener(() => {
    if (mapMode.value === '3d') {
      const pos = CoordinateConverter.cesiumToLonLatHeight(viewer.camera.position)
      viewInfo.value = {
        center: [pos[0], pos[1]],
        zoom: Math.max(0, 20 - pos[2] / 1000),
        pitch: Cesium.Math.toDegrees(viewer.camera.pitch),
        heading: Cesium.Math.toDegrees(viewer.camera.heading)
      }
    }
  })
  
  // 加载示例数据
  loadSampleData()
}

// ==================== 数据加载 ====================

function loadSampleData() {
  if (!cesiumViewer.value) return
  
  // 添加地标
  const landmarks = [
    { name: '天安门', lon: 116.3972, lat: 39.9165 },
    { name: '故宫', lon: 116.4122, lat: 39.9485 },
    { name: '天坛', lon: 116.4074, lat: 39.9042 },
    { name: '颐和园', lon: 116.2377, lat: 39.9406 }
  ]
  
  landmarks.forEach((landmark, i) => {
    cesiumViewer.value!.entities.add({
      id: `landmark_${i}`,
      name: landmark.name,
      position: Cesium.Cartesian3.fromDegrees(landmark.lon, landmark.lat),
      point: {
        color: Cesium.Color.RED,
        pixelSize: 10
      },
      label: {
        text: landmark.name,
        font: '14px sans-serif',
        fillColor: Cesium.Color.WHITE,
        outlineColor: Cesium.Color.BLACK,
        outlineWidth: 2,
        style: Cesium.LabelStyle.FILL_AND_OUTLINE,
        pixelOffset: new Cesium.Cartesian2(0, -20),
        translucencyByDistance: new Cesium.NearFarScalar(1000, 1, 15000, 0.5)
      }
    })
  })
  
  // 添加到OpenLayers
  if (olMap.map.value) {
    const olLayer = olMap.addVectorLayer('landmarks')
    
    landmarks.forEach((landmark, i) => {
      const feature = olMap.createPoint([landmark.lon, landmark.lat])
      feature.setId(`landmark_${i}`)
      feature.set('name', landmark.name)
      olMap.addFeature('landmarks', feature)
    })
  }
}

// ==================== 特效控制 ====================

function toggleEffect(effectId: string) {
  if (!cesiumEffectManager.value) return
  
  // 关闭当前特效
  effects.value.forEach(e => e.active = false)
  
  if (effectId === 'none') {
    cesiumEffectManager.value.clearAll()
    return
  }
  
  // 激活新特效
  const effect = effects.value.find(e => e.id === effectId)
  if (effect) effect.active = true
  
  switch (effectId) {
    case 'rain':
      cesiumEffectManager.value.addRainEffect()
      break
    case 'snow':
      cesiumEffectManager.value.addSnowEffect()
      break
    case 'bloom':
      cesiumEffectManager.value.addBloomEffect({ strength: 1.5 })
      break
  }
}

// ==================== 底图切换 ====================

function switchBaseLayer(layerId: string) {
  activeBaseLayer.value = layerId
  
  if (mapMode.value === '2d') {
    olMap.addBaseLayer(layerId as any)
  } else {
    // Cesium底图切换通过imageryProvider
    // 实现具体逻辑
  }
}

// ==================== 视图操作 ====================

function resetView() {
  if (mapMode.value === '2d') {
    olMap.flyTo([116.4, 39.9], 10)
  } else if (cesiumViewer.value) {
    cesiumViewer.value.camera.flyHome({ duration: 1 })
  }
}

function zoomIn() {
  if (mapMode.value === '2d') {
    const view = olMap.getViewState()
    olMap.flyTo(view.center, Math.min(view.zoom + 1, 19))
  } else if (cesiumViewer.value) {
    const camera = cesiumViewer.value.camera
    camera.zoomIn(camera.positionCartographic.height / 2)
  }
}

function zoomOut() {
  if (mapMode.value === '2d') {
    const view = olMap.getViewState()
    olMap.flyTo(view.center, Math.max(view.zoom - 1, 3))
  } else if (cesiumViewer.value) {
    const camera = cesiumViewer.value.camera
    camera.zoomOut(camera.positionCartographic.height / 2)
  }
}

function toggle3D() {
  switchMode(mapMode.value === '2d' ? '3d' : '2d')
}

// ==================== 图层控制 ====================

function toggleLayerVisibility(layerId: string) {
  const layer = layers.value.find(l => l.id === layerId)
  if (layer) {
    layer.visible = !layer.visible
    olMap.setLayerVisible(layerId, layer.visible)
  }
}

function setLayerOpacity(layerId: string, opacity: number) {
  const layer = layers.value.find(l => l.id === layerId)
  if (layer) {
    layer.opacity = opacity
    olMap.setLayerOpacity(layerId, opacity)
  }
}

// ==================== 导出 ====================

function takeScreenshot() {
  if (mapMode.value === '2d') {
    const dataUrl = olMap.exportImage()
    downloadImage(dataUrl, `map-2d-${Date.now()}.png`)
  } else if (cesiumViewer.value) {
    const canvas = cesiumViewer.value.canvas
    const dataUrl = canvas.toDataURL('image/png')
    downloadImage(dataUrl, `map-3d-${Date.now()}.png`)
  }
}

function downloadImage(dataUrl: string, filename: string) {
  const link = document.createElement('a')
  link.download = filename
  link.href = dataUrl
  link.click()
}

// ==================== 坐标显示 ====================

const coordDisplay = computed(() => {
  return `${viewInfo.value.center[0].toFixed(6)}, ${viewInfo.value.center[1].toFixed(6)}`
})
</script>

<template>
  <div class="unified-map-panel">
    <!-- 地图容器 -->
    <div class="map-container">
      <!-- OpenLayers 2D -->
      <div 
        v-show="mapMode === '2d'" 
        :id="olContainerId" 
        class="map-viewport"
      ></div>
      
      <!-- Cesium 3D -->
      <div
        v-show="mapMode === '3d'"
        ref="cesiumContainerRef"
        :id="cesiumContainerId"
        class="map-viewport"
      ></div>
      
      <!-- 加载遮罩 -->
      <div v-if="isLoading" class="loading-overlay">
        <div class="loading-spinner"></div>
        <p>{{ mapMode === '2d' ? '加载2D地图' : '加载3D场景' }}</p>
      </div>
      
      <!-- 坐标显示 -->
      <div class="coord-display">
        {{ coordDisplay }}
      </div>
    </div>
    
    <!-- 控制栏 -->
    <div v-if="showControls" class="control-bar">
      <!-- 模式切换 -->
      <div class="mode-switch">
        <button 
          :class="['mode-btn', { active: mapMode === '2d' }]"
          @click="switchMode('2d')"
        >
          <span class="mode-icon">🗺️</span>
          <span class="mode-label">2D</span>
        </button>
        <button 
          :class="['mode-btn', { active: mapMode === '3d' }]"
          @click="switchMode('3d')"
        >
          <span class="mode-icon">🌍</span>
          <span class="mode-label">3D</span>
        </button>
      </div>
      
      <!-- 缩放控制 -->
      <div class="zoom-controls">
        <button class="zoom-btn" @click="zoomIn">➕</button>
        <button class="zoom-btn" @click="zoomOut">➖</button>
      </div>
      
      <!-- 重置视图 -->
      <button class="control-btn" @click="resetView" title="重置视图">
        🏠
      </button>
      
      <!-- 截图 -->
      <button class="control-btn" @click="takeScreenshot" title="截图">
        📷
      </button>
      
      <!-- 面板切换 -->
      <button 
        class="control-btn" 
        @click="showPanel = !showPanel"
        :class="{ active: showPanel }"
      >
        {{ showPanel ? '▶' : '◀' }}
      </button>
    </div>
    
    <!-- 侧边面板 -->
    <Transition name="slide">
      <div v-if="showPanel" class="side-panel">
        <!-- 标签页 -->
        <div class="panel-tabs">
          <button 
            v-for="tab in ['layers', 'effects', 'settings']" 
            :key="tab"
            :class="['tab-btn', { active: activeTab === tab }]"
            @click="activeTab = tab as any"
          >
            {{ tab === 'layers' ? '📚' : tab === 'effects' ? '✨' : '⚙️' }}
          </button>
        </div>
        
        <!-- 图层面板 -->
        <div v-if="activeTab === 'layers'" class="panel-content">
          <h3>底图</h3>
          <div class="base-layer-grid">
            <button
              v-for="layer in baseLayers"
              :key="layer.id"
              :class="['base-layer-btn', { active: activeBaseLayer === layer.id }]"
              @click="switchBaseLayer(layer.id)"
            >
              {{ layer.icon }}
              <span>{{ layer.name }}</span>
            </button>
          </div>
          
          <h3>图层</h3>
          <div class="layer-list">
            <div 
              v-for="layer in layers" 
              :key="layer.id"
              class="layer-item"
            >
              <label class="layer-checkbox">
                <input 
                  type="checkbox" 
                  :checked="layer.visible"
                  @change="toggleLayerVisibility(layer.id)"
                />
                <span class="layer-name">{{ layer.name }}</span>
              </label>
              <input 
                type="range" 
                min="0" 
                max="1" 
                step="0.1"
                :value="layer.opacity"
                @input="setLayerOpacity(layer.id, ($event.target as any).value)"
              />
            </div>
          </div>
        </div>
        
        <!-- 特效面板 -->
        <div v-if="activeTab === 'effects'" class="panel-content">
          <h3>3D特效</h3>
          <div class="effect-grid">
            <button
              v-for="effect in effects"
              :key="effect.id"
              :class="['effect-btn', { active: effect.active }]"
              @click="toggleEffect(effect.id)"
            >
              {{ effect.name }}
            </button>
          </div>
          
          <div v-if="mapMode === '2d'" class="effect-hint">
            <p>⚠️ 切换到3D模式以使用特效</p>
          </div>
        </div>
        
        <!-- 设置面板 -->
        <div v-if="activeTab === 'settings'" class="panel-content">
          <h3>视图信息</h3>
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">缩放级别</span>
              <span class="info-value">{{ viewInfo.zoom.toFixed(1) }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">俯仰角</span>
              <span class="info-value">{{ viewInfo.pitch.toFixed(1) }}°</span>
            </div>
            <div class="info-item">
              <span class="info-label">旋转角</span>
              <span class="info-value">{{ viewInfo.heading.toFixed(1) }}°</span>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.unified-map-panel {
  width: 100%;
  height: 100%;
  position: relative;
  background: #1e293b;
}

.map-container {
  width: 100%;
  height: 100%;
  position: relative;
}

.map-viewport {
  width: 100%;
  height: 100%;
}

.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: white;
  z-index: 1000;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(255, 255, 255, 0.3);
  border-top-color: #2563eb;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.coord-display {
  position: absolute;
  bottom: 60px;
  left: 10px;
  background: rgba(30, 41, 59, 0.9);
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 12px;
  color: #94a3b8;
  font-family: monospace;
  z-index: 100;
}

.control-bar {
  position: absolute;
  top: 1rem;
  left: 1rem;
  display: flex;
  gap: 0.5rem;
  z-index: 100;
}

.mode-switch {
  display: flex;
  background: rgba(30, 41, 59, 0.95);
  border-radius: 8px;
  overflow: hidden;
}

.mode-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0.5rem 1rem;
  background: transparent;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  transition: all 0.2s;
}

.mode-btn.active {
  background: #2563eb;
  color: white;
}

.mode-icon {
  font-size: 1.25rem;
}

.mode-label {
  font-size: 0.625rem;
  margin-top: 0.25rem;
}

.zoom-controls,
.control-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(30, 41, 59, 0.95);
  border: none;
  border-radius: 8px;
  color: white;
  cursor: pointer;
  transition: all 0.2s;
}

.zoom-controls:hover,
.control-btn:hover {
  background: #334155;
}

.control-btn.active {
  background: #2563eb;
}

.side-panel {
  position: absolute;
  top: 1rem;
  right: 1rem;
  width: 280px;
  max-height: calc(100% - 2rem);
  background: rgba(30, 41, 59, 0.95);
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  z-index: 100;
}

.panel-tabs {
  display: flex;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.tab-btn {
  flex: 1;
  padding: 0.75rem;
  background: transparent;
  border: none;
  font-size: 1.25rem;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;
}

.tab-btn.active {
  background: #2563eb;
  color: white;
}

.panel-content {
  flex: 1;
  padding: 1rem;
  overflow-y: auto;
}

.panel-content h3 {
  margin: 0 0 0.75rem;
  font-size: 0.875rem;
  font-weight: 600;
  color: #94a3b8;
}

.base-layer-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.5rem;
  margin-bottom: 1.5rem;
}

.base-layer-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0.75rem;
  background: rgba(255, 255, 255, 0.05);
  border: 2px solid transparent;
  border-radius: 8px;
  color: #94a3b8;
  cursor: pointer;
  transition: all 0.2s;
}

.base-layer-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

.base-layer-btn.active {
  border-color: #2563eb;
  background: rgba(37, 99, 235, 0.2);
  color: white;
}

.base-layer-btn span {
  font-size: 0.625rem;
  margin-top: 0.25rem;
}

.layer-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.layer-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.layer-checkbox {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex: 1;
  cursor: pointer;
}

.layer-checkbox input {
  width: 16px;
  height: 16px;
  accent-color: #2563eb;
}

.layer-name {
  font-size: 0.875rem;
  color: #e2e8f0;
}

.layer-item input[type="range"] {
  width: 80px;
  accent-color: #2563eb;
}

.effect-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.5rem;
}

.effect-btn {
  padding: 0.75rem;
  background: rgba(255, 255, 255, 0.05);
  border: 2px solid transparent;
  border-radius: 8px;
  color: #94a3b8;
  cursor: pointer;
  transition: all 0.2s;
}

.effect-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

.effect-btn.active {
  border-color: #2563eb;
  background: rgba(37, 99, 235, 0.2);
  color: white;
}

.effect-hint {
  margin-top: 1rem;
  padding: 0.75rem;
  background: rgba(245, 158, 11, 0.1);
  border-radius: 8px;
  color: #f59e0b;
  font-size: 0.75rem;
}

.info-grid {
  display: grid;
  gap: 0.5rem;
}

.info-item {
  display: flex;
  justify-content: space-between;
  padding: 0.5rem;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 6px;
}

.info-label {
  color: #64748b;
  font-size: 0.875rem;
}

.info-value {
  color: #e2e8f0;
  font-size: 0.875rem;
  font-family: monospace;
}

/* Transition */
.slide-enter-active,
.slide-leave-active {
  transition: transform 0.3s ease;
}

.slide-enter-from,
.slide-leave-to {
  transform: translateX(100%);
}
</style>
