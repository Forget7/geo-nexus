/**
 * Cesium Ion 资源管理
 * 加载地形、影像、3D模型等Ion资产
 */

import * as Cesium from 'cesium'
import type { Viewer, TerrainProvider, ImageryProvider, Cesium3DTileset } from 'cesium'

export interface IonAssetOptions {
  assetId?: number
  accessToken?: string
  terrainExaggeration?: number
}

/**
 * Cesium Ion 资产管理器
 */
export class CesiumIonManager {
  private viewer: Viewer
  
  // Ion资产ID
  static readonly ASSETS = {
    // 地形
    WORLD_TERRAIN: 1,
    WORLD_TERRAIN_WITH_HILLS: 2,
    WORLD_TERRAIN_NEXT: 3,
    OPEN_STREET_MAP: 4,
    
    // 影像
    BING_MAP_AERIAL: 5,
    BING_MAP_ROAD: 6,
    ESRI_WORLD_IMAGERY: 8,
    ESRI_WORLD_TERRAIN: 10,
    OPEN_STREET_MAP_IMAGERY: 12,
    
    // 3D模型
    CESIUM_3D_TILES: 2270307,    // Cesium 3D Tileset
    PHOTOGRAMMETRY: 43978,       // Melbourne Photogrammetry (备用)
    POINT_CLOUD: 43978,          // Point Cloud (备用)
    GML2Salesforce: 16421,       // GML/Salesforce 示例
    
    // 特定资产
    NYC_BUILDINGS: 37345,
    Melbourne_BUILDINGS: 43978,
    BRADFORD_3D: 38225,
    STRASBOURG_BUILDINGS: 38405,
    WEST_LOUSA_BUILDINGS: 43978, // 复用 Melbourne (暂无独立ID)
    
    // 道路/3D
    MONaco_BUILDINGS: 43978,     // Monaco (暂无独立ID)
    OPEN_STREET_MAP_3D: 16421,  // OSM 3D (暂无独立ID)
  }
  
  constructor(viewer: Viewer) {
    this.viewer = viewer
  }
  
  /**
   * 配置Ion访问令牌
   */
  setAccessToken(token: string) {
    Cesium.Ion.defaultAccessToken = token
  }
  
  /**
   * 加载全球地形（含山体）
   */
  async loadWorldTerrain(options: {
    exaggeration?: number
    requestVertexNormals?: boolean
  } = {}): Promise<TerrainProvider> {
    const {
      exaggeration = 1.0,
      requestVertexNormals = true
    } = options
    
    const terrainProvider = await Cesium.CesiumTerrainProvider.fromIonAssetId(
      CesiumIonManager.ASSETS.WORLD_TERRAIN_WITH_HILLS,
      {
        requestVertexNormals,
        requestMetadata: true
      }
    )
    
    this.viewer.terrainProvider = terrainProvider
    
    // 设置地形夸张
    if (exaggeration !== 1.0) {
      this.viewer.scene.globe.terrainExaggeration = exaggeration
    }
    
    return terrainProvider
  }
  
  /**
   * 加载Photogrammetry地形（高精度三维地形）
   */
  async loadPhotogrammetryTerrain(options: {
    assetId?: number
    maximumScreenSpaceError?: number
  } = {}): Promise<Cesium3DTileset> {
    const {
      assetId = CesiumIonManager.ASSETS.PHOTOGRAMMETRY,
      maximumScreenSpaceError = 4
    } = options
    
    const tileset = await Cesium.Cesium3DTileset.fromIonAssetId(assetId, {
      maximumScreenSpaceError
    })
    
    this.viewer.scene.primitives.add(tileset)
    await this.viewer.zoomTo(tileset)
    
    return tileset
  }
  
  /**
   * 加载3D城市模型
   */
  async load3DCity(
    location: 'nyc' | 'melbourne' | 'bradford' | 'strasbourg' | 'monaco' | 'west_lousa',
    options: {
      maximumScreenSpaceError?: number
      cullWithChildrenBounds?: boolean
    } = {}
  ): Promise<Cesium3DTileset> {
    const {
      maximumScreenSpaceError = 16,
      cullWithChildrenBounds = false
    } = options
    
    const assetIds: Record<string, number> = {
      nyc: CesiumIonManager.ASSETS.NYC_BUILDINGS,
      melbourne: CesiumIonManager.ASSETS.Melbourne_BUILDINGS,
      bradford: CesiumIonManager.ASSETS.BRADFORD_3D,
      strasbourg: CesiumIonManager.ASSETS.STRASBOURG_BUILDINGS,
      monaco: CesiumIonManager.ASSETS.MONaco_BUILDINGS,
      west_lousa: CesiumIonManager.ASSETS.WEST_LOUSA_BUILDINGS,
    }
    
    const tileset = await Cesium.Cesium3DTileset.fromIonAssetId(assetIds[location], {
      maximumScreenSpaceError,
      cullWithChildrenBounds
    })
    
    this.viewer.scene.primitives.add(tileset)
    
    return tileset
  }
  
  /**
   * 加载特定区域的3D模型
   */
  async loadIonAsset(
    assetId: number,
    options: {
      maximumScreenSpaceError?: number
      maximumMemoryUsage?: number
      classificationType?: Cesium.ClassificationType
    } = {}
  ): Promise<Cesium3DTileset | TerrainProvider> {
    const {
      maximumScreenSpaceError = 16,
      maximumMemoryUsage = 512,
      classificationType
    } = options
    
    try {
      // 尝试作为3D Tiles加载
      const tileset = await Cesium.Cesium3DTileset.fromIonAssetId(assetId, {
        maximumScreenSpaceError,
        maximumMemoryUsage
      })
      
      if (classificationType) {
        tileset.classificationType = classificationType
      }
      
      this.viewer.scene.primitives.add(tileset)
      
      return tileset
    } catch (e) {
      // 尝试作为地形加载
      const terrainProvider = await Cesium.CesiumTerrainProvider.fromIonAssetId(assetId)
      return terrainProvider
    }
  }
  
  /**
   * 加载自定义3D Tileset（从URL）
   */
  async load3DTilesetFromUrl(
    url: string,
    options: {
      maximumScreenSpaceError?: number
      maximumMemoryUsage?: number
      preload?: boolean
      show?: boolean
    } = {}
  ): Promise<Cesium3DTileset> {
    const {
      maximumScreenSpaceError = 16,
      maximumMemoryUsage = 512,
      preload = true,
      show = true
    } = options
    
    const tileset = new Cesium.Cesium3DTileset({
      url: url,
      maximumScreenSpaceError,
      maximumMemoryUsage,
      preload: preload,
      show: show
    })
    
    this.viewer.scene.primitives.add(tileset)
    
    return tileset
  }
  
  /**
   * 加载自定义影像
   */
  async loadImagery(
    provider: 'arcgis' | 'openstreetmap' | 'bing' | 'esri' | 'mapbox',
    options: {
      style?: string
    } = {}
  ): Promise<ImageryProvider> {
    const {
      style
    } = options
    
    let imageryProvider: ImageryProvider
    
    switch (provider) {
      case 'arcgis':
        imageryProvider = await Cesium.ArcGisMapServerImageryProvider.fromIonAssetId(
          CesiumIonManager.ASSETS.ESRI_WORLD_IMAGERY
        )
        break
        
      case 'openstreetmap':
        imageryProvider = new Cesium.OpenStreetMapImageryProvider({
          url: 'https://tile.openstreetmap.org/'
        })
        break
        
      case 'esri':
        imageryProvider = await Cesium.ArcGisMapServerImageryProvider.fromIonAssetId(
          CesiumIonManager.ASSETS.ESRI_WORLD_IMAGERY
        )
        break
        
      case 'mapbox':
        imageryProvider = new Cesium.MapboxImageryProvider({
          mapId: style || 'mapbox.streets',
          accessToken: Cesium.Ion.defaultAccessToken
        })
        break
        
      case 'bing':
      default:
        imageryProvider = this.viewer.imageryLayers.get(0)?.provider
        break
    }
    
    if (imageryProvider) {
      this.viewer.imageryLayers.addImageryProvider(imageryProvider)
    }
    
    return imageryProvider
  }
  
  /**
   * 加载点云数据
   */
  async loadPointCloud(
    assetId: number,
    options: {
      maximumScreenSpaceError?: number
      intensity?: number
      pointSize?: number
    } = {}
  ): Promise<Cesium3DTileset> {
    const {
      maximumScreenSpaceError = 16,
      intensity = 1.0,
      pointSize = 2
    } = options
    
    const tileset = await Cesium.Cesium3DTileset.fromIonAssetId(assetId, {
      maximumScreenSpaceError
    })
    
    // 点云样式
    tileset.style = new Cesium.Cesium3DTileStyle({
      pointSize: pointSize,
      color: {
        conditions: [
          ['${Classification} === " Buildings" ', 'color("#FFA500")'],
          ['${Classification} === " Vegetation" ', 'color("#00FF00")'],
          ['${Classification} === " Water" ', 'color("#0000FF")'],
          ['true', `color("${Cesium.Color.fromCssColorString('#CCCCCC').toCssColorString()}")`]
        ]
      },
      pointCloudShading: {
        attenuation: true,
        geometricErrorScale: 0.5,
        maximumAngle: Math.PI / 4,
        maximumConeAngle: Math.PI / 3,
        intensity: intensity
      }
    })
    
    this.viewer.scene.primitives.add(tileset)
    await this.viewer.zoomTo(tileset)
    
    return tileset
  }
  
  /**
   * 加载CZML数据源
   */
  async loadCZML(czml: string | object): Promise<Cesium.DataSource> {
    let dataSource: Cesium.DataSource
    
    if (typeof czml === 'string') {
      dataSource = await Cesium.CzmlDataSource.load(czml)
    } else {
      dataSource = await Cesium.CzmlDataSource.load(czml)
    }
    
    await this.viewer.dataSources.add(dataSource)
    await this.viewer.zoomTo(dataSource)
    
    return dataSource
  }
  
  /**
   * 创建地形请求选项
   */
  createTerrainProvider(options: {
    assetId?: number
    requestVertexNormals?: boolean
    requestMetadata?: boolean
  } = {}): Promise<TerrainProvider> {
    return Cesium.CesiumTerrainProvider.fromIonAssetId(
      options.assetId || CesiumIonManager.ASSETS.WORLD_TERRAIN,
      {
        requestVertexNormals: options.requestVertexNormals ?? true,
        requestMetadata: options.requestMetadata ?? true
      }
    )
  }
}

/**
 * Cesium 样式构建器
 */
export class CesiumStyleBuilder {
  private style: any = {}
  
  /**
   * 设置颜色
   */
  color(color: Cesium.Color | string): this {
    if (typeof color === 'string') {
      this.style.color = Cesium.Color.fromCssColorString(color)
    } else {
      this.style.color = color
    }
    return this
  }
  
  /**
   * 设置透明度
   */
  alpha(alpha: number): this {
    this.style.alpha = alpha
    return this
  }
  
  /**
   * 设置点大小
   */
  pointSize(size: number): this {
    this.style.pointSize = size
    return this
  }
  
  /**
   * 设置线宽
   */
  lineWidth(width: number): this {
    this.style.lineWidth = width
    return this
  }
  
  /**
   * 构建样式
   */
  build(): Cesium.Cesium3DTileStyle {
    return new Cesium.Cesium3DTileStyle(this.style)
  }
}

/**
 * 地形夸张控制
 */
export function setTerrainExaggeration(viewer: Viewer, exaggeration: number) {
  viewer.scene.globe.terrainExaggeration = exaggeration
}

/**
 * 获取地形夸张
 */
export function getTerrainExaggeration(viewer: Viewer): number {
  return viewer.scene.globe.terrainExaggeration
}

/**
 * 地形阴影
 */
export function enableTerrainShadows(viewer: Viewer, enabled: boolean) {
  viewer.scene.globe.enableLighting = enabled
  viewer.scene.globe.shadows = enabled 
    ? Cesium.ShadowMode.ENABLED 
    : Cesium.ShadowMode.DISABLED
}
