/**
 * Cesium与OpenLayers地图同步
 * 实现2D/3D无缝切换
 */

import * as Cesium from 'cesium'
import { fromLonLat, toLonLat } from 'ol/proj'
import { Map as OpenLayersMap } from 'ol'
import { View as OLView } from 'ol'
import { transform } from 'ol/proj'

export interface SyncOptions {
  syncView: boolean       // 同步视图
  syncLayers: boolean     // 同步图层
  syncSelection: boolean  // 同步选择
  twoWay: boolean         // 双向同步
}

/**
 * 地图同步器 - Cesium与OpenLayers视图同步
 */
export class MapSyncManager {
  private cesiumViewer: Cesium.Viewer | null = null
  private olMap: OpenLayersMap | null = null
  private options: SyncOptions
  private isSyncing = false
  
  constructor(options: SyncOptions = {
    syncView: true,
    syncLayers: false,
    syncSelection: true,
    twoWay: true
  }) {
    this.options = options
  }
  
  /**
   * 绑定Cesium viewer
   */
  bindCesium(viewer: Cesium.Viewer) {
    this.cesiumViewer = viewer
    
    // Cesium相机变化监听
    if (this.options.syncView) {
      viewer.camera.changed.addEventListener(() => {
        if (!this.isSyncing && this.olMap) {
          this.isSyncing = true
          this.syncOlViewFromCesium()
          this.isSyncing = false
        }
      })
      
      viewer.camera.moveStart.addEventListener(() => {
        // 开始移动
      })
      
      viewer.camera.moveEnd.addEventListener(() => {
        // 结束移动
      })
    }
  }
  
  /**
   * 绑定OpenLayers地图
   */
  bindOpenLayers(map: OpenLayersMap) {
    this.olMap = map
    
    if (this.options.syncView && this.options.twoWay) {
      map.on('moveend', () => {
        if (!this.isSyncing && this.cesiumViewer) {
          this.isSyncing = true
          this.syncCesiumViewFromOl()
          this.isSyncing = false
        }
      })
    }
  }
  
  /**
   * 从Cesium同步到OpenLayers
   */
  private syncOlViewFromOl() {
    if (!this.cesiumViewer || !this.olMap) return
    
    const camera = this.cesiumViewer.camera
    const ellipsoid = Cesium.Ellipsoid.WGS84
    
    // 获取相机位置
    const cartographic = ellipsoid.cartesianToCartographic(camera.position)
    
    // 经纬度和高度
    const lon = Cesium.Math.toDegrees(cartographic.longitude)
    const lat = Cesium.Math.toDegrees(cartographic.latitude)
    const height = cartographic.height
    
    // Cesium pitch到OpenLayers zoom的映射
    const pitch = camera.pitch
    let zoom: number
    
    if (pitch < -60) {
      // 接近顶视图
      zoom = Math.max(0, 20 - height / 1000)
    } else if (pitch < -20) {
      // 斜视图
      zoom = Math.max(0, 18 - height / 2000)
    } else {
      // 侧视图
      zoom = Math.max(0, 15 - height / 3000)
    }
    
    // 获取heading
    const heading = Cesium.Math.toDegrees(camera.heading)
    
    // 应用到OpenLayers
    const view = this.olMap.getView()
    view.setCenter(fromLonLat([lon, lat]))
    view.setZoom(zoom)
    
    if (heading !== 0) {
      view.setRotation(-heading * Math.PI / 180)
    }
  }
  
  /**
   * 从OpenLayers同步到Cesium
   */
  private syncCesiumViewFromOl() {
    if (!this.cesiumViewer || !this.olMap) return
    
    const view = this.olMap.getView()
    const center = view.getCenter()
    const zoom = view.getZoom()
    const rotation = view.getRotation()
    
    if (!center || zoom === undefined) return
    
    // 转换坐标
    const lonLat = toLonLat(center)
    const lon = lonLat[0]
    const lat = lonLat[1]
    
    // 计算高度
    const height = Math.max(100, (20 - zoom) * 1500)
    
    // 飞行到位置
    this.cesiumViewer.camera.flyTo({
      destination: Cesium.Cartesian3.fromDegrees(lon, lat, height),
      orientation: {
        heading: rotation ? -rotation : 0,
        pitch: Cesium.Math.toRadians(-45),
        roll: 0
      },
      duration: 0
    })
  }
  
  /**
   * 同步位置（不带动画）
   */
  syncPosition() {
    if (!this.isSyncing) {
      this.isSyncing = true
      this.syncOlViewFromCesium()
      this.isSyncing = false
    }
  }
  
  /**
   * 销毁同步器
   */
  destroy() {
    this.cesiumViewer = null
    this.olMap = null
  }
}

/**
 * 统一的地图状态管理
 */
export class UnifiedMapState {
  // 当前模式
  private mode: '2d' | '3d' = '2d'
  
  // 视图状态
  private viewState: {
    center: [number, number]
    zoom: number
    pitch: number
    heading: number
  } = {
    center: [116.4, 39.9],
    zoom: 10,
    pitch: 0,
    heading: 0
  }
  
  // 同步管理器
  private syncManager: MapSyncManager | null = null
  
  // 图层
  private layerConfigs: Map<string, any> = new Map()
  
  // 选择集
  private selectedFeatures: Set<string> = new Set()
  
  /**
   * 切换到3D模式
   */
  switchTo3D(cesiumViewer: Cesium.Viewer) {
    this.mode = '3d'
    
    if (!this.syncManager) {
      this.syncManager = new MapSyncManager({ twoWay: true })
    }
    
    this.syncManager.bindCesium(cesiumViewer)
    
    // 应用视图状态
    cesiumViewer.camera.flyTo({
      destination: Cesium.Cartesian3.fromDegrees(
        this.viewState.center[0],
        this.viewState.center[1],
        (20 - this.viewState.zoom) * 1500
      ),
      orientation: {
        heading: Cesium.Math.toRadians(this.viewState.heading),
        pitch: Cesium.Math.toRadians(this.viewState.pitch),
        roll: 0
      }
    })
    
    // 应用图层
    this.applyLayersToCesium(cesiumViewer)
  }
  
  /**
   * 切换到2D模式
   */
  switchTo2D(olMap: OpenLayersMap) {
    this.mode = '2d'
    
    if (this.syncManager) {
      this.syncManager.bindOpenLayers(olMap)
    }
    
    // 应用视图状态
    const view = olMap.getView()
    view.setCenter(fromLonLat(this.viewState.center))
    view.setZoom(this.viewState.zoom)
    
    // 应用图层
    this.applyLayersToOpenLayers(olMap)
  }
  
  /**
   * 获取当前模式
   */
  getMode(): '2d' | '3d' {
    return this.mode
  }
  
  /**
   * 更新视图状态
   */
  updateViewState(state: Partial<typeof this.viewState>) {
    this.viewState = { ...this.viewState, ...state }
    
    // 同步到另一个地图
    if (this.syncManager) {
      this.syncManager.syncPosition()
    }
  }
  
  /**
   * 添加图层配置
   */
  addLayer(id: string, config: any) {
    this.layerConfigs.set(id, config)
  }
  
  /**
   * 移除图层
   */
  removeLayer(id: string) {
    this.layerConfigs.delete(id)
  }
  
  /**
   * 应用图层到Cesium
   */
  private applyLayersToCesium(viewer: Cesium.Viewer) {
    // 清除现有实体
    viewer.entities.removeAll()
    
    // 应用图层配置
    this.layerConfigs.forEach((config, id) => {
      if (config.type === 'geojson' && config.data) {
        viewer.entities.add({
          id,
          ...config.data
        })
      }
    })
  }
  
  /**
   * 应用图层到OpenLayers
   */
  private applyLayersToOpenLayers(olMap: OpenLayersMap) {
    // 实现图层应用逻辑
  }
  
  /**
   * 选择要素
   */
  selectFeature(id: string) {
    this.selectedFeatures.add(id)
    // 同步选择状态
  }
  
  /**
   * 取消选择
   */
  deselectFeature(id: string) {
    this.selectedFeatures.delete(id)
    // 同步选择状态
  }
  
  /**
   * 清除选择
   */
  clearSelection() {
    this.selectedFeatures.clear()
  }
  
  /**
   * 获取视图状态
   */
  getViewState() {
    return { ...this.viewState }
  }
}

/**
 * 坐标转换工具
 */
export class CoordinateConverter {
  // Cesium Cartesian3 -> [lon, lat, height]
  static cesiumToLonLatHeight(cartesian: Cesium.Cartesian3): [number, number, number] {
    const cartographic = Cesium.Cartographic.fromCartesian(cartesian)
    return [
      Cesium.Math.toDegrees(cartographic.longitude),
      Cesium.Math.toDegrees(cartographic.latitude),
      cartographic.height
    ]
  }
  
  // [lon, lat] -> Cesium Cartesian3
  static lonLatToCesium(lon: number, lat: number, height = 0): Cesium.Cartesian3 {
    return Cesium.Cartesian3.fromDegrees(lon, lat, height)
  }
  
  // OpenLayers -> [lon, lat]
  static olToLonLat(olCoords: [number, number]): [number, number] {
    return toLonLat(olCoords) as [number, number]
  }
  
  // [lon, lat] -> OpenLayers
  static lonLatToOl(coords: [number, number]): [number, number] {
    return fromLonLat(coords)
  }
  
  // OpenLayers extent -> Cesium rectangle
  static olExtentToCesiumRectangle(
    extent: [number, number, number, number]
  ): Cesium.Rectangle {
    return Cesium.Rectangle.fromDegrees(
      extent[0], extent[1],
      extent[2], extent[3]
    )
  }
  
  // Cesium rectangle -> OpenLayers extent
  static cesiumRectangleToOlExtent(
    rectangle: Cesium.Rectangle
  ): [number, number, number, number] {
    return [
      Cesium.Math.toDegrees(rectangle.west),
      Cesium.Math.toDegrees(rectangle.south),
      Cesium.Math.toDegrees(rectangle.east),
      Cesium.Math.toDegrees(rectangle.north)
    ]
  }
}

/**
 * 要素格式转换
 */
export class FeatureConverter {
  /**
   * OpenLayers Feature -> Cesium Entity
   */
  static olFeatureToCesiumEntity(
    olFeature: any,
    options: {
      id?: string
      name?: string
      style?: any
    } = {}
  ): Cesium.Entity {
    const geometry = olFeature.getGeometry()
    const type = geometry.getType()
    const coords = geometry.getCoordinates()
    
    let cesiumGeometry: any
    
    switch (type) {
      case 'Point':
        cesiumGeometry = Cesium.Cartesian3.fromDegrees(coords[0], coords[1])
        break
        
      case 'LineString':
        cesiumGeometry = Cesium.Cartesian3.fromDegreesArray(
          coords.flat(Infinity)
        )
        break
        
      case 'Polygon':
        cesiumGeometry = new Cesium.PolygonHierarchy(
          Cesium.Cartesian3.fromDegreesArrayHeights(
            coords[0].flat(Infinity),
            0
          )
        )
        break
        
      default:
        cesiumGeometry = Cesium.Cartesian3.fromDegrees(coords[0], coords[1])
    }
    
    return new Cesium.Entity({
      id: options.id || olFeature.getId(),
      name: options.name || olFeature.get('name'),
      geometry: type === 'Polygon' ? undefined : undefined,
      position: type === 'Point' ? cesiumGeometry : undefined,
      polyline: type === 'LineString' ? {
        positions: cesiumGeometry,
        material: Cesium.Color.RED,
        width: 2
      } : undefined,
      polygon: type === 'Polygon' ? {
        hierarchy: cesiumGeometry,
        material: Cesium.Color.RED.withAlpha(0.5)
      } : undefined,
      point: type === 'Point' ? {
        color: Cesium.Color.RED,
        pixelSize: 10
      } : undefined
    })
  }
  
  /**
   * Cesium Entity -> OpenLayers Feature
   */
  static cesiumEntityToOlFeature(entity: Cesium.Entity): any {
    // 获取位置
    const position = entity.position?.getValue(Cesium.JulianDate.now())
    if (!position) return null
    
    const cartographic = Cesium.Ellipsoid.WGS84.cartesianToCartographic(position)
    const coords: [number, number] = [
      Cesium.Math.toDegrees(cartographic.longitude),
      Cesium.Math.toDegrees(cartographic.latitude)
    ]
    
    const feature = new (require('ol/Feature'))({
      geometry: new (require('ol/geom/Point'))(coords)
    })
    
    feature.setId(entity.id)
    feature.set('name', entity.name)
    
    return feature
  }
  
  /**
   * GeoJSON -> Cesium Entity
   */
  static geoJsonToCesiumEntity(geojson: any, options: {
    id?: string
    style?: any
  } = {}): Cesium.Entity[] {
    const entities: Cesium.Entity[] = []
    
    if (geojson.type === 'FeatureCollection') {
      geojson.features.forEach((feature: any, index: number) => {
        const entity = this.geoJsonFeatureToCesiumEntity(
          feature,
          `${options.id || 'feature'}_${index}`
        )
        if (entity) entities.push(entity)
      })
    } else if (geojson.type === 'Feature') {
      const entity = this.geoJsonFeatureToCesiumEntity(geojson, options.id)
      if (entity) entities.push(entity)
    }
    
    return entities
  }
  
  /**
   * GeoJSON Feature -> Cesium Entity
   */
  static geoJsonFeatureToCesiumEntity(feature: any, id?: string): Cesium.Entity | null {
    const geometry = feature.geometry
    if (!geometry) return null
    
    const properties = feature.properties || {}
    const name = properties.name || id
    
    let entityOptions: any = {
      id: id || feature.id,
      name
    }
    
    switch (geometry.type) {
      case 'Point':
        const [lon, lat] = geometry.coordinates
        entityOptions.position = Cesium.Cartesian3.fromDegrees(lon, lat)
        entityOptions.point = {
          color: Cesium.Color.RED,
          pixelSize: 10
        }
        break
        
      case 'LineString':
        entityOptions.polyline = {
          positions: Cesium.Cartesian3.fromDegreesArray(geometry.coordinates.flat()),
          material: Cesium.Color.CYAN,
          width: 2
        }
        break
        
      case 'Polygon':
        entityOptions.polygon = {
          hierarchy: Cesium.Cartesian3.fromDegreesArrayHeights(
            geometry.coordinates[0].flat(),
            0
          ),
          material: Cesium.Color.BLUE.withAlpha(0.5)
        }
        break
        
      default:
        return null
    }
    
    return new Cesium.Entity(entityOptions)
  }
}

/**
 * 样式转换
 */
export class StyleConverter {
  /**
   * OpenLayers样式 -> Cesium样式
   */
  static olStyleToCesium(olStyle: any): any {
    // 简化实现
    return {
      color: Cesium.Color.RED
    }
  }
  
  /**
   * Cesium样式 -> OpenLayers样式
   */
  static cesiumStyleToOl(cesiumStyle: any): any {
    // 简化实现
    return {
      fill: {
        color: 'rgba(255, 0, 0, 0.5)'
      }
    }
  }
}
