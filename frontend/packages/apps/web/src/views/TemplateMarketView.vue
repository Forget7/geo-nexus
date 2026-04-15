<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { templateApi } from '@/service/api'

const { t } = useI18n()
const router = useRouter()

const loading = ref(false)
const templates = ref<any[]>([])
const activeCategory = ref('all')

interface Category {
  id: string
  icon: string
}

const categories: Category[] = [
  { id: 'all', icon: '🌐' },
  { id: 'government', icon: '🏛️' },
  { id: 'emergency', icon: '🚨' },
  { id: 'traffic', icon: '🚗' },
  { id: 'environment', icon: '🌿' },
  { id: 'population', icon: '👥' }
]

const filteredTemplates = computed(() => {
  if (activeCategory.value === 'all') {
    return templates.value
  }
  return templates.value.filter(t => t.category === activeCategory.value)
})

const categoryName = (categoryId: string) => {
  if (categoryId === 'all') return t('template.categories.all')
  return t(`template.categories.${categoryId}`)
}

const fetchTemplates = async () => {
  loading.value = true
  try {
    const res = await templateApi.getTemplates()
    templates.value = res.data || []
  } catch (e) {
    console.error('Failed to fetch templates', e)
  } finally {
    loading.value = false
  }
}

const applyTemplate = async (tmpl: any) => {
  try {
    const res = await templateApi.applyTemplate(tmpl.id, 'anonymous')
    if (res.data?.url) {
      router.push('/map')
    }
  } catch (e) {
    console.error('Failed to apply template', e)
  }
}

const getThumbnail = (tmpl: any) => {
  return tmpl.thumbnail || `/templates/${tmpl.id}.png`
}

onMounted(() => {
  fetchTemplates()
})
</script>

<template>
  <div class="template-market">
    <div class="market-header">
      <h2>🗺️ {{ t('template.marketTitle') }}</h2>
      <p>{{ t('template.subtitle') }}</p>
    </div>
    
    <!-- 分类筛选 -->
    <div class="category-tabs">
      <button 
        v-for="cat in categories" 
        :key="cat.id"
        :class="{ active: activeCategory === cat.id }"
        @click="activeCategory = cat.id"
      >
        {{ cat.icon }} {{ categoryName(cat.id) }}
      </button>
    </div>
    
    <!-- 模板卡片网格 -->
    <div v-if="!loading && filteredTemplates.length > 0" class="template-grid">
      <div 
        v-for="tmpl in filteredTemplates" 
        :key="tmpl.id"
        class="template-card"
        @click="applyTemplate(tmpl)"
      >
        <div class="card-thumbnail">
          <div class="thumbnail-placeholder">
            <span class="placeholder-icon">🗺️</span>
            <span class="placeholder-name">{{ tmpl.name }}</span>
          </div>
          <div class="card-overlay">
            <button class="apply-btn">{{ t('template.apply') }}</button>
          </div>
        </div>
        <div class="card-body">
          <h3>{{ tmpl.name }}</h3>
          <p class="description">{{ tmpl.description }}</p>
          <div class="card-meta">
            <span class="tag">{{ categoryName(tmpl.category) }}</span>
            <span class="zoom-level">🔍 {{ tmpl.zoom }}x</span>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 空状态 -->
    <div v-if="!loading && filteredTemplates.length === 0" class="empty-state">
      <p>{{ t('common.noData') }}</p>
    </div>
    
    <!-- 加载骨架 -->
    <div v-if="loading" class="template-grid">
      <div v-for="i in 6" :key="i" class="skeleton-card">
        <div class="skeleton-thumbnail"></div>
        <div class="skeleton-body">
          <div class="skeleton-title"></div>
          <div class="skeleton-desc"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.template-market {
  min-height: 100vh;
  background: #f8fafc;
  padding: 2rem;
}

.market-header {
  text-align: center;
  margin-bottom: 2rem;
}

.market-header h2 {
  font-size: 2rem;
  color: #1e293b;
  margin: 0 0 0.5rem;
}

.market-header p {
  color: #64748b;
  font-size: 1rem;
  margin: 0;
}

.category-tabs {
  display: flex;
  gap: 0.5rem;
  justify-content: center;
  flex-wrap: wrap;
  margin-bottom: 2rem;
}

.category-tabs button {
  padding: 0.5rem 1rem;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  font-size: 0.875rem;
  color: #475569;
  cursor: pointer;
  transition: all 0.2s;
}

.category-tabs button:hover {
  background: #f1f5f9;
}

.category-tabs button.active {
  background: #2563eb;
  color: white;
  border-color: #2563eb;
}

.template-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1.5rem;
  max-width: 1200px;
  margin: 0 auto;
}

.template-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: all 0.2s;
}

.template-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.card-thumbnail {
  position: relative;
  height: 180px;
  background: linear-gradient(135deg, #1e3a5f 0%, #0f1c2e 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.thumbnail-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  color: white;
}

.placeholder-icon {
  font-size: 3rem;
}

.placeholder-name {
  font-size: 0.875rem;
  opacity: 0.8;
}

.card-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
}

.template-card:hover .card-overlay {
  opacity: 1;
}

.apply-btn {
  padding: 0.625rem 1.5rem;
  background: #2563eb;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
}

.card-body {
  padding: 1rem;
}

.card-body h3 {
  margin: 0 0 0.5rem;
  font-size: 1rem;
  color: #1e293b;
}

.description {
  margin: 0 0 0.75rem;
  font-size: 0.875rem;
  color: #64748b;
  line-height: 1.4;
}

.card-meta {
  display: flex;
  gap: 0.5rem;
  align-items: center;
}

.tag {
  padding: 0.25rem 0.625rem;
  background: #f1f5f9;
  border-radius: 12px;
  font-size: 0.75rem;
  color: #64748b;
}

.zoom-level {
  font-size: 0.75rem;
  color: #94a3b8;
}

.empty-state {
  text-align: center;
  padding: 3rem;
  color: #64748b;
}

/* Skeleton */
.skeleton-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
}

.skeleton-thumbnail {
  height: 180px;
  background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.skeleton-body {
  padding: 1rem;
}

.skeleton-title {
  height: 20px;
  width: 60%;
  background: #f1f5f9;
  border-radius: 4px;
  margin-bottom: 0.5rem;
}

.skeleton-desc {
  height: 14px;
  width: 90%;
  background: #f1f5f9;
  border-radius: 4px;
}

@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}
</style>
