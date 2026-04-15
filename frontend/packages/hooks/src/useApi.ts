/**
 * useApi - API调用 Composable
 */

import { ref, shallowRef } from 'vue'
import { api } from '../api'

export interface UseApiOptions {
  immediate?: boolean
  onSuccess?: (data: any) => void
  onError?: (error: any) => void
}

export function useApi<T = any>(options: UseApiOptions = {}) {
  const data = shallowRef<T | null>(null)
  const loading = ref(false)
  const error = ref<any>(null)
  const status = ref<'idle' | 'pending' | 'success' | 'error'>('idle')
  
  async function execute(...args: any[]): Promise<T | null> {
    loading.value = true
    error.value = null
    status.value = 'pending'
    
    try {
      const result = await api.get<T>(...args)
      data.value = result
      status.value = 'success'
      options.onSuccess?.(result)
      return result
    } catch (e) {
      error.value = e
      status.value = 'error'
      options.onError?.(e)
      return null
    } finally {
      loading.value = false
    }
  }
  
  function reset() {
    data.value = null
    loading.value = false
    error.value = null
    status.value = 'idle'
  }
  
  if (options.immediate) {
    execute()
  }
  
  return {
    data,
    loading,
    error,
    status,
    execute,
    reset
  }
}

// 特定API的composables
export function useChat() {
  const sendMessage = async (message: string, sessionId?: string, mapMode?: string) => {
    loading.value = true
    try {
      const response = await api.post('/api/v1/chat', {
        message,
        sessionId,
        mapMode
      })
      return response
    } finally {
      loading.value = false
    }
  }
  
  const { data, loading, error, execute } = useApi()
  
  return {
    data,
    loading,
    error,
    sendMessage
  }
}

export function useMapGenerator() {
  const generate = async (params: {
    geojson?: any
    center?: [number, number]
    zoom?: number
    mode?: '2d' | '3d'
    tileType?: string
  }) => {
    loading.value = true
    try {
      const response = await api.post('/api/v1/map/generate', params)
      return response
    } finally {
      loading.value = false
    }
  }
  
  const { data, loading, error } = useApi()
  
  return {
    data,
    loading,
    error,
    generate
  }
}

export function useTools() {
  const tools = shallowRef<any[]>([])
  const loading = ref(false)
  
  const fetchTools = async () => {
    loading.value = true
    try {
      const response = await api.get('/api/v1/tools')
      tools.value = response
      return response
    } finally {
      loading.value = false
    }
  }
  
  const executeTool = async (toolName: string, params: Record<string, any>) => {
    const response = await api.post('/api/v1/tools/execute', {
      tool: toolName,
      params
    })
    return response
  }
  
  return {
    tools,
    loading,
    fetchTools,
    executeTool
  }
}
