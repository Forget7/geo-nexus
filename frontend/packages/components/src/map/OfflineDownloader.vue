<script setup lang="ts">
/**
 * OfflineDownloader - 离线瓦片下载组件
 *
 * 功能：
 * - 区域选择（绘制矩形）
 * - 缩放级别选择
 * - 预估下载大小
 * - 后台生成离线包
 * - 断网续传（navigator.onLine + localStorage）
 * - 网络状态指示器
 */
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  /** 地图实例（传入用于获取可视区域） */
  mapBounds?: { minLon: number; minLat: number; maxLon: number; maxLat: number }
  /** 瓦片源 URL */
  tileSource?: string
}>()

const emit = defineEmits<{
  downloaded: [file: File]
}>()

// ── 状态 ──────────────────────────────────────────
const isDownloading = ref(false)
const progress = ref(0)
const zoomLevels = ref<number[]>([10, 11, 12, 13, 14, 15, 16])
const selectedBounds = ref<[number, number, number, number] | null>(null)
const estimatedSize = ref('')
const estimatedCount = ref(0)
const downloadUrl = ref('')
const jobId = ref('')

// ── 断网续传状态 ──────────────────────────────────
const isOnline = ref(navigator.onLine)
const isPaused = ref(false) // 因断网而暂停
const LS_KEY = 'gn_offline_download'

interface DownloadProgress {
  jobId: string
  progress: number
  bounds: [number, number, number, number] | null
  zoomLevels: number[]
  tileSource: string
  savedAt: number
}

function saveProgress() {
  if (!jobId.value) return
  const state: DownloadProgress = {
    jobId: jobId.value,
    progress: progress.value,
    bounds: selectedBounds.value,
    zoomLevels: [...zoomLevels.value],
    tileSource: props.tileSource || '',
    savedAt: Date.now()
  }
  localStorage.setItem(LS_KEY, JSON.stringify(state))
}

function loadProgress(): DownloadProgress | null {
  const raw = localStorage.getItem(LS_KEY)
  if (!raw) return null
  try {
    const p = JSON.parse(raw) as DownloadProgress
    // 进度未完成（< 100%）且记录在 24 小时内
    if (p.progress < 100 && Date.now() - p.savedAt < 86_400_000) {
      return p
    }
  } catch { /* ignore */ }
  localStorage.removeItem(LS_KEY)
  return null
}

function clearProgress() {
  localStorage.removeItem(LS_KEY)
}

// ── 网络状态监听 ────────────────────────────────────
function onOnline() {
  isOnline.value = true
  if (isPaused.value && jobId.value) {
    isPaused.value = false
    ElMessage.success('网络已恢复，继续下载…')
    // 断点续传：重新轮询
    pollStatus(jobId.value)
  }
}

function onOffline() {
  isOnline.value = false
  if (isDownloading.value && !isPaused.value) {
    isPaused.value = true
    saveProgress()
    ElMessage.warning('网络已断开，下载已暂停。恢复网络后将自动继续。')
  }
}

onMounted(() => {
  window.addEventListener('online', onOnline)
  window.addEventListener('offline', onOffline)

  // 恢复未完成的下载
  const saved = loadProgress()
  if (saved) {
    jobId.value = saved.jobId
    progress.value = saved.progress
    selectedBounds.value = saved.bounds
    zoomLevels.value = saved.zoomLevels
    isPaused.value = false
    isDownloading.value = true
    ElMessage.info('检测到未完成的下载，正在恢复…')
    pollStatus(saved.jobId)
  }
})

onUnmounted(() => {
  window.removeEventListener('online', onOnline)
  window.removeEventListener('offline', onOffline)
})

// ── 缩放级别范围 ──────────────────────────────────
const minZoom = 3
const maxZoom = 18

const availableZooms = computed(() =>
  Array.from({ length: maxZoom - minZoom + 1 }, (_, i) => maxZoom - i)
)

// ── 业务方法 ─────────────────────────────────────

function useCurrentBounds() {
  if (props.mapBounds) {
    selectedBounds.value = [
      props.mapBounds.minLon,
      props.mapBounds.minLat,
      props.mapBounds.maxLon,
      props.mapBounds.maxLat
    ]
    estimateSize()
  }
}

async function estimateSize() {
  if (!selectedBounds.value) return

  const [minLon, minLat, maxLon, maxLat] = selectedBounds.value
  const zoomStr = zoomLevels.value.join(',')

  try {
    const resp = await fetch(
      `/api/v1/offline/estimate?minLon=${minLon}&minLat=${minLat}&maxLon=${maxLon}&maxLat=${maxLat}&zoomLevels=${zoomStr}`
    )
    const data = await resp.json()
    estimatedSize.value = data.estimatedSize
    estimatedCount.value = data.tileCount
  } catch (e) {
    estimatedSize.value = '未知'
    estimatedCount.value = 0
  }
}

async function startDownload() {
  if (!selectedBounds.value) {
    ElMessage.warning('请先选择下载区域')
    return
  }

  if (!isOnline.value) {
    ElMessage.warning('当前无网络，请连接网络后重试')
    return
  }

  isDownloading.value = true
  isPaused.value = false
  progress.value = 0

  try {
    const [minLon, minLat, maxLon, maxLat] = selectedBounds.value
    const resp = await fetch('/api/v1/offline/package', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: `离线地图_${Date.now()}`,
        tileSource: props.tileSource || 'https://webst02.is.autonavi.com/appmaptile?style=6&x={x}&y={y}&z={z}',
        bounds: [minLon, minLat, maxLon, maxLat],
        zoomLevels: zoomLevels.value,
        format: 'pbf'
      })
    })

    const result = await resp.json()
    jobId.value = result.jobId

    pollStatus(jobId.value)
  } catch (e) {
    ElMessage.error('离线包创建失败')
    isDownloading.value = false
  }
}

async function pollStatus(id: string) {
  const poll = async () => {
    if (!isOnline.value) {
      // 网络断开，暂停轮询，保存进度
      isPaused.value = true
      saveProgress()
      return
    }

    if (isPaused.value) {
      // 等待网络恢复
      setTimeout(poll, 2000)
      return
    }

    try {
      const resp = await fetch(`/api/v1/offline/package/${id}`)
      const data = await resp.json()

      if (data.status === 'READY') {
        downloadUrl.value = data.downloadUrl
        isDownloading.value = false
        progress.value = 100
        clearProgress()
        ElMessage.success('离线包生成完成，点击下载')
        return
      }

      if (data.status === 'FAILED') {
        isDownloading.value = false
        clearProgress()
        ElMessage.error('离线包生成失败')
        return
      }

      // 更新进度
      if (data.progress != null) {
        progress.value = Math.round(data.progress)
        saveProgress()
      }

      setTimeout(poll, 3000)
    } catch (e) {
      // 网络波动，重试
      setTimeout(poll, 5000)
    }
  }

  poll()
}

function downloadFile() {
  if (!downloadUrl.value) return
  const a = document.createElement('a')
  a.href = downloadUrl.value
  a.download = `geonexus-offline-${Date.now()}.geonexus`
  a.click()
}

function cancelDownload() {
  isDownloading.value = false
  isPaused.value = false
  clearProgress()
  jobId.value = ''
  progress.value = 0
}
</script>

<template>
  <div class="offline-downloader">
    <h3 class="offline-downloader__title">🗺️ 离线地图下载</h3>

    <!-- 网络状态指示器 -->
    <div class="network-status" :class="isOnline ? 'online' : 'offline'">
      <span class="status-dot"></span>
      <span>{{ isOnline ? '🟢 在线' : '🔴 离线' }}</span>
      <span v-if="isPaused" class="paused-tag">（已暂停）</span>
    </div>

    <!-- 区域选择 -->
    <div class="offline-downloader__section">
      <label class="section-label">下载区域</label>
      <div class="bounds-display" v-if="selectedBounds">
        <span>{{ selectedBounds[0].toFixed(4) }}, {{ selectedBounds[1].toFixed(4) }}</span>
        <span>→</span>
        <span>{{ selectedBounds[2].toFixed(4) }}, {{ selectedBounds[3].toFixed(4) }}</span>
      </div>
      <div v-else class="no-bounds">点击"使用当前视图"自动获取区域</div>
      <button class="btn-secondary" @click="useCurrentBounds">
        使用当前视图
      </button>
    </div>

    <!-- 缩放级别 -->
    <div class="offline-downloader__section">
      <label class="section-label">缩放级别</label>
      <div class="zoom-checkboxes">
        <label v-for="z in availableZooms" :key="z" class="zoom-checkbox">
          <input type="checkbox" v-model="zoomLevels" :value="z" />
          {{ z }}
        </label>
      </div>
    </div>

    <!-- 预估信息 -->
    <div class="offline-downloader__section" v-if="estimatedSize">
      <div class="estimate-info">
        <span class="estimate-count">约 {{ estimatedCount }} 个瓦片</span>
        <span class="estimate-size">约 {{ estimatedSize }}</span>
      </div>
    </div>

    <!-- 进度 -->
    <div class="offline-downloader__progress" v-if="isDownloading">
      <el-progress :percentage="progress" :status="progress === 100 ? 'success' : undefined" />
      <p class="progress-text">
        {{ isPaused ? '⏸ 网络断开，下载已暂停，恢复网络后自动继续...' : '正在生成离线包...' }}
      </p>
    </div>

    <!-- 操作按钮 -->
    <div class="offline-downloader__actions">
      <button
        class="btn-primary"
        @click="startDownload"
        :disabled="isDownloading || !selectedBounds || !isOnline"
      >
        {{ isDownloading ? (isPaused ? '已暂停' : '生成中...') : '开始下载' }}
      </button>
      <button
        class="btn-secondary"
        v-if="downloadUrl"
        @click="downloadFile"
      >
        📥 下载离线包
      </button>
      <button
        class="btn-cancel"
        v-if="isDownloading"
        @click="cancelDownload"
      >
        取消
      </button>
    </div>
  </div>
</template>

<style scoped>
.offline-downloader {
  padding: 12px;
  background: white;
  border-radius: 8px;
  min-width: 280px;
}

.offline-downloader__title {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 600;
}

.offline-downloader__section {
  margin-bottom: 12px;
}

.section-label {
  display: block;
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.bounds-display {
  display: flex;
  gap: 4px;
  font-size: 11px;
  color: #333;
  flex-wrap: wrap;
  padding: 6px 8px;
  background: #f5f5f5;
  border-radius: 4px;
  margin-bottom: 6px;
}

.no-bounds {
  font-size: 11px;
  color: #999;
  padding: 6px 8px;
  background: #f5f5f5;
  border-radius: 4px;
  margin-bottom: 6px;
}

.zoom-checkboxes {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.zoom-checkbox {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 11px;
  cursor: pointer;
}

.zoom-checkbox input {
  width: 14px;
  height: 14px;
}

.estimate-info {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #666;
  padding: 6px 8px;
  background: #f0f9ff;
  border-radius: 4px;
}

.progress-text {
  text-align: center;
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

.offline-downloader__actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
}

.btn-primary {
  padding: 8px 16px;
  background: #2563eb;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
}

.btn-primary:disabled {
  background: #93c5fd;
  cursor: not-allowed;
}

.btn-secondary {
  padding: 8px 16px;
  background: white;
  color: #2563eb;
  border: 1px solid #2563eb;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
}

.btn-cancel {
  padding: 6px 16px;
  background: white;
  color: #dc2626;
  border: 1px solid #dc2626;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
}

/* 网络状态 */
.network-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  padding: 6px 10px;
  border-radius: 6px;
  margin-bottom: 12px;
}

.network-status.online {
  background: #f0fdf4;
  color: #16a34a;
}

.network-status.offline {
  background: #fef2f2;
  color: #dc2626;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
}

.paused-tag {
  font-weight: 400;
  color: #f59e0b;
  font-size: 11px;
}
</style>
