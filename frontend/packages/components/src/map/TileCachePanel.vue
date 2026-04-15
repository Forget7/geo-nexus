<template>
  <div class="tile-panel">
    <div class="panel-header">
      <h3>🗺️ {{ t('tile.title') }}</h3>
    </div>

    <!-- 缓存列表 -->
    <div class="section">
      <div class="section-actions">
        <button @click="showCreate = true" class="create-btn">+ {{ t('tile.createCache') }}</button>
      </div>
      <div v-if="caches.length === 0" class="empty-state">
        <span>{{ t('tile.noCaches') }}</span>
      </div>
      <div class="cache-list">
        <div v-for="cache in caches" :key="cache.id" class="cache-item">
          <div class="cache-info">
            <strong>{{ cache.name || cache.id }}</strong>
            <div class="cache-meta">
              <span>{{ cache.tileCount || 0 }} tiles</span>
              <span>{{ formatHitRate(cache.hitRate) }}% {{ t('tile.hitRate') }}</span>
            </div>
          </div>
          <div class="cache-actions">
            <button @click="viewStatus(cache.id)" class="view-btn">{{ t('tile.status') }}</button>
            <button @click="deleteCache(cache.id)" class="del-btn">{{ t('tile.delete') }}</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 创建弹窗 -->
    <div v-if="showCreate" class="modal-overlay" @click.self="showCreate = false">
      <div class="modal">
        <h3>{{ t('tile.createCache') }}</h3>
        <input v-model="newCache.name" :placeholder="t('tile.cacheName')" class="input" />
        <input v-model="newCache.tileSource" :placeholder="t('tile.sourceUrl')" class="input" />
        <div class="input-row">
          <input v-model.number="newCache.minZoom" type="number" :placeholder="t('tile.minZoom')" class="input half" />
          <input v-model.number="newCache.maxZoom" type="number" :placeholder="t('tile.maxZoom')" class="input half" />
        </div>
        <div class="modal-actions">
          <button @click="createCache">{{ t('tile.create') }}</button>
          <button @click="showCreate = false" class="cancel-btn">{{ t('tile.cancel') }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

const t = (key: string) => key
const caches = ref<any[]>([])
const showCreate = ref(false)
const newCache = ref<any>({ minZoom: 0, maxZoom: 18 })

onMounted(() => fetchCaches())

async function fetchCaches() {
  try {
    const res = await fetch('/api/v1/tiles/caches')
    const json = await res.json()
    if (json.success) {
      caches.value = json.data || []
    }
  } catch (e) {
    console.error('Failed to fetch caches', e)
  }
}

async function createCache() {
  try {
    const res = await fetch('/api/v1/tiles/caches', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(newCache.value)
    })
    const json = await res.json()
    if (json.success) {
      newCache.value = { minZoom: 0, maxZoom: 18 }
      showCreate.value = false
      await fetchCaches()
    }
  } catch (e) {
    console.error('Failed to create cache', e)
  }
}

async function deleteCache(id: string) {
  if (!confirm(t('tile.confirmDelete'))) return
  try {
    await fetch(`/api/v1/tiles/caches/${id}`, { method: 'DELETE' })
    await fetchCaches()
  } catch (e) {
    console.error('Failed to delete cache', e)
  }
}

function viewStatus(id: string) {
  console.log('Cache status:', id)
}

function formatHitRate(rate: number): number {
  return typeof rate === 'number' ? Math.round(rate * 10) / 10 : 0
}
</script>

<style scoped>
.tile-panel { background: white; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.15); max-height: 80vh; overflow: hidden; display: flex; flex-direction: column; }
.panel-header { padding: 12px 16px; border-bottom: 1px solid #e2e8f0; }
.panel-header h3 { font-size: 15px; font-weight: 600; }
.section { flex: 1; overflow-y: auto; padding: 12px 16px; }
.section-actions { margin-bottom: 12px; }
.create-btn { padding: 8px 16px; background: #2563eb; color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 13px; }
.empty-state { text-align: center; padding: 24px; color: #94a3b8; font-size: 13px; }
.cache-list { display: flex; flex-direction: column; gap: 8px; }
.cache-item { display: flex; justify-content: space-between; align-items: center; padding: 10px; background: #f8fafc; border-radius: 8px; border: 1px solid #e2e8f0; }
.cache-info { flex: 1; }
.cache-info strong { font-size: 13px; }
.cache-meta { display: flex; gap: 10px; font-size: 11px; color: #94a3b8; margin-top: 4px; }
.cache-actions { display: flex; gap: 6px; }
.view-btn { padding: 5px 10px; border: 1px solid #e2e8f0; border-radius: 4px; background: white; cursor: pointer; font-size: 11px; }
.del-btn { padding: 5px 10px; border: 1px solid #fecaca; border-radius: 4px; background: white; color: #dc2626; cursor: pointer; font-size: 11px; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal { background: white; border-radius: 12px; padding: 24px; width: 400px; }
.modal h3 { font-size: 16px; margin-bottom: 16px; }
.input { width: 100%; padding: 10px; border: 1px solid #e2e8f0; border-radius: 6px; font-size: 13px; margin-bottom: 8px; box-sizing: border-box; }
.input-row { display: flex; gap: 8px; }
.input.half { width: 50%; }
.modal-actions { display: flex; gap: 8px; justify-content: flex-end; }
.modal-actions button { padding: 8px 16px; border-radius: 6px; cursor: pointer; font-size: 13px; border: none; }
.modal-actions button:first-child { background: #2563eb; color: white; }
.cancel-btn { background: #f1f5f9; color: #64748b; }
</style>
