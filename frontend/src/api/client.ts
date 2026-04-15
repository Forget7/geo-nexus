/**
 * API Client - 前端API调用
 */

import axios, { AxiosInstance } from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_URL
if (!API_BASE_URL) {
  throw new Error('[API] VITE_API_URL environment variable is required. Set it in your .env file.')
}

// 创建axios实例
const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
apiClient.interceptors.request.use(
  (config) => {
    // 可以添加token等
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
apiClient.interceptors.response.use(
  (response) => response.data,
  (error) => {
    console.error('API Error:', error)
    return Promise.reject(error.response?.data?.error || error.message)
  }
)

// ============== Chat API ==============

export interface ChatRequest {
  message: string
  session_id?: string
  model?: string
  map_mode?: '2d' | '3d'
}

export interface ChatResponse {
  session_id: string
  type: string
  content: string
  map_url?: string
  analysis_result?: Record<string, unknown>
}

export interface ChatMessage {
  id: string
  role: 'user' | 'assistant' | 'system'
  content: string
  timestamp: number
}

export const chatApi = {
  send: (request: ChatRequest): Promise<ChatResponse> => {
    return apiClient.post('/api/v1/chat', request)
  },
  
  getHistory: (sessionId: string): Promise<ChatMessage[]> => {
    return apiClient.get<ChatMessage[]>(`/api/v1/chat/history/${sessionId}`)
  },
  
  deleteSession: (sessionId: string): Promise<void> => {
    return apiClient.delete<void>(`/api/v1/chat/${sessionId}`)
  }
}

// ============== Map API ==============

export interface MapLayer {
  id: string
  name: string
  type: 'base' | 'overlay' | 'vector' | 'raster'
  visible: boolean
  opacity?: number
  source?: string
  url?: string
}

export interface MapGenerateRequest {
  geojson?: GeoJSON
  center?: [number, number]
  zoom?: number
  height?: number
  mode?: '2d' | '3d'
  tile_type?: 'osm' | 'satellite' | 'dark' | 'terrain'
  layers?: MapLayer[]
}

export interface GeoJSON {
  type: string
  geometry?: Record<string, unknown>
  properties?: Record<string, unknown>
  features?: GeoJSON[]
}

export interface MapGenerateResponse {
  id: string
  url: string
  url_2d?: string
  url_3d?: string
  thumbnail?: string
}

export const mapApi = {
  generate: (request: MapGenerateRequest): Promise<MapGenerateResponse> => {
    return apiClient.post('/api/v1/map/generate', request)
  },
  
  getInfo: (mapId: string): Promise<MapGenerateResponse> => {
    return apiClient.get<MapGenerateResponse>(`/api/v1/map/${mapId}`)
  },
  
  getHtml: (mapId: string, mode: '2d' | '3d' = '2d'): string => {
    return `${API_BASE_URL}/api/v1/map/${mapId}/${mode}`
  }
}

// ============== Data API ==============

export type GisFormat = 'geojson' | 'shapefile' | 'kml' | 'gml' | 'gpx'

export interface DataConvertRequest {
  data: string | object
  input_format: GisFormat
  output_format: GisFormat
  source_crs?: string
  target_crs?: string
}

export interface DataUploadResponse {
  id: string
  filename: string
  format: string
  size: number
  url: string
}

export const dataApi = {
  convert: (request: DataConvertRequest): Promise<DataUploadResponse> => {
    return apiClient.post<DataUploadResponse>('/api/v1/data/convert', request)
  },
  
  upload: async (file: File): Promise<DataUploadResponse> => {
    const formData = new FormData()
    formData.append('file', file)
    
    const response = await apiClient.post<DataUploadResponse>('/api/v1/data/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    return response
  },
  
  get: (dataId: string): Promise<DataUploadResponse> => {
    return apiClient.get<DataUploadResponse>(`/api/v1/data/${dataId}`)
  },
  
  getGeoJSON: (dataId: string): Promise<GeoJSON> => {
    return apiClient.get<GeoJSON>(`/api/v1/data/${dataId}/geojson`)
  }
}

// ============== Tools API ==============

export interface ToolParameter {
  name: string
  type: string
  description?: string
  required?: boolean
  default?: unknown
}

export interface ToolExecuteRequest {
  tool: string
  params: Record<string, unknown>
}

export interface ToolInfo {
  name: string
  description: string
  parameters: ToolParameter[]
}

export interface DistanceResult {
  distance: number
  unit: string
}

export interface GeocodeResult {
  address: string
  location: [number, number]
  confidence?: number
}

export const toolsApi = {
  list: (): Promise<ToolInfo[]> => {
    return apiClient.get<ToolInfo[]>('/api/v1/tools')
  },
  
  execute: (request: ToolExecuteRequest): Promise<unknown> => {
    return apiClient.post<unknown>('/api/v1/tools/execute', request)
  },
  
  // 便捷方法
  calculateDistance: (
    lat1: number, lon1: number,
    lat2: number, lon2: number,
    unit: string = 'km'
  ): Promise<DistanceResult> => {
    return apiClient.get<DistanceResult>('/api/v1/tools/distance', {
      params: { lat1, lon1, lat2, lon2, unit }
    })
  },
  
  geocode: (address: string): Promise<GeocodeResult> => {
    return apiClient.post<GeocodeResult>('/api/v1/tools/geocode', { address })
  }
}

// 导出默认实例
export default apiClient
