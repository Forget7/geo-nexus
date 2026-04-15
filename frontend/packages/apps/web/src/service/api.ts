import { api } from '@/api'

const BASE_URL = '/api/v1'

export const chatApi = {
  sendMessage: (request: { message: string; sessionId?: string; model?: string; mapMode?: string }) => {
    return api.post<{ data: any; success: boolean; message?: string }>(
      `${BASE_URL}/chat/simple`,
      null,
      { params: request }
    )
  },

  getHistory: (sessionId: string) => {
    return api.get<{ data: any[]; success: boolean }>(`${BASE_URL}/chat/history/${sessionId}`)
  },

  deleteSession: (sessionId: string) => {
    return api.delete<{ success: boolean }>(`${BASE_URL}/chat/session/${sessionId}`)
  },

  getSessions: (userId = 'anonymous', page = 0, size = 20) => {
    return api.get<{ data: any[]; success: boolean }>(`${BASE_URL}/chat/sessions`, {
      params: { userId, page, size }
    })
  }
}

export const spatialApi = {
  buffer: (params: { geometry: any; distanceKm: number }) => {
    return api.post<{ data: any; success: boolean }>(`${BASE_URL}/spatial/buffer`, params)
  },

  union: (params: { geometry1: any; geometry2: any }) => {
    return api.post<{ data: any; success: boolean }>(`${BASE_URL}/spatial/union`, params)
  },

  intersection: (params: { geometry1: any; geometry2: any }) => {
    return api.post<{ data: any; success: boolean }>(`${BASE_URL}/spatial/intersection`, params)
  },

  difference: (params: { geometry1: any; geometry2: any }) => {
    return api.post<{ data: any; success: boolean }>(`${BASE_URL}/spatial/difference`, params)
  },

  symDifference: (params: { geometry1: any; geometry2: any }) => {
    return api.post<{ data: any; success: boolean }>(`${BASE_URL}/spatial/sym-difference`, params)
  },

  filterByBounds: (params: { features: any[]; south: number; west: number; north: number; east: number }) => {
    return api.post<{ data: any[]; success: boolean }>(`${BASE_URL}/spatial/filter/bounds`, params)
  },

  spatialJoin: (params: { features1: any[]; features2: any[]; predicate: string }) => {
    return api.post<{ data: any[]; success: boolean }>(`${BASE_URL}/spatial/join`, params)
  },

  parseGeometry: (geojson: any) => {
    return api.post<{ data: any; success: boolean }>(`${BASE_URL}/spatial/parse`, geojson)
  }
}

export const metadataApi = {
  create: (metadata: any) => {
    return api.post<{ data: any; success: boolean }>(`${BASE_URL}/metadata`, metadata)
  },

  get: (id: string) => {
    return api.get<{ data: any; success: boolean }>(`${BASE_URL}/metadata/${id}`)
  },

  update: (id: string, metadata: any) => {
    return api.put<{ data: any; success: boolean }>(`${BASE_URL}/metadata/${id}`, metadata)
  },

  delete: (id: string) => {
    return api.delete<{ success: boolean }>(`${BASE_URL}/metadata/${id}`)
  },

  search: (query: { keyword?: string; category?: string; type?: string; format?: string; page?: number; pageSize?: number }) => {
    return api.post<{ data: any[]; success: boolean }>(`${BASE_URL}/metadata/search`, query)
  },

  exportISO19115: (id: string) => {
    return api.get<{ data: string; success: boolean }>(`${BASE_URL}/metadata/export/iso19115/${id}`)
  },

  getCategories: () => {
    return api.get<{ data: any[]; success: boolean }>(`${BASE_URL}/metadata/categories`)
  },

  createCategory: (category: any) => {
    return api.post<{ data: any; success: boolean }>(`${BASE_URL}/metadata/categories`, category)
  }
}

export const templateApi = {
  getTemplates: (category?: string) => {
    return api.get<{ data: any[]; status: string }>(
      `${BASE_URL}/templates`,
      category ? { category } : undefined
    )
  },

  getTemplate: (id: string) => {
    return api.get<{ data: any; status: string }>(`${BASE_URL}/templates/${id}`)
  },

  applyTemplate: (id: string, userId: string) => {
    return api.post<{ data: any; status: string }>(
      `${BASE_URL}/templates/${id}/apply`,
      null,
      { params: { userId } }
    )
  },

  getCategories: () => {
    return api.get<{ data: any[]; status: string }>(`${BASE_URL}/templates/categories`)
  }
}

export const gisProcessingApi = {
  batchBuffer: (params: { geometries: any[]; distanceKm: number }) => {
    return api.post<{ data: any; success: boolean }>(`${BASE_URL}/gis-processing/batch-buffer`, params)
  },

  batchDistance: (params: { points1: number[][]; points2: number[][] }) => {
    return api.post<{ data: any; success: boolean }>(`${BASE_URL}/gis-processing/batch-distance`, params)
  },

  gridAggregation: (params: { points: any[]; gridSizeDegrees: number }) => {
    return api.post<{ data: any; success: boolean }>(`${BASE_URL}/gis-processing/grid-aggregation`, params)
  },

  spatialJoin: (params: { points: any[]; polygons: any[] }) => {
    return api.post<{ data: any; success: boolean }>(`${BASE_URL}/gis-processing/spatial-join`, params)
  },

  simplifyPath: (params: { coordinates: number[][]; tolerance: number }) => {
    return api.post<{ data: any; success: boolean }>(`${BASE_URL}/gis-processing/simplify-path`, params)
  }
}

export const sanitizationApi = {
  sanitize: (params: { gisData: any; rules: Record<string, boolean> }) => {
    return api.post<{ data: any; success: boolean }>(`${BASE_URL}/sanitization/sanitize`, params)
  },

  maskPhone: (params: { phone: string }) => {
    return api.post<{ data: string; success: boolean }>(`${BASE_URL}/sanitization/mask-phone`, params)
  },

  maskEmail: (params: { email: string }) => {
    return api.post<{ data: string; success: boolean }>(`${BASE_URL}/sanitization/mask-email`, params)
  },

  roundCoord: (params: { value: number }) => {
    return api.post<{ data: number; success: boolean }>(`${BASE_URL}/sanitization/round-coord`, params)
  },

  maskAddress: (params: { address: string }) => {
    return api.post<{ data: string; success: boolean }>(`${BASE_URL}/sanitization/mask-address`, params)
  }
}
