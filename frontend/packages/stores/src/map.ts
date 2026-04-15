/**
 * Map Store - 地图状态管理
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { mapApi, MapGenerateRequest } from '../../src/api/client'

export type MapMode = '2d' | '3d'
export type TileType = 'osm' | 'satellite' | 'dark' | 'terrain'

export interface MapState {
  id: string
  mode: MapMode
  tileType: TileType
  center: [number, number]
  zoom: number
  height: number
  geojson: GeoJSON | null
}

export interface GeoJSON {
  type: string
  geometry?: Record<string, unknown>
  properties?: Record<string, unknown>
  features?: GeoJSON[]
  [key: string]: unknown
}

export const useMapStore = defineStore('map', () => {
  // State
  const currentMapId = ref<string>('')
  const currentMapUrl = ref<string>('')
  const mapMode = ref<MapMode>('2d')
  const tileType = ref<TileType>('osm')
  const center = ref<[number, number]>([35.0, 105.0]) // 中国中心
  const zoom = ref(10)
  const height = ref(10000) // 3D高度
  const geojson = ref<GeoJSON | null>(null)
  const isLoading = ref(false)
  const error = ref<string | null>(null)
  
  // History
  const mapHistory = ref<MapState[]>([])
  
  // Getters
  const hasGeojson = computed(() => geojson.value !== null)
  
  // Actions
  async function generateMap(data?: Partial<MapGenerateRequest>) {
    isLoading.value = true
    error.value = null
    
    try {
      const request: MapGenerateRequest = {
        geojson: data?.geojson || geojson.value,
        center: data?.center || center.value,
        zoom: data?.zoom || zoom.value,
        height: data?.height || height.value,
        mode: data?.mode || mapMode.value,
        tileType: data?.tileType || tileType.value
      }
      
      const response = await mapApi.generate(request)
      
      currentMapId.value = response.id
      currentMapUrl.value = response.url
      
      // 保存到历史
      mapHistory.value.push({
        id: response.id,
        mode: mapMode.value,
        tileType: tileType.value,
        center: center.value,
        zoom: zoom.value,
        height: height.value,
        geojson: geojson.value
      })
      
      return response
      
    } catch (err: unknown) {
      error.value = (err as Error).message || '生成地图失败'
      throw err
    } finally {
      isLoading.value = false
    }
  }
  
  function setCenter(newCenter: [number, number]) {
    center.value = newCenter
  }
  
  function setZoom(newZoom: number) {
    zoom.value = Math.max(1, Math.min(20, newZoom))
  }
  
  function setMode(mode: MapMode) {
    mapMode.value = mode
  }
  
  function setTileType(type: TileType) {
    tileType.value = type
  }
  
  function setGeojson(data: GeoJSON | null) {
    geojson.value = data
  }
  
  function clearMap() {
    currentMapId.value = ''
    currentMapUrl.value = ''
    geojson.value = null
    error.value = null
  }
  
  return {
    // State
    currentMapId,
    currentMapUrl,
    mapMode,
    tileType,
    center,
    zoom,
    height,
    geojson,
    isLoading,
    error,
    mapHistory,
    // Getters
    hasGeojson,
    // Actions
    generateMap,
    setCenter,
    setZoom,
    setMode,
    setTileType,
    setGeojson,
    clearMap
  }
})
