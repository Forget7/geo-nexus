<script setup lang="ts">
/**
 * CesiumPanel - Cesium 3D地图面板组件
 * 提供特效切换、位置预设、实体管理等UI
 */

import { ref, shallowRef, onMounted } from 'vue'
import CesiumView from './CesiumView.vue'
import type { Viewer } from 'cesium'
import type { EffectManager } from '../../../packages/hooks/src/useCesium'

const emit = defineEmits<{
  (e: 'ready', viewer: Viewer): void
  (e: 'error', error: Error): void
}>()

// Refs
const cesiumViewRef = ref<InstanceType<typeof CesiumView> | null>(null)
const viewer = shallowRef<Viewer | null>(null)
const effectManager = shallowRef<EffectManager | null>(null)

// 状态
const isReady = ref(false)
const currentEffect = ref<string>('none')
const cameraInfo = ref({ heading: 0, pitch: 0, range: 0 })

// 特效列表
const effects = [
  { id: 'none', name: '无', icon: '➖' },
  { id: 'rain', name: '雨', icon: '🌧️' },
  { id: 'snow', name: '雪', icon: '❄️' },
  { id: 'sandstorm', name: '沙尘', icon: '🌪️' },
  { id: 'bloom', name: '泛光', icon: '✨' },
  { id: 'nightVision', name: '夜视', icon: '🌙' },
  { id: 'thermal', name: '热成像', icon: '🌡️' },
  { id: 'mosaic', name: '马赛克', icon: '🧩' },
]

// 预设位置
const presets = [
  { name: '北京', position: [116.3972, 39.9165, 5000] },
  { name: '上海', position: [121.4737, 31.2304, 5000] },
  { name: '纽约', position: [-74.0060, 40.7128, 5000] },
  { name: '东京', position: [139.6917, 35.6895, 5000] },
  { name: '珠峰', position: [86.9250, 27.9881, 20000] },
]

// 实体数据
const entities = ref([
  { id: 'tiananmen', name: '天安门', position: [116.3972, 39.9165, 0], type: 'building' },
  { id: 'gugong', name: '故宫', position: [116.4122, 39.9485, 0], type: 'building' },
  { id: 'tiantan', name: '天坛', position: [116.4074, 39.9042, 0], type: 'building' },
  { id: 'olympic', name: '鸟巢', position: [116.3846, 39.9423, 0], type: 'building' },
])

const activeEntityId = ref<string | null>(null)

// CesiumView 就绪
function onCesiumReady(v: Viewer) {
  viewer.value = v
  effectManager.value = cesiumViewRef.value?.getEffectManager() ?? null
  isReady.value = true
  emit('ready', v)
  addDemoEntities()
}

// 添加演示实体
function addDemoEntities() {
  if (!viewer.value) return

  entities.value.forEach(entity => {
    viewer.value!.entities.add({
      id: entity.id,
      name: entity.name,
      position: Cesium.Cartesian3.fromDegrees(entity.position[0], entity.position[1], entity.position[2]),
      billboard: {
        image: getBillboardImage(entity.type),
        width: 32,
        height: 32,
        scaleByDistance: new Cesium.NearFarScalar(1000, 1.0, 50000, 0.2),
      },
      label: {
        text: entity.name,
        font: '14px sans-serif',
        fillColor: Cesium.Color.WHITE,
        outlineColor: Cesium.Color.BLACK,
        outlineWidth: 2,
        style: Cesium.LabelStyle.FILL_AND_OUTLINE,
        pixelOffset: new Cesium.Cartesian2(0, -25),
        scaleByDistance: new Cesium.NearFarScalar(1000, 1.0, 15000, 0.5),
        translucencyByDistance: new Cesium.NearFarScalar(15000, 1.0, 50000, 0.0),
      }
    })
  })
}

// 获取图标
function getBillboardImage(type: string): string {
  const icons: Record<string, string> = {
    building: 'data:image/svg+xml,' + encodeURIComponent(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32">
        <rect x="8" y="4" width="16" height="24" fill="#2563EB" stroke="#1E40AF" stroke-width="2"/>
        <rect x="12" y="8" width="4" height="4" fill="#93C5FD"/>
        <rect x="18" y="8" width="4" height="4" fill="#93C5FD"/>
        <rect x="12" y="16" width="4" height="4" fill="#93C5FD"/>
        <rect x="18" y="16" width="4" height="4" fill="#93C5FD"/>
        <rect x="14" y="22" width="6" height="6" fill="#1E3A8A"/>
      </svg>
    `),
    landmark: 'data:image/svg+xml,' + encodeURIComponent(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32">
        <path d="M16 2 L20 14 L32 14 L22 22 L26 32 L16 26 L6 32 L10 22 L0 14 L12 14 Z" fill="#F59E0B" stroke="#D97706" stroke-width="1"/>
      </svg>
    `),
    park: 'data:image/svg+xml,' + encodeURIComponent(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32">
        <circle cx="16" cy="12" r="10" fill="#10B981"/>
        <rect x="14" y="18" width="4" height="12" fill="#92400E"/>
      </svg>
    `)
  }
  return icons[type] || icons.building
}

// 切换特效
function toggleEffect(effectId: string) {
  const em = effectManager.value
  if (!em || !viewer.value) return

  // 清除之前的特效
  if (currentEffect.value !== 'none') {
    em.removeEffect(currentEffect.value)
  }

  currentEffect.value = effectId

  switch (effectId) {
    case 'none':
      em.clearAll()
      break
    case 'rain':
      em.addRainEffect()
      break
    case 'snow':
      em.addSnowEffect()
      break
    case 'sandstorm':
      em.addSandstormEffect()
      break
    case 'bloom':
      em.addBloomEffect({ strength: 1.5, threshold: 0.5 })
      break
    case 'nightVision':
      em.addNightVisionEffect()
      break
    case 'thermal':
      em.addThermalVisionEffect()
      break
    case 'mosaic':
      em.addMosaicEffect(8)
      break
  }
}

// 飞往预设
function flyToPreset(preset: typeof presets[0]) {
  if (!viewer.value) return
  const [lon, lat, height] = preset.position
  viewer.value.camera.flyTo({
    destination: Cesium.Cartesian3.fromDegrees(lon, lat, height),
    orientation: {
      heading: Cesium.Math.toRadians(0),
      pitch: Cesium.Math.toRadians(-45),
      roll: 0
    },
    duration: 2
  })
}

// 选择实体
function selectEntity(id: string | null) {
  activeEntityId.value = id
  if (!viewer.value || !id) return

  const entity = viewer.value.entities.getById(id)
  if (entity) {
    viewer.value.selectedEntity = entity
    const pos = entity.position?.getValue(Cesium.JulianDate.now())
    if (pos) {
      viewer.value.camera.flyTo({
        destination: Cesium.Cartesian3.clone(pos, new Cesium.Cartesian3()),
        offset: new Cesium.HeadingPitchRange(
          Cesium.Math.toRadians(0),
          Cesium.Math.toRadians(-45),
          500
        ),
        duration: 1
      })
    }
  }
}

// 截图
function takeScreenshot() {
  if (!viewer.value) return
  const link = document.createElement('a')
  link.download = `cesium-screenshot-${Date.now()}.png`
  link.href = viewer.value.canvas.toDataURL('image/png')
  link.click()
}

// 相机变化
function onCameraChange(heading: number, pitch: number, range: number) {
  cameraInfo.value = { heading, pitch, range }
}
</script>

<template>
  <div class="cesium-panel">
    <!-- Cesium 视图 -->
    <CesiumView
      ref="cesiumViewRef"
      :terrain="true"
      imagery="satellite"
      :enable-atmosphere="true"
      :enable-fog="true"
      @ready="onCesiumReady"
      @camera-change="onCameraChange"
    >
      <!-- 加载指示器 -->
      <div v-if="!isReady" class="loading-overlay">
        <div class="loading-spinner"></div>
        <p>加载3D场景中...</p>
      </div>
    </CesiumView>

    <!-- 控制面板 -->
    <div class="control-panel">
      <!-- 特效切换 -->
      <div class="panel-section">
        <h3>🌟 特效</h3>
        <div class="effect-grid">
          <button
            v-for="effect in effects"
            :key="effect.id"
            :class="['effect-btn', { active: currentEffect === effect.id }]"
            @click="toggleEffect(effect.id)"
          >
            <span class="effect-icon">{{ effect.icon }}</span>
            <span class="effect-name">{{ effect.name }}</span>
          </button>
        </div>
      </div>

      <!-- 位置预设 -->
      <div class="panel-section">
        <h3>📍 位置</h3>
        <div class="preset-list">
          <button
            v-for="preset in presets"
            :key="preset.name"
            class="preset-btn"
            @click="flyToPreset(preset)"
          >
            {{ preset.name }}
          </button>
        </div>
      </div>

      <!-- 实体列表 -->
      <div class="panel-section">
        <h3>🏛️ 地标</h3>
        <div class="entity-list">
          <button
            v-for="entity in entities"
            :key="entity.id"
            :class="['entity-btn', { active: activeEntityId === entity.id }]"
            @click="selectEntity(entity.id)"
          >
            {{ entity.name }}
          </button>
        </div>
      </div>

      <!-- 操作 -->
      <div class="panel-section">
        <h3>🛠️ 操作</h3>
        <div class="action-list">
          <button class="action-btn" @click="takeScreenshot">📷 截图</button>
        </div>
      </div>

      <!-- 相机信息 -->
      <div class="camera-info">
        <span>H: {{ cameraInfo.heading.toFixed(1) }}°</span>
        <span>P: {{ cameraInfo.pitch.toFixed(1) }}°</span>
        <span>R: {{ cameraInfo.range.toFixed(0) }}m</span>
      </div>
    </div>

    <!-- 快捷键提示 -->
    <div class="shortcuts-hint">
      <span>左键: 旋转</span>
      <span>右键: 平移</span>
      <span>滚轮: 缩放</span>
    </div>
  </div>
</template>

<style scoped>
.cesium-panel {
  width: 100%;
  height: 100%;
  position: relative;
  background: #000;
}

.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.8);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  color: white;
}

.loading-spinner {
  width: 48px;
  height: 48px;
  border: 4px solid #2563eb;
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.control-panel {
  position: absolute;
  top: 1rem;
  right: 1rem;
  width: 260px;
  background: rgba(30, 41, 59, 0.95);
  border-radius: 12px;
  padding: 1rem;
  color: white;
  max-height: calc(100% - 2rem);
  overflow-y: auto;
  backdrop-filter: blur(10px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

.panel-section {
  margin-bottom: 1rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.panel-section:last-child {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}

.panel-section h3 {
  margin: 0 0 0.75rem;
  font-size: 0.875rem;
  font-weight: 600;
  color: #94a3b8;
}

.effect-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0.5rem;
}

.effect-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0.5rem;
  background: rgba(255, 255, 255, 0.1);
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  color: white;
}

.effect-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.effect-btn.active {
  background: #2563eb;
}

.effect-icon {
  font-size: 1.25rem;
}

.effect-name {
  font-size: 0.625rem;
  margin-top: 0.25rem;
}

.preset-list,
.entity-list,
.action-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.preset-btn,
.entity-btn,
.action-btn {
  padding: 0.625rem 1rem;
  background: rgba(255, 255, 255, 0.1);
  border: none;
  border-radius: 6px;
  color: white;
  cursor: pointer;
  text-align: left;
  transition: all 0.2s;
  font-size: 0.875rem;
}

.preset-btn:hover,
.entity-btn:hover,
.action-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.entity-btn.active {
  background: #2563eb;
}

.camera-info {
  display: flex;
  gap: 1rem;
  font-size: 0.75rem;
  color: #64748b;
  justify-content: center;
  flex-wrap: wrap;
}

.shortcuts-hint {
  position: absolute;
  bottom: 1rem;
  left: 1rem;
  display: flex;
  gap: 1rem;
  background: rgba(30, 41, 59, 0.8);
  padding: 0.5rem 1rem;
  border-radius: 6px;
  font-size: 0.75rem;
  color: #94a3b8;
  backdrop-filter: blur(5px);
}
</style>
