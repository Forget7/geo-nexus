import { createRequest } from '@geonex/utils'

const api = createRequest({ baseURL: '/api/v1', timeout: 30000 })

// 注意：baseURL=/api/v1，路径不带 /api/v1 前缀

export const chatApi = {
  sendMessage: (request: { message: string; sessionId?: string; model?: string; mapMode?: string }) => {
    return api.post<{ data: any; success: boolean; message?: string }>(
      `/chat/simple`,
      null,
      { params: request }
    )
  },

  getHistory: (sessionId: string) => {
    return api.get<{ data: any[]; success: boolean }>(`/chat/history/${sessionId}`)
  },

  deleteSession: (sessionId: string) => {
    return api.delete<{ success: boolean }>(`/chat/session/${sessionId}`)
  },

  getSessions: (userId = 'anonymous', page = 0, size = 20) => {
    return api.get<{ data: any[]; success: boolean }>(`/chat/sessions`, {
      params: { userId, page, size }
    })
  }
}

export const spatialApi = {
  buffer: (params: { geometry: any; distanceKm: number }) => {
    return api.post<{ data: any; success: boolean }>(`/spatial/buffer`, params)
  },

  union: (params: { geometry1: any; geometry2: any }) => {
    return api.post<{ data: any; success: boolean }>(`/spatial/union`, params)
  },

  intersection: (params: { geometry1: any; geometry2: any }) => {
    return api.post<{ data: any; success: boolean }>(`/spatial/intersection`, params)
  },

  difference: (params: { geometry1: any; geometry2: any }) => {
    return api.post<{ data: any; success: boolean }>(`/spatial/difference`, params)
  },

  symDifference: (params: { geometry1: any; geometry2: any }) => {
    return api.post<{ data: any; success: boolean }>(`/spatial/sym-difference`, params)
  },

  filterByBounds: (params: { features: any[]; south: number; west: number; north: number; east: number }) => {
    return api.post<{ data: any[]; success: boolean }>(`/spatial/filter/bounds`, params)
  },

  spatialJoin: (params: { features1: any[]; features2: any[]; predicate: string }) => {
    return api.post<{ data: any[]; success: boolean }>(`/spatial/join`, params)
  },

  parseGeometry: (geojson: any) => {
    return api.post<{ data: any; success: boolean }>(`/spatial/parse`, geojson)
  }
}

export const metadataApi = {
  create: (metadata: any) => {
    return api.post<{ data: any; success: boolean }>(`/metadata`, metadata)
  },

  get: (id: string) => {
    return api.get<{ data: any; success: boolean }>(`/metadata/${id}`)
  },

  update: (id: string, metadata: any) => {
    return api.put<{ data: any; success: boolean }>(`/metadata/${id}`, metadata)
  },

  delete: (id: string) => {
    return api.delete<{ success: boolean }>(`/metadata/${id}`)
  },

  search: (query: { keyword?: string; category?: string; type?: string; format?: string; page?: number; pageSize?: number }) => {
    return api.post<{ data: any[]; success: boolean }>(`/metadata/search`, query)
  },

  exportISO19115: (id: string) => {
    return api.get<{ data: string; success: boolean }>(`/metadata/export/iso19115/${id}`)
  },

  getCategories: () => {
    return api.get<{ data: any[]; success: boolean }>(`/metadata/categories`)
  },

  createCategory: (category: any) => {
    return api.post<{ data: any; success: boolean }>(`/metadata/categories`, category)
  }
}

export const templateApi = {
  getTemplates: (category?: string) => {
    return api.get<{ data: any[]; status: string }>(
      `/templates`,
      category ? { category } : undefined
    )
  },

  getTemplate: (id: string) => {
    return api.get<{ data: any; status: string }>(`/templates/${id}`)
  },

  applyTemplate: (id: string, userId: string) => {
    return api.post<{ data: any; status: string }>(
      `/templates/${id}/apply`,
      null,
      { params: { userId } }
    )
  },

  getCategories: () => {
    return api.get<{ data: any[]; status: string }>(`/templates/categories`)
  }
}

export const gisProcessingApi = {
  batchBuffer: (params: { geometries: any[]; distanceKm: number }) => {
    return api.post<{ data: any; success: boolean }>(`/gis-processing/batch-buffer`, params)
  },

  batchDistance: (params: { points1: number[][]; points2: number[][] }) => {
    return api.post<{ data: any; success: boolean }>(`/gis-processing/batch-distance`, params)
  },

  gridAggregation: (params: { points: any[]; gridSizeDegrees: number }) => {
    return api.post<{ data: any; success: boolean }>(`/gis-processing/grid-aggregation`, params)
  },

  spatialJoin: (params: { points: any[]; polygons: any[] }) => {
    return api.post<{ data: any; success: boolean }>(`/gis-processing/spatial-join`, params)
  },

  simplifyPath: (params: { coordinates: number[][]; tolerance: number }) => {
    return api.post<{ data: any; success: boolean }>(`/gis-processing/simplify-path`, params)
  }
}

export const sanitizationApi = {
  sanitize: (params: { gisData: any; rules: Record<string, boolean> }) => {
    return api.post<{ data: any; success: boolean }>(`/sanitization/sanitize`, params)
  },

  maskPhone: (params: { phone: string }) => {
    return api.post<{ data: string; success: boolean }>(`/sanitization/mask-phone`, params)
  },

  maskEmail: (params: { email: string }) => {
    return api.post<{ data: string; success: boolean }>(`/sanitization/mask-email`, params)
  },

  roundCoord: (params: { value: number }) => {
    return api.post<{ data: number; success: boolean }>(`/sanitization/round-coord`, params)
  },

  maskAddress: (params: { address: string }) => {
    return api.post<{ data: string; success: boolean }>(`/sanitization/mask-address`, params)
  }
}
