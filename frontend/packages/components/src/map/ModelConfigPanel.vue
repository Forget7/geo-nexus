<template>
  <div class="model-panel">
    <div class="panel-header">
      <h3>🤖 {{ t('model.title') }}</h3>
      <div class="tabs">
        <button :class="{ active: tab === 'providers' }" @click="tab = 'providers'">{{ t('model.providers') }}</button>
        <button :class="{ active: tab === 'configs' }" @click="tab = 'configs'">{{ t('model.configs') }}</button>
      </div>
    </div>

    <!-- 提供商 -->
    <div v-if="tab === 'providers'" class="tab-content">
      <div class="section-actions">
        <button @click="showAddProvider = true" class="create-btn">+ {{ t('model.addProvider') }}</button>
      </div>
      <div class="provider-list">
        <div v-for="p in providers" :key="p.id" class="provider-card">
          <div class="provider-info">
            <strong>{{ p.name }}</strong>
            <span class="provider-type">{{ p.id }}</span>
          </div>
          <div class="provider-actions">
            <button @click="deleteProvider(p.id)" class="del-btn">{{ t('model.delete') }}</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 模型配置 -->
    <div v-if="tab === 'configs'" class="tab-content">
      <div class="section-actions">
        <button @click="showAddConfig = true" class="create-btn">+ {{ t('model.addConfig') }}</button>
      </div>
      <div class="config-list">
        <div v-for="cfg in configs" :key="cfg.id" class="config-card">
          <div class="config-info">
            <strong>{{ cfg.name || cfg.model }}</strong>
            <p class="config-model">{{ cfg.model }} @ {{ cfg.provider }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 弹窗 -->
    <div v-if="showAddProvider || showAddConfig" class="modal-overlay" @click.self="showAddProvider = false; showAddConfig = false">
      <div class="modal">
        <h3>{{ showAddProvider ? t('model.addProvider') : t('model.addConfig') }}</h3>
        <input v-model="form.name" :placeholder="t('model.name')" class="input" />
        <input v-if="showAddProvider" v-model="form.id" :placeholder="t('model.type')" class="input" />
        <input v-if="showAddConfig" v-model="form.provider" :placeholder="t('model.type')" class="input" />
        <input v-model="form.baseUrl" :placeholder="t('model.baseUrl')" class="input" />
        <input v-if="showAddProvider" v-model="form.apiKey" type="password" :placeholder="t('model.apiKey')" class="input" />
        <div class="modal-actions">
          <button @click="submitForm">{{ t('model.save') }}</button>
          <button @click="showAddProvider = false; showAddConfig = false" class="cancel-btn">{{ t('model.cancel') }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

const t = (key: string) => key
const tab = ref('providers')
const providers = ref<any[]>([])
const configs = ref<any[]>([])
const showAddProvider = ref(false)
const showAddConfig = ref(false)
const form = ref<any>({})

onMounted(() => { fetchProviders(); fetchConfigs() })

async function fetchProviders() {
  try {
    const { data } = await apiClient.get('/models/providers')
    providers.value = data
  } catch {}
}

async function fetchConfigs() {
  try {
    const { data } = await apiClient.get('/models/configs')
    configs.value = data
  } catch {}
}

async function deleteProvider(id: string) {
  if (!confirm(t('model.confirmDelete'))) return
  await apiClient.delete(`/models/providers/${id}`)
  await fetchProviders()
}

async function submitForm() {
  if (showAddProvider.value) {
    await apiClient.post('/models/providers', form.value)
    showAddProvider.value = false
    await fetchProviders()
  } else {
    await apiClient.post('/models/configs', form.value)
    showAddConfig.value = false
    await fetchConfigs()
  }
  form.value = {}
}
</script>

<style scoped>
.model-panel { background: white; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.15); max-height: 80vh; overflow: hidden; display: flex; flex-direction: column; }
.panel-header { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; border-bottom: 1px solid #e2e8f0; }
.panel-header h3 { font-size: 15px; font-weight: 600; }
.tabs { display: flex; gap: 4px; }
.tabs button { padding: 5px 12px; border: 1px solid #e2e8f0; border-radius: 6px; background: white; cursor: pointer; font-size: 12px; }
.tabs button.active { background: #2563eb; color: white; border-color: #2563eb; }
.tab-content { flex: 1; overflow-y: auto; padding: 12px 16px; }
.section-actions { margin-bottom: 12px; }
.create-btn { padding: 7px 14px; background: #2563eb; color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 12px; }
.provider-list, .config-list { display: flex; flex-direction: column; gap: 8px; }
.provider-card, .config-card { display: flex; justify-content: space-between; align-items: center; padding: 10px; background: #f8fafc; border-radius: 8px; border: 1px solid #e2e8f0; }
.provider-info, .config-info { flex: 1; }
.provider-info strong, .config-info strong { font-size: 13px; }
.provider-type { margin-left: 8px; padding: 2px 8px; background: #e0e7ff; color: #3730a3; border-radius: 4px; font-size: 11px; }
.config-model { font-size: 12px; color: #64748b; margin: 4px 0 0; }
.del-btn { padding: 4px 10px; border: 1px solid #fecaca; border-radius: 4px; background: white; color: #dc2626; cursor: pointer; font-size: 11px; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal { background: white; border-radius: 12px; padding: 24px; width: 400px; }
.modal h3 { font-size: 16px; margin-bottom: 16px; }
.input { width: 100%; padding: 10px; border: 1px solid #e2e8f0; border-radius: 6px; font-size: 13px; margin-bottom: 8px; box-sizing: border-box; }
.modal-actions { display: flex; gap: 8px; justify-content: flex-end; }
.modal-actions button { padding: 8px 16px; border-radius: 6px; cursor: pointer; font-size: 13px; border: none; }
.modal-actions button:first-child { background: #2563eb; color: white; }
.cancel-btn { background: #f1f5f9; color: #64748b; }
</style>
