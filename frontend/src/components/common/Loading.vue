<script setup lang="ts">
/**
 * Loading - 加载状态组件
 */

defineProps<{
  size?: 'small' | 'medium' | 'large'
  fullscreen?: boolean
  text?: string
}>()
</script>

<template>
  <div v-if="fullscreen" class="loading-fullscreen">
    <div class="loading-content">
      <div :class="['loading-spinner', size || 'medium']">
        <div class="spinner"></div>
      </div>
      <p v-if="text" class="loading-text">{{ text }}</p>
    </div>
  </div>
  
  <div v-else :class="['loading', size || 'medium']">
    <div class="spinner"></div>
    <p v-if="text" class="loading-text">{{ text }}</p>
  </div>
</template>

<style scoped>
.loading-fullscreen {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.loading-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
}

.loading {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
}

.loading.small .spinner {
  width: 16px;
  height: 16px;
  border-width: 2px;
}

.loading.medium .spinner {
  width: 24px;
  height: 24px;
  border-width: 3px;
}

.loading.large .spinner {
  width: 40px;
  height: 40px;
  border-width: 4px;
}

.spinner {
  border: 3px solid #e2e8f0;
  border-top-color: #2563eb;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.loading-text {
  margin: 0;
  color: #64748b;
  font-size: 0.875rem;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
