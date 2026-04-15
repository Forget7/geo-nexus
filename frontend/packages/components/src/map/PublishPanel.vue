<template>
  <div class="pub-panel">
    <div class="panel-header">
      <h3>🔗 {{ t('pub.title') }}</h3>
      <button @click="showPublish = true" class="create-btn">+ {{ t('pub.shareMap') }}</button>
    </div>

    <div class="section">
      <div class="share-list">
        <div v-for="share in shares" :key="share.id" class="share-item">
          <div class="share-info">
            <strong>{{ share.mapName }}</strong>
            <div class="share-meta">
              <span :class="['pub-status', share.public ? 'public' : 'private']">
                {{ share.public ? t('pub.public') : t('pub.private') }}
              </span>
              <span>{{ share.viewCount || 0 }} {{ t('pub.views') }}</span>
            </div>
          </div>
          <button @click="copyLink(share.shareUrl)" class="link-btn">{{ t('pub.copyLink') }}</button>
        </div>
      </div>
    </div>

    <!-- 发布弹窗 -->
    <div v-if="showPublish" class="modal-overlay" @click.self="showPublish = false">
      <div class="modal">
        <h3>{{ t('pub.shareMap') }}</h3>
        <input v-model="newShare.mapName" :placeholder="t('pub.mapName')" class="input" />
        <label class="checkbox-label">
          <input type="checkbox" v-model="newShare.isPublic" />
          {{ t('pub.makePublic') }}
        </label>
        <div class="modal-actions">
          <button @click="publish">{{ t('pub.publish') }}</button>
          <button @click="showPublish = false" class="cancel-btn">{{ t('pub.cancel') }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

const t = (key: string) => key
const shares = ref<any[]>([])
const showPublish = ref(false)
const newShare = ref<any>({ mapName: '', isPublic: false })

onMounted(() => fetchShares())

async function fetchShares() {
  // shares list would come from user's published maps
  shares.value = []
}

async function publish() {
  showPublish.value = false
}

function copyLink(url: string) {
  navigator.clipboard?.writeText(url)
}
</script>

<style scoped>
.pub-panel { background: white; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.15); }
.panel-header { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; border-bottom: 1px solid #e2e8f0; }
.panel-header h3 { font-size: 15px; font-weight: 600; }
.create-btn { padding: 7px 14px; background: #2563eb; color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 12px; }
.section { padding: 12px 16px; }
.share-list { display: flex; flex-direction: column; gap: 8px; }
.share-item { display: flex; justify-content: space-between; align-items: center; padding: 10px; background: #f8fafc; border-radius: 8px; border: 1px solid #e2e8f0; }
.share-info { flex: 1; }
.share-info strong { font-size: 13px; }
.share-meta { display: flex; gap: 8px; margin-top: 4px; font-size: 11px; color: #94a3b8; }
.pub-status { padding: 2px 8px; border-radius: 4px; font-size: 11px; font-weight: 600; }
.pub-status.public { background: #dcfce7; color: #16a34a; }
.pub-status.private { background: #f1f5f9; color: #94a3b8; }
.link-btn { padding: 6px 12px; border: 1px solid #e2e8f0; border-radius: 6px; background: white; cursor: pointer; font-size: 12px; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal { background: white; border-radius: 12px; padding: 24px; width: 400px; }
.modal h3 { font-size: 16px; margin-bottom: 16px; }
.input { width: 100%; padding: 10px; border: 1px solid #e2e8f0; border-radius: 6px; font-size: 13px; margin-bottom: 10px; box-sizing: border-box; }
.checkbox-label { display: flex; align-items: center; gap: 8px; font-size: 13px; cursor: pointer; }
.modal-actions { display: flex; gap: 8px; justify-content: flex-end; }
.modal-actions button { padding: 8px 16px; border-radius: 6px; cursor: pointer; font-size: 13px; border: none; }
.modal-actions button:first-child { background: #2563eb; color: white; }
.cancel-btn { background: #f1f5f9; color: #64748b; }
</style>
