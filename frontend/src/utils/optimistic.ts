/**
 * 乐观更新工具 - 数据变更时立即UI响应，后台同步
 */

import { ref, shallowRef } from 'vue'

export interface OptimisticState<T> {
  data: T | null
  loading: boolean
  error: Error | null
  isOptimistic: boolean
}

export interface OptimisticUpdate<T> {
  id: string
  type: 'create' | 'update' | 'delete'
  data: T
  previousData?: T
  timestamp: number
  status: 'pending' | 'confirmed' | 'reverted'
}

/**
 * 乐观更新管理器
 */
export class OptimisticUpdateManager {
  private pendingUpdates = new Map<string, OptimisticUpdate<any>>()
  private updateSubscribers = new Set<(update: OptimisticUpdate<any>) => void>()
  private maxPending = 100
  
  /**
   * 添加乐观更新
   */
  add<T>(id: string, type: 'create' | 'update' | 'delete', data: T, previousData?: T): OptimisticUpdate<T> {
    // 如果已有相同ID的更新，先移除
    if (this.pendingUpdates.has(id)) {
      this.revert(id)
    }
    
    // 限制待处理更新数量
    if (this.pendingUpdates.size >= this.maxPending) {
      const oldestKey = this.pendingUpdates.keys().next().value
      if (oldestKey) this.revert(oldestKey)
    }
    
    const update: OptimisticUpdate<T> = {
      id,
      type,
      data,
      previousData,
      timestamp: Date.now(),
      status: 'pending'
    }
    
    this.pendingUpdates.set(id, update)
    this.notifySubscribers(update)
    
    return update
  }
  
  /**
   * 确认更新（服务器成功）
   */
  confirm<T>(id: string): boolean {
    const update = this.pendingUpdates.get(id) as OptimisticUpdate<T> | undefined
    if (!update) return false
    
    update.status = 'confirmed'
    this.pendingUpdates.delete(id)
    this.notifySubscribers(update)
    
    return true
  }
  
  /**
   * 回滚更新（服务器失败）
   */
  revert<T>(id: string): T | null {
    const update = this.pendingUpdates.get(id) as OptimisticUpdate<T> | undefined
    if (!update) return null
    
    update.status = 'reverted'
    this.pendingUpdates.delete(id)
    this.notifySubscribers(update)
    
    return update.previousData ?? null
  }
  
  /**
   * 获取待处理更新
   */
  getPending<T>(id: string): OptimisticUpdate<T> | undefined {
    return this.pendingUpdates.get(id) as OptimisticUpdate<T> | undefined
  }
  
  /**
   * 获取所有待处理更新
   */
  getAllPending<T>(): OptimisticUpdate<T>[] {
    return Array.from(this.pendingUpdates.values()) as OptimisticUpdate<T>[]
  }
  
  /**
   * 获取指定类型的待处理更新
   */
  getPendingByType<T>(type: 'create' | 'update' | 'delete'): OptimisticUpdate<T>[] {
    return this.getAllPending<T>().filter(u => u.type === type)
  }
  
  /**
   * 是否有待处理的更新
   */
  hasPending(id?: string): boolean {
    if (id) {
      return this.pendingUpdates.has(id)
    }
    return this.pendingUpdates.size > 0
  }
  
  /**
   * 清空所有待处理更新
   */
  clear(): void {
    this.pendingUpdates.clear()
  }
  
  /**
   * 订阅更新
   */
  subscribe(callback: (update: OptimisticUpdate<any>) => void): () => void {
    this.updateSubscribers.add(callback)
    return () => this.updateSubscribers.delete(callback)
  }
  
  private notifySubscribers(update: OptimisticUpdate<any>): void {
    this.updateSubscribers.forEach(cb => {
      try {
        cb(update)
      } catch (e) {
        console.error('Error in optimistic update subscriber:', e)
      }
    })
  }
}

// 全局管理器
export const optimisticManager = new OptimisticUpdateManager()

/**
 * useOptimistic - Vue Composable for optimistic updates
 */
export function useOptimistic<T>(options: {
  fetchData: () => Promise<T>
  onError?: (error: Error) => void
} = { fetchData: async () => { throw new Error('fetchData not configured') } }) {
  const state = shallowRef<OptimisticState<T>>({
    data: null,
    loading: false,
    error: null,
    isOptimistic: false
  })
  
  const pendingUpdates = shallowRef<Map<string, OptimisticUpdate<T>>>(new Map())
  
  // 监听更新
  optimisticManager.subscribe((update) => {
    if (update.status === 'pending') {
      pendingUpdates.value = new Map(pendingUpdates.value).set(update.id, update as any)
    } else {
      const newMap = new Map(pendingUpdates.value)
      newMap.delete(update.id)
      pendingUpdates.value = newMap
    }
  })
  
  /**
   * 加载数据
   */
  async function load() {
    state.value = { ...state.value, loading: true, error: null }
    
    try {
      const data = await options.fetchData()
      state.value = { data, loading: false, error: null, isOptimistic: false }
      return data
    } catch (error) {
      state.value = { ...state.value, loading: false, error: error as Error }
      options.onError?.(error as Error)
      throw error
    }
  }
  
  /**
   * 乐观创建
   */
  async function optimisticCreate(
    id: string,
    tempData: T,
    serverCreate: () => Promise<T>
  ): Promise<T> {
    // 先添加乐观更新
    optimisticManager.add(id, 'create', tempData)
    
    // 立即更新本地状态
    state.value = {
      data: Array.isArray(state.value.data) 
        ? [...(state.value.data as T[]), tempData]
        : tempData,
      loading: false,
      error: null,
      isOptimistic: true
    }
    
    try {
      // 等待服务器响应
      const result = await serverCreate()
      
      // 确认更新
      optimisticManager.confirm(id)
      
      // 更新本地数据
      if (Array.isArray(state.value.data)) {
        state.value = {
          data: (state.value.data as T[]).map(item => 
            (item as any)?.id === id ? result : item
          ),
          loading: false,
          error: null,
          isOptimistic: false
        }
      }
      
      return result
    } catch (error) {
      // 回滚
      const previousData = optimisticManager.revert(id)
      
      // 恢复本地状态
      if (previousData !== null && Array.isArray(state.value.data)) {
        state.value = {
          data: (state.value.data as T[]).filter(item => (item as any)?.id !== id),
          loading: false,
          error: error as Error,
          isOptimistic: false
        }
      }
      
      options.onError?.(error as Error)
      throw error
    }
  }
  
  /**
   * 乐观更新
   */
  async function optimisticUpdate(
    id: string,
    updates: Partial<T>,
    currentData: T,
    serverUpdate: () => Promise<T>
  ): Promise<T> {
    // 先添加乐观更新
    optimisticManager.add(id, 'update', { ...currentData, ...updates } as T, currentData)
    
    // 立即更新本地状态
    if (Array.isArray(state.value.data)) {
      state.value = {
        data: (state.value.data as T[]).map(item =>
          (item as any)?.id === id ? { ...item, ...updates } : item
        ),
        loading: false,
        error: null,
        isOptimistic: true
      }
    }
    
    try {
      const result = await serverUpdate()
      optimisticManager.confirm(id)
      
      if (Array.isArray(state.value.data)) {
        state.value = {
          data: (state.value.data as T[]).map(item =>
            (item as any)?.id === id ? result : item
          ),
          loading: false,
          error: null,
          isOptimistic: false
        }
      }
      
      return result
    } catch (error) {
      const previousData = optimisticManager.revert(id)
      
      if (previousData !== null && Array.isArray(state.value.data)) {
        state.value = {
          data: (state.value.data as T[]).map(item =>
            (item as any)?.id === id ? previousData : item
          ),
          loading: false,
          error: error as Error,
          isOptimistic: false
        }
      }
      
      options.onError?.(error as Error)
      throw error
    }
  }
  
  /**
   * 乐观删除
   */
  async function optimisticDelete(
    id: string,
    currentData: T,
    serverDelete: () => Promise<void>
  ): Promise<void> {
    // 先添加乐观更新
    optimisticManager.add(id, 'delete', currentData)
    
    // 立即更新本地状态
    if (Array.isArray(state.value.data)) {
      state.value = {
        data: (state.value.data as T[]).filter(item => (item as any)?.id !== id),
        loading: false,
        error: null,
        isOptimistic: true
      }
    }
    
    try {
      await serverDelete()
      optimisticManager.confirm(id)
    } catch (error) {
      // 回滚
      const previousData = optimisticManager.revert(id)
      
      if (previousData !== null && Array.isArray(state.value.data)) {
        state.value = {
          data: [...(state.value.data as T[]), previousData],
          loading: false,
          error: error as Error,
          isOptimistic: false
        }
      }
      
      options.onError?.(error as Error)
      throw error
    }
  }
  
  return {
    state,
    pendingUpdates,
    load,
    optimisticCreate,
    optimisticUpdate,
    optimisticDelete
  }
}
