<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { useChatStore } from '../../packages/stores/src/chat'

const emit = defineEmits<{
  (e: 'map-generated', url: string): void
}>()

const { t } = useI18n()
const chatStore = useChatStore()

const inputMessage = ref('')
const mapMode = ref<'2d' | '3d'>('2d')
const messagesContainer = ref<HTMLElement | null>(null)

const examples = [
  '找出天安门10公里内的医院',
  '计算北京到上海的距离',
  '生成一张中国地图'
]

// Scroll to bottom when new messages arrive
watch(() => chatStore.messages.length, async () => {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
})

async function sendMessage() {
  const content = inputMessage.value.trim()
  if (!content || chatStore.isLoading) return

  inputMessage.value = ''
  await chatStore.sendMessage(content, { mapMode: mapMode.value })

  if (chatStore.currentMapUrl) {
    emit('map-generated', chatStore.currentMapUrl)
  }
}

function useExample(example: string) {
  inputMessage.value = example
  sendMessage()
}

function clearChat() {
  chatStore.clearMessages()
}
</script>

<template>
  <div class="chat-panel">
    <!-- 消息列表 -->
    <div ref="messagesContainer" class="messages" v-if="chatStore.hasMessages || chatStore.isLoading">
      <!-- 骨架屏（首次加载时显示） -->
      <div v-if="chatStore.isLoading && chatStore.messages.length === 0" class="message-skeleton">
        <div class="skeleton-msg skeleton-assistant">
          <div class="skeleton-avatar"></div>
          <div class="skeleton-content">
            <div class="skeleton-line long"></div>
            <div class="skeleton-line medium"></div>
          </div>
        </div>
        <div class="skeleton-msg skeleton-user">
          <div class="skeleton-content">
            <div class="skeleton-line long"></div>
          </div>
        </div>
        <div class="skeleton-msg skeleton-assistant">
          <div class="skeleton-avatar"></div>
          <div class="skeleton-content">
            <div class="skeleton-line short"></div>
          </div>
        </div>
      </div>

      <!-- 消息 -->
      <TransitionGroup name="message" tag="div">
        <div
          v-for="msg in chatStore.messages"
          :key="msg.id"
          :class="['message', msg.role, { 'has-error': msg.error }]"
        >
          <div class="message-avatar">
            <template v-if="msg.role === 'user'">👤</template>
            <template v-else-if="msg.loading">⏳</template>
            <template v-else-if="msg.error">⚠️</template>
            <template v-else>🤖</template>
          </div>
          <div class="message-content">
            <!-- Error banner -->
            <div v-if="msg.error" class="error-banner">
              <span>⚠️ {{ t('common.error') }}</span>
            </div>
            <div class="message-text" :class="{ 'typewriter': msg.role === 'assistant' && !msg.loading }">
              <span v-if="msg.loading" class="typing">
                <span></span><span></span><span></span>
              </span>
              <span v-else v-html="msg.content.replace(/\n/g, '<br>')"></span>
            </div>
            <div v-if="msg.mapUrl && !msg.loading" class="message-actions">
              <button @click="$emit('map-generated', msg.mapUrl)" class="btn-map">
                🗺️ {{ t('chat.viewMap') }}
              </button>
            </div>
          </div>
        </div>
      </TransitionGroup>
    </div>

    <!-- 空状态 -->
    <div v-else class="empty-state">
      <div class="empty-icon">🗺️</div>
      <h3>{{ t('chat.empty.title') }}</h3>
      <p>{{ t('chat.empty.subtitle') }}</p>
      <div class="quick-questions">
        <button v-for="q in examples" :key="q" @click="useExample(q)">
          {{ q }}
        </button>
      </div>
    </div>

    <!-- 全局错误提示 -->
    <Transition name="slide-down">
      <div v-if="chatStore.error && !chatStore.isLoading" class="global-error">
        <span>⚠️ {{ chatStore.error }}</span>
        <button @click="chatStore.error = null">×</button>
      </div>
    </Transition>

    <!-- 输入区域 -->
    <div class="input-area">
      <div class="input-options">
        <select v-model="mapMode" class="map-mode-select">
          <option value="2d">{{ t('chat.map2d') }}</option>
          <option value="3d">{{ t('chat.map3d') }}</option>
        </select>
      </div>
      <div class="input-row">
        <input
          v-model="inputMessage"
          type="text"
          :placeholder="t('chat.enterQuestion')"
          @keyup.enter="sendMessage"
          :disabled="chatStore.isLoading"
        />
        <button
          @click="sendMessage"
          :disabled="chatStore.isLoading || !inputMessage.trim()"
          class="btn-send"
        >
          <span v-if="chatStore.isLoading" class="btn-spinner"></span>
          <span v-else>{{ t('chat.send') }}</span>
        </button>
      </div>
      <div class="input-actions">
        <button @click="clearChat" class="btn-clear" v-if="chatStore.hasMessages">
          🗑️ {{ t('chat.clearChat') }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f8fafc;
  position: relative;
}

/* ---------- Messages ---------- */
.messages {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
}

.message {
  display: flex;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.message.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.25rem;
  flex-shrink: 0;
}

.message.assistant .message-avatar {
  background: #dbeafe;
}

.message.has-error .message-avatar {
  background: #fee2e2;
}

.message-content {
  max-width: 85%;
}

.message.user .message-content {
  text-align: right;
}

.message-text {
  padding: 0.75rem 1rem;
  border-radius: 12px;
  line-height: 1.6;
  display: inline-block;
  text-align: left;
}

.message.user .message-text {
  background: #2563eb;
  color: white;
  border-bottom-right-radius: 4px;
}

.message.assistant .message-text {
  background: white;
  color: #1e293b;
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}

.message.has-error .message-text {
  background: #fef2f2;
  color: #dc2626;
}

.error-banner {
  font-size: 0.8rem;
  color: #dc2626;
  margin-bottom: 4px;
  padding: 4px 8px;
  background: #fee2e2;
  border-radius: 6px;
  display: inline-block;
}

.message-actions {
  margin-top: 0.5rem;
}

.btn-map {
  padding: 0.4rem 0.8rem;
  background: #10b981;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.875rem;
  transition: background 0.2s;
}

.btn-map:hover {
  background: #059669;
}

/* ---------- Typing indicator ---------- */
.typing {
  display: inline-flex;
  gap: 4px;
  padding: 0.5rem;
}

.typing span {
  width: 8px;
  height: 8px;
  background: #94a3b8;
  border-radius: 50%;
  animation: typing 1.4s infinite;
}

.typing span:nth-child(2) { animation-delay: 0.2s; }
.typing span:nth-child(3) { animation-delay: 0.4s; }

@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-4px); opacity: 1; }
}

/* ---------- Message transition ---------- */
.message-enter-active {
  transition: all 0.3s ease-out;
}
.message-leave-active {
  transition: all 0.2s ease-in;
}
.message-enter-from {
  opacity: 0;
  transform: translateY(10px);
}
.message-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* ---------- Skeleton ---------- */
.message-skeleton {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.skeleton-msg {
  display: flex;
  gap: 0.75rem;
  align-items: flex-start;
}

.skeleton-msg.skeleton-user {
  flex-direction: row-reverse;
}

.skeleton-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #e2e8f0;
  flex-shrink: 0;
}

.skeleton-content {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
  max-width: 70%;
}

.skeleton-user .skeleton-content {
  align-items: flex-end;
}

.skeleton-line {
  height: 14px;
  border-radius: 8px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.skeleton-line.long { width: 80%; }
.skeleton-line.medium { width: 55%; }
.skeleton-line.short { width: 35%; }

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* ---------- Empty State ---------- */
.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 2rem;
  text-align: center;
}

.empty-icon {
  font-size: 3.5rem;
  margin-bottom: 1rem;
}

.empty-state h3 {
  margin: 0 0 0.5rem;
  color: #1e293b;
  font-size: 1.2rem;
}

.empty-state p {
  margin: 0 0 1.5rem;
  color: #64748b;
  font-size: 0.9rem;
}

.quick-questions {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  width: 100%;
  max-width: 320px;
}

.quick-questions button {
  padding: 0.7rem 1rem;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 0.9rem;
  color: #475569;
}

.quick-questions button:hover {
  background: #eff6ff;
  border-color: #2563eb;
  color: #2563eb;
}

/* ---------- Global Error ---------- */
.global-error {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  background: #fef2f2;
  border-bottom: 1px solid #fecaca;
  color: #dc2626;
  font-size: 0.875rem;
}

.global-error button {
  background: transparent;
  border: none;
  color: #dc2626;
  cursor: pointer;
  font-size: 1rem;
  padding: 0 4px;
}

.slide-down-enter-active, .slide-down-leave-active {
  transition: all 0.3s ease;
}
.slide-down-enter-from, .slide-down-leave-to {
  transform: translateY(-100%);
  opacity: 0;
}

/* ---------- Input Area ---------- */
.input-area {
  padding: 1rem;
  background: white;
  border-top: 1px solid #e2e8f0;
}

.input-options {
  margin-bottom: 0.5rem;
}

.map-mode-select {
  padding: 0.4rem 0.6rem;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 0.875rem;
  background: white;
  cursor: pointer;
}

.map-mode-select:focus {
  outline: none;
  border-color: #2563eb;
}

.input-row {
  display: flex;
  gap: 0.5rem;
}

.input-row input {
  flex: 1;
  padding: 0.75rem 1rem;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 1rem;
  outline: none;
  transition: border-color 0.2s;
}

.input-row input:focus {
  border-color: #2563eb;
}

.input-row input:disabled {
  background: #f1f5f9;
  cursor: not-allowed;
}

.btn-send {
  padding: 0.75rem 1.5rem;
  background: #2563eb;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 500;
  font-size: 1rem;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 80px;
}

.btn-send:hover:not(:disabled) {
  background: #1d4ed8;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
}

.btn-send:active:not(:disabled) {
  transform: translateY(0);
}

.btn-send:disabled {
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

.input-actions {
  margin-top: 0.5rem;
  display: flex;
  justify-content: flex-end;
}

.btn-clear {
  padding: 0.4rem 0.8rem;
  background: transparent;
  color: #64748b;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.8rem;
  transition: all 0.2s;
}

.btn-clear:hover {
  background: #f1f5f9;
  color: #dc2626;
}
</style>
