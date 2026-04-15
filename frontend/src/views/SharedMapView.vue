<script setup lang="ts">
/**
 * SharedMapView - 共享地图查看器
 * 用于查看通过链接/嵌入分享的地图
 */

import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../api'
import UnifiedMapPanel from '../../packages/components/src/map/UnifiedMapPanel.vue'

const route = useRoute()
const router = useRouter()

// 分享地图API响应类型
export interface MapShareConfig {
  shareId: string
  name: string
  description?: string
  ownerId: string
  publishType: string
  createdAt: number
  expiresAt?: number
  expiryDays: number
  accessToken?: string
  embedAllowed: boolean
  embedDomains?: string[]
  layerName?: string
  wmsUrl?: string
  wmtsUrl?: string
  accessCount: number
}

export interface OgcMapConfig {
  type: 'ogc'
  wmsUrl: string
  wmtsUrl: string
  layerName: string
}

// 状态
const isLoading = ref(true)
const error = ref('')
const mapConfig = ref<MapShareConfig | OgcMapConfig | null>(null)
const shareInfo = ref<MapShareConfig | null>(null)
const mapMode = ref<'2d' | '3d'>('2d')

// 从URL获取分享ID和Token
const shareId = computed(() => route.params.id as string || route.query.id as string)
const accessToken = computed(() => route.query.token as string)

// 加载分享地图
async function loadSharedMap() {
  isLoading.value = true
  error.value = ''
  
  try {
    // 获取分享配置
    const config = await api.get(`/api/v1/publish/${shareId.value}`, {
      params: { token: accessToken.value }
    })
    
    shareInfo.value = config
    mapConfig.value = config
    
    // 根据配置决定模式
    if (config.publishType === 'ogc') {
      // OGC服务模式
      mapConfig.value = {
        type: 'ogc',
        wmsUrl: config.wmsUrl,
        wmtsUrl: config.wmtsUrl,
        layerName: config.layerName
      }
    }
    
    // 检查过期
    if (config.expiresAt && Date.now() > config.expiresAt) {
      error.value = '此分享已过期'
      return
    }
    
    isLoading.value = false
    
  } catch (err: unknown) {
    const e = err as { status?: number; message?: string }
    if (e.status === 404) {
      error.value = '地图不存在或已被删除'
    } else if (e.status === 410) {
      error.value = '此分享已过期'
    } else if (e.status === 403) {
      error.value = '无权访问此地图'
    } else {
      error.value = e.message || '加载失败'
    }
    isLoading.value = false
  }
}

// 切换2D/3D模式
function toggleMapMode() {
  mapMode.value = mapMode.value === '2d' ? '3d' : '2d'
}

// 分享到其他平台
function shareTo(platform: string) {
  const url = shareInfo.value?.shareUrl || window.location.href
  const text = `查看这个GIS地图: ${shareInfo.value?.name || 'GeoNexus Map'}`
  
  const shareUrls: Record<string, string> = {
    twitter: `https://twitter.com/intent/tweet?url=${encodeURIComponent(url)}&text=${encodeURIComponent(text)}`,
    facebook: `https://www.facebook.com/sharer/sharer.php?u=${encodeURIComponent(url)}`,
    linkedin: `https://www.linkedin.com/sharing/share-offsite/?url=${encodeURIComponent(url)}`,
    weibo: `https://service.weibo.com/share/share.php?url=${encodeURIComponent(url)}&title=${encodeURIComponent(text)}`
  }
  
  if (shareUrls[platform]) {
    window.open(shareUrls[platform], '_blank', 'width=600,height=400')
  }
}

// 全屏模式
function enterFullscreen() {
  const elem = document.documentElement
  if (elem.requestFullscreen) {
    elem.requestFullscreen()
  }
}

// 打开GeoNexus主站
function openInGeoNexus() {
  window.open(`https://geonexus.ai/map/${shareId.value}`, '_blank')
}

onMounted(() => {
  loadSharedMap()
})
</script>

<template>
  <div class="shared-map-view">
    <!-- 加载中 -->
    <div v-if="isLoading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>加载地图中...</p>
    </div>
    
    <!-- 错误状态 -->
    <div v-else-if="error" class="error-state">
      <div class="error-icon">❌</div>
      <h2>{{ error }}</h2>
      <p>请检查链接是否正确，或联系地图创建者</p>
      <button @click="openInGeoNexus" class="home-btn">
        访问 GeoNexus
      </button>
    </div>
    
    <!-- 地图视图 -->
    <template v-else>
      <!-- 顶部工具栏 -->
      <header class="map-toolbar">
        <div class="toolbar-left">
          <span class="logo">🌐 GeoNexus</span>
          <span class="separator">|</span>
          <span class="map-title">{{ shareInfo?.name || '共享地图' }}</span>
        </div>
        
        <div class="toolbar-center">
          <!-- 2D/3D切换 -->
          <div class="mode-toggle">
            <button 
              :class="{ active: mapMode === '2d' }"
              @click="mapMode = '2d'"
            >
              🗺️ 2D
            </button>
            <button 
              :class="{ active: mapMode === '3d' }"
              @click="mapMode = '3d'"
            >
              🌍 3D
            </button>
          </div>
        </div>
        
        <div class="toolbar-right">
          <!-- 分享按钮 -->
          <button class="tool-btn" @click="shareTo('twitter')" title="分享到Twitter">
            𝕏
          </button>
          <button class="tool-btn" @click="shareTo('facebook')" title="分享到Facebook">
            f
          </button>
          <button class="tool-btn" @click="shareTo('weibo')" title="分享到微博">
            微博
          </button>
          
          <div class="separator"></div>
          
          <!-- 全屏 -->
          <button class="tool-btn" @click="enterFullscreen" title="全屏">
            ⛶
          </button>
          
          <!-- 在GeoNexus打开 -->
          <button class="tool-btn geonxus-btn" @click="openInGeoNexus">
            访问GeoNexus
          </button>
        </div>
      </header>
      
      <!-- 地图容器 -->
      <div class="map-container">
        <UnifiedMapPanel 
          :default-mode="mapMode"
          :show-controls="true"
          :show-layer-panel="true"
        />
      </div>
      
      <!-- 底部信息栏 -->
      <footer class="map-footer">
        <span class="footer-info">
          由 <a href="https://geonexus.ai" target="_blank">GeoNexus</a> GIS专家系统创建
        </span>
        <span v-if="shareInfo?.description" class="footer-desc">
          {{ shareInfo.description }}
        </span>
      </footer>
    </template>
  </div>
</template>

<style scoped>
.shared-map-view {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #1e293b;
}

/* 加载状态 */
.loading-state,
.error-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: white;
}

.loading-spinner {
  width: 48px;
  height: 48px;
  border: 4px solid rgba(255, 255, 255, 0.3);
  border-top-color: #2563eb;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 1rem;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.error-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
}

.error-state h2 {
  margin: 0 0 0.5rem;
  font-size: 1.5rem;
}

.error-state p {
  margin: 0 0 1.5rem;
  color: #94a3b8;
}

.home-btn {
  padding: 0.75rem 1.5rem;
  background: #2563eb;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  cursor: pointer;
}

.home-btn:hover {
  background: #1d4ed8;
}

/* 工具栏 */
.map-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1rem;
  background: rgba(30, 41, 59, 0.95);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  z-index: 100;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.logo {
  font-weight: 700;
  font-size: 1rem;
  color: white;
}

.separator {
  color: rgba(255, 255, 255, 0.3);
}

.map-title {
  color: #94a3b8;
  font-size: 0.875rem;
}

.toolbar-center {
  display: flex;
  align-items: center;
}

.mode-toggle {
  display: flex;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  overflow: hidden;
}

.mode-toggle button {
  padding: 0.5rem 1rem;
  background: transparent;
  border: none;
  color: #94a3b8;
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s;
}

.mode-toggle button.active {
  background: #2563eb;
  color: white;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.tool-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.1);
  border: none;
  border-radius: 8px;
  color: white;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 0.875rem;
}

.tool-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.geonxus-btn {
  width: auto;
  padding: 0 1rem;
  font-size: 0.75rem;
}

/* 地图容器 */
.map-container {
  flex: 1;
  position: relative;
}

/* 底部信息栏 */
.map-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.5rem 1rem;
  background: rgba(30, 41, 59, 0.95);
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  font-size: 0.75rem;
  color: #64748b;
}

.map-footer a {
  color: #2563eb;
  text-decoration: none;
}

.map-footer a:hover {
  text-decoration: underline;
}

.footer-desc {
  max-width: 60%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
