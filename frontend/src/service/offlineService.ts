/**
 * offlineService - 离线瓦片管理服务
 *
 * 使用 IndexedDB 存储瓦片数据，Service Worker 拦截瓦片请求
 *
 * 使用方式：
 * const offline = useOfflineService()
 * await offline.downloadRegion({ minLon, minLat, maxLon, maxLat }, [10, 11, 12, 13])
 */

import { ref } from 'vue'

export interface OfflineRegion {
  id: string
  name: string
  minLon: number
  minLat: number
  maxLon: number
  maxLat: number
  zoomLevels: number[]
  tileCount: number
  downloadedTiles: number
  estimatedSize: string
  status: 'pending' | 'downloading' | 'completed' | 'error'
  createdAt: Date
  completedAt?: Date
}

export interface TileCoord {
  x: number
  y: number
  z: number
}

/**
 * IndexedDB 瓦片存储
 */
const DB_NAME = 'geonexus-offline'
const DB_VERSION = 1
const STORE_NAME = 'tiles'

class TileDB {
  private db: IDBDatabase | null = null

  async open(): Promise<IDBDatabase> {
    if (this.db) return this.db

    return new Promise((resolve, reject) => {
      const request = indexedDB.open(DB_NAME, DB_VERSION)

      request.onerror = () => reject(request.error)

      request.onsuccess = () => {
        this.db = request.result
        resolve(this.db)
      }

      request.onupgradeneeded = (event) => {
        const db = (event.target as IDBOpenDBRequest).result
        if (!db.objectStoreNames.contains(STORE_NAME)) {
          const store = db.createObjectStore(STORE_NAME, { keyPath: 'key' })
          store.createIndex('region', 'regionId', { unique: false })
        }
      }
    })
  }

  async put(key: string, blob: Blob, regionId: string) {
    const db = await this.open()
    return new Promise((resolve, reject) => {
      const tx = db.transaction(STORE_NAME, 'readwrite')
      const store = tx.objectStore(STORE_NAME)
      const request = store.put({ key, blob, regionId, timestamp: Date.now() })
      request.onsuccess = () => resolve(undefined)
      request.onerror = () => reject(request.error)
    })
  }

  async get(key: string): Promise<Blob | null> {
    const db = await this.open()
    return new Promise((resolve, reject) => {
      const tx = db.transaction(STORE_NAME, 'readonly')
      const store = tx.objectStore(STORE_NAME)
      const request = store.get(key)
      request.onsuccess = () => resolve(request.result?.blob ?? null)
      request.onerror = () => reject(request.error)
    })
  }

  async getAll(regionId: string): Promise<Array<{ key: string; blob: Blob }>> {
    const db = await this.open()
    return new Promise((resolve, reject) => {
      const tx = db.transaction(STORE_NAME, 'readonly')
      const store = tx.objectStore(STORE_NAME)
      const index = store.index('region')
      const request = index.getAll(IDBKeyRange.only(regionId))
      request.onsuccess = () => resolve(request.result.map(r => ({ key: r.key, blob: r.blob })))
      request.onerror = () => reject(request.error)
    })
  }

  async delete(regionId: string) {
    const db = await this.open()
    const tx = db.transaction(STORE_NAME, 'readwrite')
    const store = tx.objectStore(STORE_NAME)
    const index = store.index('region')
    const keys = await new Promise<any[]>((resolve, reject) => {
      const req = index.getAllKeys(IDBKeyRange.only(regionId))
      req.onsuccess = () => resolve(req.result)
      req.onerror = () => reject(req.error)
    })
    for (const key of keys) {
      store.delete(key)
    }
  }

  async getTileCount(regionId: string): Promise<number> {
    const db = await this.open()
    return new Promise((resolve, reject) => {
      const tx = db.transaction(STORE_NAME, 'readonly')
      const store = tx.objectStore(STORE_NAME)
      const index = store.index('region')
      const request = index.count(IDBKeyRange.only(regionId))
      request.onsuccess = () => resolve(request.result)
      request.onerror = () => reject(request.error)
    })
  }
}

const tileDB = new TileDB()

/**
 * 计算瓦片坐标范围
 */
function getTileRange(minLon: number, minLat: number, maxLon: number, maxLat: number, zoom: number): { minX: number; maxX: number; minY: number; maxY: number } {
  const n = Math.pow(2, zoom)
  const minX = Math.floor(((minLon + 180) / 360) * n)
  const maxX = Math.floor(((maxLon + 180) / 360) * n)
  const minY = Math.floor((1 - Math.log(Math.tan(maxLat * Math.PI / 180) + 1 / Math.cos(maxLat * Math.PI / 180)) / Math.PI) / 2 * n)
  const maxY = Math.floor((1 - Math.log(Math.tan(minLat * Math.PI / 180) + 1 / Math.cos(minLat * Math.PI / 180)) / Math.PI) / 2 * n)
  return { minX, maxX: Math.min(maxX, n - 1), minY, maxY: Math.min(maxY, n - 1) }
}

/**
 * 生成瓦片 URL
 */
function getTileUrl(source: string, x: number, y: number, z: number): string {
  switch (source) {
    case 'osm':
      return `https://tile.openstreetmap.org/${z}/${x}/${y}.png`
    case 'satellite':
      return `https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/${z}/${y}/${x}`
    default:
      return `https://tile.openstreetmap.org/${z}/${x}/${y}.png`
  }
}

/**
 * useOfflineService - 离线瓦片服务
 */
export function useOfflineService() {
  const regions = ref<OfflineRegion[]>([])
  const downloading = ref(false)
  const progress = ref(0)

  /**
   * 估算瓦片数量和大小
   */
  async function estimate(minLon: number, minLat: number, maxLon: number, maxLat: number, zoomLevels: number[]): Promise<{ tileCount: number; estimatedSize: string }> {
    let total = 0
    for (const z of zoomLevels) {
      const range = getTileRange(minLon, minLat, maxLon, maxLat, z)
      total += (range.maxX - range.minX + 1) * (range.maxY - range.minY + 1)
    }
    const estimatedBytes = total * 15 * 1024 // 假设平均 15KB 每瓦片
    const sizeStr = estimatedBytes < 1024 * 1024
      ? `${(estimatedBytes / 1024).toFixed(1)} KB`
      : `${(estimatedBytes / (1024 * 1024)).toFixed(1)} MB`
    return { tileCount: total, estimatedSize: sizeStr }
  }

  /**
   * 下载离线瓦片区域
   */
  async function downloadRegion(
    name: string,
    minLon: number, minLat: number, maxLon: number, maxLat: number,
    zoomLevels: number[],
    tileSource: string = 'osm',
    onProgress?: (downloaded: number, total: number) => void
  ): Promise<string> {
    const regionId = crypto.randomUUID()
    const { tileCount } = await estimate(minLon, minLat, maxLon, maxLat, zoomLevels)

    const region: OfflineRegion = {
      id: regionId,
      name,
      minLon, minLat, maxLon, maxLat,
      zoomLevels,
      tileCount,
      downloadedTiles: 0,
      estimatedSize: '',
      status: 'pending',
      createdAt: new Date()
    }

    regions.value.push(region)
    downloading.value = true
    region.status = 'downloading'

    // 计算所有需要下载的瓦片
    const tiles: TileCoord[] = []
    for (const z of zoomLevels) {
      const range = getTileRange(minLon, minLat, maxLon, maxLat, z)
      for (let x = range.minX; x <= range.maxX; x++) {
        for (let y = range.minY; y <= range.maxY; y++) {
          tiles.push({ x, y, z })
        }
      }
    }

    // 批量下载（限制并发数）
    const CONCURRENCY = 5
    let completed = 0
    const errors: Error[] = []

    async function downloadTile(tile: TileCoord): Promise<void> {
      const url = getTileUrl(tileSource, tile.x, tile.y, tile.z)
      const key = `${regionId}/${tile.z}/${tile.x}/${tile.y}`

      try {
        // 优先从缓存读取
        const cached = await tileDB.get(key)
        if (cached) {
          completed++
          region.downloadedTiles = completed
          progress.value = completed / tiles.length
          onProgress?.(completed, tiles.length)
          return
        }

        const response = await fetch(url)
        if (response.ok) {
          const blob = await response.blob()
          await tileDB.put(key, blob, regionId)
        }
      } catch (e) {
        // 瓦片下载失败不影响整体
        errors.push(e as Error)
      } finally {
        completed++
        region.downloadedTiles = completed
        progress.value = completed / tiles.length
        onProgress?.(completed, tiles.length)
      }
    }

    // 并发下载
    const batches: TileCoord[][] = []
    for (let i = 0; i < tiles.length; i += CONCURRENCY) {
      batches.push(tiles.slice(i, i + CONCURRENCY))
    }

    for (const batch of batches) {
      await Promise.all(batch.map(downloadTile))
    }

    downloading.value = false
    region.status = errors.length > tiles.length * 0.1 ? 'error' : 'completed'
    region.completedAt = new Date()

    if (errors.length > 0) {
      console.warn(`[Offline] ${errors.length} tiles failed to download`)
    }

    return regionId
  }

  /**
   * 删除离线区域
   */
  async function deleteRegion(regionId: string) {
    await tileDB.delete(regionId)
    regions.value = regions.value.filter(r => r.id !== regionId)
  }

  /**
   * 获取区域瓦片 URL（优先从 IndexedDB 返回）
   */
  async function getTileUrlForRegion(regionId: string, x: number, y: number, z: number): Promise<string | null> {
    const key = `${regionId}/${z}/${x}/${y}`
    const blob = await tileDB.get(key)
    if (blob) {
      return URL.createObjectURL(blob)
    }
    return null
  }

  /**
   * 加载已保存的区域列表
   */
  async function loadRegions() {
    // 从 localStorage 加载区域元数据
    const saved = localStorage.getItem('geonexus-offline-regions')
    if (saved) {
      regions.value = JSON.parse(saved)
    }
  }

  /**
   * 保存区域列表
   */
  function saveRegions() {
    localStorage.setItem('geonexus-offline-regions', JSON.stringify(regions.value))
  }

  return {
    regions,
    downloading,
    progress,
    estimate,
    downloadRegion,
    deleteRegion,
    getTileUrlForRegion,
    loadRegions,
    saveRegions
  }
}
