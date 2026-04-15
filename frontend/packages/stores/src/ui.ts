/**
 * UI状态管理 - Pinia Store
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface Breadcrumb {
  path: string
  title: string
  icon?: string
}

export interface Notification {
  id: string
  type: 'success' | 'error' | 'warning' | 'info'
  title: string
  message?: string
  duration?: number
  dismissible?: boolean
}

export interface ModalState {
  visible: boolean
  title?: string
  content?: string
  size?: 'small' | 'medium' | 'large' | 'fullscreen'
  closable?: boolean
}

export interface LoadingState {
  global: boolean
  actions: Map<string, boolean>
}

export const useUIStore = defineStore('ui', () => {
  // ========== 状态 ==========
  
  // 侧边栏
  const sidebarCollapsed = ref(false)
  const sidebarWidth = ref(80)
  
  // 主题
  const theme = ref<'light' | 'dark' | 'auto'>('light')
  const primaryColor = ref('#2563eb')
  
  // 面包屑
  const breadcrumbs = ref<Breadcrumb[]>([])
  
  // 通知
  const notifications = ref<Notification[]>([])
  
  // 模态框
  const modal = ref<ModalState>({
    visible: false,
    title: '',
    size: 'medium',
    closable: true
  })
  
  // 全局加载
  const loading = ref<LoadingState>({
    global: false,
    actions: new Map()
  })
  
  // 响应式断点
  const windowSize = ref({
    width: window.innerWidth,
    height: window.innerHeight
  })
  
  // 滚动位置
  const scrollPositions = ref<Map<string, number>>(new Map())
  
  // 快捷键提示
  const showKeyboardShortcuts = ref(false)
  
  // ========== 计算属性 ==========
  
  const isMobile = computed(() => windowSize.value.width < 768)
  const isTablet = computed(() => windowSize.value.width >= 768 && windowSize.value.width < 1024)
  const isDesktop = computed(() => windowSize.value.width >= 1024)
  
  const isDarkMode = computed(() => {
    if (theme.value === 'auto') {
      return window.matchMedia('(prefers-color-scheme: dark)').matches
    }
    return theme.value === 'dark'
  })
  
  const effectiveSidebarWidth = computed(() => 
    sidebarCollapsed.value ? 64 : sidebarWidth.value
  )
  
  const hasNotifications = computed(() => notifications.value.length > 0)
  
  const isLoading = computed(() => loading.value.global)
  
  const isAnyActionLoading = computed(() => 
    Array.from(loading.value.actions.values()).some(v => v)
  )
  
  // ========== 方法 ==========
  
  // 侧边栏
  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }
  
  function setSidebarCollapsed(collapsed: boolean) {
    sidebarCollapsed.value = collapsed
  }
  
  // 主题
  function setTheme(newTheme: 'light' | 'dark' | 'auto') {
    theme.value = newTheme
    applyTheme()
  }
  
  function setPrimaryColor(color: string) {
    primaryColor.value = color
    document.documentElement.style.setProperty('--color-primary', color)
  }
  
  function applyTheme() {
    const root = document.documentElement
    
    if (theme.value === 'auto') {
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
      root.setAttribute('data-theme', prefersDark ? 'dark' : 'light')
    } else {
      root.setAttribute('data-theme', theme.value)
    }
  }
  
  // 面包屑
  function setBreadcrumbs(items: Breadcrumb[]) {
    breadcrumbs.value = items
  }
  
  function addBreadcrumb(item: Breadcrumb) {
    breadcrumbs.value.push(item)
  }
  
  function clearBreadcrumbs() {
    breadcrumbs.value = []
  }
  
  // 通知
  function showNotification(notification: Omit<Notification, 'id'>) {
    const id = `notif-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
    
    notifications.value.push({
      ...notification,
      id,
      dismissible: notification.dismissible ?? true,
      duration: notification.duration ?? 5000
    })
    
    // 自动移除
    if (notification.duration && notification.duration > 0) {
      setTimeout(() => {
        removeNotification(id)
      }, notification.duration)
    }
    
    return id
  }
  
  function removeNotification(id: string) {
    const index = notifications.value.findIndex(n => n.id === id)
    if (index > -1) {
      notifications.value.splice(index, 1)
    }
  }
  
  function clearNotifications() {
    notifications.value = []
  }
  
  // 快捷方法
  function notifySuccess(title: string, message?: string) {
    return showNotification({ type: 'success', title, message })
  }
  
  function notifyError(title: string, message?: string) {
    return showNotification({ type: 'error', title, message, duration: 8000 })
  }
  
  function notifyWarning(title: string, message?: string) {
    return showNotification({ type: 'warning', title, message })
  }
  
  function notifyInfo(title: string, message?: string) {
    return showNotification({ type: 'info', title, message })
  }
  
  // 模态框
  function openModal(options: Partial<ModalState> = {}) {
    modal.value = {
      visible: true,
      title: options.title || '',
      content: options.content || '',
      size: options.size || 'medium',
      closable: options.closable !== false
    }
  }
  
  function closeModal() {
    modal.value.visible = false
  }
  
  // 全屏模态
  function openFullscreenModal(title?: string) {
    openModal({ title, size: 'fullscreen' })
  }
  
  // 加载状态
  function setGlobalLoading(loadingState: boolean) {
    loading.value.global = loadingState
  }
  
  function startAction(actionId: string) {
    loading.value.actions.set(actionId, true)
  }
  
  function endAction(actionId: string) {
    loading.value.actions.set(actionId, false)
  }
  
  function withLoading<T>(actionId: string, fn: () => Promise<T>): Promise<T> {
    startAction(actionId)
    return fn().finally(() => endAction(actionId))
  }
  
  // 滚动位置
  function saveScrollPosition(key: string, position: number) {
    scrollPositions.value.set(key, position)
  }
  
  function getScrollPosition(key: string): number {
    return scrollPositions.value.get(key) || 0
  }
  
  // 窗口大小
  function updateWindowSize(width: number, height: number) {
    windowSize.value = { width, height }
  }
  
  // 快捷键提示
  function toggleKeyboardShortcuts() {
    showKeyboardShortcuts.value = !showKeyboardShortcuts.value
  }
  
  // 初始化
  function init() {
    // 监听窗口大小变化
    window.addEventListener('resize', () => {
      updateWindowSize(window.innerWidth, window.innerHeight)
    })
    
    // 监听主题变化
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
      if (theme.value === 'auto') {
        applyTheme()
      }
    })
    
    // 应用初始主题
    applyTheme()
  }
  
  return {
    // 状态
    sidebarCollapsed,
    sidebarWidth,
    theme,
    primaryColor,
    breadcrumbs,
    notifications,
    modal,
    loading,
    windowSize,
    scrollPositions,
    showKeyboardShortcuts,
    
    // 计算属性
    isMobile,
    isTablet,
    isDesktop,
    isDarkMode,
    effectiveSidebarWidth,
    hasNotifications,
    isLoading,
    isAnyActionLoading,
    
    // 方法
    toggleSidebar,
    setSidebarCollapsed,
    setTheme,
    setPrimaryColor,
    applyTheme,
    setBreadcrumbs,
    addBreadcrumb,
    clearBreadcrumbs,
    showNotification,
    removeNotification,
    clearNotifications,
    notifySuccess,
    notifyError,
    notifyWarning,
    notifyInfo,
    openModal,
    closeModal,
    openFullscreenModal,
    setGlobalLoading,
    startAction,
    endAction,
    withLoading,
    saveScrollPosition,
    getScrollPosition,
    updateWindowSize,
    toggleKeyboardShortcuts,
    init
  }
})
