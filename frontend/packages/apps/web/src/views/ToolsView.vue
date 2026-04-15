<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { toolsApi, ToolInfo, DistanceResult, GeocodeResult } from '../../../src/api/client'

const { t } = useI18n()

const tools = ref<ToolInfo[]>([])
const selectedTool = ref<string>('')
const isLoading = ref(false)
const result = ref<DistanceResult | GeocodeResult | null>(null)
const error = ref('')

// 表单数据
const distanceForm = ref({
  lat1: 39.9042,
  lon1: 116.4074,
  lat2: 31.2304,
  lon2: 121.4737,
  unit: 'km'
})

const bufferForm = ref({
  geometry: { type: 'Point', coordinates: [116.4074, 39.9042] },
  distanceKm: 10
})

const geocodeForm = ref({
  address: ''
})

onMounted(async () => {
  try {
    isLoading.value = true
    tools.value = await toolsApi.list()
  } catch (e) {
    error.value = (e as Error).message || '加载工具列表失败'
  } finally {
    isLoading.value = false
  }
})

async function executeDistance() {
  isLoading.value = true
  error.value = ''
  result.value = null

  try {
    const res = await toolsApi.calculateDistance(
      distanceForm.value.lat1,
      distanceForm.value.lon1,
      distanceForm.value.lat2,
      distanceForm.value.lon2,
      distanceForm.value.unit
    )
    result.value = res
  } catch (err: unknown) {
    error.value = (err as Error).message || '执行失败'
  } finally {
    isLoading.value = false
  }
}

async function executeGeocode() {
  isLoading.value = true
  error.value = ''
  result.value = null

  try {
    const res = await toolsApi.geocode(geocodeForm.value.address)
    result.value = res
  } catch (err: unknown) {
    error.value = (err as Error).message || '执行失败'
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <div class="tools-view">
    <header class="tools-header">
      <h1>🛠️ GIS工具</h1>
      <p>专业的地理空间分析工具</p>
    </header>

    <main class="tools-content">
      <!-- 工具列表 -->
      <section class="tools-list-section">
        <h2>可用工具</h2>

        <!-- Loading skeleton -->
        <div v-if="isLoading && tools.length === 0" class="tools-skeleton">
          <div class="skeleton-card"></div>
          <div class="skeleton-card"></div>
          <div class="skeleton-card"></div>
          <div class="skeleton-card"></div>
        </div>

        <!-- Empty state -->
        <div v-else-if="tools.length === 0 && !error" class="empty-tools">
          <div class="empty-icon">🔧</div>
          <p>暂无可用工具</p>
        </div>

        <!-- Error state -->
        <div v-else-if="error && tools.length === 0" class="error-tools">
          <div class="error-icon">⚠️</div>
          <p>{{ error }}</p>
          <button @click="() => { error = ''; onMounted() }" class="retry-btn">
            {{ t('common.retry') }}
          </button>
        </div>

        <!-- Tools grid -->
        <div v-else class="tools-grid">
          <div
            v-for="tool in tools"
            :key="tool.name"
            class="tool-card"
            @click="selectedTool = tool.name"
            :class="{ active: selectedTool === tool.name }"
          >
            <h3>{{ tool.name }}</h3>
            <p>{{ tool.description }}</p>
          </div>
        </div>
      </section>

      <!-- 距离计算 -->
      <section class="tool-panel">
        <h2>📏 距离计算</h2>

        <div class="form-grid">
          <div class="form-group">
            <label>起点纬度</label>
            <input v-model.number="distanceForm.lat1" type="number" step="0.0001" />
          </div>
          <div class="form-group">
            <label>起点经度</label>
            <input v-model.number="distanceForm.lon1" type="number" step="0.0001" />
          </div>
          <div class="form-group">
            <label>终点纬度</label>
            <input v-model.number="distanceForm.lat2" type="number" step="0.0001" />
          </div>
          <div class="form-group">
            <label>终点经度</label>
            <input v-model.number="distanceForm.lon2" type="number" step="0.0001" />
          </div>
        </div>

        <div class="form-row">
          <div class="form-group">
            <label>单位</label>
            <select v-model="distanceForm.unit">
              <option value="km">公里 (km)</option>
              <option value="m">米 (m)</option>
              <option value="miles">英里 (miles)</option>
            </select>
          </div>
        </div>

        <button @click="executeDistance" :disabled="isLoading" class="execute-btn">
          <span v-if="isLoading" class="btn-spinner"></span>
          <span v-else>{{ isLoading ? '计算中...' : '计算距离' }}</span>
        </button>
      </section>

      <!-- 缓冲区分析 -->
      <section class="tool-panel">
        <h2>🔵 缓冲区分析</h2>

        <div class="form-grid">
          <div class="form-group full">
            <label>几何坐标 [经度, 纬度]</label>
            <input
              v-model="bufferForm.geometry.coordinates"
              type="text"
              placeholder="116.4074, 39.9042"
            />
          </div>
          <div class="form-group full">
            <label>缓冲距离 (公里)</label>
            <input v-model.number="bufferForm.distanceKm" type="number" step="0.1" />
          </div>
        </div>

        <button class="execute-btn" disabled>执行 (开发中)</button>
      </section>

      <!-- 地理编码 -->
      <section class="tool-panel">
        <h2>📍 地理编码</h2>

        <div class="form-group full">
          <label>地址</label>
          <input
            v-model="geocodeForm.address"
            type="text"
            placeholder="输入地址，如：北京市天安门"
          />
        </div>

        <button @click="executeGeocode" :disabled="isLoading" class="execute-btn">
          <span v-if="isLoading" class="btn-spinner"></span>
          <span v-else>{{ isLoading ? '查询中...' : '查询坐标' }}</span>
        </button>
      </section>

      <!-- 结果展示 -->
      <section class="result-section">
        <h2>结果</h2>

        <!-- Error state -->
        <Transition name="slide">
          <div v-if="error" class="error-message">
            <span class="error-icon">⚠️</span>
            <span>{{ error }}</span>
            <button @click="error = ''" class="error-close">×</button>
          </div>
        </Transition>

        <!-- Success result -->
        <Transition name="slide">
          <div v-if="result && !error" class="result-content">
            <pre>{{ JSON.stringify(result, null, 2) }}</pre>
          </div>
        </Transition>
      </section>
    </main>
  </div>
</template>

<style scoped>
.tools-view {
  min-height: 100vh;
  background: #f8fafc;
}

.tools-header {
  padding: 2rem;
  background: white;
  border-bottom: 1px solid #e2e8f0;
}

.tools-header h1 {
  margin: 0 0 0.5rem;
  color: #1e293b;
}

.tools-header p {
  margin: 0;
  color: #64748b;
}

.tools-content {
  padding: 2rem;
  max-width: 900px;
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

/* ---------- Tools Grid ---------- */
.tools-skeleton {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 1rem;
}

.skeleton-card {
  height: 80px;
  border-radius: 8px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.empty-tools, .error-tools {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 2rem;
  color: #94a3b8;
}

.empty-icon, .error-icon {
  font-size: 2.5rem;
  margin-bottom: 0.5rem;
}

.empty-tools p, .error-tools p {
  margin: 0;
  font-size: 0.9rem;
}

.error-tools {
  color: #dc2626;
}

.retry-btn {
  margin-top: 0.75rem;
  padding: 0.5rem 1rem;
  background: #2563eb;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 0.875rem;
  cursor: pointer;
  transition: background 0.2s;
}

.retry-btn:hover {
  background: #1d4ed8;
}

.tools-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 1rem;
}

.tool-card {
  padding: 1rem;
  background: #f8fafc;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;
}

.tool-card:hover {
  background: #eff6ff;
  border-color: #bfdbfe;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.1);
}

.tool-card.active {
  background: #eff6ff;
  border-color: #2563eb;
}

.tool-card h3 {
  margin: 0 0 0.5rem;
  font-size: 0.95rem;
  color: #1e293b;
}

.tool-card p {
  margin: 0;
  font-size: 0.8rem;
  color: #64748b;
  line-height: 1.4;
}

/* ---------- Form ---------- */
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1rem;
  margin-bottom: 1rem;
}

.form-group {
  display: flex;
  flex-direction: column;
}

.form-group.full {
  grid-column: span 2;
}

.form-group label {
  margin-bottom: 0.5rem;
  font-size: 0.875rem;
  color: #475569;
}

.form-group input,
.form-group select {
  padding: 0.625rem 0.75rem;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 0.875rem;
  transition: border-color 0.2s;
}

.form-group input:focus,
.form-group select:focus {
  outline: none;
  border-color: #2563eb;
}

.execute-btn {
  padding: 0.75rem 1.5rem;
  background: #2563eb;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 0.875rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.2s;
}

.execute-btn:hover:not(:disabled) {
  background: #1d4ed8;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
}

.execute-btn:active:not(:disabled) {
  transform: translateY(0);
}

.execute-btn:disabled {
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

/* ---------- Result Section ---------- */
.result-section {
  background: #f8fafc;
}

.error-message {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 1rem;
  background: #fef2f2;
  color: #dc2626;
  border-radius: 8px;
  font-size: 0.875rem;
}

.error-icon {
  font-size: 1rem;
}

.error-message span:nth-child(2) {
  flex: 1;
}

.error-close {
  background: transparent;
  border: none;
  color: #dc2626;
  cursor: pointer;
  font-size: 1rem;
  padding: 0 4px;
  border-radius: 4px;
  transition: background 0.2s;
}

.error-close:hover {
  background: #fee2e2;
}

.result-content {
  background: #1e293b;
  color: #e2e8f0;
  padding: 1rem;
  border-radius: 8px;
  overflow-x: auto;
}

.result-content pre {
  margin: 0;
  font-family: 'Monaco', 'Menlo', monospace;
  font-size: 0.85rem;
}

/* Transition */
.slide-enter-active, .slide-leave-active {
  transition: all 0.3s ease;
}
.slide-enter-from, .slide-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
