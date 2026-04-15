<template>
  <div class="tool-registry-panel">
    <div class="panel-header">
      <h3>🛠️ {{ t('tool.title') }}</h3>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <input v-model="searchKeyword" :placeholder="t('tool.searchPlaceholder')" class="input" @keyup.enter="search" />
      <button @click="search" class="search-btn">🔍</button>
    </div>

    <!-- 分类过滤 -->
    <div class="category-filter">
      <button :class="{ active: !selectedCategory }" @click="filterByCategory(null)">{{ t('tool.all') }}</button>
      <button v-for="cat in categories" :key="cat" :class="{ active: selectedCategory === cat }" @click="filterByCategory(cat)">{{ cat }}</button>
    </div>

    <!-- 工具列表 -->
    <div class="tool-list">
      <div v-for="tool in filteredTools" :key="tool.id" class="tool-item" @click="selectTool(tool)">
        <div class="tool-icon">{{ tool.icon || '🔧' }}</div>
        <div class="tool-info">
          <strong>{{ tool.name }}</strong>
          <p class="tool-desc">{{ tool.description }}</p>
          <div class="tool-meta">
            <span class="category-tag">{{ tool.category }}</span>
            <span class="type-tag" :class="tool.type">{{ tool.type }}</span>
          </div>
        </div>
      </div>
      <div v-if="filteredTools.length === 0" class="empty">{{ t('tool.empty') }}</div>
    </div>

    <!-- 注册工具 -->
    <div class="panel-footer">
      <button @click="showRegister = true" class="action-btn">+ {{ t('tool.register') }}</button>
    </div>

    <!-- 工具详情弹窗 -->
    <div v-if="selectedTool" class="modal-overlay" @click.self="selectedTool = null">
      <div class="modal tool-detail">
        <div class="modal-header">
          <span class="tool-icon-lg">{{ selectedTool.icon || '🔧' }}</span>
          <div>
            <h3>{{ selectedTool.name }}</h3>
            <div class="tool-meta">
              <span class="category-tag">{{ selectedTool.category }}</span>
              <span class="type-tag" :class="selectedTool.type">{{ selectedTool.type }}</span>
            </div>
          </div>
        </div>
        <p class="tool-desc-full">{{ selectedTool.description }}</p>
        <div v-if="selectedTool.capabilities && selectedTool.capabilities.length" class="capabilities">
          <h4>{{ t('tool.capabilities') }}</h4>
          <div class="cap-list">
            <span v-for="cap in selectedTool.capabilities" :key="cap" class="cap-tag">{{ cap }}</span>
          </div>
        </div>
        <div v-if="selectedTool.apiConfig && Object.keys(selectedTool.apiConfig).length" class="api-config">
          <h4>API Config</h4>
          <pre>{{ JSON.stringify(selectedTool.apiConfig, null, 2) }}</pre>
        </div>
        <div v-if="selectedTool.tags && selectedTool.tags.length" class="tags">
          <span v-for="tag in selectedTool.tags" :key="tag" class="tag">#{{ tag }}</span>
        </div>
        <div class="modal-actions">
          <button v-if="selectedTool.type !== 'builtin'" @click="editTool(selectedTool)" class="edit-btn">{{ t('tool.edit') }}</button>
          <button v-if="selectedTool.type !== 'builtin'" @click="deleteTool(selectedTool.id)" class="del-btn">{{ t('tool.delete') }}</button>
          <button @click="selectedTool = null" class="cancel-btn">{{ t('tool.close') }}</button>
        </div>
      </div>
    </div>

    <!-- 注册弹窗 -->
    <div v-if="showRegister" class="modal-overlay" @click.self="showRegister = false">
      <div class="modal">
        <h3>{{ t('tool.registerTool') }}</h3>
        <input v-model="newTool.name" :placeholder="t('tool.name')" class="input" />
        <input v-model="newTool.description" :placeholder="t('tool.description')" class="input" />
        <input v-model="newTool.category" :placeholder="t('tool.category')" class="input" />
        <input v-model="newTool.icon" :placeholder="t('tool.icon') + ' (e.g. 🗺️)'" class="input" />
        <div class="modal-actions">
          <button @click="registerTool">{{ t('tool.register') }}</button>
          <button @click="showRegister = false" class="cancel-btn">{{ t('tool.cancel') }}</button>
        </div>
      </div>
    </div>

    <!-- 编辑弹窗 -->
    <div v-if="editingTool" class="modal-overlay" @click.self="editingTool = null">
      <div class="modal">
        <h3>{{ t('tool.editTool') }}</h3>
        <input v-model="editingTool.name" :placeholder="t('tool.name')" class="input" />
        <input v-model="editingTool.description" :placeholder="t('tool.description')" class="input" />
        <input v-model="editingTool.category" :placeholder="t('tool.category')" class="input" />
        <input v-model="editingTool.icon" :placeholder="t('tool.icon')" class="input" />
        <div class="modal-actions">
          <button @click="updateTool">{{ t('tool.save') }}</button>
          <button @click="editingTool = null" class="cancel-btn">{{ t('tool.cancel') }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'

const t = (key: string) => key
const tools = ref<any[]>([])
const searchKeyword = ref('')
const selectedCategory = ref<string | null>(null)
const selectedTool = ref<any>(null)
const showRegister = ref(false)
const editingTool = ref<any>(null)
const newTool = ref({ name: '', description: '', category: '', icon: '' })

const categories = ['map', 'analysis', 'data', 'ai', 'external', 'custom']

const filteredTools = computed(() => {
  let result = tools.value
  if (selectedCategory.value) {
    result = result.filter((t: any) => t.category === selectedCategory.value)
  }
  return result
})

onMounted(() => fetchTools())

async function fetchTools() {
  try {
    const { data } = await apiClient.get('/tools')
    tools.value = data
  } catch {}
}

async function search() {
  if (!searchKeyword.value.trim()) {
    await fetchTools()
    return
  }
  try {
    const { data } = await apiClient.get(`/tools/search?keyword=${encodeURIComponent(searchKeyword.value)}`)
    tools.value = data
  } catch {}
}

function filterByCategory(cat: string | null) {
  selectedCategory.value = cat
}

function selectTool(tool: any) {
  selectedTool.value = tool
}

function editTool(tool: any) {
  editingTool.value = { ...tool }
  selectedTool.value = null
}

async function registerTool() {
  try {
    await apiClient.post('/tools', newTool.value)
    newTool.value = { name: '', description: '', category: '', icon: '' }
    showRegister.value = false
    await fetchTools()
  } catch {}
}

async function updateTool() {
  if (!editingTool.value) return
  try {
    await apiClient.put(`/tools/${editingTool.value.id}`, editingTool.value)
    editingTool.value = null
    await fetchTools()
  } catch {}
}

async function deleteTool(id: string) {
  if (!confirm(t('tool.confirmDelete'))) return
  await apiClient.delete(`/tools/${id}`)
  selectedTool.value = null
  await fetchTools()
}
</script>

<style scoped>
.tool-registry-panel { background: white; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.15); max-height: 80vh; overflow: hidden; display: flex; flex-direction: column; }
.panel-header { padding: 12px 16px; border-bottom: 1px solid #e2e8f0; }
.panel-header h3 { font-size: 15px; font-weight: 600; }
.search-bar { display: flex; gap: 8px; padding: 10px 16px; border-bottom: 1px solid #f1f5f9; }
.search-bar .input { flex: 1; padding: 8px 10px; border: 1px solid #e2e8f0; border-radius: 6px; font-size: 13px; }
.search-btn { padding: 8px 12px; background: #f1f5f9; border: none; border-radius: 6px; cursor: pointer; }
.category-filter { display: flex; gap: 4px; padding: 8px 16px; border-bottom: 1px solid #f1f5f9; flex-wrap: wrap; }
.category-filter button { padding: 4px 10px; border: 1px solid #e2e8f0; border-radius: 20px; background: white; cursor: pointer; font-size: 12px; color: #64748b; }
.category-filter button.active { background: #2563eb; color: white; border-color: #2563eb; }
.tool-list { flex: 1; overflow-y: auto; padding: 12px 16px; display: flex; flex-direction: column; gap: 8px; }
.tool-item { display: flex; gap: 10px; padding: 10px; background: #f8fafc; border-radius: 8px; border: 1px solid #e2e8f0; cursor: pointer; transition: border-color 0.2s; }
.tool-item:hover { border-color: #2563eb; }
.tool-icon { font-size: 24px; flex-shrink: 0; width: 32px; text-align: center; }
.tool-info { flex: 1; min-width: 0; }
.tool-info strong { font-size: 13px; display: block; margin-bottom: 2px; }
.tool-desc { font-size: 11px; color: #64748b; margin: 0 0 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tool-meta { display: flex; gap: 4px; flex-wrap: wrap; }
.category-tag { padding: 2px 6px; background: #f0fdf4; color: #16a34a; border-radius: 4px; font-size: 10px; }
.type-tag.builtin { background: #dbeafe; color: #1d4ed8; padding: 2px 6px; border-radius: 4px; font-size: 10px; }
.type-tag.external { background: #fef3c7; color: #d97706; padding: 2px 6px; border-radius: 4px; font-size: 10px; }
.type-tag.custom { background: #f3e8ff; color: #7c3aed; padding: 2px 6px; border-radius: 4px; font-size: 10px; }
.empty { text-align: center; color: #94a3b8; padding: 24px; font-size: 13px; }
.panel-footer { padding: 10px 16px; border-top: 1px solid #e2e8f0; }
.action-btn { width: 100%; padding: 10px; background: #2563eb; color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 13px; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal { background: white; border-radius: 12px; padding: 20px; width: 420px; max-height: 80vh; overflow-y: auto; }
.modal h3 { font-size: 15px; margin-bottom: 12px; }
.tool-detail .modal-header { display: flex; gap: 12px; align-items: center; margin-bottom: 12px; }
.tool-icon-lg { font-size: 36px; }
.tool-detail .tool-desc-full { font-size: 13px; color: #475569; margin-bottom: 12px; line-height: 1.5; }
.capabilities h4, .api-config h4 { font-size: 12px; color: #64748b; margin-bottom: 6px; }
.cap-list { display: flex; flex-wrap: wrap; gap: 4px; margin-bottom: 12px; }
.cap-tag { padding: 3px 8px; background: #f1f5f9; color: #475569; border-radius: 4px; font-size: 11px; }
.api-config pre { background: #f8fafc; padding: 8px; border-radius: 6px; font-size: 11px; overflow-x: auto; margin-bottom: 12px; }
.tags { display: flex; flex-wrap: wrap; gap: 4px; margin-bottom: 12px; }
.tag { font-size: 11px; color: #64748b; }
.input { width: 100%; padding: 9px; border: 1px solid #e2e8f0; border-radius: 6px; font-size: 13px; margin-bottom: 8px; box-sizing: border-box; }
.modal-actions { display: flex; gap: 8px; justify-content: flex-end; margin-top: 12px; }
.modal-actions button { padding: 8px 16px; border-radius: 6px; cursor: pointer; font-size: 13px; border: none; }
.modal-actions button:first-child { background: #2563eb; color: white; }
.edit-btn { background: #7c3aed; color: white; }
.del-btn { background: #dc2626; color: white; }
.cancel-btn { background: #f1f5f9; color: #64748b; }
</style>
