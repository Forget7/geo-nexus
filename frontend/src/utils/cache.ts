/**
 * 请求缓存与去重工具
 */

interface CacheEntry<T> {
  data: T
  timestamp: number
  ttl: number
}

interface PendingRequest {
  promise: Promise<unknown>
  timestamp: number
}

/**
 * 请求缓存管理器
 */
export class RequestCache {
  private cache = new Map<string, CacheEntry<unknown>>()
  private pending = new Map<string, PendingRequest>()
  
  // 默认TTL: 5分钟
  private defaultTtl = 5 * 60 * 1000
  
  /**
   * 生成缓存key
   */
  private generateKey(url: string, params?: Record<string, unknown>): string {
    if (!params) return url
    return `${url}:${JSON.stringify(params)}`
  }
  
  /**
   * 获取缓存
   */
  get<T>(url: string, params?: Record<string, unknown>): T | null {
    const key = this.generateKey(url, params)
    const entry = this.cache.get(key)
    
    if (!entry) return null
    
    // 检查过期
    if (Date.now() - entry.timestamp > entry.ttl) {
      this.cache.delete(key)
      return null
    }
    
    return entry.data as T
  }
  
  /**
   * 设置缓存
   */
  set<T>(url: string, data: T, ttl?: number): void {
    const key = this.generateKey(url)
    this.cache.set(key, {
      data,
      timestamp: Date.now(),
      ttl: ttl || this.defaultTtl
    })
  }
  
  /**
   * 删除缓存
   */
  delete(url: string, params?: Record<string, unknown>): boolean {
    const key = this.generateKey(url, params)
    return this.cache.delete(key)
  }
  
  /**
   * 清除所有缓存
   */
  clear(): void {
    this.cache.clear()
  }
  
  /**
   * 清除过期缓存
   */
  cleanExpired(): void {
    const now = Date.now()
    for (const [key, entry] of this.cache.entries()) {
      if (now - entry.timestamp > entry.ttl) {
        this.cache.delete(key)
      }
    }
  }
  
  /**
   * 获取带去重的请求
   * 如果有相同请求正在进行，返回那个Promise
   */
  getOrSet<T>(url: string, params: Record<string, unknown>, factory: () => Promise<T>, ttl?: number): Promise<T> {
    const key = this.generateKey(url, params)
    
    // 检查是否有正在进行的请求
    const pending = this.pending.get(key)
    if (pending) {
      console.debug(`[Cache] Deduplicating request: ${key}`)
      return pending.promise
    }
    
    // 检查缓存
    const cached = this.get<T>(url, params)
    if (cached !== null) {
      console.debug(`[Cache] Cache hit: ${key}`)
      return Promise.resolve(cached)
    }
    
    // 创建新请求
    console.debug(`[Cache] New request: ${key}`)
    const promise = factory().then(data => {
      this.set(url, data, ttl)
      this.pending.delete(key)
      return data
    }).catch(error => {
      this.pending.delete(key)
      throw error
    })
    
    this.pending.set(key, {
      promise,
      timestamp: Date.now()
    })
    
    return promise
  }
  
  /**
   * 获取缓存统计
   */
  getStats() {
    return {
      size: this.cache.size,
      pending: this.pending.size,
      entries: Array.from(this.cache.keys())
    }
  }
}

// 全局缓存实例
export const requestCache = new RequestCache()

// 定期清理过期缓存
setInterval(() => {
  requestCache.cleanExpired()
}, 60000) // 每分钟
