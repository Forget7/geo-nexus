/**
 * useGeoJSON - GeoJSON数据处理 Composable
 */

import { ref, computed, shallowRef } from 'vue'
import type { Geometry, GeoJSONFeature, GeoJSONFeatureCollection, BoundingBox } from '../types'

export interface UseGeoJSONOptions {
  validate?: boolean
  simplify?: boolean
  simplifyTolerance?: number
  /** 要素数量超过此阈值时自动启用简化（默认 10000） */
  simplifyThreshold?: number
  /** 要素数量超过此阈值时自动启用 clustering（默认 5000） */
  clusteringThreshold?: number
  /** 视口范围，用于 viewport culling */
  viewportBounds?: BoundingBox | null
}

export interface ClusteredFeature<G = Geometry> {
  type: 'Feature'
  geometry: G
  properties: {
    cluster: true
    cluster_id: number
    point_count: number
    point_count_abbreviated: string
  }
  id?: string | number
}

export function useGeoJSON(options: UseGeoJSONOptions = {}) {
  const data = shallowRef<GeoJSONFeatureCollection | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)
  const selectedFeatureId = ref<string | number | null>(null)
  
  // 计算属性
  const featureCount = computed(() => data.value?.features.length ?? 0)
  
  const bounds = computed<BoundingBox | null>(() => {
    if (!data.value) return null
    
    let minLng = Infinity, minLat = Infinity
    let maxLng = -Infinity, maxLat = -Infinity
    
    for (const feature of data.value.features) {
      const coords = getFeatureCoordinates(feature)
      for (const [lng, lat] of coords) {
        minLng = Math.min(minLng, lng)
        minLat = Math.min(minLat, lat)
        maxLng = Math.max(maxLng, lng)
        maxLat = Math.max(maxLat, lat)
      }
    }
    
    if (!isFinite(minLng)) return null
    
    return { minLng, minLat, maxLng, maxLat }
  })
  
  const center = computed<[number, number] | null>(() => {
    const b = bounds.value
    if (!b) return null
    return [(b.minLng + b.maxLng) / 2, (b.minLat + b.maxLat) / 2]
  })
  
  const geometryTypes = computed(() => {
    if (!data.value) return []
    const types = new Set<string>()
    for (const f of data.value.features) {
      types.add(f.geometry.type)
    }
    return Array.from(types)
  })
  
  const properties = computed(() => {
    if (!data.value || data.value.features.length === 0) return []
    return Object.keys(data.value.features[0].properties || {})
  })
  
  // 加载GeoJSON
  async function loadGeoJSON(source: string | object) {
    loading.value = true
    error.value = null
    
    try {
      let json: any
      
      if (typeof source === 'string') {
        // 可能是URL或JSON字符串
        if (source.startsWith('http')) {
          const response = await fetch(source)
          if (!response.ok) throw new Error(`HTTP ${response.status}`)
          json = await response.json()
        } else {
          json = JSON.parse(source)
        }
      } else {
        json = source
      }
      
      // 验证结构
      if (options.validate !== false) {
        validateGeoJSON(json)
      }
      
      data.value = json
      return json
      
    } catch (e: any) {
      error.value = e.message
      throw e
    } finally {
      loading.value = false
    }
  }
  
  // 验证GeoJSON
  function validateGeoJSON(json: any): void {
    if (!json.type) {
      throw new Error('Invalid GeoJSON: missing type')
    }
    
    if (json.type === 'FeatureCollection') {
      if (!Array.isArray(json.features)) {
        throw new Error('Invalid GeoJSON: features must be an array')
      }
    } else if (json.type === 'Feature') {
      if (!json.geometry) {
        throw new Error('Invalid GeoJSON: missing geometry')
      }
    } else {
      // 可能是原始几何对象
      const validTypes = ['Point', 'LineString', 'Polygon', 'MultiPoint', 'MultiLineString', 'MultiPolygon', 'GeometryCollection']
      if (!validTypes.includes(json.type)) {
        throw new Error(`Invalid GeoJSON: unknown type ${json.type}`)
      }
    }
  }
  
  // 选择要素
  function selectFeature(id: string | number | null) {
    selectedFeatureId.value = id
  }
  
  // 获取选中要素
  function getSelectedFeature(): GeoJSONFeature | null {
    if (!data.value || selectedFeatureId.value === null) return null
    return data.value.features.find(f => f.id === selectedFeatureId.value) || null
  }
  
  // 按属性过滤
  function filterFeatures(predicate: (feature: GeoJSONFeature) => boolean): GeoJSONFeature[] {
    if (!data.value) return []
    return data.value.features.filter(predicate)
  }
  
  // 按属性分组
  function groupByProperty(propertyKey: string): Record<string, GeoJSONFeature[]> {
    if (!data.value) return {}
    
    const groups: Record<string, GeoJSONFeature[]> = {}
    
    for (const feature of data.value.features) {
      const key = String(feature.properties?.[propertyKey] ?? 'undefined')
      if (!groups[key]) groups[key] = []
      groups[key].push(feature)
    }
    
    return groups
  }
  
  // 统计属性值
  function countPropertyValues(propertyKey: string): Record<string, number> {
    const groups = groupByProperty(propertyKey)
    const counts: Record<string, number> = {}
    
    for (const [key, features] of Object.entries(groups)) {
      counts[key] = features.length
    }
    
    return counts
  }
  
  // 导出GeoJSON
  function exportGeoJSON(pretty = false): string {
    if (!data.value) return '{}'
    return JSON.stringify(data.value, null, pretty ? 2 : 0)
  }
  
  // 创建Feature
  function createFeature<G extends Geometry>(
    geometry: G, 
    properties: Record<string, any> = {},
    id?: string | number
  ): GeoJSONFeature<G> {
    const feature: GeoJSONFeature<G> = {
      type: 'Feature',
      geometry,
      properties,
    }
    if (id !== undefined) feature.id = id
    return feature
  }
  
  // 添加Feature
  function addFeature(feature: GeoJSONFeature) {
    if (!data.value) {
      data.value = {
        type: 'FeatureCollection',
        features: [feature]
      }
    } else {
      data.value.features.push(feature)
    }
  }
  
  // 移除Feature
  function removeFeature(id: string | number) {
    if (!data.value) return
    data.value.features = data.value.features.filter(f => f.id !== id)
    if (selectedFeatureId.value === id) {
      selectedFeatureId.value = null
    }
  }
  
  // 更新Feature
  function updateFeature(id: string | number, updates: Partial<GeoJSONFeature>) {
    if (!data.value) return
    
    const index = data.value.features.findIndex(f => f.id === id)
    if (index === -1) return
    
    data.value.features[index] = {
      ...data.value.features[index],
      ...updates
    }
  }
  
  // 清空数据
  function clear() {
    data.value = null
    selectedFeatureId.value = null
    error.value = null
  }

  // ---- 大数据优化方法 ----

  /**
   * Douglas-Peucker 简化算法（用于线/面数据降采样）
   */
  function simplifyGeoJSON(tolerance: number = 0.0001): GeoJSONFeatureCollection | null {
    if (!data.value) return null
    const threshold = options.simplifyThreshold ?? 10000
    if (data.value.features.length < threshold && !options.simplify) return data.value

    return {
      ...data.value,
      features: data.value.features.map(f => {
        if (f.geometry.type === 'LineString' || f.geometry.type === 'MultiLineString') {
          return {
            ...f,
            geometry: {
              ...f.geometry,
              coordinates: simplifyLine(f.geometry.coordinates as number[][][], tolerance)
            }
          }
        }
        if (f.geometry.type === 'Polygon' || f.geometry.type === 'MultiPolygon') {
          return {
            ...f,
            geometry: {
              ...f.geometry,
              coordinates: simplifyPolygon(f.geometry.coordinates as number[][][][], tolerance)
            }
          }
        }
        return f
      })
    }
  }

  function simplifyLine(coords: number[][][], _tolerance: number): number[][][] {
    // 简化的 Douglas-Peucker 实现（点数据不简化，线/面才简化）
    return coords.map(ring => {
      if (ring.length < 10) return ring
      return douglasPeucker(ring as [number, number][], _tolerance)
    })
  }

  function simplifyPolygon(coords: number[][][][], _tolerance: number): number[][][][] {
    return coords.map(poly => poly.map(ring => {
      if (ring.length < 10) return ring
      return douglasPeucker(ring as [number, number][], _tolerance)
    }))
  }

  function douglasPeucker(points: [number, number][], tolerance: number): [number, number][] {
    if (points.length <= 2) return points
    let maxDist = 0, maxIdx = 0
    const start = points[0], end = points[points.length - 1]
    for (let i = 1; i < points.length - 1; i++) {
      const d = perpendicularDist(points[i], start, end)
      if (d > maxDist) { maxDist = d; maxIdx = i }
    }
    if (maxDist > tolerance) {
      const left = douglasPeucker(points.slice(0, maxIdx + 1), tolerance)
      const right = douglasPeucker(points.slice(maxIdx), tolerance)
      return [...left.slice(0, -1), ...right]
    }
    return [start, end]
  }

  function perpendicularDist(point: [number, number], lineStart: [number, number], lineEnd: [number, number]): number {
    const dx = lineEnd[0] - lineStart[0]
    const dy = lineEnd[1] - lineStart[1]
    if (dx === 0 && dy === 0) return Math.hypot(point[0] - lineStart[0], point[1] - lineStart[1])
    const t = ((point[0] - lineStart[0]) * dx + (point[1] - lineStart[1]) * dy) / (dx * dx + dy * dy)
    const nearX = lineStart[0] + t * dx
    const nearY = lineStart[1] + t * dy
    return Math.hypot(point[0] - nearX, point[1] - nearY)
  }

  /**
   * 视口裁剪：只返回落在视口范围内的要素
   */
  function filterByViewport(viewport: BoundingBox | null): GeoJSONFeature[] {
    if (!data.value || !viewport) return data.value.features
    const { minLng, minLat, maxLng, maxLat } = viewport
    return data.value.features.filter(f => {
      const coords = getFeatureCoordinates(f)
      return coords.some(([lng, lat]) =>
        lng >= minLng && lng <= maxLng && lat >= minLat && lat <= maxLat
      )
    })
  }

  /**
   * 简易网格聚合：将点按网格聚合，生成 cluster 要素
   * 用于万级点数据的展示优化
   */
  function clusterFeatures(gridSize: number = 0.01): (GeoJSONFeature | ClusteredFeature)[] {
    if (!data.value) return []
    const threshold = options.clusteringThreshold ?? 5000
    const features = data.value.features

    if (features.length < threshold) return features

    const clusters = new Map<string, { coords: [number, number], count: number, ids: (string | number)[] }>()
    for (const f of features) {
      if (f.geometry.type !== 'Point') continue
      const [lng, lat] = f.geometry.coordinates as [number, number]
      const key = `${Math.floor(lng / gridSize)},${Math.floor(lat / gridSize)}`
      const existing = clusters.get(key)
      if (existing) {
        existing.count++
        if (f.id !== undefined) existing.ids.push(f.id)
      } else {
        clusters.set(key, {
          coords: [lng, lat],
          count: 1,
          ids: f.id !== undefined ? [f.id] : []
        })
      }
    }

    // 有聚合意义的点才替换
    const clustered: (GeoJSONFeature | ClusteredFeature)[] = []
    const clusteredKeys = new Set<string>()
    for (const [key, cluster] of clusters) {
      if (cluster.count > 1) {
        clusteredKeys.add(key)
        clustered.push({
          type: 'Feature',
          geometry: { type: 'Point', coordinates: cluster.coords } as Geometry,
          properties: {
            cluster: true,
            cluster_id: key,
            point_count: cluster.count,
            point_count_abbreviated: String(cluster.count)
          },
          id: key
        } as ClusteredFeature)
      }
    }
    // 非点要素和未聚合的点直接保留
    for (const f of features) {
      if (f.geometry.type !== 'Point') clustered.push(f)
      else {
        const [lng, lat] = f.geometry.coordinates as [number, number]
        const key = `${Math.floor(lng / gridSize)},${Math.floor(lat / gridSize)}`
        if (!clusteredKeys.has(key)) clustered.push(f)
      }
    }
    return clustered
  }

  return {
    // 状态
    data,
    loading,
    error,
    selectedFeatureId,
    
    // 计算属性
    featureCount,
    bounds,
    center,
    geometryTypes,
    properties,
    
    // 方法
    loadGeoJSON,
    validateGeoJSON,
    selectFeature,
    getSelectedFeature,
    filterFeatures,
    groupByProperty,
    countPropertyValues,
    exportGeoJSON,
    createFeature,
    addFeature,
    removeFeature,
    updateFeature,
    clear,

    // 大数据优化方法
    simplifyGeoJSON,
    filterByViewport,
    clusterFeatures,
  }
}

// 辅助函数：获取Feature的所有坐标
function getFeatureCoordinates(feature: GeoJSONFeature): [number, number][] {
  const coords: [number, number][] = []
  extractCoordinates(feature.geometry, coords)
  return coords
}

function extractCoordinates(geometry: Geometry, coords: [number, number][]): void {
  switch (geometry.type) {
    case 'Point':
      coords.push(geometry.coordinates as [number, number])
      break
    case 'LineString':
      for (const c of geometry.coordinates) {
        coords.push(c as [number, number])
      }
      break
    case 'Polygon':
      for (const ring of geometry.coordinates) {
        for (const c of ring) {
          coords.push(c as [number, number])
        }
      }
      break
    case 'MultiPoint':
      for (const c of geometry.coordinates) {
        coords.push(c as [number, number])
      }
      break
    case 'MultiLineString':
      for (const line of geometry.coordinates) {
        for (const c of line) {
          coords.push(c as [number, number])
        }
      }
      break
    case 'MultiPolygon':
      for (const poly of geometry.coordinates) {
        for (const ring of poly) {
          for (const c of ring) {
            coords.push(c as [number, number])
          }
        }
      }
      break
    case 'GeometryCollection':
      for (const g of geometry.geometries) {
        extractCoordinates(g, coords)
      }
      break
  }
}
