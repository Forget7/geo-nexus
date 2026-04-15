<script setup lang="ts">
/**
 * MapSharePanel - 地图分享面板
 * 将GIS专家系统创建的地图发布为可分享服务
 */

import { ref, computed } from 'vue'
import { api } from '../../../src/api/client'
import { useClipboard } from '@geonex/hooks/useClipboard'
import UnifiedMapPanel from './UnifiedMapPanel.vue'

const props = defineProps<{
  mapConfig?: any
  mapId?: string
}>()

const emit = defineEmits<{
  (e: 'published', result: any): void
  (e: 'error', error: any): void
}>()

// 分享状态
const isPublishing = ref(false)
const publishedResult = ref<any>(null)
const publishError = ref('')

// 发布表单
const publishForm = ref({
  name: '',
  description: '',
  publishType: 'public', // public, link, embed, ogc
  expiryDays: 7,
  style: null,
  embedOptions: {
    width: 800,
    height: 600,
    title: 'GeoNexus Map',
    responsive: true,
    showControls: true,
    showSidebar: false
  }
})

// 发布类型选项
const publishTypes = [
  { value: 'public', label: '公开展示', icon: '🌐', desc: '任何人都可以查看' },
  { value: 'link', label: '链接分享', icon: '🔗', desc: '通过链接访问，需要Token' },
  { value: 'embed', label: '嵌入网站', icon: '📥', desc: '可嵌入到其他网站' },
  { value: 'ogc', label: 'OGC服务', icon: '🗂️', desc: '发布为WMS/WMTS标准服务' }
]

// 过期时间选项
const expiryOptions = [
  { value: 1, label: '1天' },
  { value: 7, label: '7天' },
  { value: 30, label: '30天' },
  { value: 90, label: '90天' },
  { value: 365, label: '1年' }
]

// 剪贴板
const { copied, copy } = useClipboard()

// 计算属性
const shareUrl = computed(() => publishedResult.value?.shareUrl || '')
const embedCode = computed(() => publishedResult.value?.embedIframe || '')
const qrCodeUrl = computed(() => publishedResult.value?.qrCodeUrl || '')

// 发布地图
async function publishMap() {
  if (!publishForm.value.name) {
    publishError.value = '请输入地图名称'
    return
  }
  
  isPublishing.value = true
  publishError.value = ''
  
  try {
    const result = await api.post('/api/v1/publish', {
      name: publishForm.value.name,
      description: publishForm.value.description,
      publishType: publishForm.value.publishType,
      expiryDays: publishForm.value.expiryDays,
      geoJson: props.mapConfig,
      style: publishForm.value.style,
      embedOptions: publishForm.value.embedOptions
    })
    
    publishedResult.value = result
    emit('published', result)
  } catch (e: any) {
    publishError.value = e.message || '发布失败'
    emit('error', e)
  } finally {
    isPublishing.value = false
  }
}

// 复制链接
async function copyLink() {
  await copy(shareUrl.value)
}

// 复制嵌入代码
async function copyEmbedCode() {
  await copy(embedCode.value)
}

// 下载二维码
function downloadQRCode() {
  if (!qrCodeUrl.value) return
  
  const link = document.createElement('a')
  link.href = qrCodeUrl.value
  link.download = `map-share-${publishedResult.value?.shareId}.png`
  link.click()
}

// 刷新分享
async function refreshShare() {
  publishedResult.value = null
}

// 重置表单
function resetForm() {
  publishForm.value = {
    name: '',
    description: '',
    publishType: 'public',
    expiryDays: 7,
    style: null,
    embedOptions: {
      width: 800,
      height: 600,
      title: 'GeoNexus Map',
      responsive: true,
      showControls: true,
      showSidebar: false
    }
  }
  publishedResult.value = null
  publishError.value = ''
}
</script>

<template>
  <div class="map-share-panel">
    <!-- 分享预览 -->
    <div v-if="publishedResult" class="share-result">
      <div class="result-header">
        <span class="success-icon">✅</span>
        <h3>发布成功！</h3>
      </div>
      
      <!-- 分享链接 -->
      <div class="share-section">
        <label>分享链接</label>
        <div class="url-box">
          <input type="text" :value="shareUrl" readonly />
          <button @click="copyLink" class="copy-btn">
            {{ copied ? '已复制' : '复制' }}
          </button>
        </div>
      </div>
      
      <!-- 二维码 -->
      <div v-if="qrCodeUrl" class="share-section">
        <label>二维码</label>
        <div class="qrcode-container">
          <img :src="qrCodeUrl" alt="QR Code" />
          <button @click="downloadQRCode" class="download-btn">
            下载二维码
          </button>
        </div>
      </div>
      
      <!-- 嵌入代码 -->
      <div v-if="embedCode" class="share-section">
        <label>嵌入代码</label>
        <div class="embed-box">
          <textarea :value="embedCode" readonly rows="4"></textarea>
          <button @click="copyEmbedCode" class="copy-btn">
            {{ copied ? '已复制' : '复制代码' }}
          </button>
        </div>
      </div>
      
      <!-- OGC服务信息 -->
      <div v-if="publishedResult.ogcService" class="share-section">
        <label>OGC服务</label>
        <div class="ogc-info">
          <div class="ogc-item">
            <span class="ogc-label">WMS:</span>
            <input type="text" :value="publishedResult.ogcService.wmsUrl" readonly />
          </div>
          <div class="ogc-item">
            <span class="ogc-label">WMTS:</span>
            <input type="text" :value="publishedResult.ogcService.wmtsUrl" readonly />
          </div>
        </div>
      </div>
      
      <!-- 操作按钮 -->
      <div class="result-actions">
        <button @click="refreshShare" class="secondary-btn">
          继续发布
        </button>
        <button @click="resetForm" class="secondary-btn">
          新建发布
        </button>
      </div>
    </div>
    
    <!-- 发布表单 -->
    <div v-else class="share-form">
      <h3>🗺️ 发布地图</h3>
      
      <!-- 地图名称 -->
      <div class="form-group">
        <label>地图名称 *</label>
        <input 
          v-model="publishForm.name" 
          type="text" 
          placeholder="给地图起个名字"
          maxlength="50"
        />
      </div>
      
      <!-- 描述 -->
      <div class="form-group">
        <label>描述</label>
        <textarea 
          v-model="publishForm.description" 
          placeholder="地图描述（可选）"
          rows="2"
          maxlength="200"
        ></textarea>
      </div>
      
      <!-- 发布类型 -->
      <div class="form-group">
        <label>发布方式</label>
        <div class="publish-type-grid">
          <label 
            v-for="type in publishTypes" 
            :key="type.value"
            :class="['type-option', { active: publishForm.publishType === type.value }]"
          >
            <input 
              type="radio" 
              v-model="publishForm.publishType" 
              :value="type.value"
            />
            <span class="type-icon">{{ type.icon }}</span>
            <span class="type-label">{{ type.label }}</span>
            <span class="type-desc">{{ type.desc }}</span>
          </label>
        </div>
      </div>
      
      <!-- 过期时间 -->
      <div class="form-group">
        <label>链接有效期</label>
        <select v-model="publishForm.expiryDays">
          <option v-for="opt in expiryOptions" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </option>
        </select>
      </div>
      
      <!-- 嵌入选项（嵌入模式时显示） -->
      <div v-if="publishForm.publishType === 'embed'" class="form-group embed-options">
        <label>嵌入选项</label>
        <div class="options-grid">
          <div class="option-item">
            <span>宽度</span>
            <input type="number" v-model.number="publishForm.embedOptions.width" min="320" max="1920" />
          </div>
          <div class="option-item">
            <span>高度</span>
            <input type="number" v-model.number="publishForm.embedOptions.height" min="240" max="1080" />
          </div>
          <div class="option-item">
            <span>标题</span>
            <input type="text" v-model="publishForm.embedOptions.title" />
          </div>
        </div>
        <div class="checkbox-group">
          <label>
            <input type="checkbox" v-model="publishForm.embedOptions.responsive" />
            响应式尺寸
          </label>
          <label>
            <input type="checkbox" v-model="publishForm.embedOptions.showControls" />
            显示控制栏
          </label>
          <label>
            <input type="checkbox" v-model="publishForm.embedOptions.showSidebar" />
            显示侧边栏
          </label>
        </div>
      </div>
      
      <!-- 错误提示 -->
      <div v-if="publishError" class="error-message">
        {{ publishError }}
      </div>
      
      <!-- 提交按钮 -->
      <button 
        @click="publishMap" 
        :disabled="isPublishing || !publishForm.name"
        class="publish-btn"
      >
        {{ isPublishing ? '发布中...' : '🚀 发布地图' }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.map-share-panel {
  background: white;
  border-radius: 12px;
  padding: 1.5rem;
  max-width: 600px;
  margin: 0 auto;
}

.share-form h3 {
  margin: 0 0 1.5rem;
  color: #1e293b;
  font-size: 1.25rem;
}

.form-group {
  margin-bottom: 1.25rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-size: 0.875rem;
  font-weight: 600;
  color: #475569;
}

.form-group input[type="text"],
.form-group input[type="number"],
.form-group textarea,
.form-group select {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 0.875rem;
  transition: border-color 0.2s;
}

.form-group input:focus,
.form-group textarea:focus,
.form-group select:focus {
  outline: none;
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.form-group textarea {
  resize: vertical;
  min-height: 60px;
}

/* 发布类型 */
.publish-type-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.75rem;
}

.type-option {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 1rem;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  text-align: center;
}

.type-option input {
  display: none;
}

.type-option:hover {
  border-color: #94a3b8;
}

.type-option.active {
  border-color: #2563eb;
  background: #eff6ff;
}

.type-icon {
  font-size: 1.5rem;
  margin-bottom: 0.5rem;
}

.type-label {
  font-weight: 600;
  color: #1e293b;
  font-size: 0.875rem;
}

.type-desc {
  font-size: 0.75rem;
  color: #64748b;
  margin-top: 0.25rem;
}

/* 嵌入选项 */
.embed-options {
  background: #f8fafc;
  padding: 1rem;
  border-radius: 8px;
}

.options-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.75rem;
  margin-bottom: 0.75rem;
}

.option-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.option-item span {
  font-size: 0.75rem;
  color: #64748b;
}

.option-item input {
  padding: 0.5rem;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 0.875rem;
}

.checkbox-group {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
}

.checkbox-group label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  color: #475569;
  cursor: pointer;
}

.checkbox-group input[type="checkbox"] {
  width: 16px;
  height: 16px;
  accent-color: #2563eb;
}

/* 错误 */
.error-message {
  padding: 0.75rem;
  background: #fef2f2;
  color: #dc2626;
  border-radius: 8px;
  font-size: 0.875rem;
  margin-bottom: 1rem;
}

/* 发布按钮 */
.publish-btn {
  width: 100%;
  padding: 1rem;
  background: #2563eb;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.publish-btn:hover:not(:disabled) {
  background: #1d4ed8;
}

.publish-btn:disabled {
  background: #94a3b8;
  cursor: not-allowed;
}

/* 结果 */
.share-result {
  text-align: center;
}

.result-header {
  margin-bottom: 1.5rem;
}

.success-icon {
  font-size: 3rem;
  display: block;
  margin-bottom: 0.5rem;
}

.result-header h3 {
  margin: 0;
  color: #059669;
  font-size: 1.25rem;
}

.share-section {
  text-align: left;
  margin-bottom: 1.5rem;
}

.share-section label {
  display: block;
  font-size: 0.875rem;
  font-weight: 600;
  color: #475569;
  margin-bottom: 0.5rem;
}

.url-box,
.embed-box {
  display: flex;
  gap: 0.5rem;
}

.url-box input,
.embed-box textarea {
  flex: 1;
  padding: 0.75rem;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 0.875rem;
  font-family: monospace;
  background: #f8fafc;
}

.copy-btn {
  padding: 0.75rem 1.25rem;
  background: #2563eb;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 0.875rem;
  cursor: pointer;
  white-space: nowrap;
}

.copy-btn:hover {
  background: #1d4ed8;
}

.qrcode-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
}

.qrcode-container img {
  width: 150px;
  height: 150px;
  border-radius: 8px;
}

.download-btn {
  padding: 0.5rem 1rem;
  background: #64748b;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 0.875rem;
  cursor: pointer;
}

.ogc-info {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.ogc-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.ogc-label {
  width: 50px;
  font-size: 0.75rem;
  color: #64748b;
}

.ogc-item input {
  flex: 1;
  padding: 0.5rem;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 0.75rem;
  font-family: monospace;
}

.result-actions {
  display: flex;
  gap: 0.75rem;
  justify-content: center;
}

.secondary-btn {
  padding: 0.75rem 1.5rem;
  background: #f1f5f9;
  color: #475569;
  border: none;
  border-radius: 8px;
  font-size: 0.875rem;
  cursor: pointer;
}

.secondary-btn:hover {
  background: #e2e8f0;
}
</style>
