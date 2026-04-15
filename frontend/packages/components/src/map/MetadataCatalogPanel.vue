<template>
  <div class="gn-metadata-panel">
    <div class="panel-header">
      <h3>📚 {{ t('metadata.title') || '元数据目录' }}</h3>
      <el-button type="primary" size="small" @click="showCreateDialog = true">
        {{ t('metadata.create') }}
      </el-button>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane :label="t('metadata.search')" name="search">
        <div class="search-section">
          <el-input
            v-model="searchQuery.keyword"
            :placeholder="t('metadata.searchPlaceholder')"
            clearable
            @keyup.enter="search"
          >
            <template #append>
              <el-button @click="search">{{ t('metadata.search') }}</el-button>
            </template>
          </el-input>

          <div class="filters">
            <el-select v-model="searchQuery.category" :placeholder="t('metadata.category')" clearable>
              <el-option
                v-for="cat in categories"
                :key="cat.id"
                :label="cat.name"
                :value="cat.id"
              />
            </el-select>

            <el-select v-model="searchQuery.type" :placeholder="t('metadata.type')" clearable>
              <el-option label="Raster" value="raster" />
              <el-option label="Vector" value="vector" />
              <el-option label="Table" value="table" />
            </el-select>

            <el-select v-model="searchQuery.format" :placeholder="t('metadata.format')" clearable>
              <el-option label="GeoJSON" value="geojson" />
              <el-option label="Shapefile" value="shapefile" />
              <el-option label="KML" value="kml" />
              <el-option label="GeoTIFF" value="geotiff" />
            </el-select>
          </div>

          <el-button v-if="searchResults.length > 0" type="text" @click="clearSearch">
            {{ t('common.clear') }}
          </el-button>
        </div>

        <div class="results-list">
          <div v-if="loading" class="loading">
            {{ t('common.loading') }}
          </div>

          <div v-else-if="searchResults.length === 0" class="empty">
            {{ t('common.noData') }}
          </div>

          <div
            v-else
            v-for="item in searchResults"
            :key="item.id"
            class="result-item"
            @click="viewDetail(item)"
          >
            <div class="result-title">{{ item.title }}</div>
            <div class="result-meta">
              <el-tag size="small">{{ item.category }}</el-tag>
              <el-tag size="small" type="info">{{ item.format }}</el-tag>
            </div>
            <div v-if="item.abstractText" class="result-abstract">
              {{ item.abstractText.substring(0, 100) }}...
            </div>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane :label="t('metadata.categories')" name="categories">
        <div class="categories-list">
          <div v-for="cat in categories" :key="cat.id" class="category-item">
            <div class="category-info">
              <span class="category-name">{{ cat.name }}</span>
              <span class="category-desc">{{ cat.description }}</span>
            </div>
            <span class="category-count">{{ cat.recordIds?.length || 0 }} {{ t('metadata.records') }}</span>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane :label="t('metadata.export')" name="export">
        <div class="export-section">
          <el-input
            v-model="exportId"
            :placeholder="t('metadata.enterId')"
          />
          <el-button type="primary" :loading="exporting" @click="exportISO19115">
            {{ t('metadata.exportISO') }}
          </el-button>
          <pre v-if="exportResult" class="export-result">{{ exportResult }}</pre>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="showCreateDialog" :title="t('metadata.create')" width="600px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item :label="t('metadata.title')">
          <el-input v-model="createForm.title" />
        </el-form-item>
        <el-form-item :label="t('metadata.abstract')">
          <el-input v-model="createForm.abstractText" type="textarea" rows="3" />
        </el-form-item>
        <el-form-item :label="t('metadata.category')">
          <el-select v-model="createForm.category">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('metadata.type')">
          <el-select v-model="createForm.type">
            <el-option label="Raster" value="raster" />
            <el-option label="Vector" value="vector" />
            <el-option label="Table" value="table" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('metadata.format')">
          <el-select v-model="createForm.format">
            <el-option label="GeoJSON" value="geojson" />
            <el-option label="Shapefile" value="shapefile" />
            <el-option label="KML" value="kml" />
            <el-option label="GeoTIFF" value="geotiff" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('metadata.keywords')">
          <el-input v-model="createForm.keywords" placeholder="keyword1, keyword2" />
        </el-form-item>
        <el-form-item :label="t('metadata.boundingBox')">
          <el-input v-model="boundingBoxStr" placeholder="minLon, minLat, maxLon, maxLat" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="creating" @click="createMetadata">
          {{ t('common.save') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { metadataApi } from '@/service/api'
import { ElMessage } from 'element-plus'

defineOptions({ name: 'GnMetadataCatalogPanel' })

const { t } = useI18n()

const activeTab = ref('search')
const loading = ref(false)
const creating = ref(false)
const exporting = ref(false)
const showCreateDialog = ref(false)
const searchResults = ref<any[]>([])
const categories = ref<any[]>([])
const exportResult = ref('')
const exportId = ref('')

const searchQuery = reactive({
  keyword: '',
  category: '',
  type: '',
  format: ''
})

const createForm = reactive({
  title: '',
  abstractText: '',
  category: 'vector',
  type: 'vector',
  format: 'geojson',
  keywords: ''
})

const boundingBoxStr = ref('')

onMounted(() => {
  loadCategories()
})

async function loadCategories() {
  try {
    const response = await metadataApi.getCategories()
    if (response.data?.success) {
      categories.value = response.data.data
    }
  } catch (e) {
    console.error('Failed to load categories', e)
  }
}

async function search() {
  loading.value = true
  try {
    const response = await metadataApi.search({
      keyword: searchQuery.keyword || undefined,
      category: searchQuery.category || undefined,
      type: searchQuery.type || undefined,
      format: searchQuery.format || undefined,
      page: 0,
      pageSize: 50
    })
    if (response.data?.success) {
      searchResults.value = response.data.data
    }
  } catch (e) {
    ElMessage.error(t('metadata.searchFailed'))
  } finally {
    loading.value = false
  }
}

function clearSearch() {
  searchQuery.keyword = ''
  searchQuery.category = ''
  searchQuery.type = ''
  searchQuery.format = ''
  searchResults.value = []
}

function viewDetail(item: any) {
  ElMessage.info(`Viewing: ${item.title}`)
}

async function createMetadata() {
  creating.value = true
  try {
    const record = {
      ...createForm,
      keywords: createForm.keywords ? createForm.keywords.split(',').map(k => k.trim()) : [],
      boundingBox: boundingBoxStr.value
        ? boundingBoxStr.value.split(',').map(Number)
        : undefined,
      language: 'zh-CN'
    }
    const response = await metadataApi.create(record)
    if (response.data?.success) {
      ElMessage.success(t('common.success'))
      showCreateDialog.value = false
      search()
    }
  } catch (e) {
    ElMessage.error(t('common.error'))
  } finally {
    creating.value = false
  }
}

async function exportISO19115() {
  if (!exportId.value) return
  exporting.value = true
  try {
    const response = await metadataApi.exportISO19115(exportId.value)
    if (response.data?.success) {
      exportResult.value = response.data.data
    }
  } catch (e) {
    ElMessage.error(t('metadata.exportFailed'))
  } finally {
    exporting.value = false
  }
}
</script>

<style scoped>
.gn-metadata-panel {
  padding: 16px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.panel-header h3 {
  margin: 0;
  font-size: 16px;
}

.search-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.filters {
  display: flex;
  gap: 8px;
}

.results-list {
  max-height: 400px;
  overflow-y: auto;
}

.result-item {
  padding: 12px;
  border-bottom: 1px solid var(--border-color, #e0e0e0);
  cursor: pointer;
  transition: background 0.2s;
}

.result-item:hover {
  background: var(--bg-hover, #f5f5f5);
}

.result-title {
  font-weight: 500;
  margin-bottom: 4px;
}

.result-meta {
  display: flex;
  gap: 4px;
  margin-bottom: 4px;
}

.result-abstract {
  font-size: 12px;
  color: var(--text-secondary, #666);
}

.categories-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.category-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: var(--bg-secondary, #f5f5f5);
  border-radius: 8px;
}

.category-name {
  font-weight: 500;
}

.category-desc {
  display: block;
  font-size: 12px;
  color: var(--text-secondary, #666);
}

.category-count {
  color: var(--text-secondary, #666);
  font-size: 12px;
}

.export-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.export-result {
  padding: 12px;
  background: var(--bg-secondary, #f5f5f5);
  border-radius: 8px;
  font-size: 12px;
  max-height: 300px;
  overflow: auto;
}

.loading,
.empty {
  text-align: center;
  padding: 40px;
  color: var(--text-secondary, #666);
}
</style>
