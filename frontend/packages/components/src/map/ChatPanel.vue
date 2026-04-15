<template>
  <div class="gn-chat-panel">
    <div class="panel-header">
      <h3>💬 {{ t('chat.panelTitle') || '智能对话' }}</h3>
      <el-button size="small" @click="clearChat">{{ t('chat.clearChat') }}</el-button>
    </div>

    <div class="messages-container" ref="messagesContainer">
      <div v-if="messages.length === 0" class="empty-state">
        <div class="welcome">
          <h4>{{ t('chat.welcomeTitle') }}</h4>
          <p>{{ t('chat.welcomeDesc') }}</p>
        </div>
        <div class="suggestions">
          <p>{{ t('chat.tryQuestions') }}</p>
          <button
            v-for="q in suggestedQuestions"
            :key="q"
            class="suggestion-btn"
            @click="sendMessage(q)"
          >
            {{ q }}
          </button>
        </div>
      </div>

      <div v-for="(msg, index) in messages" :key="index" :class="['message', msg.role]">
        <div class="message-avatar">
          {{ msg.role === 'user' ? '👤' : '🤖' }}
        </div>
        <div class="message-content">
          <div class="message-text">{{ msg.content }}</div>
          <div v-if="msg.mapUrl" class="message-map">
            <a :href="msg.mapUrl" target="_blank">{{ t('chat.viewMap') }}</a>
          </div>
          <div class="message-time">{{ formatTime(msg.timestamp) }}</div>
        </div>
      </div>

      <div v-if="loading" class="message assistant">
        <div class="message-avatar">🤖</div>
        <div class="message-content">
          <div class="message-text loading">{{ t('chat.thinking') }}</div>
        </div>
      </div>
    </div>

    <div class="input-area">
      <el-input
        v-model="inputMessage"
        type="textarea"
        :rows="2"
        :placeholder="t('chat.enterQuestion')"
        @keydown.enter.ctrl="sendMessage(inputMessage)"
      />
      <el-button type="primary" :loading="loading" @click="sendMessage(inputMessage)">
        {{ t('chat.send') }}
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { chatApi } from '@/service/api'

defineOptions({ name: 'GnChatPanel' })

const { t } = useI18n()

const messagesContainer = ref<HTMLElement>()
const inputMessage = ref('')
const messages = ref<Array<{ role: string; content: string; mapUrl?: string; timestamp: Date }>>([])
const loading = ref(false)
const currentSessionId = ref<string | null>(null)

const suggestedQuestions = [
  '天安门附近500米有什么',
  '计算北京到上海的距离',
  '生成一张3D地图'
]

async function sendMessage(text: string) {
  if (!text.trim() || loading.value) return

  const userMessage = { role: 'user', content: text, timestamp: new Date() }
  messages.value.push(userMessage)
  inputMessage.value = ''
  loading.value = true
  scrollToBottom()

  try {
    const response = await chatApi.sendMessage({
      message: text,
      sessionId: currentSessionId.value || undefined,
      model: 'default',
      mapMode: '2d'
    })

    if (response.data?.data) {
      const { message, sessionId, mapUrl } = response.data.data
      currentSessionId.value = sessionId || currentSessionId.value

      messages.value.push({
        role: 'assistant',
        content: message || '收到您的消息',
        mapUrl,
        timestamp: new Date()
      })
    }
  } catch (error) {
    messages.value.push({
      role: 'assistant',
      content: t('chat.error'),
      timestamp: new Date()
    })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

function clearChat() {
  messages.value = []
  currentSessionId.value = null
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

function formatTime(date: Date) {
  return date.toLocaleTimeString()
}
</script>

<style scoped>
.gn-chat-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--bg-color, #fff);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-color, #e0e0e0);
}

.panel-header h3 {
  margin: 0;
  font-size: 16px;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.empty-state {
  text-align: center;
  padding: 40px 20px;
}

.welcome h4 {
  margin: 0 0 8px;
  color: var(--text-primary, #333);
}

.welcome p {
  color: var(--text-secondary, #666);
  margin-bottom: 24px;
}

.suggestions {
  text-align: left;
}

.suggestions p {
  color: var(--text-secondary, #666);
  margin-bottom: 12px;
}

.suggestion-btn {
  display: block;
  width: 100%;
  padding: 10px 16px;
  margin-bottom: 8px;
  text-align: left;
  background: var(--bg-secondary, #f5f5f5);
  border: 1px solid var(--border-color, #e0e0e0);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.suggestion-btn:hover {
  background: var(--primary-light, #e3f2fd);
  border-color: var(--primary-color, #2196f3);
}

.message {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.message.user {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
  font-size: 24px;
}

.message-content {
  max-width: 80%;
}

.message-text {
  padding: 12px 16px;
  border-radius: 12px;
  background: var(--bg-secondary, #f5f5f5);
  white-space: pre-wrap;
  word-break: break-word;
}

.message.user .message-text {
  background: var(--primary-color, #2196f3);
  color: white;
}

.message-map {
  margin-top: 8px;
}

.message-map a {
  color: var(--primary-color, #2196f3);
}

.message-time {
  font-size: 11px;
  color: var(--text-secondary, #999);
  margin-top: 4px;
}

.message.user .message-time {
  text-align: right;
}

.input-area {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  border-top: 1px solid var(--border-color, #e0e0e0);
}

.input-area .el-textarea {
  flex: 1;
}

.loading {
  opacity: 0.7;
}
</style>
