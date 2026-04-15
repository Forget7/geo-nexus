<template>
  <div class="gn-streaming-chat">
    <!-- 欢迎界面 -->
    <div v-if="messages.length === 0" class="welcome-container">
      <div class="welcome-icon">🗺️</div>
      <h2 class="welcome-title">{{ t('chat.welcomeTitle') }}</h2>
      <p class="welcome-desc">{{ t('chat.welcomeDesc') }}</p>
      
      <div class="suggestion-list">
        <p class="suggestion-title">{{ t('chat.tryQuestions') }}</p>
        <button 
          v-for="(question, index) in suggestions" 
          :key="index"
          class="suggestion-btn"
          @click="sendMessage(question)"
        >
          {{ question }}
        </button>
      </div>
    </div>

    <!-- 消息列表 -->
    <div v-else ref="messageContainer" class="message-container">
      <div 
        v-for="(msg, index) in messages" 
        :key="index"
        :class="['message-item', msg.role === 'user' ? 'user' : 'assistant']"
      >
        <div class="message-avatar">
          <span v-if="msg.role === 'user'">👤</span>
          <span v-else>🤖</span>
        </div>
        <div class="message-content">
          <div class="message-text" v-html="formatMessage(msg.content)"></div>
          <div v-if="msg.mapUrl" class="message-map">
            <button class="view-map-btn" @click="viewMap(msg.mapUrl)">
              🗺️ {{ t('chat.viewMap') }}
            </button>
          </div>
          <div v-if="msg.status === 'loading'" class="typing-indicator">
            <span></span><span></span><span></span>
          </div>
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="input-container">
      <div class="input-wrapper">
        <textarea
          ref="inputRef"
          v-model="inputText"
          :placeholder="t('chat.enterQuestion')"
          class="chat-input"
          rows="1"
          @keydown.enter.exact.prevent="handleSend"
          @input="autoResize"
        ></textarea>
        <button 
          class="send-btn" 
          :disabled="!inputText.trim() || isLoading"
          @click="handleSend"
        >
          {{ t('chat.send') }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'

defineOptions({
  name: 'GnStreamingChatPanel'
})

const { t } = useI18n()

// 建议问题
const suggestions = [
  '计算北京到上海的距离',
  '如何做缓冲区分析？',
  '生成一张中国地图'
]

// 响应式数据
const inputText = ref('')
const messages = ref<Array<{
  role: 'user' | 'assistant'
  content: string
  mapUrl?: string
  status?: 'loading' | 'done' | 'error'
}>>([])
const isLoading = ref(false)
const sessionId = ref('')
const messageContainer = ref<HTMLElement | null>(null)
const inputRef = ref<HTMLTextAreaElement | null>(null)
let eventSource: EventSource | null = null

// 发送消息
function handleSend() {
  const text = inputText.value.trim()
  if (!text || isLoading.value) return
  sendMessage(text)
}

// 发送消息
async function sendMessage(text: string) {
  if (!sessionId.value) {
    await createSession()
  }
  
  // 添加用户消息
  messages.value.push({
    role: 'user',
    content: text,
    status: 'done'
  })
  
  inputText.value = ''
  scrollToBottom()
  
  // 添加助手消息占位
  const assistantMsg = {
    role: 'assistant' as const,
    content: '',
    status: 'loading' as const
  }
  messages.value.push(assistantMsg)
  
  isLoading.value = true
  
  // 建立SSE连接
  connectSSE(assistantMsg)
}

function connectSSE(assistantMsg: typeof messages.value[0]) {
  if (eventSource) {
    eventSource.close()
  }
  
  const url = `/api/v1/chat/sessions/${sessionId.value}/stream`
  eventSource = new EventSource(url)
  
  let fullContent = ''
  let mapUrl = ''
  
  eventSource.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data)
      
      switch (data.type) {
        case 'processing':
          // 处理中状态
          break
        case 'chunk':
          fullContent += data.content
          assistantMsg.content = fullContent
          scrollToBottom()
          break
        case 'done':
          mapUrl = data.mapUrl || ''
          assistantMsg.content = data.content
          assistantMsg.mapUrl = mapUrl
          assistantMsg.status = 'done'
          isLoading.value = false
          break
        case 'error':
          assistantMsg.content = data.content || t('chat.error')
          assistantMsg.status = 'done'
          isLoading.value = false
          break
      }
    } catch (e) {
      console.error('SSE解析失败:', e)
    }
  }
  
  eventSource.onerror = () => {
    console.error('SSE连接错误')
    eventSource?.close()
    if (assistantMsg.status === 'loading') {
      assistantMsg.content = t('chat.error')
      assistantMsg.status = 'done'
    }
    isLoading.value = false
  }
  
  // 发送消息到后端
  fetch(`/api/v1/chat/sessions/${sessionId.value}/stream`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      message: messages.value[messages.value.length - 2].content,
      model: 'default'
    })
  }).catch(err => {
    console.error('发送消息失败:', err)
    assistantMsg.content = t('chat.error')
    assistantMsg.status = 'done'
    isLoading.value = false
  })
}

// 创建会话
async function createSession() {
  try {
    const res = await fetch('/api/v1/chat/sessions', { method: 'POST' })
    const data = await res.json()
    sessionId.value = data.sessionId
  } catch (e) {
    console.error('创建会话失败:', e)
    // 使用本地会话ID
    sessionId.value = 'local-' + Date.now()
  }
}

// 格式化消息（Markdown简易转换）
function formatMessage(text: string): string {
  if (!text) return ''
  
  return text
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
}

// 查看地图
function viewMap(mapUrl: string) {
  if (mapUrl) {
    window.open(mapUrl, '_blank')
  }
}

// 自动调整输入框高度
function autoResize() {
  const textarea = inputRef.value
  if (textarea) {
    textarea.style.height = 'auto'
    textarea.style.height = Math.min(textarea.scrollHeight, 120) + 'px'
  }
}

// 滚动到底部
function scrollToBottom() {
  nextTick(() => {
    if (messageContainer.value) {
      messageContainer.value.scrollTop = messageContainer.value.scrollHeight
    }
  })
}

// 组件卸载
onUnmounted(() => {
  if (eventSource) {
    eventSource.close()
  }
  if (sessionId.value) {
    fetch(`/api/v1/chat/sessions/${sessionId.value}`, { method: 'DELETE' }).catch(() => {})
  }
})
</script>

<style scoped>
.gn-streaming-chat {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 400px;
  background: #f5f7fa;
  border-radius: 8px;
  overflow: hidden;
}

.welcome-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  text-align: center;
}

.welcome-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.welcome-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0;
}

.welcome-desc {
  font-size: 14px;
  color: #909399;
  margin: 0 0 24px 0;
  max-width: 400px;
}

.suggestion-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
  max-width: 400px;
}

.suggestion-title {
  font-size: 12px;
  color: #909399;
  margin: 0 0 4px 0;
  text-align: left;
}

.suggestion-btn {
  padding: 10px 16px;
  background: white;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  font-size: 13px;
  color: #606266;
  cursor: pointer;
  text-align: left;
  transition: all 0.2s;
}

.suggestion-btn:hover {
  border-color: #409eff;
  color: #409eff;
}

.message-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message-item {
  display: flex;
  gap: 12px;
  max-width: 85%;
}

.message-item.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.message-item.assistant {
  align-self: flex-start;
}

.message-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #e8eef4;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
}

.message-item.user .message-avatar {
  background: #409eff;
}

.message-content {
  flex: 1;
}

.message-text {
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.message-item.user .message-text {
  background: #409eff;
  color: white;
  border-bottom-right-radius: 4px;
}

.message-item.assistant .message-text {
  background: white;
  color: #303133;
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.message-map {
  margin-top: 8px;
}

.view-map-btn {
  padding: 6px 12px;
  background: #f0f9eb;
  border: 1px solid #c2e7b0;
  border-radius: 4px;
  font-size: 12px;
  color: #67c23a;
  cursor: pointer;
}

.view-map-btn:hover {
  background: #e1f3d8;
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 8px 0;
}

.typing-indicator span {
  width: 6px;
  height: 6px;
  background: #909399;
  border-radius: 50%;
  animation: typing 1.4s infinite ease-in-out;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-4px); opacity: 1; }
}

.input-container {
  padding: 12px 16px;
  background: white;
  border-top: 1px solid #e8eef4;
}

.input-wrapper {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.chat-input {
  flex: 1;
  padding: 10px 14px;
  border: 1px solid #dcdfe6;
  border-radius: 20px;
  font-size: 14px;
  line-height: 1.4;
  resize: none;
  outline: none;
  font-family: inherit;
}

.chat-input:focus {
  border-color: #409eff;
}

.send-btn {
  padding: 10px 20px;
  background: #409eff;
  color: white;
  border: none;
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
}

.send-btn:hover:not(:disabled) {
  background: #66b1ff;
}

.send-btn:disabled {
  background: #c0c4cc;
  cursor: not-allowed;
}
</style>
