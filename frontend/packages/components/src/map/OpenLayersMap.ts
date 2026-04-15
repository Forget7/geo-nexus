/**
 * OpenLayers地图组件 - 与Cesium无缝集成
 * 统一的坐标系统、投影、图层管理
 */

import { ref, shallowRef, onMounted, onUnmounted, type Ref } from 'vue'
import Map from 'ol/Map'
import View from 'ol/View'
import TileLayer from 'ol/layer/Tile'
import VectorLayer from 'ol/layer/Vector'
import VectorSource from 'ol/source/Vector'
import XYZ from 'ol/source/XYZ'
import OSM from 'ol/source/OSM'
import WMS from 'ol/source/WMS'
import WMTS from 'ol/source/WMTS'
import BingMaps from 'ol/source/BingMaps'
import { fromLonLat, toLonLat, transform, get as getProjection } from 'ol/proj'
import { OverviewMap, ScaleLine, ZoomSlider, Controls } from 'ol/control'
import { defaults as defaultControls } from 'ol/control'
import Feature from 'ol/Feature'
import Point from 'ol/geom/Point'
import LineString from 'ol/geom/LineString'
import Polygon from 'ol/geom/Polygon'
import MultiPoint from 'ol/geom/MultiPoint'
import MultiLineString from 'ol/geom/MultiLineString'
import MultiPolygon from 'ol/geom/MultiPolygon'
import Circle from 'ol/geom/Circle'
import { Style, Stroke, Fill, Text, Icon, Circle as CircleStyle, RegularShape } from 'ol/style'
import { Draw, Modify, Select, Interaction } from 'ol/interaction'
import { getWidth, getHeight } from 'ol/extent'
import TileWMS from 'ol/source/TileWMS'
import VectorTileLayer from 'ol/layer/VectorTile'
import VectorTileSource from 'ol/source/VectorTile'
import MVT from 'ol/format/MVT'

// 常用投影
export const EPSG_4326 = 'EPSG:4326'      // WGS84 经纬度
export const EPSG_3857 = 'EPSG:3857'        // Web墨卡托
export const EPSG_4490 = 'EPSG:4490'        // 中国CGCS2000

// 底图类型
export type TileSourceType = 'osm' | 'bing' | 'satellite' | 'dark' | 'wms' | 'wmts' | 'custom'

export interface MapOptions {
  center?: [number, number]
  zoom?: number
  projection?: string
  maxZoom?: number
  minZoom?: number
  extent?: [number, number, number, number]
  controls?: boolean
  overviewMap?: boolean
}

// 样式接口
export interface LayerStyle {
  fill?: {
    color?: string
    opacity?: number
  }
  stroke?: {
    color?: string
    width?: number
    opacity?: number
    lineCap?: 'round' | 'square' | 'butt'
    lineJoin?: 'round' | 'bevel' | 'miter'
    lineDash?: number[]
  }
  text?: {
    font?: string
    text?: string
    fillColor?: string
    strokeColor?: string
    strokeWidth?: number
    offsetX?: number
    offsetY?: number
    scale?: number
  }
  icon?: {
    src?: string
    scale?: number
    size?: [number, number]
    anchor?: [number, number]
  }
  circle?: {
    radius?: number
    fillColor?: string
    strokeColor?: string
    strokeWidth?: number
  }
}

// 默认样式
const DEFAULT_STYLE = {
  fill: new Fill({ color: 'rgba(37, 99, 235, 0.3)' }),
  stroke: new Stroke({ color: '#1E40AF', width: 2 }),
  text: new Text({
    font: '12px sans-serif',
    fill: new Fill({ color: '#1E293B' }),
    stroke: new Stroke({ color: '#fff', width: 3 })
  }),
  circle: new CircleStyle({
    radius: 6,
    fill: new Fill({ color: '#2563EB' }),
    stroke: new Stroke({ color: '#fff', width: 2 })
  })
}

/**
 * OpenLayers地图Hook
 */
export function useOpenLayers(containerId: string, options: MapOptions = {}) {
  const map = shallowRef<Map | null>(null)
  const layers = ref<Map<string, any>>(new Map())
  const interactions = ref<Map<string, Interaction>>(new Map())
  const isReady = ref(false)
  
  // 默认配置
  const defaultCenter: [number, number] = options.center || [116.4, 39.9] // 北京
  const defaultZoom = options.zoom || 10
  const defaultProjection = options.projection || EPSG_3857
  
  // 初始化地图
  function initMap() {
    if (map.value) return
    
    const container = document.getElementById(containerId)
    if (!container) {
      console.error(`Container #${containerId} not found`)
      return
    }
    
    // 控件配置
    const controls = options.controls !== false 
      ? defaultControls({
          zoom: true,
          rotate: false,
          attribution: true
        })
      : []
    
    // 添加概述图
    if (options.overviewMap) {
      controls.push(new OverviewMap({
        collapsed: false,
        layers: [
          new TileLayer({
            source: new OSM()
          })
        ]
      }))
    }
    
    // 添加比例尺
    controls.push(new ScaleLine({
      units: 'metric',
      bar: true,
      steps: 4,
      text: true,
      minWidth: 140
    }))
    
    // 创建地图
    map.value = new Map({
      target: containerId,
      layers: [],
      view: new View({
        center: fromLonLat(defaultCenter, defaultProjection),
        zoom: defaultZoom,
        maxZoom: options.maxZoom || 19,
        minZoom: options.minZoom || 3,
        projection: defaultProjection,
        extent: options.extent
      }),
      controls,
      logo: false
    })
    
    // 添加默认底图
    addBaseLayer('osm')
    
    isReady.value = true
    
    // 监听事件
    map.value.on('moveend', () => {
      emitMapChange()
    })
    
    map.value.on('click', (e) => {
      const features = map.value?.getFeaturesAtPixel(e.pixel)
      if (features && features.length > 0) {
        console.log('Clicked features:', features.map(f => f.get('name') || f.getId()))
      }
    })
  }
  
  // 销毁地图
  function destroyMap() {
    if (map.value) {
      map.value.setTarget(undefined)
      map.value = null
    }
    layers.value.clear()
    interactions.value.clear()
    isReady.value = false
  }
  
  // 发送地图变化事件
  function emitMapChange() {
    if (!map.value) return
    const view = map.value.getView()
    const center = toLonLat(view.getCenter()!, view.getProjection())
    const zoom = view.getZoom()
    console.log(`Map center: ${center}, zoom: ${zoom}`)
  }
  
  // ==================== 图层管理 ====================
  
  /**
   * 添加底图
   */
  function addBaseLayer(type: TileSourceType, options: {
    url?: string
    layer?: string
    matrixSet?: string
  } = {}) {
    if (!map.value) return
    
    // 移除现有底图
    const existingBase = layers.value.get('base')
    if (existingBase) {
      map.value.removeLayer(existingBase)
    }
    
    let source: any
    let layer: TileLayer
    
    switch (type) {
      case 'osm':
        source = new OSM({
          attributions: '© OpenStreetMap contributors'
        })
        break
        
      case 'bing':
        source = new BingMaps({
          key: options.url || 'your-bing-api-key',
          imagerySet: options.layer || 'Aerial'
        })
        break
        
      case 'satellite':
        source = new XYZ({
          url: 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}'
        })
        break
        
      case 'dark':
        source = new XYZ({
          url: 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png',
          attributions: '© CartoDB'
        })
        break
        
      case 'wms':
        source = new TileWMS({
          url: options.url || 'http://localhost:8080/geoserver/wms',
          params: {
            'LAYERS': options.layer || 'workspace:layer',
            'TILED': true
          },
          serverType: 'geoserver'
        })
        break
        
      case 'wmts':
        source = new WMTS({
          url: options.url || 'http://localhost:8080/geoserver/gwc/service/wmts',
          layer: options.layer || 'workspace:layer',
          matrixSet: options.matrixSet || 'EPSG:3857',
          format: 'image/png',
          projection: EPSG_3857,
          tileGrid: undefined // 使用默认
        })
        break
        
      case 'custom':
        source = new XYZ({
          url: options.url,
          attributions: ''
        })
        break
        
      default:
        source = new OSM()
    }
    
    layer = new TileLayer({
      source,
      zIndex: 0
    })
    
    layer.set('id', 'base')
    layer.set('type', 'base')
    
    map.value.addLayer(layer)
    layers.value.set('base', layer)
    
    return layer
  }
  
  /**
   * 添加矢量图层
   */
  function addVectorLayer(
    id: string, 
    features: Feature[] = [],
    style?: LayerStyle
  ): VectorLayer<VectorSource> {
    if (!map.value) throw new Error('Map not initialized')
    
    // 移除已存在的
    if (layers.value.has(id)) {
      map.value.removeLayer(layers.value.get(id))
    }
    
    const source = new VectorSource({
      features,
      wrapX: false
    })
    
    const layer = new VectorLayer({
      source,
      style: style ? createStyleFunction(style) : undefined,
      zIndex: 1
    })
    
    layer.set('id', id)
    layer.set('type', 'vector')
    
    map.value.addLayer(layer)
    layers.value.set(id, layer)
    
    return layer
  }
  
  /**
   * 添加VectorTile图层
   */
  function addVectorTileLayer(
    id: string,
    url: string,
    style?: any
  ): VectorTileLayer {
    if (!map.value) throw new Error('Map not initialized')
    
    const source = new VectorTileSource({
      format: new MVT(),
      url
    })
    
    const layer = new VectorTileLayer({
      source,
      style,
      zIndex: 2
    })
    
    layer.set('id', id)
    layer.set('type', 'vectortile')
    
    map.value.addLayer(layer)
    layers.value.set(id, layer)
    
    return layer
  }
  
  /**
   * 获取图层
   */
  function getLayer(id: string): any {
    return layers.value.get(id)
  }
  
  /**
   * 移除图层
   */
  function removeLayer(id: string) {
    const layer = layers.value.get(id)
    if (layer) {
      map.value?.removeLayer(layer)
      layers.value.delete(id)
    }
  }
  
  /**
   * 设置图层可见性
   */
  function setLayerVisible(id: string, visible: boolean) {
    const layer = layers.value.get(id)
    if (layer) {
      layer.setVisible(visible)
    }
  }
  
  /**
   * 设置图层透明度
   */
  function setLayerOpacity(id: string, opacity: number) {
    const layer = layers.value.get(id)
    if (layer) {
      layer.setOpacity(opacity)
    }
  }
  
  // ==================== 要素操作 ====================
  
  /**
   * 添加要素
   */
  function addFeature(layerId: string, feature: Feature | Feature[]) {
    const layer = layers.value.get(layerId) as VectorLayer<VectorSource>
    if (!layer) return
    
    const source = layer.getSource() as VectorSource
    if (Array.isArray(feature)) {
      source.addFeatures(feature)
    } else {
      source.addFeature(feature)
    }
  }
  
  /**
   * 移除要素
   */
  function removeFeature(layerId: string, feature: Feature) {
    const layer = layers.value.get(layerId) as VectorLayer<VectorSource>
    if (!layer) return
    
    const source = layer.getSource() as VectorSource
    source.removeFeature(feature)
  }
  
  /**
   * 清空图层要素
   */
  function clearFeatures(layerId: string) {
    const layer = layers.value.get(layerId) as VectorLayer<VectorSource>
    if (!layer) return
    
    const source = layer.getSource() as VectorSource
    source.clear()
  }
  
  /**
   * 获取所有要素
   */
  function getFeatures(layerId: string): Feature[] {
    const layer = layers.value.get(layerId) as VectorLayer<VectorSource>
    if (!layer) return []
    return layer.getSource().getFeatures()
  }
  
  // ==================== 几何创建 ====================
  
  /**
   * 创建点
   */
  function createPoint(coords: [number, number], projection = EPSG_4326): Feature {
    const transformed = projection === EPSG_3857 
      ? coords 
      : fromLonLat(coords, EPSG_3857)
    
    return new Feature({
      geometry: new Point(transformed)
    })
  }
  
  /**
   * 创建线
   */
  function createLineString(
    coords: [number, number][], 
    projection = EPSG_4326
  ): Feature {
    const transformed = projection === EPSG_3857 
      ? coords 
      : coords.map(c => fromLonLat(c, EPSG_3857))
    
    return new Feature({
      geometry: new LineString(transformed)
    })
  }
  
  /**
   * 创建多边形
   */
  function createPolygon(
    coords: [number, number][][],
    projection = EPSG_4326
  ): Feature {
    const transformed = coords.map(ring => 
      projection === EPSG_3857 
        ? ring 
        : ring.map(c => fromLonLat(c, EPSG_3857))
    )
    
    return new Feature({
      geometry: new Polygon(transformed)
    })
  }
  
  /**
   * 创建圆
   */
  function createCircle(
    center: [number, number],
    radius: number, // 米
    projection = EPSG_4326
  ): Feature {
    const transformed = projection === EPSG_3857 
      ? center 
      : fromLonLat(center, EPSG_3857)
    
    return new Feature({
      geometry: new Circle(transformed, radius)
    })
  }
  
  // ==================== 样式 ====================
  
  /**
   * 创建样式函数
   */
  function createStyleFunction(style: LayerStyle): (feature: Feature) => Style {
    return (feature: Feature) => {
      const s = new Style()
      
      // 填充
      if (style.fill) {
        s.setFill(new Fill({
          color: hexToRgba(
            style.fill.color || '#2563EB',
            style.fill.opacity ?? 0.3
          )
        }))
      }
      
      // 描边
      if (style.stroke) {
        s.setStroke(new Stroke({
          color: hexToRgba(
            style.stroke.color || '#1E40AF',
            style.stroke.opacity ?? 1
          ),
          width: style.stroke.width || 2,
          lineCap: style.stroke.lineCap,
          lineJoin: style.stroke.lineJoin,
          lineDash: style.stroke.lineDash
        }))
      }
      
      // 文字
      if (style.text?.text) {
        s.setText(new Text({
          font: style.text.font || '12px sans-serif',
          text: style.text.text,
          fill: new Fill({
            color: style.text.fillColor || '#1E293B'
          }),
          stroke: new Stroke({
            color: style.text.strokeColor || '#fff',
            width: style.text.strokeWidth || 3
          }),
          offsetX: style.text.offsetX || 0,
          offsetY: style.text.offsetY || 0,
          scale: style.text.scale
        }))
      }
      
      // 图标
      if (style.icon?.src) {
        s.setImage(new Icon({
          src: style.icon.src,
          scale: style.icon.scale || 1,
          size: style.icon.size,
          anchor: style.icon.anchor
        }))
      }
      
      // 圆点
      if (style.circle) {
        s.setImage(new CircleStyle({
          radius: style.circle.radius || 6,
          fill: new Fill({
            color: style.circle.fillColor || '#2563EB'
          }),
          stroke: new Stroke({
            color: style.circle.strokeColor || '#fff',
            width: style.circle.strokeWidth || 2
          })
        }))
      }
      
      return s
    }
  }
  
  // ==================== 交互 ====================
  
  /**
   * 添加绘制交互
   */
  function addDrawInteraction(
    id: string,
    type: 'Point' | 'LineString' | 'Polygon' | 'Circle',
    layerId: string
  ): Draw {
    if (!map.value) throw new Error('Map not initialized')
    
    const layer = layers.value.get(layerId) as VectorLayer<VectorSource>
    if (!layer) throw new Error(`Layer ${layerId} not found`)
    
    const draw = new Draw({
      source: layer.getSource() as VectorSource,
      type: type
    })
    
    map.value.addInteraction(draw)
    interactions.value.set(id, draw)
    
    draw.on('drawend', (e) => {
      console.log('Feature drawn:', e.feature)
    })
    
    return draw
  }
  
  /**
   * 添加选择交互
   */
  function addSelectInteraction(
    id: string,
    layerIds?: string[],
    options?: {
      multi?: boolean
      click?: boolean
      hover?: boolean
    }
  ): Select {
    if (!map.value) throw new Error('Map not initialized')
    
    const select = new Select({
      layers: layerIds ? layerIds.map(id => layers.value.get(id)) : undefined,
      multi: options?.multi ?? false,
      condition: options?.click 
        ? undefined 
        : options?.hover 
          ? (mapEvent) => mapEvent.map.forEachFeatureAtPixel(
              mapEvent.pixel, 
              f => f
            )
          : undefined
    })
    
    map.value.addInteraction(select)
    interactions.value.set(id, select)
    
    select.on('select', (e) => {
      console.log('Selected features:', e.selected)
    })
    
    return select
  }
  
  /**
   * 添加编辑交互
   */
  function addModifyInteraction(id: string, layerId: string): Modify {
    if (!map.value) throw new Error('Map not initialized')
    
    const layer = layers.value.get(layerId) as VectorLayer<VectorSource>
    if (!layer) throw new Error(`Layer ${layerId} not found`)
    
    const modify = new Modify({
      source: layer.getSource() as VectorSource
    })
    
    map.value.addInteraction(modify)
    interactions.value.set(id, modify)
    
    return modify
  }
  
  /**
   * 移除交互
   */
  function removeInteraction(id: string) {
    const interaction = interactions.value.get(id)
    if (interaction && map.value) {
      map.value.removeInteraction(interaction)
      interactions.value.delete(id)
    }
  }
  
  // ==================== 视图控制 ====================
  
  /**
   * 飞往某地
   */
  function flyTo(
    coords: [number, number],
    zoom?: number,
    duration = 1000
  ) {
    if (!map.value) return
    
    const view = map.value.getView()
    const projection = view.getProjection()
    
    map.value.getView().animate({
      center: fromLonLat(coords, projection),
      zoom: zoom || view.getZoom(),
      duration
    })
  }
  
  /**
   * 设置视图
   */
  function setView(
    center: [number, number],
    zoom?: number,
    rotation?: number
  ) {
    if (!map.value) return
    
    const view = map.value.getView()
    view.setCenter(fromLonLat(center))
    if (zoom !== undefined) view.setZoom(zoom)
    if (rotation !== undefined) view.setRotation(rotation)
  }
  
  /**
   * 获取当前视图状态
   */
  function getViewState(): {
    center: [number, number]
    zoom: number
    projection: string
  } {
    if (!map.value) {
      return { center: [0, 0], zoom: 0, projection: '' }
    }
    
    const view = map.value.getView()
    const center = toLonLat(view.getCenter()!, view.getProjection())
    
    return {
      center: center as [number, number],
      zoom: view.getZoom() || 0,
      projection: view.getProjection().getCode()
    }
  }
  
  /**
   * 缩放到图层
   */
  function fitToLayer(layerId: string, padding = [50, 50, 50, 50]) {
    const layer = layers.value.get(layerId)
    if (!layer || !map.value) return
    
    const source = layer.getSource()
    if (source) {
      const extent = source.getExtent()
      if (extent && !extent.some(e => !isFinite(e))) {
        map.value.getView().fit(extent, {
          size: map.value.getSize(),
          padding
        })
      }
    }
  }
  
  /**
   * 缩放到要素
   */
  function fitToFeature(feature: Feature, padding = [50, 50, 50, 50]) {
    if (!map.value) return
    
    const extent = feature.getGeometry()?.getExtent()
    if (extent) {
      map.value.getView().fit(extent, {
        size: map.value.getSize(),
        padding
      })
    }
  }
  
  // ==================== 工具方法 ====================
  
  /**
   * 坐标转换
   */
  function transformCoord(
    coords: [number, number],
    from: string,
    to: string
  ): [number, number] {
    return transform(coords, from, to) as [number, number]
  }
  
  /**
   * 获取地图范围
   */
  function getExtent(): [number, number, number, number] | null {
    if (!map.value) return null
    return map.value.getView().calculateExtent(map.value.getSize())
  }
  
  /**
   * 获取像素坐标
   */
  function getPixelFromCoord(coords: [number, number]): [number, number] | null {
    if (!map.value) return null
    return map.value.getPixelFromCoordinate(fromLonLat(coords))
  }
  
  /**
   * 导出地图为图片
   */
  function exportImage(format = 'image/png', quality = 0.92): string {
    if (!map.value) return ''
    
    const canvas = document.createElement('canvas')
    const size = map.value.getSize()
    canvas.width = size![0]
    canvas.height = size![1]
    
    const ctx = canvas.getContext('2d')
    if (!ctx) return ''
    
    // 临时禁用动画
    const view = map.value.getView()
    const viewState = {
      center: view.getCenter(),
      resolution: view.getResolution(),
      rotation: view.getRotation()
    }
    
    map.value.renderSync()
    
    // 绘制
    map.value.getViewport().querySelector('canvas')?.render(ctx)
    
    return canvas.toDataURL(format, quality)
  }
  
  // 生命周期
  onMounted(() => {
    initMap()
  })
  
  onUnmounted(() => {
    destroyMap()
  })
  
  // 返回
  return {
    // 状态
    map,
    layers,
    isReady,
    
    // 初始化
    initMap,
    destroyMap,
    
    // 图层
    addBaseLayer,
    addVectorLayer,
    addVectorTileLayer,
    getLayer,
    removeLayer,
    setLayerVisible,
    setLayerOpacity,
    
    // 要素
    addFeature,
    removeFeature,
    clearFeatures,
    getFeatures,
    
    // 几何
    createPoint,
    createLineString,
    createPolygon,
    createCircle,
    
    // 样式
    createStyleFunction,
    
    // 交互
    addDrawInteraction,
    addSelectInteraction,
    addModifyInteraction,
    removeInteraction,
    
    // 视图
    flyTo,
    setView,
    getViewState,
    fitToLayer,
    fitToFeature,
    
    // 工具
    transformCoord,
    getExtent,
    getPixelFromCoord,
    exportImage,
    
    // 投影常量
    EPSG_4326,
    EPSG_3857,
    EPSG_4490
  }
}

// ==================== 辅助函数 ====================

function hexToRgba(hex: string, opacity: number): string {
  const r = parseInt(hex.slice(1, 3), 16)
  const g = parseInt(hex.slice(3, 5), 16)
  const b = parseInt(hex.slice(5, 7), 16)
  return `rgba(${r}, ${g}, ${b}, ${opacity})`
}
