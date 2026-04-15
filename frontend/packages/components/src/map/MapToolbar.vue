<script setup lang="ts">
/**
 * MapToolbar - 专业GIS地图工具栏
 * 借鉴MapStore2/SuperMap iClient设计
 */

import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useMap } from '@geonex/hooks/useMap'
import SldEditor from './SldEditor.vue'

const props = defineProps<{
  mapId?: string
}>()

const emit = defineEmits<{
  (e: 'tool-select', tool: string): void
  (e: 'layer-change', layer: any): void
  (e: 'print', options: any): void
}>()

// 地图状态
const {
  viewer,
  mode,
  currentBaseLayer,
  layers,
  setBaseLayer,
  flyTo,
  homeView,
  fullscreen
} = useMap()

const { t } = useI18n()

// 工具状态
const activeTool = ref<string | null>(null)
const showLayerPanel = ref(false)
const showMeasurePanel = ref(false)
const showPrintPanel = ref(false)
const showSettingsPanel = ref(false)
const showSldEditor = ref(false)
const sldLayerName = ref('')

// 底图列表
const baseLayers = [
  { id: 'osm', name: 'OpenStreetMap', icon: '🗺️', type: 'tile', url: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png' },
  { id: 'satellite', name: 'map.toolbar.satellite', icon: '🛰️', type: 'tile', url: 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}' },
  { id: 'dark', name: 'map.toolbar.darkTheme', icon: '🌙', type: 'tile', url: 'https://cartodb-basemaps-{s}.global.ssl.fastly.net/dark_all/{z}/{x}/{y}.png' },
  { id: 'terrain', name: 'map.toolbar.terrain', icon: '⛰️', type: 'terrain' },
  { id: 'cesium-ion', name: 'Cesium Ion', icon: '🌐', type: 'ion', assetId: 1 }
]

// 工具列表
const tools = [
  { id: 'select', name: 'map.toolbar.select', icon: '👆', shortcut: 'V' },
  { id: 'pan', name: 'map.toolbar.pan', icon: '✋', shortcut: 'H' },
  { id: 'draw-point', name: 'map.toolbar.drawPoint', icon: '📍', shortcut: 'P' },
  { id: 'draw-line', name: 'map.toolbar.drawLine', icon: '📏', shortcut: 'L' },
  { id: 'draw-polygon', name: 'map.toolbar.drawPolygon', icon: '🔷', shortcut: 'G' },
  { id: 'draw-circle', name: 'map.toolbar.drawCircle', icon: '⭕', shortcut: 'C' },
  { id: 'measure', name: 'map.toolbar.measure', icon: '📐', shortcut: 'M' },
  { id: 'query', name: 'map.toolbar.query', icon: '🔍', shortcut: 'Q' },
  { id: 'buffer', name: 'map.toolbar.buffer', icon: '🔵', shortcut: 'B' },
  { id: 'heatmap', name: 'map.toolbar.heatmap', icon: '🔥', shortcut: 'E' }
]

// 当前激活的工具
const currentTool = computed(() => tools.find(t => t.id === activeTool.value))

// Measure state
const measureType = ref<'distance' | 'area' | 'azimuth'>('distance')
const measureResult = ref('0.00 km')

// Drawing state
const drawType = ref<'point' | 'line' | 'polygon' | null>(null)

// 切换工具
function selectTool(toolId: string) {
  if (activeTool.value === toolId) {
    activeTool.value = null
  } else {
    activeTool.value = toolId
  }
  emit('tool-select', activeTool.value || '')
}

// 设置测量类型
function setMeasureType(type: 'distance' | 'area' | 'azimuth') {
  measureType.value = type
}

// 执行测量
async function executeMeasure() {
  emit('tool-select', `measure-${measureType.value}`)
}

// 清除所有实体
function clearAll() {
  emit('tool-select', 'clear-all')
  activeTool.value = null
}

// 切换底图
function switchBaseLayer(layer: any) {
  setBaseLayer(layer)
  emit('layer-change', layer)
}

// 视图预设
const viewPresets = [
  { name: 'map.toolbar.beijing', position: { lon: 116.4, lat: 39.9, zoom: 10 } },
  { name: 'map.toolbar.shanghai', position: { lon: 121.47, lat: 31.23, zoom: 10 } },
  { name: 'map.toolbar.guangzhou', position: { lon: 113.26, lat: 23.13, zoom: 10 } },
  { name: 'map.toolbar.shenzhen', position: { lon: 114.06, lat: 22.54, zoom: 11 } },
  { name: 'map.toolbar.chengdu', position: { lon: 104.06, lat: 30.67, zoom: 10 } },
  { name: 'map.toolbar.wuhan', position: { lon: 114.31, lat: 30.52, zoom: 10 } },
  { name: 'map.toolbar.xian', position: { lon: 108.94, lat: 34.34, zoom: 10 } },
  { name: 'map.toolbar.hangzhou', position: { lon: 120.15, lat: 30.28, zoom: 10 } }
]

// 飞至预设位置
function flyToPreset(preset: any) {
  flyTo(preset.position.lon, preset.position.lat, preset.position.zoom)
}

// 书签管理
const bookmarks = ref<Array<{ id: string, name: string, position: any, description?: string }>>([
  { id: '1', name: '公司总部', position: { lon: 116.4, lat: 39.9, zoom: 15 }, description: '北京总部' },
  { id: '2', name: '研发中心', position: { lon: 116.5, lat: 39.95, zoom: 16 }, description: '研发中心' }
])

// 添加书签
function addBookmark() {
  if (!viewer.value) return
  
  const camera = viewer.value.camera
  const position = {
    lon: camera.positionCartographic.longitude * 180 / Math.PI,
    lat: camera.positionCartographic.latitude * 180 / Math.PI,
    height: camera.positionCartographic.height,
    heading: camera.heading,
    pitch: camera.pitch,
    roll: camera.roll
  }
  
  const name = prompt('请输入书签名称:')
  if (name) {
    bookmarks.value.push({
      id: Date.now().toString(),
      name,
      position
    })
  }
}

// 打印设置
const printOptions = ref({
  format: 'png',
  quality: 100,
  width: 1920,
  height: 1080,
  title: 'GeoNexus地图',
  legend: true,
  scale: true,
  northArrow: true,
 attribution: true
})

// 执行打印
function printMap() {
  emit('print', printOptions.value)
  showPrintPanel.value = false
}

// 坐标显示
const cursorPosition = ref({ lon: 0, lat: 0, height: 0 })

// 更新光标位置
function updateCursorPosition(cartographic: any) {
  cursorPosition.value = {
    lon: (cartographic.longitude * 180 / Math.PI).toFixed(6),
    lat: (cartographic.latitude * 180 / Math.PI).toFixed(6),
    height: cartographic.height.toFixed(2)
  }
}

// 快捷键提示
const showShortcuts = ref(false)
</script>

<template>
  <div class="map-toolbar">
    <!-- 左侧工具栏 -->
    <div class="toolbar-left">
      <!-- 工具选择 -->
      <div class="tool-group">
        <div 
          v-for="tool in tools" 
          :key="tool.id"
          :class="['tool-btn', { active: activeTool === tool.id }]"
          :title="`${t(tool.name)} (${tool.shortcut})`"
          @click="selectTool(tool.id)"
        >
          <span class="tool-icon">{{ tool.icon }}</span>
          <span class="tool-name">{{ t(tool.name) }}</span>
        </div>
      </div>
      
      <div class="separator"></div>
      
      <!-- 测量工具面板 -->
      <div v-if="activeTool === 'measure'" class="measure-panel">
        <div class="measure-types">
          <button 
            :class="['measure-btn', { active: measureType === 'distance' }]"
            @click="setMeasureType('distance')"
          >
            <span>📏</span> {{ t('map.toolbar.distance') }}
          </button>
          <button 
            :class="['measure-btn', { active: measureType === 'area' }]"
            @click="setMeasureType('area')"
          >
            <span>📐</span> {{ t('map.toolbar.area') }}
          </button>
          <button 
            :class="['measure-btn', { active: measureType === 'azimuth' }]"
            @click="setMeasureType('azimuth')"
          >
            <span>↗️</span> {{ t('map.toolbar.azimuth') }}
          </button>
        </div>
        <button class="measure-start-btn" @click="executeMeasure">
          {{ t('map.toolbar.measureDistance') }}
        </button>
        <div class="measure-result">
          <span>{{ t('map.toolbar.result') }}:</span>
          <span class="result-value">{{ measureResult }}</span>
        </div>
      </div>
    </div>
    
    <!-- 中间视图控制 -->
    <div class="toolbar-center">
      <!-- 底图切换 -->
      <div class="basemap-selector">
        <button 
          v-for="layer in baseLayers" 
          :key="layer.id"
          :class="['basemap-btn', { active: currentBaseLayer?.id === layer.id }]"
          @click="switchBaseLayer(layer)"
        >
          {{ layer.icon }}
        </button>
      </div>
      
      <div class="separator"></div>
      
      <!-- 视图预设 -->
      <div class="view-presets">
        <button 
          v-for="preset in viewPresets" 
          :key="preset.name"
          class="preset-btn"
          :title="t(preset.name)"
          @click="flyToPreset(preset)"
        >
          {{ t(preset.name) }}
        </button>
      </div>
      
      <div class="separator"></div>
      
      <!-- 主控制按钮 -->
      <div class="main-controls">
        <button class="control-btn" @click="homeView" :title="t('map.toolbar.homeView')">
          🏠
        </button>
        <button class="control-btn" @click="fullscreen" :title="t('map.toolbar.fullscreen')">
          ⛶
        </button>
        <button class="control-btn" @click="showLayerPanel = !showLayerPanel" :title="t('map.toolbar.layers')">
          📚
        </button>
        <button class="control-btn" @click="showPrintPanel = !showPrintPanel" :title="t('map.toolbar.printSettings')">
          🖨️
        </button>
        <button class="control-btn" @click="showSettingsPanel = !showSettingsPanel" :title="t('common.edit')">
          ⚙️
        </button>
        <button class="control-btn" @click="showShortcuts = !showShortcuts" :title="t('map.toolbar.shortcuts')">
          ⌨️
        </button>
        <button class="control-btn" @click="showSldEditor = !showSldEditor" title="SLD 样式编辑器">
          🎨
        </button>
        <button class="control-btn" @click="clearAll" :title="t('map.toolbar.clearAll')">
          🗑️
        </button>
      </div>

      <!-- Drawing Panel -->
      <div v-if="['draw-point', 'draw-line', 'draw-polygon', 'draw-circle'].includes(activeTool || '')" class="draw-panel">
        <div class="draw-hint">
          <span v-if="activeTool === 'draw-point'">📍 {{ t('map.toolbar.drawPointPanel') }} - 点击地图放置点位</span>
          <span v-else-if="activeTool === 'draw-line'">📏 {{ t('map.toolbar.drawLinePanel') }} - 依次点击绘制线条，双击结束</span>
          <span v-else-if="activeTool === 'draw-polygon'">🔷 {{ t('map.toolbar.drawPolygonPanel') }} - 依次点击绘制多边形，双击结束</span>
          <span v-else-if="activeTool === 'draw-circle'">⭕ {{ t('map.toolbar.drawCircle') }} - 点击圆心，再点击圆上一点</span>
        </div>
        <button class="draw-cancel-btn" @click="activeTool = null">
          {{ t('common.cancel') }}
        </button>
      </div>
    </div>
    
    <!-- 右侧信息 -->
    <div class="toolbar-right">
      <!-- 书签 -->
      <div class="bookmark-dropdown">
        <button class="info-btn" @click="$refs.bookmarkMenu.toggle($event)">
          🔖 {{ t('map.toolbar.bookmarks') }}
        </button>
        <b-dropdown ref="bookmarkMenu" hide-on-click>
          <template #header>{{ t('map.toolbar.bookmarks') }}</template>
          <b-dropdown-item v-for="bm in bookmarks" :key="bm.id" @click="flyTo(bm.position.lon, bm.position.lat, bm.position.zoom)">
            <strong>{{ bm.name }}</strong>
            <small v-if="bm.description">{{ bm.description }}</small>
          </b-dropdown-item>
          <b-dropdown-divider />
          <b-dropdown-item @click="addBookmark">
            ➕ {{ t('map.toolbar.addBookmark') }}
          </b-dropdown-item>
        </b-dropdown>
      </div>
      
      <!-- 坐标信息 -->
      <div class="coordinates">
        <span class="coord-label">📍</span>
        <span class="coord-value">
          {{ cursorPosition.lat }}, {{ cursorPosition.lon }}
        </span>
        <span class="coord-height">
          H: {{ cursorPosition.height }}m
        </span>
      </div>
    </div>
    
    <!-- 图层面板 (弹出) -->
    <Transition name="panel-slide">
    <div v-if="showLayerPanel" class="floating-panel layer-panel">
      <div class="panel-header">
        <h4>📚 {{ t('map.toolbar.layerManager') }}</h4>
        <button class="close-btn" @click="showLayerPanel = false">×</button>
      </div>
      <div class="panel-content">
        <div v-for="layer in layers" :key="layer.id" class="layer-item">
          <label class="layer-toggle">
            <input type="checkbox" v-model="layer.visible" />
            <span class="layer-name">{{ layer.name }}</span>
          </label>
          <div class="layer-opacity">
            <input type="range" min="0" max="100" v-model="layer.opacity" />
            <span>{{ layer.opacity }}%</span>
          </div>
        </div>
      </div>
    </div>
    </Transition>
    
    <!-- 打印面板 (弹出) -->
    <Transition name="panel-slide">
    <div v-if="showPrintPanel" class="floating-panel print-panel">
      <div class="panel-header">
        <h4>🖨️ {{ t('map.toolbar.printSettings') }}</h4>
        <button class="close-btn" @click="showPrintPanel = false">×</button>
      </div>
      <div class="panel-content">
        <div class="form-group">
          <label>{{ t('map.toolbar.title') }}</label>
          <input type="text" v-model="printOptions.title" />
        </div>
        <div class="form-group">
          <label>{{ t('map.toolbar.format') }}</label>
          <select v-model="printOptions.format">
            <option value="png">PNG</option>
            <option value="jpeg">JPEG</option>
            <option value="pdf">PDF</option>
          </select>
        </div>
        <div class="form-group">
          <label>{{ t('map.toolbar.resolution') }}</label>
          <select v-model="printOptions.width">
            <option :value="1920">1920×1080 (Full HD)</option>
            <option :value="2560">2560×1440 (2K)</option>
            <option :value="3840">3840×2160 (4K)</option>
          </select>
        </div>
        <div class="form-group checkbox">
          <label>
            <input type="checkbox" v-model="printOptions.legend" /> {{ t('map.toolbar.legend') }}
          </label>
          <label>
            <input type="checkbox" v-model="printOptions.scale" /> {{ t('map.toolbar.scale') }}
          </label>
          <label>
            <input type="checkbox" v-model="printOptions.northArrow" /> {{ t('map.toolbar.northArrow') }}
          </label>
        </div>
        <button class="print-btn" @click="printMap">{{ t('map.toolbar.startPrint') }}</button>
      </div>
    </div>
    </Transition>
    
    <!-- 快捷键提示 -->
    <Transition name="panel-slide">
    <div v-if="showShortcuts" class="floating-panel shortcuts-panel">
      <div class="panel-header">
        <h4>⌨️ {{ t('map.toolbar.shortcuts') }}</h4>
        <button class="close-btn" @click="showShortcuts = false">×</button>
      </div>
      <div class="panel-content">
        <div class="shortcut-grid">
          <div v-for="tool in tools" :key="tool.id" class="shortcut-item">
            <kbd>{{ tool.shortcut }}</kbd>
            <span>{{ t(tool.name) }}</span>
          </div>
        </div>
        <div class="shortcut-item">
          <kbd>2D</kbd>
          <span>{{ t('map.toolbar.switch2D') }}</span>
        </div>
        <div class="shortcut-item">
          <kbd>3D</kbd>
          <span>{{ t('map.toolbar.switch3D') }}</span>
        </div>
        <div class="shortcut-item">
          <kbd>Esc</kbd>
          <span>{{ t('map.toolbar.cancelOperation') }}</span>
        </div>
      </div>
    </div>
    </Transition>

    <!-- SLD 样式编辑器面板 -->
    <Transition name="panel-slide">
    <div v-if="showSldEditor" class="sld-editor-panel">
      <div class="panel-header">
        <h4>🎨 SLD 样式编辑器</h4>
        <button class="close-btn" @click="showSldEditor = false">×</button>
      </div>
      <div class="sld-editor-container">
        <SldEditor
          :layer-name="sldLayerName"
          @save="(sld, name) => { sldLayerName = name; showSldEditor = false; }"
          @cancel="showSldEditor = false"
        />
      </div>
    </div>
    </Transition>
  </div>
</template>

<style scoped>
.map-toolbar {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.5rem 1rem;
  background: linear-gradient(180deg, rgba(30, 41, 59, 0.98) 0%, rgba(30, 41, 59, 0.95) 100%);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  gap: 1rem;
  flex-wrap: wrap;
}

/* 工具栏 */
.toolbar-left,
.toolbar-center,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.tool-group {
  display: flex;
  gap: 0.25rem;
  background: rgba(255, 255, 255, 0.05);
  padding: 0.25rem;
  border-radius: 8px;
}

.tool-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0.5rem 0.75rem;
  background: transparent;
  border: none;
  border-radius: 6px;
  color: #94a3b8;
  cursor: pointer;
  transition: all 0.2s;
  min-width: 50px;
}

.tool-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: white;
}

.tool-btn.active {
  background: #2563eb;
  color: white;
}

.tool-icon {
  font-size: 1.25rem;
  line-height: 1;
}

.tool-name {
  font-size: 0.625rem;
  margin-top: 0.25rem;
  white-space: nowrap;
}

.separator {
  width: 1px;
  height: 32px;
  background: rgba(255, 255, 255, 0.2);
  margin: 0 0.25rem;
}

/* 底图选择 */
.basemap-selector {
  display: flex;
  gap: 0.25rem;
  background: rgba(255, 255, 255, 0.05);
  padding: 0.25rem;
  border-radius: 8px;
}

.basemap-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 2px solid transparent;
  border-radius: 6px;
  font-size: 1.125rem;
  cursor: pointer;
  transition: all 0.2s;
}

.basemap-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

.basemap-btn.active {
  background: rgba(37, 99, 235, 0.3);
  border-color: #2563eb;
}

/* 视图预设 */
.view-presets {
  display: flex;
  gap: 0.25rem;
}

.preset-btn {
  padding: 0.375rem 0.625rem;
  background: rgba(255, 255, 255, 0.05);
  border: none;
  border-radius: 6px;
  color: #94a3b8;
  font-size: 0.75rem;
  cursor: pointer;
  transition: all 0.2s;
}

.preset-btn:hover {
  background: rgba(255, 255, 255, 0.15);
  color: white;
}

/* 主控制按钮 */
.main-controls {
  display: flex;
  gap: 0.25rem;
}

.control-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  border-radius: 6px;
  font-size: 1.125rem;
  cursor: pointer;
  transition: all 0.2s;
}

.control-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

/* 信息区域 */
.bookmark-dropdown {
  position: relative;
}

.info-btn {
  padding: 0.5rem 0.75rem;
  background: rgba(255, 255, 255, 0.05);
  border: none;
  border-radius: 6px;
  color: #94a3b8;
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s;
}

.info-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: white;
}

.coordinates {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  background: rgba(0, 0, 0, 0.3);
  border-radius: 6px;
  font-family: 'Monaco', 'Menlo', monospace;
  font-size: 0.75rem;
}

.coord-label {
  font-size: 0.875rem;
}

.coord-value {
  color: #60a5fa;
}

.coord-height {
  color: #64748b;
}

/* 测量面板 */
.measure-panel {
  position: absolute;
  top: 100%;
  left: 0;
  margin-top: 0.5rem;
  background: rgba(30, 41, 59, 0.98);
  border-radius: 12px;
  padding: 1rem;
  min-width: 250px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.5);
  z-index: 1000;
}

.measure-types {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.measure-btn {
  flex: 1;
  padding: 0.5rem;
  background: rgba(255, 255, 255, 0.05);
  border: none;
  border-radius: 6px;
  color: #94a3b8;
  font-size: 0.75rem;
  cursor: pointer;
  transition: all 0.2s;
}

.measure-btn:hover,
.measure-btn.active {
  background: #2563eb;
  color: white;
}

.measure-start-btn {
  width: 100%;
  padding: 0.5rem;
  background: #2563eb;
  border: none;
  border-radius: 6px;
  color: white;
  font-size: 0.75rem;
  cursor: pointer;
  margin-bottom: 0.5rem;
  transition: background 0.2s;
}

.measure-start-btn:hover {
  background: #1d4ed8;
}

.measure-result {
  display: flex;
  justify-content: space-between;
  padding: 0.5rem;
  background: rgba(0, 0, 0, 0.2);
  border-radius: 6px;
}

.result-value {
  color: #60a5fa;
  font-weight: 600;
}

/* Drawing Panel */
.draw-panel {
  position: absolute;
  top: 100%;
  left: 0;
  margin-top: 0.5rem;
  background: rgba(30, 41, 59, 0.98);
  border-radius: 12px;
  padding: 0.75rem 1rem;
  min-width: 300px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.5);
  z-index: 1000;
}

.draw-hint {
  font-size: 0.75rem;
  color: #94a3b8;
  margin-bottom: 0.5rem;
  padding: 0.5rem;
  background: rgba(0, 0, 0, 0.2);
  border-radius: 6px;
}

.draw-cancel-btn {
  padding: 0.375rem 0.75rem;
  background: rgba(255, 255, 255, 0.1);
  border: none;
  border-radius: 6px;
  color: #94a3b8;
  font-size: 0.75rem;
  cursor: pointer;
  transition: all 0.2s;
}

.draw-cancel-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  color: white;
}

/* 浮动面板 */
.floating-panel {
  position: absolute;
  top: calc(100% + 0.5rem);
  right: 0;
  background: rgba(30, 41, 59, 0.98);
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.5);
  z-index: 1000;
  min-width: 280px;
  max-height: 400px;
  overflow: hidden;
}

.layer-panel,
.print-panel,
.shortcuts-panel {
  right: 1rem;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.panel-header h4 {
  margin: 0;
  font-size: 0.875rem;
  font-weight: 600;
  color: white;
}

.close-btn {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.1);
  border: none;
  border-radius: 4px;
  color: #94a3b8;
  cursor: pointer;
  font-size: 1.25rem;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  color: white;
}

.panel-content {
  padding: 1rem;
  overflow-y: auto;
  max-height: 320px;
}

/* 图层项 */
.layer-item {
  margin-bottom: 0.75rem;
}

.layer-toggle {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
}

.layer-toggle input[type="checkbox"] {
  width: 16px;
  height: 16px;
  accent-color: #2563eb;
}

.layer-name {
  color: white;
  font-size: 0.875rem;
}

.layer-opacity {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.25rem;
  margin-left: 1.5rem;
}

.layer-opacity input[type="range"] {
  flex: 1;
  height: 4px;
  accent-color: #2563eb;
}

.layer-opacity span {
  font-size: 0.75rem;
  color: #64748b;
  min-width: 35px;
}

/* 表单 */
.form-group {
  margin-bottom: 0.75rem;
}

.form-group label {
  display: block;
  font-size: 0.75rem;
  color: #94a3b8;
  margin-bottom: 0.25rem;
}

.form-group input[type="text"],
.form-group select {
  width: 100%;
  padding: 0.5rem;
  background: rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  color: white;
  font-size: 0.875rem;
}

.form-group.checkbox {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem 1rem;
}

.form-group.checkbox label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
}

.form-group.checkbox input[type="checkbox"] {
  accent-color: #2563eb;
}

.print-btn {
  width: 100%;
  padding: 0.75rem;
  background: #2563eb;
  border: none;
  border-radius: 8px;
  color: white;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.print-btn:hover {
  background: #1d4ed8;
}

/* 快捷键 */
.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.5rem;
}

.shortcut-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.25rem 0;
}

kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  height: 24px;
  padding: 0 0.5rem;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  font-family: inherit;
  font-size: 0.75rem;
  color: #60a5fa;
}

.shortcut-item span {
  font-size: 0.75rem;
  color: #94a3b8;
}

/* SLD 编辑器面板 */
.sld-editor-panel {
  position: absolute;
  top: calc(100% + 0.5rem);
  left: 50%;
  transform: translateX(-50%);
  width: 900px;
  max-width: 90vw;
  background: rgba(30, 41, 59, 0.98);
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.5);
  z-index: 1001;
  overflow: hidden;
}

.sld-editor-panel .panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.sld-editor-panel .panel-header h4 {
  margin: 0;
  font-size: 0.875rem;
  font-weight: 600;
  color: white;
}

.sld-editor-panel .close-btn {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.1);
  border: none;
  border-radius: 4px;
  color: #94a3b8;
  cursor: pointer;
  font-size: 1.25rem;
}

.sld-editor-panel .close-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  color: white;
}

.sld-editor-container {
  height: 500px;
}

/* Panel slide transition */
.panel-slide-enter-active,
.panel-slide-leave-active {
  transition: transform 0.3s ease, opacity 0.3s ease;
}
.panel-slide-enter-from,
.panel-slide-leave-to {
  transform: translateX(100%);
  opacity: 0;
}
</style>
