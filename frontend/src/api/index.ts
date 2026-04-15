/**
 * API客户端增强 - 请求拦截器、响应拦截器、错误处理
 */

import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse, AxiosError } from 'axios'

// 创建axios实例
const apiClient: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求队列
let isRefreshing = false
let requestQueue: Array<(token: string) => void> = []

// Token管理
let accessToken: string | null = localStorage.getItem('accessToken')

// ==================== 请求拦截器 ====================

apiClient.interceptors.request.use(
  (config) => {
    // 添加Token
    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`
    }
    
    // 添加时间戳防止缓存
    if (config.method === 'get') {
      config.params = {
        ...config.params,
        _t: Date.now()
      }
    }
    
    // 请求日志
    console.debug(`[API Request] ${config.method?.toUpperCase()} ${config.url}`, {
      params: config.params,
      data: config.data
    })
    
    return config
  },
  (error) => {
    console.error('[API Request Error]', error)
    return Promise.reject(error)
  }
)

// ==================== 响应拦截器 ====================

apiClient.interceptors.response.use(
  (response: AxiosResponse) => {
    // 响应日志
    console.debug(`[API Response] ${response.config.url}`, response.data)
    
    // 保存Token
    if (response.data.accessToken) {
      accessToken = response.data.accessToken
      localStorage.setItem('accessToken', accessToken)
    }
    
    return response
  },
  async (error: AxiosError) => {
    const originalRequest = error.config as AxiosRequestConfig & { _retry?: boolean }
    
    // 错误日志
    console.error(`[API Error] ${originalRequest.url}`, {
      status: error.response?.status,
      data: error.response?.data,
      message: error.message
    })
    
    // 401 Unauthorized - Token过期
    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        // 等待刷新完成
        return new Promise((resolve) => {
          requestQueue.push((token: string) => {
            if (originalRequest.headers) {
              originalRequest.headers.Authorization = `Bearer ${token}`
            }
            resolve(apiClient(originalRequest))
          })
        })
      }
      
      originalRequest._retry = true
      isRefreshing = true
      
      try {
        // 尝试刷新Token
        const response = await axios.post(
          `${import.meta.env.VITE_API_URL}/api/v1/auth/refresh`,
          {},
          { headers: { Authorization: `Bearer ${accessToken}` } }
        )
        
        const newToken = response.data.accessToken
        accessToken = newToken
        localStorage.setItem('accessToken', newToken)
        
        // 重试排队的请求
        requestQueue.forEach(cb => cb(newToken))
        requestQueue = []
        
        // 重试原始请求
        if (originalRequest.headers) {
          originalRequest.headers.Authorization = `Bearer ${newToken}`
        }
        return apiClient(originalRequest)
        
      } catch (refreshError) {
        // 刷新失败，清除Token
        accessToken = null
        localStorage.removeItem('accessToken')
        requestQueue = []
        
        // 跳转到登录页
        window.location.href = '/login'
        return Promise.reject(refreshError)
        
      } finally {
        isRefreshing = false
      }
    }
    
    // 429 Too Many Requests - 限流
    if (error.response?.status === 429) {
      const retryAfter = error.response.headers['retry-after']
      const waitTime = retryAfter ? parseInt(retryAfter) * 1000 : 60000
      
      return Promise.reject({
        type: 'RATE_LIMITED',
        message: '请求过于频繁，请稍后再试',
        retryAfter: waitTime
      })
    }
    
    // 422 Validation Error
    if (error.response?.status === 422) {
      const errors = error.response.data?.detail || error.response.data
      return Promise.reject({
        type: 'VALIDATION_ERROR',
        message: '数据验证失败',
        errors
      })
    }
    
    // 其他错误
    const errorMessage = 
      error.response?.data?.message ||
      error.response?.data?.error ||
      error.message ||
      '网络错误'
    
    return Promise.reject({
      type: 'API_ERROR',
      message: errorMessage,
      status: error.response?.status
    })
  }
)

// ==================== API方法增强 ====================

export const api = {
  // GET请求
  async get<T = unknown>(url: string, params?: Record<string, unknown>, config?: AxiosRequestConfig): Promise<T> {
    const response = await apiClient.get<T>(url, { ...config, params })
    return response.data
  },
  
  // POST请求
  async post<T = unknown>(url: string, data?: Record<string, unknown>, config?: AxiosRequestConfig): Promise<T> {
    const response = await apiClient.post<T>(url, data, config)
    return response.data
  },
  
  // PUT请求
  async put<T = unknown>(url: string, data?: Record<string, unknown>, config?: AxiosRequestConfig): Promise<T> {
    const response = await apiClient.put<T>(url, data, config)
    return response.data
  },
  
  // DELETE请求
  async delete<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T> {
    const response = await apiClient.delete<T>(url, config)
    return response.data
  },
  
  // PATCH请求
  async patch<T = unknown>(url: string, data?: Record<string, unknown>, config?: AxiosRequestConfig): Promise<T> {
    const response = await apiClient.patch<T>(url, data, config)
    return response.data
  }
}

// 重新导出
export { apiClient }
export default api
