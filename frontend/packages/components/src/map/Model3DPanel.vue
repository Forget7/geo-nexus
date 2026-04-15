<template>
  <div class="model3d-panel">
    <div class="panel-header">
      <h3>🎮 {{ t('3d.title') }}</h3>
      <button @click="showUpload = true" class="create-btn">+ {{ t('3d.upload') }}</button>
    </div>
    <div class="model-grid">
      <div v-for="m in models" :key="m.id" class="model-card">
        <div class="model-preview">🎮</div>
        <div class="model-info">
          <strong>{{ m.name }}</strong>
          <span class="model-format">{{ m.format || m.type }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
const t = (key: string) => key
const models = ref<any[]>([])
const showUpload = ref(false)
onMounted(async () => {
  try { const { data } = await apiClient.get('/models-3d'); models.value = data } catch {}
})
</script>

<style scoped>
.model3d-panel { background: white; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.15); }
.panel-header { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; border-bottom: 1px solid #e2e8f0; }
.panel-header h3 { font-size: 15px; font-weight: 600; }
.create-btn { padding: 7px 14px; background: #2563eb; color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 12px; }
.model-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px; padding: 12px; }
.model-card { background: #f8fafc; border-radius: 8px; border: 1px solid #e2e8f0; overflow: hidden; }
.model-preview { height: 60px; background: #e2e8f0; display: flex; align-items: center; justify-content: center; font-size: 28px; }
.model-info { padding: 8px; }
.model-info strong { font-size: 12px; display: block; }
.model-format { font-size: 11px; color: #94a3b8; }
</style>
