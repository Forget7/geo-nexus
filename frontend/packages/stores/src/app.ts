import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAppStore = defineStore('app', () => {
  const sidebar = ref({
    opened: true,
    withoutAnimation: false
  })
  const device = ref<'desktop' | 'mobile'>('desktop')
  const size = ref<'large' | 'default' | 'small'>('default')

  const isMobile = computed(() => device.value === 'mobile')

  function toggleSidebar() {
    sidebar.value.opened = !sidebar.value.opened
  }

  function closeSidebar(withoutAnimation = false) {
    sidebar.value.opened = false
    sidebar.value.withoutAnimation = withoutAnimation
  }

  return {
    sidebar,
    device,
    size,
    isMobile,
    toggleSidebar,
    closeSidebar
  }
})
