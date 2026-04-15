<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { dataApi } from '../../../src/api/client'

const { t } = useI18n()

const fileInput = ref<HTMLInputElement | null>(null)
const uploadedFiles = ref<any[]>([])
const isLoading = ref(false)
const message = ref('')
const messageType = ref<'success' | 'error'>('success')

// 格式转换
const convertForm = ref({
  inputFormat: 'geojson',
  outputFormat: 'shapefile'
})

async function handleUpload() {
  const input = fileInput.value
  if (!input?.files?.length) return

  isLoading.value = true
  message.value = ''

  try {
    const file = input.files[0]
    const response = await dataApi.upload(file)

    uploadedFiles.value.push(response)
    message.value = `文件 "${file.name}" 上传成功`
    messageType.value = 'success'

    // 清空input
    input.value = ''
  } catch (err: unknown) {
    message.value = `上传失败: ${(err as Error).message}`
    messageType.value = 'error'
  } finally {
    isLoading.value = false
  }
}

async function convertData() {
  if (!convertForm.value.inputFormat || !convertForm.value.outputFormat) {
    message.value = '请选择输入和输出格式'
    messageType.value = 'error'
    return
  }

  message.value = '格式转换功能开发中...'
  messageType.value = 'success'
}
</script>

<template>
  <div class="data-view">
    <header class="data-header">
      <h1>📁 数据管理</h1>
      <p>上传、转换和管理地理空间数据</p>
    </header>

    <main class="data-content">
      <!-- 上传区域 -->
      <section class="upload-section">
        <h2>上传数据</h2>

        <!-- Loading skeleton -->
        <div v-if="isLoading" class="upload-skeleton">
          <div class="skeleton-icon"></div>
          <div class="skeleton-lines">
            <div class="skeleton-line"></div>
            <div class="skeleton-line short"></div>
          </div>
        </div>

        <div v-else class="upload-area" :class="{ 'has-error': messageType === 'error' }">
          <input
            ref="fileInput"
            type="file"
            accept=".geojson,.json,.shp,.kml,.gml,.gpx"
            @change="handleUpload"
          />
          <p class="upload-hint">
            支持格式: GeoJSON, Shapefile, KML, GML, GPX
          </p>
        </div>

        <Transition name="slide">
          <div v-if="message" :class="['message', messageType]">
            {{ message }}
          </div>
        </Transition>
      </section>

      <!-- 已上传文件 -->
      <section class="files-section">
        <h2>已上传文件</h2>

        <!-- 空状态 -->
        <div v-if="uploadedFiles.length === 0" class="empty-files">
          <div class="empty-icon">📂</div>
          <p>{{ t('common.noData') }}</p>
        </div>

        <!-- 文件骨架屏 -->
        <div v-else-if="isLoading && uploadedFiles.length === 0" class="files-skeleton">
          <div class="skeleton-file"></div>
          <div class="skeleton-file"></div>
          <div class="skeleton-file"></div>
        </div>

        <!-- 文件列表 -->
        <div v-else class="files-list">
          <TransitionGroup name="file-item">
            <div v-for="file in uploadedFiles" :key="file.id" class="file-item">
              <span class="file-icon">📄</span>
              <span class="file-name">{{ file.filename }}</span>
              <span class="file-format">{{ file.format }}</span>
              <span class="file-size">{{ (file.size / 1024).toFixed(1) }} KB</span>
            </div>
          </TransitionGroup>
        </div>
      </section>

      <!-- 格式转换 -->
      <section class="convert-section">
        <h2>格式转换</h2>

        <div class="convert-form">
          <div class="form-group">
            <label>输入格式</label>
            <select v-model="convertForm.inputFormat">
              <option value="geojson">GeoJSON</option>
              <option value="shapefile">Shapefile</option>
              <option value="kml">KML</option>
              <option value="gml">GML</option>
              <option value="gpx">GPX</option>
            </select>
          </div>

          <div class="arrow">→</div>

          <div class="form-group">
            <label>输出格式</label>
            <select v-model="convertForm.outputFormat">
              <option value="geojson">GeoJSON</option>
              <option value="shapefile">Shapefile</option>
              <option value="kml">KML</option>
              <option value="gml">GML</option>
              <option value="gpx">GPX</option>
            </select>
          </div>

          <button class="convert-btn" @click="convertData" :disabled="isLoading">
            <span v-if="isLoading" class="btn-spinner"></span>
            <span v-else>转换</span>
          </button>
        </div>
      </section>

      <!-- 坐标系转换 -->
      <section class="crs-section">
        <h2>坐标投影转换</h2>

        <div class="crs-form">
          <div class="form-group">
            <label>源坐标系</label>
            <select>
              <option value="EPSG:4326">WGS 84 (EPSG:4326)</option>
              <option value="EPSG:3857">Web Mercator (EPSG:3857)</option>
              <option value="EPSG:4490">China 2000 (EPSG:4490)</option>
            </select>
          </div>

          <div class="arrow">→</div>

          <div class="form-group">
            <label>目标坐标系</label>
            <select>
              <option value="EPSG:4326">WGS 84 (EPSG:4326)</option>
              <option value="EPSG:3857">Web Mercator (EPSG:3857)</option>
              <option value="EPSG:4490">China 2000 (EPSG:4490)</option>
            </select>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<style scoped>
.data-view {
  min-height: 100vh;
  background: #f8fafc;
}

.data-header {
  padding: 2rem;
  background: white;
  border-bottom: 1px solid #e2e8f0;
}

.data-header h1 {
  margin: 0 0 0.5rem;
  color: #1e293b;
}

.data-header p {
  margin: 0;
  color: #64748b;
}

.data-content {
  padding: 2rem;
  max-width: 800px;
  margin: 0 auto;
}

section {
  background: white;
  border-radius: 12px;
  padding: 1.5rem;
  margin-bottom: 1.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

section h2 {
  margin: 0 0 1rem;
  font-size: 1.1rem;
  color: #1e293b;
}

/* ---------- Upload Section ---------- */
.upload-area {
  border: 2px dashed #e2e8f0;
  border-radius: 8px;
  padding: 2rem;
  text-align: center;
  transition: all 0.2s;
}

.upload-area:hover {
  border-color: #2563eb;
  background: #f8fafc;
}

.upload-area.has-error {
  border-color: #dc2626;
  background: #fef2f2;
}

.upload-area input {
  margin-bottom: 0.5rem;
  cursor: pointer;
}

.upload-hint {
  margin: 0;
  font-size: 0.875rem;
  color: #64748b;
}

/* Upload Skeleton */
.upload-skeleton {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1.5rem;
  border: 2px dashed #e2e8f0;
  border-radius: 8px;
}

.skeleton-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  flex-shrink: 0;
}

.skeleton-lines {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.skeleton-line {
  height: 14px;
  border-radius: 6px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.skeleton-line.short { width: 50%; }

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* Message */
.message {
  margin-top: 1rem;
  padding: 0.75rem 1rem;
  border-radius: 6px;
  font-size: 0.875rem;
}

.message.success {
  background: #ecfdf5;
  color: #059669;
  border: 1px solid #a7f3d0;
}

.message.error {
  background: #fef2f2;
  color: #dc2626;
  border: 1px solid #fecaca;
}

.slide-enter-active, .slide-leave-active {
  transition: all 0.3s ease;
}
.slide-enter-from, .slide-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* ---------- Files Section ---------- */
.empty-files {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 2rem;
  color: #94a3b8;
}

.empty-files .empty-icon {
  font-size: 2.5rem;
  margin-bottom: 0.5rem;
}

.empty-files p {
  margin: 0;
  font-size: 0.9rem;
}

.files-skeleton {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.skeleton-file {
  height: 48px;
  border-radius: 8px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.files-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem;
  background: #f8fafc;
  border-radius: 6px;
  transition: all 0.2s;
}

.file-item:hover {
  background: #eff6ff;
}

.file-icon {
  font-size: 1.25rem;
}

.file-name {
  flex: 1;
  font-weight: 500;
  color: #1e293b;
}

.file-format, .file-size {
  font-size: 0.875rem;
  color: #64748b;
}

/* File item transition */
.file-item-enter-active {
  transition: all 0.3s ease;
}
.file-item-leave-active {
  transition: all 0.2s ease;
}
.file-item-enter-from {
  opacity: 0;
  transform: translateX(-10px);
}
.file-item-leave-to {
  opacity: 0;
  transform: translateX(10px);
}

/* ---------- Convert Section ---------- */
.convert-form, .crs-form {
  display: flex;
  align-items: flex-end;
  gap: 1rem;
}

.form-group {
  flex: 1;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-size: 0.875rem;
  color: #475569;
}

.form-group select {
  width: 100%;
  padding: 0.625rem 0.75rem;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 0.875rem;
  background: white;
  cursor: pointer;
  transition: border-color 0.2s;
}

.form-group select:focus {
  outline: none;
  border-color: #2563eb;
}

.arrow {
  font-size: 1.25rem;
  color: #64748b;
  padding-bottom: 0.5rem;
}

.convert-btn, .crs-btn {
  padding: 0.625rem 1.5rem;
  background: #2563eb;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 80px;
}

.convert-btn:hover:not(:disabled) {
  background: #1d4ed8;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
}

.convert-btn:active:not(:disabled) {
  transform: translateY(0);
}

.convert-btn:disabled {
  background: #94a3b8;
  cursor: not-allowed;
}

.btn-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
