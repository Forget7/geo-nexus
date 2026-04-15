<template>
  <div class="kb-panel">
    <div class="panel-header">
      <h3>📚 {{ t('kb.title') }}</h3>
      <button class="close-btn" @click="$emit('close')">×</button>
    </div>

    <!-- Tab 切换 -->
    <div class="tab-bar">
      <button :class="{ active: activeTab === 'entries' }" @click="activeTab = 'entries'">
        📖 {{ t('kb.tabEntries') }}
      </button>
      <button :class="{ active: activeTab === 'kg' }" @click="activeTab = 'kg'">
        🕸️ {{ t('kb.tabKG') }}
      </button>
    </div>

    <!-- 知识库 Tab -->
    <div v-if="activeTab === 'entries'" class="tab-content">
      <!-- 搜索区域 -->
      <div class="search-section">
        <input v-model="searchKeyword" :placeholder="t('kb.searchPlaceholder')" class="search-input" />
        <select v-model="searchCategory" class="category-select">
          <option value="">{{ t('kb.allCategories') }}</option>
          <option value="GIS基础">{{ t('kb.catGIS') }}</option>
          <option value="空间分析">{{ t('kb.catSpatial') }}</option>
          <option value="数据管理">{{ t('kb.catData') }}</option>
          <option value="开发指南">{{ t('kb.catDev') }}</option>
          <option value="行业应用">{{ t('kb.catIndustry') }}</option>
        </select>
        <button @click="searchEntries" class="search-btn">🔍</button>
        <button @click="showAddEntry = !showAddEntry" class="add-btn">+ {{ t('kb.add') }}</button>
      </div>

      <!-- 添加知识条目表单 -->
      <div v-if="showAddEntry" class="entry-form">
        <input v-model="newEntry.title" :placeholder="t('kb.entryTitle')" class="form-input" />
        <textarea v-model="newEntry.content" :placeholder="t('kb.entryContent')" class="form-textarea"></textarea>
        <input v-model="newEntry.description" :placeholder="t('kb.entryDesc')" class="form-input" />
        <select v-model="newEntry.category" class="form-select">
          <option value="">{{ t('kb.selectCategory') }}</option>
          <option value="GIS基础">{{ t('kb.catGIS') }}</option>
          <option value="空间分析">{{ t('kb.catSpatial') }}</option>
          <option value="数据管理">{{ t('kb.catData') }}</option>
          <option value="开发指南">{{ t('kb.catDev') }}</option>
          <option value="行业应用">{{ t('kb.catIndustry') }}</option>
        </select>
        <input v-model="newEntry.tags" :placeholder="t('kb.entryTags')" class="form-input" />
        <div class="form-actions">
          <button @click="submitEntry" class="submit-btn">{{ t('kb.submit') }}</button>
          <button @click="showAddEntry = false" class="cancel-btn">{{ t('kb.cancel') }}</button>
        </div>
      </div>

      <!-- 知识条目列表 -->
      <div class="entry-list">
        <div v-if="entries.length === 0" class="empty-state">
          {{ t('kb.noEntries') }}
        </div>
        <div v-for="entry in entries" :key="entry.id" class="entry-card" @click="selectEntry(entry)">
          <div class="entry-header">
            <strong>{{ entry.title }}</strong>
            <span class="entry-category">{{ entry.category }}</span>
          </div>
          <p class="entry-desc">{{ entry.description || entry.content.substring(0, 100) }}...</p>
          <div class="entry-meta">
            <span>{{ t('kb.views') }}: {{ entry.viewCount || 0 }}</span>
            <span>{{ t('kb.rating') }}: {{ entry.rating || 0 }}</span>
            <span>{{ formatTime(entry.updatedAt) }}</span>
          </div>
          <div v-if="entry.tags && entry.tags.length" class="entry-tags">
            <span v-for="tag in entry.tags" :key="tag" class="tag">{{ tag }}</span>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="totalPages > 1" class="pagination">
        <button @click="prevPage" :disabled="currentPage === 0">{{ t('kb.prev') }}</button>
        <span>{{ currentPage + 1 }} / {{ totalPages }}</span>
        <button @click="nextPage" :disabled="currentPage >= totalPages - 1">{{ t('kb.next') }}</button>
      </div>
    </div>

    <!-- 知识图谱 Tab -->
    <div v-if="activeTab === 'kg'" class="tab-content">
      <div class="kg-toolbar">
        <button @click="showAddEntity = !showAddEntity" class="add-btn">+ {{ t('kb.addEntity') }}</button>
        <button @click="showAddRelation = !showAddRelation" class="add-btn">+ {{ t('kb.addRelation') }}</button>
      </div>

      <!-- 添加实体 -->
      <div v-if="showAddEntity" class="kg-form">
        <h4>{{ t('kb.newEntity') }}</h4>
        <input v-model="newEntity.name" :placeholder="t('kb.entityName')" class="form-input" />
        <select v-model="newEntity.type" class="form-select">
          <option value="">{{ t('kb.selectType') }}</option>
          <option value="城市">{{ t('kb.typeCity') }}</option>
          <option value="河流">{{ t('kb.typeRiver') }}</option>
          <option value="山脉">{{ t('kb.typeMountain') }}</option>
          <option value="建筑">{{ t('kb.typeBuilding') }}</option>
          <option value="事件">{{ t('kb.typeEvent') }}</option>
          <option value="概念">{{ t('kb.typeConcept') }}</option>
        </select>
        <div class="form-actions">
          <button @click="submitEntity" class="submit-btn">{{ t('kb.submit') }}</button>
          <button @click="showAddEntity = false" class="cancel-btn">{{ t('kb.cancel') }}</button>
        </div>
      </div>

      <!-- 添加关系 -->
      <div v-if="showAddRelation" class="kg-form">
        <h4>{{ t('kb.newRelation') }}</h4>
        <input v-model="newRelation.sourceId" :placeholder="t('kb.sourceEntity')" class="form-input" />
        <input v-model="newRelation.targetId" :placeholder="t('kb.targetEntity')" class="form-input" />
        <select v-model="newRelation.type" class="form-select">
          <option value="">{{ t('kb.selectRelationType') }}</option>
          <option value="位于">{{ t('kb.relLocated') }}</option>
          <option value="流入">{{ t('kb.relFlowsInto') }}</option>
          <option value="相邻">{{ t('kb.relAdjacent') }}</option>
          <option value="属于">{{ t('kb.relBelongsTo') }}</option>
          <option value="相关">{{ t('kb.relRelated') }}</option>
        </select>
        <div class="form-actions">
          <button @click="submitRelation" class="submit-btn">{{ t('kb.submit') }}</button>
          <button @click="showAddRelation = false" class="cancel-btn">{{ t('kb.cancel') }}</button>
        </div>
      </div>

      <!-- 实体列表 -->
      <div class="entity-list">
        <div v-if="entities.length === 0" class="empty-state">
          {{ t('kb.noEntities') }}
        </div>
        <div v-for="entity in entities" :key="entity.id" class="entity-card" @click="selectEntity(entity)">
          <strong>{{ entity.name }}</strong>
          <span class="entity-type">{{ entity.type }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from '@/plugins/i18n'

const { t } = useI18n()

const emit = defineEmits(['close'])

const activeTab = ref<'entries' | 'kg'>('entries')

// 知识库状态
const searchKeyword = ref('')
const searchCategory = ref('')
const entries = ref<any[]>([])
const currentPage = ref(0)
const pageSize = ref(10)
const showAddEntry = ref(false)

const newEntry = ref({
  title: '',
  content: '',
  description: '',
  category: '',
  tags: ''
})

// 知识图谱状态
const entities = ref<any[]>([])
const relations = ref<any[]>([])
const showAddEntity = ref(false)
const showAddRelation = ref(false)

const newEntity = ref({ name: '', type: '' })
const newRelation = ref({ sourceId: '', targetId: '', type: '' })

const totalPages = computed(() => Math.ceil(entries.value.length / pageSize.value) || 1)

// 搜索知识条目
async function searchEntries() {
  try {
    const response = await fetch('/api/v1/knowledge/search', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        keyword: searchKeyword.value,
        category: searchCategory.value || undefined,
        page: currentPage.value,
        pageSize: pageSize.value
      })
    })
    const result = await response.json()
    if (result.success) {
      entries.value = result.data || []
    }
  } catch (error) {
    console.error('Search failed:', error)
  }
}

// 提交新条目
async function submitEntry() {
  try {
    const tags = newEntry.value.tags ? newEntry.value.tags.split(',').map((t: string) => t.trim()) : []
    const response = await fetch('/api/v1/knowledge/entries', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ ...newEntry.value, tags })
    })
    const result = await response.json()
    if (result.success) {
      entries.value.unshift(result.data)
      showAddEntry.value = false
      newEntry.value = { title: '', content: '', description: '', category: '', tags: '' }
    }
  } catch (error) {
    console.error('Add entry failed:', error)
  }
}

// 选择条目
async function selectEntry(entry: any) {
  try {
    const response = await fetch(`/api/v1/knowledge/entries/${entry.id}`)
    const result = await response.json()
    if (result.success) {
      entry.viewCount = (entry.viewCount || 0) + 1
    }
  } catch (error) {
    console.error('Get entry failed:', error)
  }
}

// 分页
function prevPage() {
  if (currentPage.value > 0) {
    currentPage.value--
    searchEntries()
  }
}

function nextPage() {
  if (currentPage.value < totalPages.value - 1) {
    currentPage.value++
    searchEntries()
  }
}

// 提交实体
async function submitEntity() {
  try {
    const response = await fetch('/api/v1/knowledge/kg/entities', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(newEntity.value)
    })
    const result = await response.json()
    if (result.success) {
      entities.value.unshift(result.data)
      showAddEntity.value = false
      newEntity.value = { name: '', type: '' }
    }
  } catch (error) {
    console.error('Add entity failed:', error)
  }
}

// 提交关系
async function submitRelation() {
  try {
    const response = await fetch('/api/v1/knowledge/kg/relations', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(newRelation.value)
    })
    const result = await response.json()
    if (result.success) {
      relations.value.unshift(result.data)
      showAddRelation.value = false
      newRelation.value = { sourceId: '', targetId: '', type: '' }
    }
  } catch (error) {
    console.error('Add relation failed:', error)
  }
}

// 选择实体
function selectEntity(entity: any) {
  fetchEntityRelations(entity.id)
}

// 获取实体关系
async function fetchEntityRelations(entityId: string) {
  try {
    const response = await fetch(`/api/v1/knowledge/kg/entities/${entityId}/relations`)
    const result = await response.json()
    if (result.success) {
      console.log('Relations:', result.data)
    }
  } catch (error) {
    console.error('Get relations failed:', error)
  }
}

// 格式化时间
function formatTime(timestamp: number): string {
  if (!timestamp) return '-'
  return new Date(timestamp).toLocaleString()
}

// 初始化加载
searchEntries()
</script>

<style scoped>
.kb-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--bg-primary, #fff);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-color, #e0e0e0);
}

.panel-header h3 {
  margin: 0;
  font-size: 16px;
}

.close-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  padding: 4px 8px;
}

.tab-bar {
  display: flex;
  border-bottom: 1px solid var(--border-color, #e0e0e0);
}

.tab-bar button {
  flex: 1;
  padding: 10px;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 14px;
}

.tab-bar button.active {
  background: var(--bg-secondary, #f5f5f5);
  border-bottom: 2px solid var(--primary-color, #007bff);
}

.tab-content {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.search-section {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.search-input {
  flex: 1;
  padding: 8px;
  border: 1px solid var(--border-color, #e0e0e0);
  border-radius: 4px;
}

.category-select, .form-select {
  padding: 8px;
  border: 1px solid var(--border-color, #e0e0e0);
  border-radius: 4px;
}

.search-btn, .add-btn {
  padding: 8px 12px;
  background: var(--primary-color, #007bff);
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.entry-form, .kg-form {
  background: var(--bg-secondary, #f5f5f5);
  padding: 12px;
  border-radius: 4px;
  margin-bottom: 12px;
}

.form-input, .form-textarea {
  width: 100%;
  padding: 8px;
  margin-bottom: 8px;
  border: 1px solid var(--border-color, #e0e0e0);
  border-radius: 4px;
  box-sizing: border-box;
}

.form-textarea {
  min-height: 80px;
  resize: vertical;
}

.form-actions {
  display: flex;
  gap: 8px;
}

.submit-btn {
  padding: 8px 16px;
  background: var(--primary-color, #007bff);
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.cancel-btn {
  padding: 8px 16px;
  background: var(--bg-secondary, #f5f5f5);
  border: 1px solid var(--border-color, #e0e0e0);
  border-radius: 4px;
  cursor: pointer;
}

.entry-list, .entity-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.entry-card, .entity-card {
  padding: 12px;
  background: var(--bg-secondary, #f5f5f5);
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.2s;
}

.entry-card:hover, .entity-card:hover {
  background: var(--border-color, #e0e0e0);
}

.entry-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
}

.entry-category, .entity-type {
  font-size: 12px;
  padding: 2px 6px;
  background: var(--primary-color, #007bff);
  color: white;
  border-radius: 2px;
}

.entry-desc {
  font-size: 13px;
  color: var(--text-secondary, #666);
  margin: 4px 0;
}

.entry-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: var(--text-secondary, #999);
  margin-top: 4px;
}

.entry-tags {
  display: flex;
  gap: 4px;
  margin-top: 4px;
  flex-wrap: wrap;
}

.tag {
  font-size: 11px;
  padding: 2px 6px;
  background: var(--bg-tertiary, #eee);
  border-radius: 2px;
}

.kg-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.empty-state {
  text-align: center;
  padding: 24px;
  color: var(--text-secondary, #999);
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
}

.pagination button {
  padding: 6px 12px;
  border: 1px solid var(--border-color, #e0e0e0);
  background: white;
  border-radius: 4px;
  cursor: pointer;
}

.pagination button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>