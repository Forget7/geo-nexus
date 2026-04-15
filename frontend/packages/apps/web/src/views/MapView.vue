<script setup lang="ts">
import { ref, computed } from 'vue'
import { MapView as MapViewComponent, OfflineDownloader } from '@geonex/components/map'

const mapMode = ref<'2d' | '3d'>('2d')
const tileType = ref('osm')
const center = ref<[number, number]>([35.0, 105.0])
const zoom = ref(10)
const activeSidebarTab = ref<'layers' | 'offline' | 'route' | null>('layers')

const tileOptions = [
  { value: 'osm', label: 'OpenStreetMap' },
  { value: 'satellite', label: 'Esri卫星' },
  { value: 'dark', label: '暗色主题' },
  { value: 'terrain', label: '地形图' }
]

const mapBounds = ref({
  minLon: 73.0,
  minLat: 18.0,
  maxLon: 135.0,
  maxLat: 53.0
})

// ========== 图层管理 ==========
interface LayerItem {
  id: string
  name: string
  type: 'vector' | 'raster' | '3d' | 'geojson'
  visible: boolean
  zIndex: number
  url?: string
}

const layers = ref<LayerItem[]>([
  { id: 'layer-1', name: '河流', type: 'geojson', visible: true, zIndex: 0, url: '' },
  { id: 'layer-2', name: '道路', type: 'vector', visible: true, zIndex: 1, url: '' },
  { id: 'layer-3', name: '行政区划', type: 'vector', visible: false, zIndex: 2, url: '' },
])

const dragIndex = ref<number | null>(null)
const dropTargetIndex = ref<number | null>(null)

function onDragStart(index: number) {
  dragIndex.value = index
}

function onDragOver(event: DragEvent, index: number) {
  event.preventDefault()
  dropTargetIndex.value = index
}

function onDrop(index: number) {
  if (dragIndex.value === null || dragIndex.value === index) {
    dragIndex.value = null
    dropTargetIndex.value = null
    return
  }
  const draggedItem = layers.value[dragIndex.value]
  const targetItem = layers.value[index]

  // 找到实际的两个位置
  const oldIndex = dragIndex.value
  const newIndex = index

  // 从原位置移除
  layers.value.splice(oldIndex, 1)
  // 插入到新位置
  layers.value.splice(newIndex, 0, draggedItem)

  // 更新 z-index
  layers.value.forEach((layer, i) => {
    layer.zIndex = i
  })

  dragIndex.value = null
  dropTargetIndex.value = null
}

function onDragEnd() {
  dragIndex.value = null
  dropTargetIndex.value = null
}

function toggleLayerVisibility(index: number) {
  layers.value[index].visible = !layers.value[index].visible
}

function removeLayer(index: number) {
  layers.value.splice(index, 1)
  // 重新排 z-index
  layers.value.forEach((layer, i) => {
    layer.zIndex = i
  })
}

function addLayer() {
  const id = 'layer-' + Date.now()
  layers.value.push({
    id,
    name: '新图层',
    type: 'geojson',
    visible: true,
    zIndex: layers.value.length,
    url: ''
  })
}

const layerTypeIcon = (type: string) => {
  const icons: Record<string, string> = {
    vector: '🔷',
    raster: '🗺️',
    '3d': '🎲',
    geojson: '📍'
  }
  return icons[type] || '📄'
}

const sortedLayers = computed(() =>
  [...layers.value].sort((a, b) => b.zIndex - a.zIndex)
)

// ========== 路径规划 ==========
const routeMode = ref<'driving' | 'walking' | 'cycling'>('driving')
const routeFrom = ref('')
const routeTo = ref('')
const routeResult = ref<{ geometry: any; distance: number; duration: number; warning?: string } | null>(null)
const routeLoading = ref(false)

async function planRoute() {
  // 解析经纬度
  const parseCoord = (str: string): [number, number] | null => {
    str = str.trim()
    // 支持 "lon,lat" 或 "lat lon" 两种格式
    if (str.includes(',')) {
      const parts = str.split(',').map(Number)
      if (parts.length === 2 && !isNaN(parts[0]) && !isNaN(parts[1])) {
        return [parts[0], parts[1]] // [lon, lat]
      }
    }
    return null
  }

  const from = parseCoord(routeFrom.value)
  const to = parseCoord(routeTo.value)

  if (!from || !to) {
    alert('请输入有效的坐标，格式：经度,纬度（例如：116.4074,39.9042）')
    return
  }

  routeLoading.value = true
  routeResult.value = null

  try {
    const resp = await fetch('/api/v1/routes/plan', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        from: [from[0], from[1]],   // [lon, lat]
        to: [to[0], to[1]],         // [lon, lat]
        mode: routeMode.value
      })
    })
    const data = await resp.json()
    routeResult.value = data

    if (data.warning) {
      console.warn(data.warning)
    }
  } catch (e) {
    alert('路线规划请求失败')
  } finally {
    routeLoading.value = false
  }
}

function formatDistance(m: number) {
  if (m >= 1000) return (m / 1000).toFixed(2) + ' km'
  return m.toFixed(0) + ' m'
}

function formatDuration(s: number) {
  if (s >= 3600) return Math.floor(s / 3600) + 'h ' + Math.floor((s % 3600) / 60) + 'min'
  if (s >= 60) return Math.floor(s / 60) + ' min'
  return s.toFixed(0) + ' s'
}

// ========== 场景模板 ==========
const sceneName = ref('')
const sceneDesc = ref('')
const sceneTemplates = ref<any[]>([])
const showSceneSave = ref(false)

async function saveScene() {
  if (!sceneName.value.trim()) {
    alert('请输入模板名称')
    return
  }

  try {
    await fetch('/api/v1/scenes/templates', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: sceneName.value,
        description: sceneDesc.value,
        camera: {
          lon: center.value[1],
          lat: center.value[0],
          height: 10000,
          heading: 0,
          pitch: 0
        },
        timeRangeStart: '',
        timeRangeEnd: '',
        layers: layers.value.map((l, i) => ({
          id: l.id,
          name: l.name,
          type: l.type,
          visible: l.visible,
          zIndex: l.zIndex,
          url: l.url
        }))
      })
    })
    sceneName.value = ''
    sceneDesc.value = ''
    showSceneSave.value = false
    loadSceneTemplates()
    alert('场景已保存')
  } catch (e) {
    alert('保存失败')
  }
}

async function loadSceneTemplates() {
  try {
    const resp = await fetch('/api/v1/scenes/templates?page=0&size=20')
    const data = await resp.json()
    sceneTemplates.value = data.content || []
  } catch (e) {
    // ignore
  }
}

async function applyScene(id: string) {
  try {
    const resp = await fetch(`/api/v1/scenes/templates/${id}/apply`)
    const data = await resp.json()
    if (data.camera) {
      center.value = [data.camera.lat || 39.9, data.camera.lon || 116.4]
    }
    if (data.layers) {
      layers.value = data.layers.map((l: any, i: number) => ({
        id: l.id || 'layer-' + i,
        name: l.name || '图层' + i,
        type: l.type || 'geojson',
        visible: l.visible !== false,
        zIndex: l.zIndex ?? i,
        url: l.url || ''
      }))
    }
    activeSidebarTab.value = 'layers'
  } catch (e) {
    alert('应用场景失败')
  }
}

// 初始化加载模板
loadSceneTemplates()

function switchMapMode(mode: '2d' | '3d') {
  mapMode.value = mode
}

function flyTo(lat: number, lon: number) {
  center.value = [lat, lon]
  zoom.value = 14
}
</script>

<template>
  <div class="map-view">
    <!-- 顶部工具栏 -->
    <header class="map-toolbar">
      <div class="toolbar-left">
        <h1>🗺️ 地图</h1>
      </div>

      <div class="toolbar-center">
        <!-- 地图模式切换 -->
        <div class="mode-switch">
          <button
            :class="{ active: mapMode === '2d' }"
            @click="switchMapMode('2d')"
          >
            2D
          </button>
          <button
            :class="{ active: mapMode === '3d' }"
            @click="switchMapMode('3d')"
          >
            3D
          </button>
        </div>

        <!-- 底图切换 -->
        <select v-model="tileType" class="tile-select">
          <option v-for="opt in tileOptions" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </option>
        </select>
      </div>

      <div class="toolbar-right">
        <button class="tool-btn" @click="flyTo(39.9042, 116.4074)">
          北京
        </button>
        <button class="tool-btn" @click="flyTo(31.2304, 121.4737)">
          上海
        </button>
        <button class="tool-btn route-btn" @click="activeSidebarTab = activeSidebarTab === 'route' ? null : 'route'">
          🛣 路径规划
        </button>
      </div>
    </header>

    <!-- 主内容区 -->
    <div class="map-content-wrapper">
      <!-- 侧边栏 -->
      <aside class="map-sidebar">
        <div class="sidebar-tabs">
          <button
            :class="['sidebar-tab', { active: activeSidebarTab === 'layers' }]"
            @click="activeSidebarTab = activeSidebarTab === 'layers' ? null : 'layers'"
          >
            📚 图层
          </button>
          <button
            :class="['sidebar-tab', { active: activeSidebarTab === 'route' }]"
            @click="activeSidebarTab = activeSidebarTab === 'route' ? null : 'route'"
          >
            🛣 路线
          </button>
          <button
            :class="['sidebar-tab', { active: activeSidebarTab === 'offline' }]"
            @click="activeSidebarTab = activeSidebarTab === 'offline' ? null : 'offline'"
          >
            📥 离线
          </button>
        </div>

        <!-- ========== 图层面板 ========== -->
        <div v-if="activeSidebarTab === 'layers'" class="sidebar-panel">
          <!-- 操作栏 -->
          <div class="layer-toolbar">
            <button class="layer-add-btn" @click="addLayer">+ 添加图层</button>
            <button class="layer-save-btn" @click="showSceneSave = true">💾 保存场景</button>
          </div>

          <!-- 拖拽提示 -->
          <p class="drag-hint">👆 拖拽排序，👁 切换可见，🗑 删除</p>

          <!-- 保存场景弹窗 -->
          <div v-if="showSceneSave" class="scene-save-form">
            <input v-model="sceneName" placeholder="模板名称" class="scene-input" />
            <input v-model="sceneDesc" placeholder="描述（可选）" class="scene-input" />
            <div class="scene-save-actions">
              <button @click="saveScene" class="save-confirm-btn">保存</button>
              <button @click="showSceneSave = false" class="save-cancel-btn">取消</button>
            </div>
          </div>

          <!-- 已有模板列表 -->
          <div v-if="sceneTemplates.length > 0" class="scene-templates-list">
            <p class="section-label">已保存场景：</p>
            <div
              v-for="tmpl in sceneTemplates"
              :key="tmpl.id"
              class="scene-template-item"
              @click="applyScene(tmpl.id)"
            >
              <span>{{ tmpl.name }}</span>
              <button class="apply-btn" @click.stop="applyScene(tmpl.id)">应用</button>
            </div>
          </div>

          <!-- 图层列表 -->
          <div v-if="layers.length === 0" class="no-layers">
            <p>暂无图层</p>
          </div>

          <div
            v-for="(layer, index) in layers"
            :key="layer.id"
            class="layer-item"
            :class="{
              'dragging': dragIndex === index,
              'drop-target': dropTargetIndex === index && dragIndex !== index
            }"
            draggable="true"
            @dragstart="onDragStart(index)"
            @dragover="onDragOver($event, index)"
            @drop="onDrop(index)"
            @dragend="onDragEnd"
          >
            <!-- 拖拽手柄 -->
            <span class="drag-handle">⋮⋮</span>

            <!-- 可见性开关 -->
            <button class="visibility-btn" @click="toggleLayerVisibility(index)">
              {{ layer.visible ? '👁' : '🔲' }}
            </button>

            <!-- 图层名称 -->
            <span class="layer-name" :class="{ 'layer-hidden': !layer.visible }">
              {{ layer.name }}
            </span>

            <!-- 类型图标 -->
            <span class="layer-type-icon">{{ layerTypeIcon(layer.type) }}</span>

            <!-- 删除按钮 -->
            <button class="layer-delete-btn" @click="removeLayer(index)">
              🗑
            </button>
          </div>
        </div>

        <!-- ========== 路径规划面板 ========== -->
        <div v-if="activeSidebarTab === 'route'" class="sidebar-panel">
          <h3 class="panel-title">🛣 路径规划</h3>

          <div class="route-form">
            <label class="route-label">起点</label>
            <input
              v-model="routeFrom"
              placeholder="经度,纬度（如：116.4074,39.9042）"
              class="route-input"
            />

            <label class="route-label">终点</label>
            <input
              v-model="routeTo"
              placeholder="经度,纬度"
              class="route-input"
            />

            <label class="route-label">出行方式</label>
            <div class="route-modes">
              <button
                :class="['mode-btn', { active: routeMode === 'driving' }]"
                @click="routeMode = 'driving'"
              >
                🚗 驾车
              </button>
              <button
                :class="['mode-btn', { active: routeMode === 'walking' }]"
                @click="routeMode = 'walking'"
              >
                🚶 步行
              </button>
              <button
                :class="['mode-btn', { active: routeMode === 'cycling' }]"
                @click="routeMode = 'cycling'"
              >
                🚴 骑行
              </button>
            </div>

            <button
              class="route-plan-btn"
              :disabled="routeLoading || !routeFrom || !routeTo"
              @click="planRoute"
            >
              {{ routeLoading ? '规划中...' : '🛣 规划路线' }}
            </button>
          </div>

          <!-- 路线结果 -->
          <div v-if="routeResult" class="route-result">
            <div class="result-row">
              <span class="result-label">距离</span>
              <span class="result-value">{{ formatDistance(routeResult.distance) }}</span>
            </div>
            <div class="result-row">
              <span class="result-label">预计时间</span>
              <span class="result-value">{{ formatDuration(routeResult.duration) }}</span>
            </div>
            <div v-if="routeResult.warning" class="result-warning">
              ⚠️ {{ routeResult.warning }}
            </div>
          </div>
        </div>

        <!-- ========== 离线下载面板 ========== -->
        <div v-if="activeSidebarTab === 'offline'" class="sidebar-panel">
          <OfflineDownloader :map-bounds="mapBounds" />
        </div>
      </aside>

      <!-- 地图内容 -->
      <div class="map-content">
        <MapViewComponent
          :mode="mapMode"
          :tile-type="tileType"
        />
      </div>
    </div>

    <!-- 底部状态栏 -->
    <footer class="map-footer">
      <span>中心点: {{ center[0].toFixed(4) }}, {{ center[1].toFixed(4) }}</span>
      <span>缩放: {{ zoom }}</span>
      <span>模式: {{ mapMode === '2d' ? 'Leaflet' : 'Cesium' }}</span>
      <span>图层: {{ layers.length }}</span>
    </footer>
  </div>
</template>

<style scoped>
.map-view {
  display: flex;
  flex-direction: column;
  height: 100vh;
}

.map-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1.5rem;
  background: white;
  border-bottom: 1px solid #e2e8f0;
}

.toolbar-left h1 {
  margin: 0;
  font-size: 1.25rem;
  color: #1e293b;
}

.toolbar-center {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.mode-switch {
  display: flex;
  background: #f1f5f9;
  border-radius: 8px;
  padding: 4px;
}

.mode-switch button {
  padding: 0.5rem 1rem;
  border: none;
  background: transparent;
  border-radius: 6px;
  font-size: 0.875rem;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;
}

.mode-switch button.active {
  background: white;
  color: #2563eb;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.tile-select {
  padding: 0.5rem 0.75rem;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 0.875rem;
  background: white;
}

.toolbar-right {
  display: flex;
  gap: 0.5rem;
}

.tool-btn {
  padding: 0.5rem 1rem;
  border: 1px solid #e2e8f0;
  background: white;
  border-radius: 6px;
  font-size: 0.875rem;
  color: #475569;
  cursor: pointer;
  transition: all 0.2s;
}

.tool-btn:hover {
  background: #f1f5f9;
  border-color: #2563eb;
}

.route-btn {
  background: #eff6ff;
  border-color: #3b82f6;
  color: #2563eb;
}

/* 侧边栏 */
.map-content-wrapper {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.map-sidebar {
  width: 300px;
  background: white;
  border-right: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-tabs {
  display: flex;
  border-bottom: 1px solid #e2e8f0;
}

.sidebar-tab {
  flex: 1;
  padding: 0.75rem 0.5rem;
  background: transparent;
  border: none;
  border-bottom: 2px solid transparent;
  font-size: 0.8rem;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;
}

.sidebar-tab:hover {
  color: #2563eb;
}

.sidebar-tab.active {
  color: #2563eb;
  border-bottom-color: #2563eb;
}

.sidebar-panel {
  flex: 1;
  overflow-y: auto;
  padding: 0.75rem;
}

/* ========== 图层面板 ========== */
.layer-toolbar {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}

.layer-add-btn, .layer-save-btn {
  flex: 1;
  padding: 0.4rem;
  border: 1px solid #e2e8f0;
  background: white;
  border-radius: 6px;
  font-size: 0.75rem;
  cursor: pointer;
  transition: all 0.2s;
}

.layer-add-btn:hover {
  background: #eff6ff;
  border-color: #3b82f6;
  color: #2563eb;
}

.layer-save-btn:hover {
  background: #f0fdf4;
  border-color: #22c55e;
  color: #16a34a;
}

.drag-hint {
  font-size: 0.7rem;
  color: #94a3b8;
  margin: 0.25rem 0 0.75rem 0;
  text-align: center;
}

/* 场景保存表单 */
.scene-save-form {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 0.75rem;
  margin-bottom: 0.75rem;
}

.scene-input {
  width: 100%;
  padding: 0.4rem;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
  font-size: 0.8rem;
  margin-bottom: 0.5rem;
  box-sizing: border-box;
}

.scene-save-actions {
  display: flex;
  gap: 0.5rem;
}

.save-confirm-btn {
  flex: 1;
  padding: 0.4rem;
  background: #2563eb;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.8rem;
}

.save-cancel-btn {
  flex: 1;
  padding: 0.4rem;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.8rem;
}

/* 已保存场景 */
.scene-templates-list {
  margin-bottom: 0.75rem;
}

.section-label {
  font-size: 0.7rem;
  color: #64748b;
  margin: 0.25rem 0;
}

.scene-template-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.4rem 0.5rem;
  background: #f8fafc;
  border-radius: 6px;
  margin-bottom: 0.25rem;
  font-size: 0.8rem;
  cursor: pointer;
}

.scene-template-item:hover {
  background: #eff6ff;
}

.apply-btn {
  padding: 0.2rem 0.5rem;
  background: #2563eb;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 0.7rem;
  cursor: pointer;
}

/* 图层列表 */
.no-layers {
  text-align: center;
  padding: 2rem 0;
  color: #94a3b8;
  font-size: 0.875rem;
}

.layer-item {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.5rem;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  margin-bottom: 0.4rem;
  background: white;
  cursor: grab;
  transition: all 0.2s;
  user-select: none;
}

.layer-item:hover {
  background: #f8fafc;
  border-color: #cbd5e1;
}

.layer-item.dragging {
  opacity: 0.5;
  background: #eff6ff;
  border-color: #3b82f6;
}

.layer-item.drop-target {
  border-top: 2px solid #3b82f6;
  background: #eff6ff;
}

.drag-handle {
  color: #94a3b8;
  font-size: 0.8rem;
  cursor: grab;
}

.visibility-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 1rem;
  padding: 0;
  line-height: 1;
}

.layer-name {
  flex: 1;
  font-size: 0.85rem;
  color: #1e293b;
}

.layer-name.layer-hidden {
  color: #94a3b8;
  text-decoration: line-through;
}

.layer-type-icon {
  font-size: 0.9rem;
}

.layer-delete-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 0.9rem;
  padding: 0;
  opacity: 0.5;
  transition: opacity 0.2s;
}

.layer-delete-btn:hover {
  opacity: 1;
}

/* ========== 路径规划面板 ========== */
.panel-title {
  margin: 0 0 0.75rem 0;
  font-size: 1rem;
  color: #1e293b;
}

.route-form {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.route-label {
  font-size: 0.8rem;
  color: #64748b;
  font-weight: 500;
}

.route-input {
  padding: 0.5rem;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 0.85rem;
  background: white;
}

.route-input:focus {
  outline: none;
  border-color: #3b82f6;
}

.route-modes {
  display: flex;
  gap: 0.4rem;
}

.mode-btn {
  flex: 1;
  padding: 0.5rem;
  border: 1px solid #e2e8f0;
  background: white;
  border-radius: 6px;
  font-size: 0.8rem;
  cursor: pointer;
  transition: all 0.2s;
}

.mode-btn.active {
  background: #eff6ff;
  border-color: #3b82f6;
  color: #2563eb;
}

.route-plan-btn {
  margin-top: 0.5rem;
  padding: 0.6rem;
  background: #2563eb;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s;
}

.route-plan-btn:hover:not(:disabled) {
  background: #1d4ed8;
}

.route-plan-btn:disabled {
  background: #94a3b8;
  cursor: not-allowed;
}

/* 路线结果 */
.route-result {
  margin-top: 1rem;
  padding: 0.75rem;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: 8px;
}

.result-row {
  display: flex;
  justify-content: space-between;
  padding: 0.3rem 0;
}

.result-label {
  font-size: 0.8rem;
  color: #64748b;
}

.result-value {
  font-size: 0.85rem;
  font-weight: 600;
  color: #15803d;
}

.result-warning {
  margin-top: 0.5rem;
  font-size: 0.75rem;
  color: #b45309;
  background: #fffbeb;
  padding: 0.4rem;
  border-radius: 4px;
}

/* ========== 通用 ========== */
.map-content {
  flex: 1;
  overflow: hidden;
}

.map-footer {
  display: flex;
  gap: 2rem;
  padding: 0.5rem 1.5rem;
  background: #f8fafc;
  border-top: 1px solid #e2e8f0;
  font-size: 0.75rem;
  color: #64748b;
}
</style>
