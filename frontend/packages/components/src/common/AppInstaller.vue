<template>
  <Transition name="installer">
    <div v-if="showInstaller" class="app-installer">
      <div class="installer-icon">🗺️</div>
      <div class="installer-content">
        <h3>安装 GeoNexus</h3>
        <p>添加到主屏幕，更快访问</p>
      </div>
      <div class="installer-actions">
        <button @click="install" class="install-btn">安装</button>
        <button @click="dismiss" class="dismiss-btn">稍后</button>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

const showInstaller = ref(false)
let deferredPrompt: BeforeInstallPromptEvent | null = null

interface BeforeInstallPromptEvent extends Event {
  prompt(): Promise<void>
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>
}

window.addEventListener('beforeinstallprompt', (e) => {
  e.preventDefault()
  deferredPrompt = e as BeforeInstallPromptEvent
  // 已在桌面版使用过则不再提示
  if (!localStorage.getItem('geoinstall-dismissed')) {
    showInstaller.value = true
  }
})

async function install() {
  if (!deferredPrompt) return
  await deferredPrompt.prompt()
  const { outcome } = await deferredPrompt.userChoice
  if (outcome === 'accepted') {
    showInstaller.value = false
  }
  deferredPrompt = null
}

function dismiss() {
  showInstaller.value = false
  localStorage.setItem('geoinstall-dismissed', 'true')
}
</script>

<style scoped>
.app-installer {
  position: fixed;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  background: #1e293b;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.24);
  z-index: 9998;
  min-width: 320px;
}

.installer-icon {
  font-size: 2rem;
  flex-shrink: 0;
}

.installer-content {
  flex: 1;
}

.installer-content h3 {
  font-size: 1rem;
  font-weight: 600;
  color: white;
  margin: 0 0 4px 0;
}

.installer-content p {
  font-size: 0.8rem;
  color: #94a3b8;
  margin: 0;
}

.installer-actions {
  display: flex;
  gap: 8px;
}

.install-btn {
  padding: 8px 16px;
  background: #2563eb;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.install-btn:hover {
  background: #1d4ed8;
}

.dismiss-btn {
  padding: 8px 12px;
  background: transparent;
  color: #94a3b8;
  border: 1px solid #334155;
  border-radius: 8px;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.2s;
}

.dismiss-btn:hover {
  background: #334155;
  color: white;
}

/* Transition animations */
.installer-enter-active,
.installer-leave-active {
  transition: all 0.3s ease;
}

.installer-enter-from,
.installer-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(20px);
}

/* Mobile responsive */
@media (max-width: 480px) {
  .app-installer {
    left: 16px;
    right: 16px;
    transform: none;
    min-width: unset;
  }

  .installer-enter-from,
  .installer-leave-to {
    transform: translateY(20px);
  }
}
</style>
