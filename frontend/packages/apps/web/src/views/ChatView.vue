<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { useChatStore } from '../../packages/stores/src/chat'
import ChatPanel from '../../../packages/components/src/chat/ChatPanel.vue'
import MapView from '../../../packages/components/src/map/MapView.vue'

const chatStore = useChatStore()
const route = useRoute()

const showMap = ref(false)
const mapUrl = ref('')

onMounted(() => {
  // 处理URL参数
  if (route.query.msg) {
    const msg = route.query.msg as string
    setTimeout(() => {
      chatStore.sendMessage(msg)
    }, 500)
  }
})

function onMapGenerated(url: string) {
  mapUrl.value = url
  showMap.value = true
}
</script>

<template>
  <div class="chat-view">
    <div class="chat-container">
      <ChatPanel @map-generated="onMapGenerated" />
    </div>
    
    <div v-if="showMap" class="map-container">
      <div class="map-header">
        <h3>🗺️ 地图预览</h3>
        <button class="close-btn" @click="showMap = false">×</button>
      </div>
      <div class="map-content">
        <iframe 
          v-if="mapUrl"
          :src="mapUrl"
          class="map-iframe"
          title="Map Preview"
        ></iframe>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-view {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.chat-container {
  flex: 1;
  max-width: 500px;
  border-right: 1px solid #e2e8f0;
  overflow: hidden;
}

.map-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: white;
}

.map-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.5rem;
  border-bottom: 1px solid #e2e8f0;
}

.map-header h3 {
  margin: 0;
  font-size: 1rem;
  color: #1e293b;
}

.close-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: #f1f5f9;
  border-radius: 50%;
  font-size: 1.25rem;
  color: #64748b;
  cursor: pointer;
}

.close-btn:hover {
  background: #e2e8f0;
}

.map-content {
  flex: 1;
  overflow: hidden;
}

.map-iframe {
  width: 100%;
  height: 100%;
  border: none;
}
</style>
