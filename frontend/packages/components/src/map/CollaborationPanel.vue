<template>
  <div class="collab-panel">
    <div class="panel-header">
      <h3>👥 {{ t('collab.title') }}</h3>
      <button class="close-btn" @click="$emit('close')">×</button>
    </div>

    <!-- 锁定状态 -->
    <div class="section">
      <h4>🔒 {{ t('collab.lockStatus') }}</h4>
      <div v-if="lockInfo" class="lock-card">
        <p>{{ t('collab.lockedBy') }}: <strong>{{ lockInfo.lockedByName }}</strong></p>
        <p>{{ t('collab.lockedAt') }}: {{ formatTime(lockInfo.lockedAt) }}</p>
        <button v-if="canUnlock" @click="releaseLock" class="unlock-btn">
          {{ t('collab.releaseLock') }}
        </button>
      </div>
      <div v-else class="no-lock">
        <p>✅ {{ t('collab.noLock') }}</p>
        <button v-if="!hasLock" @click="acquireLock" class="lock-btn">
          🔒 {{ t('collab.acquireLock') }}
        </button>
      </div>
    </div>

    <!-- 版本历史 -->
    <div class="section">
      <div class="section-header">
        <h4>📜 {{ t('collab.versionHistory') }}</h4>
        <button @click="createVersion" class="create-btn">+ {{ t('collab.newVersion') }}</button>
      </div>
      <div class="version-list">
        <div v-if="versions.length === 0" class="empty-state">
          {{ t('collab.noVersions') }}
        </div>
        <div v-for="v in versions" :key="v.timestamp" class="version-item">
          <div class="version-info">
            <strong>v{{ v.timestamp }}</strong>
            <span>{{ v.userName || v.userId }}</span>
            <span class="version-time">{{ formatTime(v.timestamp) }}</span>
          </div>
          <div class="version-actions">
            <button @click="rollback(v.timestamp)" class="rollback-btn">
              {{ t('collab.rollback') }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 评论 -->
    <div class="section">
      <h4>💬 {{ t('collab.comments') }}</h4>
      <div class="comment-input">
        <input v-model="newComment" :placeholder="t('collab.commentPlaceholder')" @keyup.enter="submitComment" />
        <button @click="submitComment" :disabled="!newComment.trim()">
          {{ t('collab.submit') }}
        </button>
      </div>
      <div class="comment-list">
        <div v-if="comments.length === 0" class="empty-state">
          {{ t('collab.noComments') }}
        </div>
        <div v-for="c in comments" :key="c.id" class="comment-item">
          <div class="comment-header">
            <strong>{{ c.userName }}</strong>
            <span class="comment-time">{{ formatTime(c.createdAt) }}</span>
          </div>
          <p>{{ c.content }}</p>
          <div class="comment-actions">
            <button @click="replyTo(c.id)" class="reply-btn">{{ t('collab.reply') }}</button>
          </div>
          <!-- 回复列表 -->
          <div v-if="c.replies && c.replies.length > 0" class="reply-list">
            <div v-for="r in c.replies" :key="r.id" class="reply-item">
              <strong>{{ r.userName }}</strong>
              <span>{{ r.content }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 标注 -->
    <div class="section">
      <div class="section-header">
        <h4>📍 {{ t('collab.annotations') }}</h4>
        <button @click="showAnnotationForm = !showAnnotationForm" class="create-btn">
          + {{ t('collab.addAnnotation') }}
        </button>
      </div>
      <div v-if="showAnnotationForm" class="annotation-form">
        <select v-model="annotationType" class="annotation-select">
          <option value="point">{{ t('collab.typePoint') }}</option>
          <option value="line">{{ t('collab.typeLine') }}</option>
          <option value="polygon">{{ t('collab.typePolygon') }}</option>
          <option value="text">{{ t('collab.typeText') }}</option>
        </select>
        <input v-model="annotationText" :placeholder="t('collab.annotationPlaceholder')" />
        <button @click="submitAnnotation" class="submit-annotation-btn">
          {{ t('collab.submit') }}
        </button>
      </div>
      <div class="annotation-list">
        <div v-if="annotations.length === 0" class="empty-state">
          {{ t('collab.noAnnotations') }}
        </div>
        <div v-for="a in annotations" :key="a.id" class="annotation-item">
          <span class="annotation-type">{{ annotationIcon(a.type) }}</span>
          <span class="annotation-info">
            <strong>{{ a.userName || a.userId }}</strong>
            <span>{{ a.type }}</span>
          </span>
          <button @click="deleteAnnotation(a.id)" class="delete-annotation-btn">🗑</button>
        </div>
      </div>
    </div>

    <!-- 参与者 -->
    <div class="section">
      <h4>👤 {{ t('collab.participants') }}</h4>
      <div class="participant-list">
        <div v-if="participants.length === 0" class="empty-state">
          {{ t('collab.noParticipants') }}
        </div>
        <div v-for="p in participants" :key="p" class="participant-item">
          <span class="online-dot"></span>
          <span>{{ p }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'

const props = defineProps<{ docId: string }>()
const emit = defineEmits(['close'])

const t = (key: string) => key

const lockInfo = ref<any>(null)
const hasLock = ref(false)
const canUnlock = ref(false)
const versions = ref<any[]>([])
const comments = ref<any[]>([])
const newComment = ref('')
const replyingTo = ref<string | null>(null)
const annotations = ref<any[]>([])
const showAnnotationForm = ref(false)
const annotationType = ref('point')
const annotationText = ref('')
const participants = ref<string[]>([])
const currentUserId = 'current-user' // TODO: 从认证上下文获取

const apiBase = '/api/v1/collab'

onMounted(() => {
  fetchLockStatus()
  fetchVersions()
  fetchComments()
  fetchAnnotations()
  fetchParticipants()
})

async function fetchLockStatus() {
  try {
    const res = await fetch(`${apiBase}/documents/${props.docId}/lock`)
    if (res.ok) {
      lockInfo.value = await res.json()
    } else {
      lockInfo.value = null
    }
    hasLock.value = lockInfo.value?.lockedBy === currentUserId
    canUnlock.value = hasLock.value
  } catch {}
}

async function fetchVersions() {
  try {
    const res = await fetch(`${apiBase}/documents/${props.docId}/versions`)
    if (res.ok) {
      versions.value = await res.json()
    }
  } catch {}
}

async function fetchComments() {
  try {
    const res = await fetch(`${apiBase}/documents/${props.docId}/comments`)
    if (res.ok) {
      comments.value = await res.json()
    }
  } catch {}
}

async function fetchAnnotations() {
  try {
    const res = await fetch(`${apiBase}/documents/${props.docId}/annotations`)
    if (res.ok) {
      annotations.value = await res.json()
    }
  } catch {}
}

async function fetchParticipants() {
  try {
    const res = await fetch(`${apiBase}/documents/${props.docId}/participants`)
    if (res.ok) {
      participants.value = await res.json()
    }
  } catch {}
}

async function acquireLock() {
  await fetch(`${apiBase}/documents/${props.docId}/lock?userId=${currentUserId}`, { method: 'POST' })
  await fetchLockStatus()
}

async function releaseLock() {
  await fetch(`${apiBase}/documents/${props.docId}/lock?userId=${currentUserId}`, { method: 'DELETE' })
  await fetchLockStatus()
}

async function createVersion() {
  // 通过更新文档触发版本记录
  await fetch(`${apiBase}/documents/${props.docId}?userId=${currentUserId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ changeSummary: '手动创建版本快照' })
  })
  await fetchVersions()
}

async function rollback(version: number) {
  await fetch(`${apiBase}/documents/${props.docId}/rollback?userId=${currentUserId}&version=${version}`, {
    method: 'POST'
  })
  await fetchVersions()
}

async function submitComment() {
  if (!newComment.value.trim()) return
  await fetch(`${apiBase}/documents/${props.docId}/comments?userId=${currentUserId}&parentId=${replyingTo.value || ''}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ content: newComment.value })
  })
  newComment.value = ''
  replyingTo.value = null
  await fetchComments()
}

async function replyTo(commentId: string) {
  replyingTo.value = commentId
  newComment.value = ''
}

async function submitAnnotation() {
  await fetch(`${apiBase}/documents/${props.docId}/annotations?userId=${currentUserId}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      type: annotationType.value,
      properties: { text: annotationText.value }
    })
  })
  annotationText.value = ''
  showAnnotationForm.value = false
  await fetchAnnotations()
}

async function deleteAnnotation(annotationId: string) {
  await fetch(`${apiBase}/documents/${props.docId}/annotations/${annotationId}?userId=${currentUserId}`, {
    method: 'DELETE'
  })
  await fetchAnnotations()
}

function formatTime(ts: number) {
  if (!ts) return '-'
  return new Date(ts).toLocaleString()
}

function annotationIcon(type: string) {
  const icons: Record<string, string> = { point: '📍', line: '〰️', polygon: '⬡', text: '💬' }
  return icons[type] || '📍'
}
</script>

<style scoped>
.collab-panel { background: white; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.15); max-height: 80vh; overflow-y: auto; font-family: inherit; }
.panel-header { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; border-bottom: 1px solid #e2e8f0; }
.panel-header h3 { font-size: 15px; font-weight: 600; margin: 0; }
.close-btn { background: none; border: none; font-size: 20px; cursor: pointer; color: #64748b; padding: 0; line-height: 1; }
.section { padding: 12px 16px; border-bottom: 1px solid #f1f5f9; }
.section:last-child { border-bottom: none; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.section h4 { font-size: 13px; font-weight: 600; margin: 0 0 8px 0; }
.empty-state { color: #94a3b8; font-size: 12px; text-align: center; padding: 12px 0; }

/* Lock */
.lock-card, .no-lock { background: #f8fafc; border-radius: 8px; padding: 10px; font-size: 12px; }
.lock-card p, .no-lock p { margin: 4px 0; }
.lock-btn, .unlock-btn, .create-btn {
  margin-top: 8px; padding: 6px 12px; border-radius: 6px;
  border: 1px solid #e2e8f0; background: white; cursor: pointer; font-size: 12px;
}
.lock-btn { background: #eff6ff; border-color: #2563eb; color: #2563eb; }
.unlock-btn { color: #dc2626; border-color: #fecaca; }

/* Version */
.version-list { display: flex; flex-direction: column; gap: 6px; }
.version-item { display: flex; justify-content: space-between; align-items: center; padding: 8px; background: #f8fafc; border-radius: 6px; font-size: 12px; }
.version-info { display: flex; gap: 8px; align-items: center; }
.version-time { color: #94a3b8; font-size: 11px; }
.rollback-btn { padding: 3px 8px; border: 1px solid #e2e8f0; border-radius: 4px; background: white; cursor: pointer; font-size: 11px; }

/* Comment */
.comment-input { display: flex; gap: 6px; margin-bottom: 10px; }
.comment-input input { flex: 1; padding: 7px 10px; border: 1px solid #e2e8f0; border-radius: 6px; font-size: 13px; outline: none; }
.comment-input input:focus { border-color: #2563eb; }
.comment-input button { padding: 7px 14px; background: #2563eb; color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 12px; }
.comment-input button:disabled { opacity: 0.5; cursor: not-allowed; }
.comment-list { display: flex; flex-direction: column; gap: 8px; max-height: 300px; overflow-y: auto; }
.comment-item { padding: 10px; background: #f8fafc; border-radius: 8px; font-size: 12px; }
.comment-header { display: flex; gap: 8px; align-items: center; margin-bottom: 4px; }
.comment-time { color: #94a3b8; font-size: 11px; }
.comment-item p { margin: 4px 0; }
.comment-actions { margin-top: 4px; }
.reply-btn { background: none; border: none; color: #2563eb; cursor: pointer; font-size: 11px; padding: 0; }
.reply-list { margin-top: 8px; padding-left: 12px; border-left: 2px solid #e2e8f0; }
.reply-item { font-size: 11px; padding: 4px 0; }

/* Annotation */
.annotation-form { display: flex; flex-direction: column; gap: 6px; margin-bottom: 10px; }
.annotation-select { padding: 6px 8px; border: 1px solid #e2e8f0; border-radius: 6px; font-size: 12px; }
.annotation-form input { padding: 6px 8px; border: 1px solid #e2e8f0; border-radius: 6px; font-size: 12px; }
.submit-annotation-btn { padding: 6px 12px; background: #2563eb; color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 12px; }
.annotation-list { display: flex; flex-direction: column; gap: 4px; }
.annotation-item { display: flex; align-items: center; gap: 8px; padding: 6px 8px; background: #f8fafc; border-radius: 6px; font-size: 12px; }
.annotation-type { font-size: 14px; }
.annotation-info { flex: 1; display: flex; gap: 8px; align-items: center; }
.annotation-info span { color: #64748b; }
.delete-annotation-btn { background: none; border: none; cursor: pointer; font-size: 12px; padding: 0; }

/* Participants */
.participant-list { display: flex; flex-direction: column; gap: 4px; }
.participant-item { display: flex; align-items: center; gap: 8px; font-size: 12px; padding: 4px 0; }
.online-dot { width: 8px; height: 8px; border-radius: 50%; background: #22c55e; display: inline-block; }
</style>
