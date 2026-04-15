/**
 * Chat Store - Pinia状态管理
 * 
 * 支持 WebSocket 流式对话，自动降级到 HTTP REST
 */

import { defineStore } from 'pinia'
import { ref, computed, onUnmounted } from 'vue'
import { chatApi, ChatRequest, ChatResponse } from '../../src/api/client'

export interface Message {
  id: string
  role: 'user' | 'assistant' | 'system'
  content: string
  mapUrl?: string
  timestamp: Date
  loading?: boolean
  error?: string
}

export const useChatStore = defineStore('chat', () => {
  // State
  const messages = ref<Message[]>([])
  const sessionId = ref<string>('')
  const isLoading = ref(false)
  const error = ref<string | null>(null)
  const currentMapUrl = ref<string>('')
  
  // WebSocket 状态
  const wsConnected = ref(false)
  const wsReconnecting = ref(false)
  let wsSocket: WebSocket | null = null
  let wsReconnectTimer: ReturnType<typeof setTimeout> | null = null
  const WS_URL = (import.meta.env.VITE_API_URL || 'http://localhost:8080').replace(/^http/, 'ws') + '/ws/v1/chat'
  
  // Getters
  const hasMessages = computed(() => messages.value.length > 0)
  const lastMessage = computed(() => messages.value[messages.value.length - 1])
  const userMessages = computed(() => 
    messages.value.filter(m => m.role === 'user'))
  
  // ==================== WebSocket 管理 ====================
  
  function connectWebSocket(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (wsSocket && (wsSocket.readyState === WebSocket.OPEN || wsSocket.readyState === WebSocket.CONNECTING)) {
        resolve()
        return
      }
      
      try {
        wsSocket = new WebSocket(WS_URL)
        
        wsSocket.onopen = () => {
          wsConnected.value = true
          wsReconnecting.value = false
          console.debug('[ChatStore] WebSocket 已连接')
          resolve()
        }
        
        wsSocket.onmessage = (event) => {
          try {
            const data = JSON.parse(event.data)
            handleWsMessage(data)
          } catch (e) {
            console.error('[ChatStore] 解析 WebSocket 消息失败:', e)
          }
        }
        
        wsSocket.onclose = (event) => {
          wsConnected.value = false
          console.debug('[ChatStore] WebSocket 连接关闭:', event.code)
          
          // 非正常关闭时尝试重连
          if (event.code !== 1000 && !wsReconnecting.value) {
            scheduleReconnect()
          }
        }
        
        wsSocket.onerror = (e) => {
          console.error('[ChatStore] WebSocket 错误:', e)
          wsConnected.value = false
          // 连接失败时不用 reject，让它降级到 HTTP
          resolve()
        }
        
        // 3秒超时
        setTimeout(() => {
          if (wsSocket && wsSocket.readyState === WebSocket.CONNECTING) {
            wsSocket.close()
            resolve() // 超时也降级到 HTTP
          }
        }, 3000)
        
      } catch (e) {
        console.error('[ChatStore] WebSocket 连接失败:', e)
        resolve() // 降级到 HTTP
      }
    })
  }
  
  function scheduleReconnect() {
    if (wsReconnecting.value) return
    wsReconnecting.value = true
    console.debug('[ChatStore] 5秒后尝试重连 WebSocket...')
    wsReconnectTimer = setTimeout(() => {
      wsReconnecting.value = false
      connectWebSocket()
    }, 5000)
  }
  
  function disconnectWebSocket() {
    if (wsReconnectTimer) {
      clearTimeout(wsReconnectTimer)
      wsReconnectTimer = null
    }
    if (wsSocket) {
      wsSocket.close(1000, '用户离开')
      wsSocket = null
    }
    wsConnected.value = false
  }
  
  function sendWsMessage(data: object) {
    if (wsSocket && wsSocket.readyState === WebSocket.OPEN) {
      wsSocket.send(JSON.stringify(data))
    }
  }
  
  // 处理 WebSocket 消息
  let currentStreamingMessage: Message | null = null
  
  function handleWsMessage(data: any) {
    switch (data.type) {
      case 'connected':
        console.debug('[ChatStore] WebSocket 连接确认:', data.sessionId)
        break
        
      case 'processing':
        // 正在处理，显示提示
        if (currentStreamingMessage) {
          currentStreamingMessage.content = data.message || '正在思考...'
        }
        break
        
      case 'chunk':
        // 流式内容块
        if (currentStreamingMessage) {
          // 追加新内容（避免重复）
          const newContent = data.content
          if (!currentStreamingMessage.content.includes(newContent)) {
            currentStreamingMessage.content = newContent
          }
        }
        break
        
      case 'message':
      case 'done':
        // 完成消息
        if (currentStreamingMessage) {
          currentStreamingMessage.content = data.content
          currentStreamingMessage.loading = false
          currentStreamingMessage.mapUrl = data.mapUrl
          if (data.sessionId) {
            sessionId.value = data.sessionId
          }
          if (data.mapUrl) {
            currentMapUrl.value = data.mapUrl
          }
          currentStreamingMessage = null
          isLoading.value = false
        }
        break
        
      case 'error':
        // 错误消息
        if (currentStreamingMessage) {
          currentStreamingMessage.content = `错误: ${data.message}`
          currentStreamingMessage.loading = false
          currentStreamingMessage.error = data.message
          error.value = data.message
          currentStreamingMessage = null
          isLoading.value = false
        }
        break
        
      case 'pong':
        // 心跳响应
        break
    }
  }
  
  // ==================== 聊天方法 ====================
  
  /**
   * 发送消息 - 优先使用 WebSocket 流式输出，失败时降级到 HTTP
   */
  async function sendMessage(content: string, options?: { mapMode?: '2d' | '3d' }) {
    if (!content.trim() || isLoading.value) return
    
    // 添加用户消息
    const userMessage: Message = {
      id: Date.now().toString(),
      role: 'user',
      content: content.trim(),
      timestamp: new Date()
    }
    messages.value.push(userMessage)
    
    isLoading.value = true
    error.value = null
    
    // 添加一个临时的加载消息（用于 WebSocket 流式更新）
    const loadingMessage: Message = {
      id: 'loading-' + Date.now(),
      role: 'assistant',
      content: '正在连接...',
      timestamp: new Date(),
      loading: true
    }
    messages.value.push(loadingMessage)
    currentStreamingMessage = loadingMessage
    
    // 尝试 WebSocket
    await connectWebSocket()
    
    if (wsConnected.value && wsSocket && wsSocket.readyState === WebSocket.OPEN) {
      // WebSocket 模式
      sendWsMessage({
        action: 'chat',
        message: content.trim(),
        sessionId: sessionId.value || undefined,
        mapMode: options?.mapMode || '2d'
      })
      // 后续消息通过 onmessage 回调处理
    } else {
      // 降级到 HTTP REST
      console.debug('[ChatStore] WebSocket 不可用，降级到 HTTP')
      await sendMessageHttp(content, options)
    }
  }
  
  /**
   * HTTP REST 模式发送消息（降级方案）
   */
  async function sendMessageHttp(content: string, options?: { mapMode?: '2d' | '3d' }) {
    // 移除之前的加载消息（如果还在）
    const loadingIndex = messages.value.findIndex(m => m.id === currentStreamingMessage?.id)
    if (loadingIndex !== -1) {
      messages.value.splice(loadingIndex, 1)
    }
    
    // 添加新的加载消息
    const loadingMessage: Message = {
      id: 'loading-' + Date.now(),
      role: 'assistant',
      content: '',
      timestamp: new Date(),
      loading: true
    }
    messages.value.push(loadingMessage)
    currentStreamingMessage = loadingMessage
    
    try {
      const request: ChatRequest = {
        message: content,
        session_id: sessionId.value || undefined,
        map_mode: options?.mapMode || '2d'
      }
      
      const response = await chatApi.send(request)
      
      // 更新session
      if (response.session_id) {
        sessionId.value = response.session_id
      }
      
      // 移除加载消息
      const idx = messages.value.findIndex(m => m.id === loadingMessage.id)
      if (idx !== -1) {
        messages.value.splice(idx, 1)
      }
      
      // 添加助手回复
      const assistantMessage: Message = {
        id: (Date.now() + 1).toString(),
        role: 'assistant',
        content: response.content,
        mapUrl: response.map_url,
        timestamp: new Date()
      }
      messages.value.push(assistantMessage)
      
      // 更新地图
      if (response.map_url) {
        currentMapUrl.value = response.map_url
      }
      
      currentStreamingMessage = null
      
    } catch (err: unknown) {
      const e = err as Error
      // 移除加载消息
      const idx = messages.value.findIndex(m => m.id === loadingMessage.id)
      if (idx !== -1) {
        messages.value.splice(idx, 1)
      }
      
      // 添加错误消息
      const errorMessage: Message = {
        id: (Date.now() + 1).toString(),
        role: 'assistant',
        content: `错误: ${e.message || '请求失败'}`,
        timestamp: new Date(),
        error: e.message
      }
      messages.value.push(errorMessage)
      error.value = e.message
      currentStreamingMessage = null
    } finally {
      isLoading.value = false
    }
  }
  
  function clearMessages() {
    messages.value = []
    currentStreamingMessage = null
    error.value = null
    // 不清除 sessionId，保持对话连续性
  }
  
  function deleteSession() {
    if (sessionId.value) {
      chatApi.deleteSession(sessionId.value)
    }
    clearMessages()
    sessionId.value = ''
    currentMapUrl.value = ''
    disconnectWebSocket()
  }
  
  // 组件卸载时清理
  onUnmounted(() => {
    disconnectWebSocket()
  })
  
  return {
    // State
    messages,
    sessionId,
    isLoading,
    error,
    currentMapUrl,
    wsConnected,
    // Getters
    hasMessages,
    lastMessage,
    userMessages,
    // Actions
    sendMessage,
    clearMessages,
    deleteSession,
    connectWebSocket,
    disconnectWebSocket
  }
})
