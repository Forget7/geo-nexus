<template>
  <div ref="containerRef" class="cesium-view">
    <slot />

    <!-- Tileset Load Panel -->
    <div class="tileset-panel">
      <button
        class="tileset-toggle"
        @click="showTilesetPanel = !showTilesetPanel"
        :title="t('map.cesium.tilesetLoad')"
      >
        🏗️
      </button>

      <Transition name="panel-slide">
        <div v-if="showTilesetPanel" class="tileset-panel-content">
        <div class="panel-header">
          <span>🏗️ {{ t('map.cesium.tilesetPanel') }}</span>
          <button @click="showTilesetPanel = false">×</button>
        </div>

        <!-- Quick Load Presets -->
        <div class="tileset-presets">
          <p class="section-label">{{ t('map.cesium.quickLoad') }}</p>
          <button @click="loadPresetTileset('beijing')">{{ t('map.cesium.beijing') }}</button>
          <button @click="loadPresetTileset('shanghai')">{{ t('map.cesium.shanghai') }}</button>
          <button @click="loadPresetTileset('world')">{{ t('map.cesium.globalBuildings') }}</button>
        </div>

        <!-- Custom URL -->
        <div class="tileset-custom">
          <p class="section-label">{{ t('map.cesium.customUrl') }}</p>
          <input
            v-model="customTilesetUrl"
            type="text"
            :placeholder="t('map.cesium.enterTilesetUrl')"
          />
          <button
            @click="loadCustomTileset"
            :disabled="!customTilesetUrl.trim() || isLoadingTileset"
          >
            {{ isLoadingTileset ? t('map.cesium.loading') : t('map.cesium.load') }}
          </button>
        </div>

        <!-- Loaded Tilesets -->
        <div v-if="loadedTilesetNames.length > 0" class="tileset-list">
          <p class="section-label">{{ t('map.cesium.loadedCount', { count: loadedTilesetNames.length }) }}</p>
          <div
            v-for="name in loadedTilesetNames"
            :key="name"
            class="tileset-item"
          >
            <span>{{ name }}</span>
            <button @click="unloadTileset(name)">×</button>
          </div>
        </div>

        <!-- Visual Effects -->
        <div class="section-divider"></div>
        <div class="effect-presets">
          <p class="section-label">{{ t('map.cesium.effects') }}</p>
          <div class="effect-buttons">
            <button
              :class="{ active: rainActive }"
              @click="toggleRain"
              :title="t('map.cesium.rain')"
            >🌧️ {{ t('map.cesium.rain') }}</button>
            <button
              :class="{ active: snowActive }"
              @click="toggleSnow"
              :title="t('map.cesium.snow')"
            >❄️ {{ t('map.cesium.snow') }}</button>
            <button
              :class="{ active: sandstormActive }"
              @click="toggleSandstorm"
              :title="t('map.cesium.sandstorm')"
            >🏜️ {{ t('map.cesium.sandstorm') }}</button>
          </div>
        </div>

        <!-- Imagery Switching -->
        <div class="section-divider"></div>
        <div class="imagery-section">
          <p class="section-label">{{ t('map.cesium.imagery') }}</p>
          <div class="imagery-buttons">
            <button
              :class="{ active: currentImagery === 'osm' }"
              @click="switchImagery('osm')"
            >🗺️ {{ t('map.cesium.osm') }}</button>
            <button
              :class="{ active: currentImagery === 'arcgis' }"
              @click="switchImagery('arcgis')"
            >🛰️ {{ t('map.cesium.arcgisSatellite') }}</button>
            <button
              :class="{ active: currentImagery === 'cesiumWorld' }"
              @click="switchImagery('cesiumWorld')"
            >🌍 {{ t('map.cesium.cesiumImagery') }}</button>
            <button
              :class="{ active: currentImagery === 'cesiumIon' }"
              @click="switchImagery('cesiumIon')"
            >🛸 {{ t('map.cesium.cesiumIonImagery') }}</button>
          </div>
        </div>

        <!-- Point Cloud Section -->
        <div class="section-divider"></div>
        <div class="pointcloud-section">
          <p class="section-label">{{ t('map.cesium.pointCloud') }}</p>
          <div class="pointcloud-buttons">
            <button @click="loadPresetPointCloud" :disabled="isLoadingPointCloud">
              📡 {{ t('map.cesium.loadMelbournePC') }}
            </button>
            <button @click="showPointCloudUrlInput = true">
              🔗 {{ t('map.cesium.customPointCloud') }}
            </button>
          </div>
          <div v-if="loadedPointCloudNames.length > 0" class="pointcloud-list">
            <p class="section-label">{{ t('map.cesium.loadedCount', { count: loadedPointCloudNames.length }) }}</p>
            <div
              v-for="name in loadedPointCloudNames"
              :key="name"
              class="pointcloud-item"
            >
              <span>{{ name }}</span>
              <button @click="removePointCloud(name)">×</button>
            </div>
          </div>
        </div>

        <!-- Point Cloud URL Input Modal -->
        <div v-if="showPointCloudUrlInput" class="pointcloud-modal">
          <div class="modal-header">
            <span>{{ t('map.cesium.customPointCloud') }}</span>
            <button @click="showPointCloudUrlInput = false">×</button>
          </div>
          <input
            v-model="pointCloudUrl"
            type="text"
            :placeholder="t('map.cesium.enterPointCloudUrl')"
          />
          <button
            @click="loadCustomPointCloud"
            :disabled="!pointCloudUrl.trim() || isLoadingPointCloud"
          >
            {{ isLoadingPointCloud ? t('map.cesium.loading') : t('map.cesium.load') }}
          </button>
        </div>

        <!-- Temporal Data (CZML) -->
        <div class="section-divider"></div>
        <div class="czml-section">
          <p class="section-label">{{ t('map.czml.title') }}</p>
          <div class="czml-buttons">
            <button @click="openCzmlFile" :disabled="isLoadingCzml">
              📡 {{ t('map.czml.loadCzml') }}
            </button>
            <button @click="showTrajectoryInput = true">
              📍 {{ t('map.czml.loadTrajectory') }}
            </button>
            <button
              v-if="loadedDataSourceNames.length > 0"
              @click="clearAllDataSources"
            >
              🗑️ {{ t('map.czml.clearAll') }}
            </button>
          </div>
          <div v-if="loadedDataSourceNames.length > 0" class="data-source-list">
            <p class="section-label">{{ t('map.czml.loadedCount', { count: loadedDataSourceNames.length }) }}</p>
            <div
              v-for="name in loadedDataSourceNames"
              :key="name"
              class="data-source-item"
            >
              <span>{{ name }}</span>
              <button @click="removeDataSource(name)">×</button>
            </div>
          </div>
        </div>

        <!-- Viewshed Analysis -->
        <div class="section-divider"></div>
        <div class="viewshed-section">
          <p class="section-label">{{ t('map.viewshed.title') }}</p>
          <div class="viewshed-buttons">
            <button
              :class="{ active: viewshedActive }"
              @click="toggleViewshed"
            >
              👁 {{ viewshedActive ? t('map.viewshed.stop') : t('map.viewshed.start') }}
            </button>
          </div>
          <p v-if="viewshedActive" class="viewshed-hint">{{ t('map.viewshed.clickToPlace') }}</p>
        </div>

        <!-- Trajectory URL Input Modal -->
        <div v-if="showTrajectoryInput" class="trajectory-modal">
          <div class="modal-header">
            <span>{{ t('map.czml.loadTrajectory') }}</span>
            <button @click="showTrajectoryInput = false">×</button>
          </div>
          <input
            v-model="trajectoryUrl"
            type="text"
            :placeholder="t('map.czml.enterUrl')"
          />
          <button 
            @click="loadTrajectory"
            :disabled="!trajectoryUrl.trim() || isLoadingCzml"
          >
            {{ isLoadingCzml ? t('map.cesium.loading') : t('map.cesium.load') }}
          </button>
        </div>

        <!-- Terrain Profile -->
        <div class="section-divider"></div>
        <div class="profile-section">
          <p class="section-label">{{ t('map.profile.title') }}</p>
          <div class="profile-buttons">
            <button 
              :class="{ active: profileActive }"
              @click="toggleProfile"
            >
              📏 {{ profileActive ? t('map.profile.clear') : t('map.profile.drawLine') }}
            </button>
          </div>
          <p v-if="profileActive" class="profile-hint">{{ t('map.viewshed.clickToPlace') }}</p>
        </div>

        <!-- Terrain Profile Chart -->
        <div v-if="profileChartData.length > 0" class="profile-chart-container">
          <div class="profile-chart-header">
            <span>📊 {{ t('map.profile.title') }}</span>
            <button @click="clearProfile">×</button>
          </div>
          <svg class="profile-chart" :viewBox="`0 0 ${profileChartWidth} ${profileChartHeight}`">
            <!-- Grid lines -->
            <line v-for="i in 4" :key="'grid-'+i"
              :x1="chartPadding" 
              :y1="chartPadding + (i-1) * (profileChartHeight - 2 * chartPadding) / 3"
              :x2="profileChartWidth - chartPadding"
              :y2="chartPadding + (i-1) * (profileChartHeight - 2 * chartPadding) / 3"
              stroke="#e2e8f0" stroke-width="1"
            />
            <!-- Profile line -->
            <polyline
              :points="profileChartPoints"
              fill="none"
              stroke="#3b82f6"
              stroke-width="2"
              stroke-linejoin="round"
              stroke-linecap="round"
            />
            <!-- Elevation labels -->
            <text v-for="(label, i) in profileChartLabels" :key="'label-'+i"
              :x="chartPadding - 4"
              :y="label.y + 4"
              text-anchor="end"
              font-size="10"
              fill="#94a3b8"
            >{{ label.value }}m</text>
          </svg>
          <div class="profile-stats">
            <span>Max: {{ profileStats.max }}m</span>
            <span>Min: {{ profileStats.min }}m</span>
            <span>Dist: {{ profileStats.distance }}m</span>
          </div>
        </div>
      </Transition>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * CesiumView - Cesium 3D 地球视图组件
 *
 * 提供 Cesium viewer 创建、3D Tiles 加载、地形、影像、特效等核心功能
 * 整合了原 CesiumScene 的特效系统
 */
import { ref, computed, onMounted, onBeforeUnmount, watch, shallowRef } from 'vue'
import { useI18n } from 'vue-i18n'
import type { Viewer, Cesium3DTileset } from 'cesium'
import { EffectManager } from '@geonex/hooks/useCesium'

const { t } = useI18n()

export interface CesiumViewProps {
  /** Cesium ion token */
  ionToken?: string
  /** Cesium ion server URL */
  ionUrl?: string
  /** 默认地形 */
  terrain?: boolean
  /** 地形夸张系数 */
  terrainExaggeration?: number
  /** 默认影像 */
  imagery?: string
  /** 是否显示 Credit */
  showCredit?: boolean
  /** 是否显示 FPS */
  showFps?: boolean
  /** 初始相机位置 */
  initialPosition?: { lon: number; lat: number; height?: number }
  /** Cesium ion asset ID for terrain */
  terrainIonAssetId?: number
  /** Cesium ion asset ID for imagery */
  imageryIonAssetId?: number
  /** 启用光照 */
  enableLighting?: boolean
  /** 启用大气 */
  enableAtmosphere?: boolean
  /** 初始启用雾效 */
  enableFog?: boolean
  /** 初始启用雨效 */
  enableRain?: boolean
  /** 初始启用雪效 */
  enableSnow?: boolean
}

const props = withDefaults(defineProps<CesiumViewProps>(), {
  terrain: true,
  terrainExaggeration: 1.0,
  showCredit: false,
  showFps: false,
  initialPosition: () => ({ lon: 116.3972, lat: 39.9165, height: 5000 }),
  enableLighting: false,
  enableAtmosphere: true,
  enableFog: true,
  enableRain: false,
  enableSnow: false,
})

const emit = defineEmits<{
  ready: [viewer: Viewer]
  error: [error: Error]
  tilesetProgress: [id: string, loaded: number, total: number]
  entitySelect: [entity: any]
  cameraChange: [heading: number, pitch: number, range: number]
}>()

const containerRef = ref<HTMLDivElement | null>(null)
const viewerRef = ref<Viewer | null>(null)
const tilesetsRef = shallowRef<Map<string, Cesium3DTileset>>(new Map())
const effectManagerRef = shallowRef<EffectManager | null>(null)

// Tileset panel 状态
const showTilesetPanel = ref(false)
const customTilesetUrl = ref('')
const isLoadingTileset = ref(false)

// Effect toggles
const rainActive = ref(false)
const snowActive = ref(false)
const sandstormActive = ref(false)

// Imagery switching
const currentImagery = ref(props.imagery || 'osm')

// Cesium ion configured check
const cesiumIonConfigured = computed(() => {
  return CESIUM_ION_TOKEN && CESIUM_ION_TOKEN !== 'your-cesium-ion-token'
})

// CZML / Temporal data
const isLoadingCzml = ref(false)
const showTrajectoryInput = ref(false)
const trajectoryUrl = ref('')
const loadedDataSourceNames = ref<string[]>([])
const dataSourcesRef = shallowRef<Map<string, any>>(new Map())

// Viewshed analysis
const viewshedActive = ref(false)
const viewshedHandler = ref<any>(null)
const viewshedEntity = ref<any>(null)

// Terrain profile
const profileActive = ref(false)
const profileHandler = ref<any>(null)
const profileLineEntity = ref<any>(null)
const profileDrawingPoints = ref<any[]>([])
const profileChartData = ref<{distance: number; height: number}[]>([])
const profileChartWidth = 240
const profileChartHeight = 100
const chartPadding = 20

// Computed profile stats
const profileStats = computed(() => {
  const data = profileChartData.value
  if (data.length === 0) return { max: 0, min: 0, distance: 0 }
  const heights = data.map(d => d.height)
  const max = Math.max(...heights)
  const min = Math.min(...heights)
  const last = data[data.length - 1]
  return { max: Math.round(max), min: Math.round(min), distance: Math.round(last.distance) }
})

// Computed SVG points for profile chart
const profileChartPoints = computed(() => {
  const data = profileChartData.value
  if (data.length < 2) return ''
  const maxDist = data[data.length - 1].distance || 1
  const heights = data.map(d => d.height)
  const maxH = Math.max(...heights)
  const minH = Math.min(...heights)
  const range = maxH - minH || 1
  const w = profileChartWidth - 2 * chartPadding
  const h = profileChartHeight - 2 * chartPadding
  
  return data.map(d => {
    const x = chartPadding + (d.distance / maxDist) * w
    const y = chartPadding + h - ((d.height - minH) / range) * h
    return `${x},${y}`
  }).join(' ')
})

// Computed chart labels
const profileChartLabels = computed(() => {
  const data = profileChartData.value
  if (data.length === 0) return []
  const heights = data.map(d => d.height)
  const maxH = Math.max(...heights)
  const minH = Math.min(...heights)
  const range = maxH - minH || 1
  const h = profileChartHeight - 2 * chartPadding
  
  return [
    { value: Math.round(maxH), y: chartPadding + 4 },
    { value: Math.round(minH), y: chartPadding + h }
  ]
})

// Point cloud
const isLoadingPointCloud = ref(false)
const showPointCloudUrlInput = ref(false)
const pointCloudUrl = ref('')
const loadedPointCloudNames = ref<string[]>([])
const pointCloudsRef = shallowRef<Map<string, any>>(new Map())

function toggleRain() {
  rainActive.value = !rainActive.value
  enableRain(rainActive.value)
}
function toggleSnow() {
  snowActive.value = !snowActive.value
  enableSnow(snowActive.value)
}
function toggleSandstorm() {
  sandstormActive.value = !sandstormActive.value
  enableSandstorm(sandstormActive.value)
}

// 预设 Tileset 配置
const presetTilesets: Record<string, { name: string; url?: string; ionAssetId?: number; position?: { lon: number; lat: number } }> = {
  beijing: {
    name: '北京建筑',
    ionAssetId: 43978, // Cesium Ion 的北京市 3D 建筑
    position: { lon: 116.3972, lat: 39.9165 }
  },
  shanghai: {
    name: '上海建筑',
    ionAssetId: 43978,
    position: { lon: 121.4737, lat: 31.2304 }
  },
  world: {
    name: '全球建筑',
    ionAssetId: 43978,
    position: { lon: 0, lat: 0 }
  }
}

// 加载的 tileset 名称列表
const loadedTilesetNames = ref<string[]>([])

// Cesium token - 优先用 props,其次用环境变量
const CESIUM_ION_TOKEN = import.meta.env.VITE_CESIUM_ION_TOKEN || 'your-cesium-ion-token'

// 内部创建 viewer
async function createViewer(container: HTMLDivElement): Promise<Viewer> {
  const Cesium = (await import('cesium')).default
  await import('cesium/Build/Cesium/Widgets/widgets.css')

  const terrainProvider = props.terrain
    ? await getTerrainProvider(Cesium)
    : undefined

  const imageryProvider = props.imagery
    ? await getImageryProvider(Cesium, props.imagery)
    : undefined

  const viewer = new Cesium.Viewer(container, {
    terrainProvider,
    imageryProvider,
    baseLayerPicker: false,
    geocoder: false,
    homeButton: true,
    sceneModePicker: true,
    navigationHelpButton: false,
    animation: true,
    timeline: true,
    fullscreenButton: false,
    vrButton: false,
    infoBox: true,
    selectionIndicator: true,
    shadows: false,
    shouldAnimate: true,
    showCredit: props.showCredit,
    skyBox: new Cesium.SkyBox({ show: props.enableAtmosphere }),
    skyAtmosphere: props.enableAtmosphere ? new Cesium.SkyAtmosphere() : undefined,
    orderIndependentTranslucency: false,
    contextOptions: {
      webgl: {
        alpha: true,
        powerPreference: 'high-performance'
      }
    }
  } as any)

  // 设置光照
  if (props.enableLighting) {
    viewer.scene.light = new Cesium.DirectionalLight({
      direction: new Cesium.Cartesian3(-1, -1, -1),
      intensity: 1.0
    })
    viewer.scene.globe.enableLighting = true
  }

  // 启用帧率显示
  if (props.showFps) {
    (viewer as any).scene.debugShowFramesPerSecond = true
  }

  // 设置地形夸张
  if (props.terrainExaggeration !== 1.0) {
    viewer.scene.globe.terrainExaggeration = props.terrainExaggeration
  }

  // 设置初始位置
  if (props.initialPosition) {
    viewer.camera.flyTo({
      destination: Cesium.Cartesian3.fromDegrees(
        props.initialPosition.lon,
        props.initialPosition.lat,
        props.initialPosition.height ?? 5000
      )
    })
  }

  // 抗锯齿
  viewer.scene.fxaa = true
  viewer.scene.postProcessStages.fxaa = true

  // 初始化特效管理器
  effectManagerRef.value = new EffectManager(viewer)

  // 监听实体选择
  viewer.selectedEntityChanged.addEventListener((entity: any) => {
    if (entity) {
      emit('entitySelect', entity)
    }
  })

  // 监听相机变化
  viewer.camera.changed.addEventListener(() => {
    const heading = Cesium.Math.toDegrees(viewer.camera.heading)
    const pitch = Cesium.Math.toDegrees(viewer.camera.pitch)
    const range = viewer.camera.pitch !== Cesium.Math.toRadians(-90)
      ? viewer.camera.distance
      : 0
    emit('cameraChange', heading, pitch, range)
  })

  // 应用初始特效
  if (props.enableFog) enableFog()
  if (props.enableRain) enableRain()
  if (props.enableSnow) enableSnow()

  return viewer
}

async function getTerrainProvider(Cesium: any) {
  if (props.terrainIonAssetId) {
    return Cesium.Terrain.fromIon(props.terrainIonAssetId, {
      accessToken: props.ionToken || CESIUM_ION_TOKEN
    })
  }
  return Cesium.Terrain.fromWorldTerrain()
}

async function getImageryProvider(Cesium: any, name: string): Promise<any> {
  switch (name) {
    case 'osm':
      return new Cesium.OpenStreetMapImageryProvider()
    case 'arcgis':
      return new Cesium.ArcGisMapServerImageryProvider({
        url: 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer'
      })
    case 'cesiumWorld':
      // Cesium World Imagery - free, no token required
      return Cesium.IonImagery.fromAssetId(2, {
        accessToken: CESIUM_ION_TOKEN
      })
    case 'cesiumIon':
      // Cesium ion global imagery - requires ion token
      if (!cesiumIonConfigured) {
        throw new Error('Cesium ion token not configured')
      }
      return Cesium.IonImagery.fromAssetId(1, {
        accessToken: CESIUM_ION_TOKEN
      })
    default:
      return new Cesium.OpenStreetMapImageryProvider()
  }
}

// ==================== Tileset 加载 ====================

async function loadPresetTileset(preset: string) {
  const config = presetTilesets[preset]
  if (!config) return

  isLoadingTileset.value = true
  try {
    await load3DTileset(config.name, {
      ionAssetId: config.ionAssetId,
      ionToken: CESIUM_ION_TOKEN,
      position: config.position
    })
    updateLoadedTilesets()
  } finally {
    isLoadingTileset.value = false
  }
}

async function loadCustomTileset() {
  const url = customTilesetUrl.value.trim()
  if (!url) return

  isLoadingTileset.value = true
  try {
    const name = url.split('/').pop() || url
    await load3DTileset(url, { url })
    updateLoadedTilesets()
    customTilesetUrl.value = ''
  } finally {
    isLoadingTileset.value = false
  }
}

function unloadTileset(name: string) {
  removeTileset(name)
  updateLoadedTilesets()
}

function updateLoadedTilesets() {
  loadedTilesetNames.value = Array.from(tilesetsRef.value.keys())
}

// ==================== 特效控制 ====================

function enableFog(enabled = true) {
  const viewer = viewerRef.value
  if (!viewer) return
  viewer.scene.fog.enabled = enabled
  viewer.scene.fog.density = 0.0001
}

function setFogDensity(density: number) {
  const viewer = viewerRef.value
  if (!viewer) return
  viewer.scene.fog.density = density
}

function enableRain(enabled = true) {
  const em = effectManagerRef.value
  if (!em) return
  if (enabled) {
    em.addRainEffect()
  } else {
    em.removeEffect('rain')
  }
}

function enableSnow(enabled = true) {
  const em = effectManagerRef.value
  if (!em) return
  if (enabled) {
    em.addSnowEffect()
  } else {
    em.removeEffect('snow')
  }
}

function enableSandstorm(enabled = true) {
  const em = effectManagerRef.value
  if (!em) return
  if (enabled) {
    em.addSandstormEffect()
  } else {
    em.removeEffect('sandstorm')
  }
}

// ==================== Imagery Switching ====================

async function switchImagery(type: string) {
  const viewer = viewerRef.value
  if (!viewer) return

  // cesiumIon requires token
  if (type === 'cesiumIon' && !cesiumIonConfigured) {
    alert(t('map.cesium.configureIonToken'))
    return
  }

  const Cesium = (await import('cesium')).default
  try {
    const imageryProvider = await getImageryProvider(Cesium, type)
    // Remove existing imagery layers
    viewer.imageryLayers.removeAll()
    viewer.imageryLayers.addImageryProvider(imageryProvider)
    currentImagery.value = type
  } catch (e) {
    console.error('[CesiumView] 切换影像失败:', e)
  }
}

// ==================== CZML / Temporal Data ====================

function openCzmlFile() {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = '.czml,.json'
  input.onchange = async (e) => {
    const file = (e.target as HTMLInputElement).files?.[0]
    if (file) {
      const url = URL.createObjectURL(file)
      await loadCzml(url, file.name)
    }
  }
  input.click()
}

async function loadCzml(url: string, name?: string) {
  const viewer = viewerRef.value
  if (!viewer) return

  isLoadingCzml.value = true
  try {
    const Cesium = (await import('cesium')).default
    const dataSource = await Cesium.CzmlDataSource.load(url)
    await viewer.dataSources.add(dataSource)
    viewer.trackedEntity = dataSource.entities.get(0)

    const dsName = name || url.split('/').pop() || 'CZML Data'
    dataSourcesRef.value.set(dsName, dataSource)
    updateLoadedDataSources()
  } catch (e) {
    console.error('[CesiumView] 加载 CZML 失败:', e)
  } finally {
    isLoadingCzml.value = false
  }
}

async function loadTrajectory() {
  const url = trajectoryUrl.value.trim()
  if (!url) return

  isLoadingCzml.value = true
  try {
    // 尝试作为 CZML 加载
    await loadCzml(url, 'Trajectory')
    showTrajectoryInput.value = false
    trajectoryUrl.value = ''
  } catch (e) {
    console.error('[CesiumView] 加载轨迹失败:', e)
  } finally {
    isLoadingCzml.value = false
  }
}

function removeDataSource(name: string) {
  const viewer = viewerRef.value
  if (!viewer) return

  const dataSource = dataSourcesRef.value.get(name)
  if (dataSource) {
    viewer.dataSources.remove(dataSource)
    dataSourcesRef.value.delete(name)
    updateLoadedDataSources()
  }
}

function updateLoadedDataSources() {
  loadedDataSourceNames.value = Array.from(dataSourcesRef.value.keys())
}

function clearAllDataSources() {
  const viewer = viewerRef.value
  if (!viewer) return
  dataSourcesRef.value.forEach((ds: any) => {
    viewer.dataSources.remove(ds)
  })
  dataSourcesRef.value.clear()
  updateLoadedDataSources()
}

// ==================== Viewshed Analysis ====================

async function toggleViewshed() {
  const viewer = viewerRef.value
  if (!viewer) return

  if (viewshedActive.value) {
    stopViewshed()
  } else {
    startViewshed()
  }
}

async function startViewshed() {
  const viewer = viewerRef.value
  if (!viewer) return

  const Cesium = (await import('cesium')).default

  // 创建视域分析观察点
  const handler = new Cesium.ScreenSpaceEventHandler(viewer.scene.canvas)

  // 等待用户点击设置观察点
  handler.setInputAction(async (click: any) => {
    const pickedPosition = await getCartesianPosition(viewer, click.position)
    if (!pickedPosition) return

    // 创建观察点标记
    viewshedEntity.value = viewer.entities.add({
      position: pickedPosition,
      point: {
        pixelSize: 10,
        color: Cesium.Color.RED,
        outlineColor: Cesium.Color.WHITE,
        outlineWidth: 2
      },
      label: {
        text: 'Observer',
        pixelOffset: new Cesium.Cartesian2(0, -20),
        scale: 0.5
      }
    })

    // 创建视域锥形区域(简化的可视化)
    const startCartographic = Cesium.Cartographic.fromCartesian(pickedPosition)
    const heading = Cesium.Math.toRadians(0)
    const pitch = Cesium.Math.toRadians(-45)
    const range = 1000 // 1000米范围

    // 创建视域锥形实体
    createViewshedCone(viewer, pickedPosition, heading, pitch, range, Cesium)

    // 完成后移除点击处理器
    handler.destroy()
  }, Cesium.ScreenSpaceEventType.LEFT_CLICK)

  viewshedHandler.value = handler
  viewshedActive.value = true
}

function createViewshedCone(viewer: any, position: any, heading: number, pitch: number, range: number, Cesium: any) {
  // 创建简化的视域锥形区域
  const positions = []
  const numSegments = 32

  for (let i = 0; i <= numSegments; i++) {
    const angle = (i / numSegments) * Math.PI * 2
    const dx = range * Math.cos(angle)
    const dy = range * Math.sin(angle)

    const offset = new Cesium.Cartesian3(dx, dy, 0)
    const rotatedOffset = rotatePoint(offset, heading, pitch)
    const pos = Cesium.Cartesian3.add(position, rotatedOffset, new Cesium.Cartesian3())

    positions.push(pos)
  }

  viewer.entities.add({
    polygon: {
      hierarchy: new Cesium.PolygonHierarchy(positions),
      material: Cesium.Color.RED.withAlpha(0.3),
      outline: true,
      outlineColor: Cesium.Color.RED
    }
  })
}

function rotatePoint(point: any, heading: number, pitch: number): any {
  // 简单的旋转计算
  const cosH = Math.cos(heading)
  const sinH = Math.sin(heading)
  const cosP = Math.cos(pitch)
  const sinP = Math.sin(pitch)

  return {
    x: point.x * cosH - point.y * sinH,
    y: point.x * sinH + point.y * cosH,
    z: point.z * cosP + (point.x * sinH + point.y * cosH) * sinP
  }
}

async function getCartesianPosition(viewer: any, screenPosition: any): Promise<any> {
  const Cesium = (await import('cesium')).default
  const pickRay = viewer.camera.getPickRay(screenPosition)
  const pickedObjects = viewer.scene.pick(screenPosition)

  if (pickedObjects.length > 0) {
    return viewer.scene.pickPosition(screenPosition)
  }

  // 如果没有选中物体,使用地形表面
  const cartesian = viewer.scene.globe.pick(pickRay, viewer.scene)
  return cartesian
}

function stopViewshed() {
  const viewer = viewerRef.value
  if (!viewer) return

  if (viewshedHandler.value) {
    viewshedHandler.value.destroy()
    viewshedHandler.value = null
  }

  if (viewshedEntity.value) {
    viewer.entities.remove(viewshedEntity.value)
    viewshedEntity.value = null
  }

  viewshedActive.value = false
}

// ==================== Terrain Profile ====================

async function toggleProfile() {
  const viewer = viewerRef.value
  if (!viewer) return
  
  if (profileActive.value) {
    stopProfile()
  } else {
    startProfile()
  }
}

async function startProfile() {
  const viewer = viewerRef.value
  if (!viewer) return
  
  const Cesium = (await import('cesium')).default
  profileDrawingPoints.value = []
  profileChartData.value = []
  
  const handler = new Cesium.ScreenSpaceEventHandler(viewer.scene.canvas)
  
  // Left click to add points
  handler.setInputAction(async (click: any) => {
    const pickedPos = await getCartesianPosition(viewer, click.position)
    if (!pickedPos) return
    
    profileDrawingPoints.value.push(pickedPos)
    updateProfileLine(Cesium)
    
    // Need at least 2 points to calculate profile
    if (profileDrawingPoints.value.length >= 2) {
      await calculateProfile(Cesium)
    }
  }, Cesium.ScreenSpaceEventType.LEFT_CLICK)
  
  // Right click to finish
  handler.setInputAction(() => {
    if (profileDrawingPoints.value.length >= 2) {
      handler.destroy()
      profileActive.value = false
    }
  }, Cesium.ScreenSpaceEventType.RIGHT_CLICK)
  
  profileHandler.value = handler
  profileActive.value = true
}

function updateProfileLine(Cesium: any) {
  const viewer = viewerRef.value
  if (!viewer) return
  
  // Remove old line
  if (profileLineEntity.value) {
    viewer.entities.remove(profileLineEntity.value)
    profileLineEntity.value = null
  }
  
  const points = profileDrawingPoints.value
  if (points.length === 0) return
  
  profileLineEntity.value = viewer.entities.add({
    polyline: {
      positions: points,
      width: 3,
      material: Cesium.Color.CYAN,
      clampToGround: true
    }
  })
}

async function calculateProfile(Cesium: any) {
  const viewer = viewerRef.value
  if (!viewer) return
  
  const points = profileDrawingPoints.value
  if (points.length < 2) return
  
  // Sample terrain heights along the line
  const numSamples = Math.max(points.length * 10, 20)
  const cartoArray = points.map((p: any) => Cesium.Cartographic.fromCartesian(p))
  
  // Interpolate between points
  const totalDist = calculateLineDistance(points, Cesium)
  const profileData: {distance: number; height: number}[] = []
  
  for (let i = 0; i < numSamples; i++) {
    const t = i / (numSamples - 1)
    const carto = interpolateCartographic(cartoArray, t, Cesium)
    
    // Sample terrain height
    try {
      const clamped = await viewer.scene.clampToHeight(Cesium.Cartesian3.fromRadians(carto.longitude, carto.latitude, 0))
      if (clamped) {
        const h = Cesium.Cartographic.fromCartesian(clamped).height
        const dist = t * totalDist
        profileData.push({ distance: dist, height: Math.max(h, 0) })
      }
    } catch (e) {
      // fallback - skip this point
    }
  }
  
  profileChartData.value = profileData
}

function interpolateCartographic(cartoArray: any[], t: number, Cesium: any): any {
  const totalSegments = cartoArray.length - 1
  const segmentT = t * totalSegments
  const segIndex = Math.min(Math.floor(segmentT), totalSegments - 1)
  const localT = segmentT - segIndex
  
  const c1 = cartoArray[segIndex]
  const c2 = cartoArray[segIndex + 1]
  
  return new Cesium.Cartographic(
    Cesium.Math.lerp(c1.longitude, c2.longitude, localT),
    Cesium.Math.lerp(c1.latitude, c2.latitude, localT),
    Cesium.Math.lerp(c1.height || 0, c2.height || 0, localT)
  )
}

function calculateLineDistance(points: any[], Cesium: any): number {
  let dist = 0
  for (let i = 1; i < points.length; i++) {
    dist += Cesium.Cartesian3.distance(points[i - 1], points[i])
  }
  return dist
}

function clearProfile() {
  const viewer = viewerRef.value
  if (!viewer) return
  
  stopProfile()
  profileChartData.value = []
  profileDrawingPoints.value = []
  
  if (profileLineEntity.value) {
    viewer.entities.remove(profileLineEntity.value)
    profileLineEntity.value = null
  }
}

function stopProfile() {
  if (profileHandler.value) {
    profileHandler.value.destroy()
    profileHandler.value = null
  }
  profileActive.value = false
}

// ==================== Point Cloud ====================

async function loadPresetPointCloud() {
  const viewer = viewerRef.value
  if (!viewer) return

  isLoadingPointCloud.value = true
  try {
    const Cesium = (window as any).Cesium
    // Melbourne Point Cloud on Cesium Ion (asset ID 43978)
    const pointCloud = await Cesium.PointCloud.fromIon(43978, {
      accessToken: CESIUM_ION_TOKEN
    })
    viewer.scene.primitives.add(pointCloud)
    pointCloudsRef.value.set('Melbourne Point Cloud', pointCloud)
    updateLoadedPointClouds()
  } catch (e) {
    console.error('[CesiumView] 加载点云失败:', e)
    alert(t('map.cesium.pointCloudLoadError'))
  } finally {
    isLoadingPointCloud.value = false
  }
}

async function loadCustomPointCloud() {
  const url = pointCloudUrl.value.trim()
  if (!url) return

  const viewer = viewerRef.value
  if (!viewer) return

  isLoadingPointCloud.value = true
  try {
    const Cesium = (window as any).Cesium
    const pointCloud = await Cesium.PointCloud.fromUrl(url)
    const name = url.split('/').pop() || url
    viewer.scene.primitives.add(pointCloud)
    pointCloudsRef.value.set(name, pointCloud)
    updateLoadedPointClouds()
    showPointCloudUrlInput.value = false
    pointCloudUrl.value = ''
  } catch (e) {
    console.error('[CesiumView] 加载点云失败:', e)
    alert(t('map.cesium.pointCloudLoadError'))
  } finally {
    isLoadingPointCloud.value = false
  }
}

function removePointCloud(name: string) {
  const viewer = viewerRef.value
  if (!viewer) return

  const pc = pointCloudsRef.value.get(name)
  if (pc) {
    viewer.scene.primitives.remove(pc)
    pointCloudsRef.value.delete(name)
    updateLoadedPointClouds()
  }
}

function updateLoadedPointClouds() {
  loadedPointCloudNames.value = Array.from(pointCloudsRef.value.keys())
}

// ==================== 公开 API ====================

async function load3DTileset(url: string, options?: {
  ionAssetId?: number
  ionToken?: string
  maxScreenSpaceError?: number
  position?: { lon: number; lat: number; height?: number }
  heading?: number
  pitch?: number
  roll?: number
  style?: any
}): Promise<Cesium3DTileset | null> {
  const viewer = viewerRef.value
  if (!viewer) return null

  try {
    const Cesium = (window as any).Cesium
    let tileset: Cesium3DTileset

    if (options?.ionAssetId) {
      tileset = await Cesium3DTileset.fromIon(options.ionAssetId, {
        accessToken: options.ionToken
      })
    } else {
      tileset = await Cesium3DTileset.fromUrl(url)
    }

    tileset.maximumScreenSpaceError = options?.maxScreenSpaceError ?? 16

    // 位置变换
    if (options?.position) {
      const cartographic = Cesium.Cartographic.fromDegrees(
        options.position.lon,
        options.position.lat,
        options.position.height ?? 0
      )
      tileset.modelMatrix = Cesium.Transforms.headingPitchRollToFixedFrame(
        Cesium.Cartesian3.fromRadians(cartographic.longitude, cartographic.latitude, cartographic.height),
        new Cesium.HeadingPitchRoll(
          Cesium.Math.toRadians(options.heading ?? 0),
          Cesium.Math.toRadians(options.pitch ?? 0),
          Cesium.Math.toRadians(options.roll ?? 0)
        )
      )
    }

    // 监听加载进度
    let total = 0
    tileset.progressEvent.addEventListener((p: number) => {
      total = Math.round(p * tileset.numberOfLoadedTiles)
      emit('tilesetProgress', url, tileset.numberOfLoadedTiles, total)
    })

    viewer.scene.primitives.add(tileset)
    tilesetsRef.value.set(url, tileset)

    await tileset.readyPromise
    return tileset
  } catch (e) {
    console.error('[CesiumView] 加载 3D Tileset 失败:', e)
    emit('error', e as Error)
    return null
  }
}

function removeTileset(idOrUrl: string) {
  const viewer = viewerRef.value
  if (!viewer) return

  const tileset = tilesetsRef.value.get(idOrUrl)
  if (tileset) {
    viewer.scene.primitives.remove(tileset)
    tileset.destroy()
    tilesetsRef.value.delete(idOrUrl)
  }
}

function getTilesets(): Cesium3DTileset[] {
  return Array.from(tilesetsRef.value.values())
}

function getEffectManager(): EffectManager | null {
  return effectManagerRef.value
}

function destroy() {
  const viewer = viewerRef.value
  if (viewer) {
    if (effectManagerRef.value) {
      effectManagerRef.value.destroy()
      effectManagerRef.value = null
    }
    tilesetsRef.value.forEach(tileset => {
      viewer.scene.primitives.remove(tileset)
      tileset.destroy()
    })
    tilesetsRef.value.clear()
    viewer.destroy()
    viewerRef.value = null
  }
}

function getViewer(): Viewer | null {
  return viewerRef.value
}

// ==================== 生命周期 ====================

onMounted(async () => {
  if (!containerRef.value) return

  try {
    const viewer = await createViewer(containerRef.value)
    viewerRef.value = viewer
    ;(window as any).cesiumViewer = viewer
    emit('ready', viewer)
  } catch (e) {
    console.error('[CesiumView] 初始化失败:', e)
    emit('error', e as Error)
  }
})

onBeforeUnmount(() => {
  destroy()
})

// 暴露方法
defineExpose({
  load3DTileset,
  removeTileset,
  getTilesets,
  getEffectManager,
  destroy,
  getViewer,
  // 特效
  enableFog,
  setFogDensity,
  enableRain,
  enableSnow,
  enableSandstorm,
})
</script>

<style scoped>
.cesium-view {
  width: 100%;
  height: 100%;
  position: relative;
  overflow: hidden;
}

/* Tileset 加载面板 */
.tileset-panel {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 1000;
}

.tileset-toggle {
  width: 40px;
  height: 40px;
  border: none;
  background: white;
  border-radius: 8px;
  font-size: 20px;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
  transition: all 0.2s;
}

.tileset-toggle:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(0,0,0,0.2);
}

.tileset-panel-content {
  position: absolute;
  top: 50px;
  right: 0;
  width: 280px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.15);
  overflow: hidden;
}

.tileset-panel-content .panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  font-weight: 600;
  font-size: 14px;
}

.tileset-panel-content .panel-header button {
  width: 24px;
  height: 24px;
  border: none;
  background: transparent;
  font-size: 18px;
  color: #64748b;
  cursor: pointer;
  border-radius: 4px;
}

.tileset-panel-content .panel-header button:hover {
  background: #fee2e2;
  color: #dc2626;
}

.tileset-presets,
.tileset-custom,
.tileset-list {
  padding: 12px 16px;
}

.section-label {
  margin: 0 0 8px;
  font-size: 12px;
  color: #64748b;
  font-weight: 500;
}

.tileset-presets button {
  margin-right: 8px;
  margin-bottom: 8px;
  padding: 6px 12px;
  border: 1px solid #e2e8f0;
  background: white;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.tileset-presets button:hover {
  background: #eff6ff;
  border-color: #2563eb;
  color: #2563eb;
}

.tileset-custom input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 13px;
  margin-bottom: 8px;
}

.tileset-custom input:focus {
  outline: none;
  border-color: #2563eb;
}

.tileset-custom button {
  width: 100%;
  padding: 8px 12px;
  border: none;
  background: #2563eb;
  color: white;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
}

.tileset-custom button:disabled {
  background: #93c5fd;
  cursor: not-allowed;
}

.tileset-custom button:hover:not(:disabled) {
  background: #1d4ed8;
}

.tileset-list {
  border-top: 1px solid #e2e8f0;
}

.tileset-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  border-bottom: 1px solid #f1f5f9;
}

.tileset-item:last-child {
  border-bottom: none;
}

.tileset-item span {
  font-size: 13px;
  color: #475569;
}

.tileset-item button {
  width: 20px;
  height: 20px;
  border: none;
  background: #fee2e2;
  color: #dc2626;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.tileset-item button:hover {
  background: #fecaca;
}

.section-divider {
  border-top: 1px solid #e2e8f0;
  margin: 8px 0;
}

.effect-presets {
  padding: 0 4px 4px;
}

.effect-buttons {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.effect-buttons button {
  padding: 6px 10px;
  border: 1px solid #e2e8f0;
  background: white;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  color: #475569;
}

.effect-buttons button:hover {
  border-color: #3b82f6;
  color: #3b82f6;
}

.effect-buttons button.active {
  background: #3b82f6;
  border-color: #3b82f6;
  color: white;
}

/* Imagery Section */
.imagery-section {
  padding: 0 16px 12px;
}

.imagery-buttons {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.imagery-buttons button {
  padding: 6px 10px;
  border: 1px solid #e2e8f0;
  background: white;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  color: #475569;
}

.imagery-buttons button:hover {
  border-color: #3b82f6;
  color: #3b82f6;
}

.imagery-buttons button.active {
  background: #3b82f6;
  border-color: #3b82f6;
  color: white;
}

.ion-hint {
  margin: 4px 0 0;
  font-size: 11px;
  color: #94a5fb;
}

/* Point Cloud Section */
.pointcloud-section {
  padding: 0 16px 12px;
}

.pointcloud-buttons {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.pointcloud-buttons button {
  padding: 6px 10px;
  border: 1px solid #e2e8f0;
  background: white;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  color: #475569;
}

.pointcloud-buttons button:hover:not(:disabled) {
  border-color: #3b82f6;
  color: #3b82f6;
}

.pointcloud-buttons button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.pointcloud-list {
  margin-top: 8px;
}

.pointcloud-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  border-bottom: 1px solid #f1f5f9;
}

.pointcloud-item:last-child {
  border-bottom: none;
}

.pointcloud-item span {
  font-size: 13px;
  color: #475569;
}

.pointcloud-item button {
  width: 20px;
  height: 20px;
  border: none;
  background: #fee2e2;
  color: #dc2626;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pointcloud-item button:hover {
  background: #fecaca;
}

/* Point Cloud Modal */
.pointcloud-modal {
  margin: 8px 16px 12px;
  padding: 12px;
  background: #f8fafc;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.pointcloud-modal .modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
}

.pointcloud-modal .modal-header button {
  width: 20px;
  height: 20px;
  border: none;
  background: transparent;
  font-size: 16px;
  color: #64748b;
  cursor: pointer;
  border-radius: 4px;
}

.pointcloud-modal .modal-header button:hover {
  background: #fee2e2;
  color: #dc2626;
}

.pointcloud-modal input {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 13px;
  margin-bottom: 8px;
  box-sizing: border-box;
}

.pointcloud-modal input:focus {
  outline: none;
  border-color: #2563eb;
}

.pointcloud-modal > button {
  width: 100%;
  padding: 8px 12px;
  border: none;
  background: #2563eb;
  color: white;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
}

.pointcloud-modal > button:disabled {
  background: #93c5fd;
  cursor: not-allowed;
}

.pointcloud-modal > button:hover:not(:disabled) {
  background: #1d4ed8;
}

/* CZML Section */
.czml-section {
  padding: 0 16px 12px;
}

.czml-buttons {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.czml-buttons button {
  padding: 6px 10px;
  border: 1px solid #e2e8f0;
  background: white;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  color: #475569;
}

.czml-buttons button:hover:not(:disabled) {
  border-color: #3b82f6;
  color: #3b82f6;
}

.czml-buttons button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.data-source-list {
  margin-top: 8px;
}

.data-source-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  border-bottom: 1px solid #f1f5f9;
}

.data-source-item:last-child {
  border-bottom: none;
}

.data-source-item span {
  font-size: 13px;
  color: #475569;
}

.data-source-item button {
  width: 20px;
  height: 20px;
  border: none;
  background: #fee2e2;
  color: #dc2626;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.data-source-item button:hover {
  background: #fecaca;
}

/* Viewshed Section */
.viewshed-section {
  padding: 0 16px 12px;
}

.viewshed-buttons {
  display: flex;
  gap: 6px;
}

.viewshed-buttons button {
  padding: 6px 12px;
  border: 1px solid #e2e8f0;
  background: white;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  color: #475569;
}

.viewshed-buttons button:hover {
  border-color: #3b82f6;
  color: #3b82f6;
}

.viewshed-buttons button.active {
  background: #3b82f6;
  border-color: #3b82f6;
  color: white;
}

.viewshed-hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: #64748b;
}

/* Trajectory Modal */
.trajectory-modal {
  margin: 8px 16px 12px;
  padding: 12px;
  background: #f8fafc;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.trajectory-modal .modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
}

.trajectory-modal .modal-header button {
  width: 20px;
  height: 20px;
  border: none;
  background: transparent;
  font-size: 16px;
  color: #64748b;
  cursor: pointer;
  border-radius: 4px;
}

.trajectory-modal .modal-header button:hover {
  background: #fee2e2;
  color: #dc2626;
}

.trajectory-modal input {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 13px;
  margin-bottom: 8px;
  box-sizing: border-box;
}

.trajectory-modal input:focus {
  outline: none;
  border-color: #2563eb;
}

.trajectory-modal > button {
  width: 100%;
  padding: 8px 12px;
  border: none;
  background: #2563eb;
  color: white;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
}

.trajectory-modal > button:disabled {
  background: #93c5fd;
  cursor: not-allowed;
}

.trajectory-modal > button:hover:not(:disabled) {
  background: #1d4ed8;
}

/* Terrain Profile Section */
.profile-section {
  padding: 0 16px 12px;
}

.profile-buttons {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.profile-buttons button {
  padding: 6px 12px;
  border: 1px solid #e2e8f0;
  background: white;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  color: #475569;
}

.profile-buttons button:hover {
  border-color: #3b82f6;
  color: #3b82f6;
}

.profile-buttons button.active {
  background: #3b82f6;
  border-color: #3b82f6;
  color: white;
}

.profile-hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: #64748b;
}

/* Terrain Profile Chart */
.profile-chart-container {
  margin: 8px 16px 12px;
  padding: 12px;
  background: #f8fafc;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.profile-chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
}

.profile-chart-header button {
  width: 20px;
  height: 20px;
  border: none;
  background: transparent;
  font-size: 16px;
  color: #64748b;
  cursor: pointer;
  border-radius: 4px;
}

.profile-chart-header button:hover {
  background: #fee2e2;
  color: #dc2626;
}

.profile-chart {
  width: 100%;
  height: auto;
  background: white;
  border-radius: 4px;
  border: 1px solid #e2e8f0;
}

.profile-stats {
  display: flex;
  justify-content: space-around;
  margin-top: 8px;
  font-size: 11px;
  color: #64748b;
}

.profile-stats span {
  padding: 2px 6px;
  background: white;
  border-radius: 4px;
  border: 1px solid #e2e8f0;
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
