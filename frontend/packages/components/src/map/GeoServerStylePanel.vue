<template>
  <div class="style-panel">
    <div class="panel-header">
      <h3>🎨 {{ t('style.title') }}</h3>
    </div>

    <div class="style-list">
      <div v-for="s in styles" :key="s.name" class="style-item">
        <div class="style-preview" :style="{ background: s.previewColor || '#e2e8f0' }">
          <span>🎨</span>
        </div>
        <div class="style-info">
          <strong>{{ s.name }}</strong>
          <span class="style-ws">{{ s.workspace }}</span>
        </div>
        <div class="style-actions">
          <button @click="deleteStyle(s.workspace, s.name)" class="del-btn">🗑️</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
const t = (key: string) => key
const styles = ref<any[]>([])
onMounted(async () => {
  try { const { data } = await apiClient.get('/geoserver/styles'); styles.value = data } catch {}
})
async function deleteStyle(ws: string, name: string) {
  if (!confirm(t('style.confirmDelete'))) return
  await apiClient.delete(`/geoserver/styles/${ws}/${name}`)
  styles.value = styles.value.filter(s => !(s.workspace === ws && s.name === name))
}
</script>

<style scoped>
.style-panel { background: white; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.15); }
.panel-header { padding: 12px 16px; border-bottom: 1px solid #e2e8f0; }
.panel-header h3 { font-size: 15px; font-weight: 600; }
.style-list { padding: 12px 16px; display: flex; flex-direction: column; gap: 8px; }
.style-item { display: flex; gap: 10px; align-items: center; padding: 8px; background: #f8fafc; border-radius: 8px; border: 1px solid #e2e8f0; }
.style-preview { width: 40px; height: 40px; border-radius: 6px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.style-info { flex: 1; }
.style-info strong { font-size: 13px; display: block; }
.style-ws { font-size: 11px; color: #94a3b8; }
.del-btn { padding: 4px 8px; background: none; border: 1px solid #fecaca; border-radius: 4px; cursor: pointer; font-size: 12px; }
</style>
