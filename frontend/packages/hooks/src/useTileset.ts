/**
 * useTileset - 3D Tileset 加载 Hook
 *
 * 管理 Cesium 3D Tileset 的加载、卸载、样式切换
 *
 * 使用方式：
 * const { load, unload, list, setStyle, ready } = useTileset(viewer)
 *
 * await load({ name: '北京', type: 'CESIUM_ION', ionAssetId: 43978 })
 */

import { ref, shallowRef } from 'vue'
import type { Viewer, Cesium3DTileset, Cesium3DTilesetStyle } from 'cesium'
import { useApi } from './useApi'

export interface TilesetInfo {
  id: string
  name: string
  url?: string
  ionAssetId?: number
  type: 'CESIUM_ION' | 'SELF_HOSTED' | 'MINIO'
  position?: { longitude: number; latitude: number; height: number }
  orientation?: { heading: number; pitch: number; roll: number }
  maxScreenSpaceError?: number
  status?: 'loading' | 'ready' | 'error'
}

export interface LoadTilesetOptions {
  /** Cesium ion asset ID（type=CESIUM_ION 时必需）*/
  ionAssetId?: number
  /** Tileset URL（type=SELF_HOSTED/MINIO 时必需） */
  url?: string
  /** Cesium ion token */
  accessToken?: string
  /** 位置 */
  position?: { longitude: number; latitude: number; height?: number }
  /** 朝向 */
  orientation?: { heading?: number; pitch?: number; roll?: number }
  /** 最大屏幕空间误差，默认 16 */
  maxScreenSpaceError?: number
  /** 样式 */
  style?: Cesium3DTilesetStyle
  /** Cesium ion server URL */
  ionServerUrl?: string
}

export function useTileset(viewer: Viewer) {
  const { get, post, del } = useApi()
  const loadedTilesets = shallowRef<Map<string, Cesium3DTileset>>(new Map())
  const loading = ref(false)
  const ready = ref(false)

  /**
   * 从后端加载 tileset 列表
   */
  async function listTilesets(): Promise<TilesetInfo[]> {
    return get<TilesetInfo[]>('/api/v1/tilesets')
  }

  /**
   * 注册 tileset 到后端
   */
  async function registerTileset(info: Omit<TilesetInfo, 'id' | 'status'>): Promise<TilesetInfo> {
    return post<TilesetInfo>('/api/v1/tilesets', info)
  }

  /**
   * 加载 3D Tileset
   */
  async function load(id: string, options: LoadTilesetOptions = {}): Promise<Cesium3DTileset | null> {
    try {
      loading.value = true

      // 获取 tileset 配置
      const config = await get<any>(`/api/v1/tilesets/${id}/config`)

      let tileset: Cesium3DTileset

      if (config.ionAssetId && config.ionAssetId !== 0) {
        // Cesium ion 方式
        tileset = await Cesium3DTileset.fromIon(config.ionAssetId)
      } else if (config.url) {
        // URL 方式
        tileset = await Cesium3DTileset.fromUrl(config.url)
      } else {
        throw new Error('Tileset 配置无效：无 URL 或 ionAssetId')
      }

      // 配置参数
      tileset.maximumScreenSpaceError = options.maxScreenSpaceError ?? config.maxScreenSpaceError ?? 16

      // 位置变换
      if (config.position) {
        const cartographic = Cesium.Cartographic.fromDegrees(
          config.position.longitude,
          config.position.latitude,
          config.position.height ?? 0
        )
        tileset.modelMatrix = Cesium.Transforms.headingPitchRollToFixedFrame(
          Cesium.Cartesian3.fromRadians(cartographic.longitude, cartographic.latitude, cartographic.height),
          new Cesium.HeadingPitchRoll(
            Cesium.Math.toRadians(config.orientation?.heading ?? 0),
            Cesium.Math.toRadians(config.orientation?.pitch ?? 0),
            Cesium.Math.toRadians(config.orientation?.roll ?? 0)
          )
        )
      }

      // 添加到场景
      viewer.scene.primitives.add(tileset)
      loadedTilesets.value.set(id, tileset)

      // 监听加载完成
      tileset.readyPromise.then(() => {
        ready.value = true
        console.info(`[Tileset] 加载完成: ${config.name}, ${tileset.totalTiles} tiles`)
        viewer.zoomTo(tileset)
      })

      return tileset
    } catch (e) {
      console.error('[Tileset] 加载失败:', e)
      return null
    } finally {
      loading.value = false
    }
  }

  /**
   * 卸载 tileset
   */
  function unload(id: string) {
    const tileset = loadedTilesets.value.get(id)
    if (tileset) {
      viewer.scene.primitives.remove(tileset)
      tileset.destroy()
      loadedTilesets.value.delete(id)
    }
  }

  /**
   * 卸载所有 tileset
   */
  function unloadAll() {
    loadedTilesets.value.forEach((tileset, id) => {
      viewer.scene.primitives.remove(tileset)
      tileset.destroy()
    })
    loadedTilesets.value.clear()
  }

  /**
   * 设置 tileset 样式
   */
  async function setStyle(id: string, style: Cesium3DTilesetStyle) {
    const tileset = loadedTilesets.value.get(id)
    if (tileset) {
      tileset.style = style
    }
  }

  /**
   * 获取 tileset 数量
   */
  function getCount() {
    return loadedTilesets.value.size
  }

  return {
    loadedTilesets,
    loading,
    ready,
    load,
    unload,
    unloadAll,
    setStyle,
    listTilesets,
    registerTileset,
    getCount
  }
}
