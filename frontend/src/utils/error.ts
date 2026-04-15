/**
 * 错误边界组件 - 捕获React/Vue渲染错误
 */

import { ref, onErrorCaptured, type Ref } from 'vue'

export interface ErrorInfo {
  error: Error
  source: string
  timestamp: number
  componentStack?: string
}

export interface ErrorBoundaryState {
  hasError: boolean
  error: Error | null
  errorInfo: ErrorInfo | null
  errorCount: number
}

// 全局错误状态
const globalErrorState: ErrorBoundaryState = {
  hasError: false,
  error: null,
  errorInfo: null,
  errorCount: 0
}

// 错误处理器列表
const errorHandlers: Array<(info: ErrorInfo) => void> = []

/**
 * 错误边界 Composable
 */
export function useErrorBoundary() {
  const localState: Ref<ErrorBoundaryState> = ref({ ...globalErrorState })
  
  function handleError(error: Error, source = 'unknown') {
    const errorInfo: ErrorInfo = {
      error,
      source,
      timestamp: Date.now()
    }
    
    // 更新状态
    localState.value = {
      hasError: true,
      error,
      errorInfo,
      errorCount: globalErrorState.errorCount + 1
    }
    
    // 更新全局状态
    globalErrorState.hasError = true
    globalErrorState.error = error
    globalErrorState.errorInfo = errorInfo
    globalErrorState.errorCount++
    
    // 调用处理器
    errorHandlers.forEach(handler => {
      try {
        handler(errorInfo)
      } catch (e) {
        console.error('Error in error handler:', e)
      }
    })
    
    // 控制台输出
    console.error(`[ErrorBoundary] ${source}:`, error)
    
    return errorInfo
  }
  
  function clearError() {
    localState.value = {
      hasError: false,
      error: null,
      errorInfo: null,
      errorCount: localState.value.errorCount
    }
    globalErrorState.hasError = false
    globalErrorState.error = null
    globalErrorState.errorInfo = null
  }
  
  // Vue 3错误捕获
  onErrorCaptured((err, instance, info) => {
    handleError(err as Error, `Vue: ${info}`)
    // 返回false阻止错误传播
    return false
  })
  
  return {
    state: localState,
    hasError: () => localState.value.hasError,
    handleError,
    clearError
  }
}

/**
 * 全局错误处理器
 */
export function onGlobalError(handler: (info: ErrorInfo) => void) {
  errorHandlers.push(handler)
  return () => {
    const index = errorHandlers.indexOf(handler)
    if (index > -1) {
      errorHandlers.splice(index, 1)
    }
  }
}

/**
 * 错误重试
 */
export async function withRetry<T>(
  fn: () => Promise<T>,
  options: {
    maxAttempts?: number
    delayMs?: number
    backoffMultiplier?: number
    onRetry?: (attempt: number, error: Error) => void
    shouldRetry?: (error: Error) => boolean
  } = {}
): Promise<T> {
  const {
    maxAttempts = 3,
    delayMs = 1000,
    backoffMultiplier = 2,
    onRetry,
    shouldRetry
  } = options
  
  let lastError: Error
  
  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    try {
      return await fn()
    } catch (error) {
      lastError = error as Error
      
      if (attempt === maxAttempts) {
        break
      }
      
      // 检查是否应该重试
      if (shouldRetry && !shouldRetry(lastError)) {
        throw lastError
      }
      
      // 回调
      onRetry?.(attempt, lastError)
      
      // 等待
      const delay = delayMs * Math.pow(backoffMultiplier, attempt - 1)
      await new Promise(resolve => setTimeout(resolve, delay))
    }
  }
  
  throw lastError!
}

/**
 * 异步错误包装
 */
export function withError<T>(
  promise: Promise<T>,
  errorHandler?: (error: Error) => void
): Promise<[T | null, Error | null]> {
  return promise
    .then(data => [data, null])
    .catch(error => {
      errorHandler?.(error)
      return [null, error]
    })
}

/**
 * 错误格式化
 */
export function formatError(error: unknown): {
  message: string
  code?: string
  details?: string
  stack?: string
} {
  if (error instanceof Error) {
    // API错误格式
    if ((error as any).response?.data) {
      const response = (error as any).response.data
      return {
        message: response.message || error.message,
        code: response.code,
        details: response.details,
        stack: error.stack
      }
    }
    
    return {
      message: error.message,
      code: (error as any).code,
      details: (error as any).details,
      stack: error.stack
    }
  }
  
  return {
    message: String(error)
  }
}

/**
 * 静默失败包装
 */
export function silent<T>(fn: () => T, fallback: T): T {
  try {
    return fn()
  } catch {
    return fallback
  }
}

/**
 * 异步静默失败
 */
export async function silentAsync<T>(
  fn: () => Promise<T>,
  fallback: T
): Promise<T> {
  try {
    return await fn()
  } catch {
    return fallback
  }
}

/**
 * 错误边界组件配置
 */
export const errorBoundaryConfig = {
  // 是否在控制台输出错误
  logToConsole: true,
  // 是否向上下文发送错误
  sendToParent: false,
  // 是否显示错误提示
  showNotification: true,
  // 最大错误数
  maxErrors: 10,
  // 错误忽略列表
  ignoreErrors: [
    // 忽略网络取消
    'cancel',
    'Cancel',
    'Network Error',
    // 忽略浏览器关闭
    'Failed to fetch',
    'NetworkError',
    // 忽略超时
    'timeout',
    'TimeoutError'
  ]
}

/**
 * 检查是否应该忽略错误
 */
export function shouldIgnoreError(error: Error): boolean {
  return errorBoundaryConfig.ignoreErrors.some(ignore => 
    error.message.includes(ignore)
  )
}
