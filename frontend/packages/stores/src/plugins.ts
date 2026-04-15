import { defineStore } from 'pinia'
import { ref } from 'vue'
import { createRequest } from '@geonex/utils'

const request = createRequest({ baseURL: '/api' })

export interface PluginManifest {
  id: string
  pluginId: string
  name: string
  version: string
  description: string
  category: string
  iconUrl?: string
  authorName: string
  tags: string[]
  downloadSize: number
  installCount: number
  rating: number
  status: string
}

export interface PluginInstance {
  id: string
  pluginId: string
  userId: string
  status: 'ACTIVE' | 'DISABLED'
  config: Record<string, any>
  installedAt: string
}

export const usePluginStore = defineStore('plugins', () => {
  const installed = ref<PluginInstance[]>([])
  const marketplace = ref<PluginManifest[]>([])
  const activePlugin = ref<string | null>(null)

  async function fetchMarketplace(q?: string, category?: string) {
    const params = new URLSearchParams()
    if (q) params.set('q', q)
    if (category) params.set('category', category)
    const res = await request.get<any>(`/plugins/search?${params}`)
    marketplace.value = res.data || []
  }

  async function fetchMyPlugins(userId: string) {
    const res = await request.get<any>(`/plugins/my?userId=${userId}`)
    installed.value = res.data || []
  }

  async function install(pluginId: string, userId: string, config?: Record<string, any>) {
    await request.post(`/plugins/${pluginId}/install?userId=${userId}`, config || {})
    await fetchMyPlugins(userId)
  }

  async function uninstall(pluginId: string, userId: string) {
    await request.delete(`/plugins/${pluginId}/install?userId=${userId}`)
  }

  async function setEnabled(pluginId: string, userId: string, enabled: boolean) {
    await request.patch(`/plugins/${pluginId}/enabled?userId=${userId}&enabled=${enabled}`)
  }

  function renderPlugin(pluginId: string, container: HTMLElement, config: Record<string, any>) {
    console.log('Rendering plugin:', pluginId, config)
  }

  return { installed, marketplace, activePlugin, fetchMarketplace, fetchMyPlugins, install, uninstall, setEnabled, renderPlugin }
})
