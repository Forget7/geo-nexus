import axios, { type AxiosInstance, type AxiosError } from 'axios'

export interface RequestOptions {
  baseURL?: string
  timeout?: number
  retry?: number
  retryDelay?: number
}

export function createRequest(options: RequestOptions = {}) {
  const {
    baseURL = '/api',
    timeout = 30000,
    retry = 3,
    retryDelay = 1000
  } = options

  const instance: AxiosInstance = axios.create({ baseURL, timeout })

  // Request interceptor
  instance.interceptors.request.use(
    (config) => {
      // Add auth token
      const token = localStorage.getItem('token')
      if (token) {
        config.headers.Authorization = `Bearer ${token}`
      }
      return config
    },
    (error) => Promise.reject(error)
  )

  // Response interceptor
  instance.interceptors.response.use(
    (response) => response.data,
    async (error: AxiosError) => {
      const config = error.config as any
      if (!config || error.response?.status !== 500 || retry <= 0) {
        return Promise.reject(error)
      }
      config.retryCount = config.retryCount || 0
      if (config.retryCount < retry) {
        config.retryCount += 1
        await new Promise(resolve => setTimeout(resolve, retryDelay))
        return instance(config)
      }
      return Promise.reject(error)
    }
  )

  return instance
}
