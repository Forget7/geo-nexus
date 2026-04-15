/**
 * useMap - 地图操作 Composable
 */

import { ref, shallowRef, onMounted, onUnmounted, type Ref } from 'vue'
import L from 'leaflet'

export interface MapOptions {
  center?: [number, number]
  zoom?: number
  tileLayer?: string
  maxZoom?: number
}

export interface MarkerOptions {
  lat: number
  lng: number
  popup?: string
  tooltip?: string
  icon?: L.Icon | L.DivIcon
}

const DEFAULT_TILE_LAYERS: Record<string, string> = {
  osm: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
  satellite: 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
  dark: 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png',
  terrain: 'https://{s}.tile.opentopomap.org/{z}/{x}/{y}.png'
}

export function useMap(containerId: string, options: MapOptions = {}) {
  const map = shallowRef<L.Map | null>(null)
  const markers = ref<L.Marker[]>([])
  const layers = ref<L.LayerGroup[]>([])
  const isReady = ref(false)
  
  function initMap() {
    if (map.value) return
    
    const container = document.getElementById(containerId)
    if (!container) {
      console.error(`Container #${containerId} not found`)
      return
    }
    
    const center = options.center || [35.0, 105.0]
    const zoom = options.zoom || 5
    
    map.value = L.map(containerId, {
      center,
      zoom,
      maxZoom: options.maxZoom || 19
    })
    
    // 添加底图
    const tileUrl = options.tileLayer 
      ? (DEFAULT_TILE_LAYERS[options.tileLayer] || options.tileLayer)
      : DEFAULT_TILE_LAYERS.osm
    
    L.tileLayer(tileUrl, {
      attribution: '© OpenStreetMap contributors',
      maxZoom: options.maxZoom || 19
    }).addTo(map.value)
    
    // 添加控件
    L.control.scale().addTo(map.value)
    L.control.layers().addTo(map.value)
    
    isReady.value = true
  }
  
  function destroyMap() {
    markers.value.forEach(m => m.remove())
    markers.value = []
    
    layers.value.forEach(l => l.remove())
    layers.value = []
    
    if (map.value) {
      map.value.remove()
      map.value = null
    }
    isReady.value = false
  }
  
  // 添加标记
  function addMarker(options: MarkerOptions): L.Marker | null {
    if (!map.value) return null
    
    const marker = L.marker([options.lat, options.lng], {
      icon: options.icon
    })
    
    if (options.popup) {
      marker.bindPopup(options.popup)
    }
    if (options.tooltip) {
      marker.bindTooltip(options.tooltip)
    }
    
    marker.addTo(map.value)
    markers.value.push(marker)
    return marker
  }
  
  // 批量添加标记
  function addMarkers(markerOptions: MarkerOptions[]): L.Marker[] {
    return markerOptions.map(opt => addMarker(opt)).filter(Boolean) as L.Marker[]
  }
  
  // 添加GeoJSON图层
  function addGeoJSON(geojson: any, style?: L.GeoJSONOptions): L.GeoJSON | null {
    if (!map.value) return null
    
    const geoJsonLayer = L.geoJSON(geojson, {
      style: style || {
        fillColor: '#2563EB',
        color: '#1E40AF',
        weight: 2,
        fillOpacity: 0.7
      },
      onEachFeature: (feature, layer) => {
        if (feature.properties) {
          let popup = ''
          for (const key in feature.properties) {
            popup += `<b>${key}:</b> ${feature.properties[key]}<br>`
          }
          layer.bindPopup(popup)
        }
      }
    })
    
    geoJsonLayer.addTo(map.value)
    
    const layerGroup = L.layerGroup([geoJsonLayer])
    layers.value.push(layerGroup)
    
    return geoJsonLayer
  }
  
  // 飞到某地
  function flyTo(lat: number, lng: number, zoom?: number) {
    if (!map.value) return
    map.value.flyTo([lat, lng], zoom || 14, {
      duration: 1
    })
  }
  
  // 调整视图适应所有标记
  function fitBounds() {
    if (!map.value || markers.value.length === 0) return
    
    const group = L.featureGroup(markers.value)
    map.value.fitBounds(group.getBounds(), {
      padding: [50, 50]
    })
  }
  
  // 设置底图
  function setTileLayer(type: string) {
    if (!map.value) return
    
    // 移除旧图层
    map.value.eachLayer((layer) => {
      if (layer instanceof L.TileLayer) {
        map.value?.removeLayer(layer)
      }
    })
    
    // 添加新图层
    const tileUrl = DEFAULT_TILE_LAYERS[type] || DEFAULT_TILE_LAYERS.osm
    L.tileLayer(tileUrl, {
      attribution: '© OpenStreetMap contributors',
      maxZoom: 19
    }).addTo(map.value)
  }
  
  // 获取地图中心
  function getCenter(): [number, number] | null {
    if (!map.value) return null
    const center = map.value.getCenter()
    return [center.lat, center.lng]
  }
  
  // 获取缩放级别
  function getZoom(): number {
    return map.value?.getZoom() || 0
  }
  
  // 清除所有标记
  function clearMarkers() {
    markers.value.forEach(m => m.remove())
    markers.value = []
  }
  
  // 移除图层
  function removeLayer(layer: L.LayerGroup) {
    layer.remove()
    layers.value = layers.value.filter(l => l !== layer)
  }
  
  onMounted(() => {
    initMap()
  })
  
  onUnmounted(() => {
    destroyMap()
  })
  
  return {
    map,
    markers,
    layers,
    isReady,
    initMap,
    destroyMap,
    addMarker,
    addMarkers,
    addGeoJSON,
    flyTo,
    fitBounds,
    setTileLayer,
    getCenter,
    getZoom,
    clearMarkers,
    removeLayer
  }
}
