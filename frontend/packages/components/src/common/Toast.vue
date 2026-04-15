<script setup lang="ts">
import { ref, computed } from 'vue'

export type ToastType = 'success' | 'error' | 'info' | 'warning'

export interface ToastItem {
  id: number
  message: string
  type: ToastType
  duration: number
}

const toasts = ref<ToastItem[]>([])
let nextId = 0

function show(message: string, type: ToastType = 'info', duration = 3000) {
  const id = nextId++
  toasts.value.push({ id, message, type, duration })
  setTimeout(() => {
    remove(id)
  }, duration)
}

function remove(id: number) {
  const idx = toasts.value.findIndex(t => t.id === id)
  if (idx !== -1) toasts.value.splice(idx, 1)
}

function success(msg: string, duration?: number) { show(msg, 'success', duration) }
function error(msg: string, duration?: number) { show(msg, 'error', duration) }
function info(msg: string, duration?: number) { show(msg, 'info', duration) }
function warning(msg: string, duration?: number) { show(msg, 'warning', duration) }

defineExpose({ show, success, error, info, warning })
</script>

<template>
  <Teleport to="body">
    <div class="toast-container">
      <TransitionGroup name="toast">
        <div
          v-for="toast in toasts"
          :key="toast.id"
          :class="['toast', `toast-${toast.type}`]"
          @click="remove(toast.id)"
        >
          <span class="toast-icon">
            <template v-if="toast.type === 'success'">✓</template>
            <template v-else-if="toast.type === 'error'">✕</template>
            <template v-else-if="toast.type === 'warning'">⚠</template>
            <template v-else>ℹ</template>
          </span>
          <span class="toast-message">{{ toast.message }}</span>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<style scoped>
.toast-container {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 8px;
  pointer-events: none;
}

.toast {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 18px;
  border-radius: 10px;
  font-size: 0.9rem;
  font-weight: 500;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  cursor: pointer;
  pointer-events: all;
  max-width: 360px;
  min-width: 220px;
}

.toast-icon {
  font-size: 1rem;
  flex-shrink: 0;
}

.toast-success {
  background: #ecfdf5;
  color: #059669;
  border: 1px solid #a7f3d0;
}

.toast-error {
  background: #fef2f2;
  color: #dc2626;
  border: 1px solid #fecaca;
}

.toast-warning {
  background: #fffbeb;
  color: #d97706;
  border: 1px solid #fde68a;
}

.toast-info {
  background: #eff6ff;
  color: #2563eb;
  border: 1px solid #bfdbfe;
}

/* Transition animations */
.toast-enter-active {
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.toast-leave-active {
  transition: all 0.2s ease-in;
}

.toast-enter-from {
  opacity: 0;
  transform: translateX(100%);
}

.toast-leave-to {
  opacity: 0;
  transform: translateX(100%);
}
</style>
