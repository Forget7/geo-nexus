<script setup lang="ts">
/**
 * VirtualList - 虚拟滚动列表组件
 * 用于大量数据的高效渲染
 */

import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'

export interface VirtualItem extends Record<string, unknown> {
  [key: string]: unknown
}

const props = withDefaults(defineProps<{
  items: VirtualItem[]
  itemHeight?: number
  buffer?: number
  keyField?: string
}>(), {
  itemHeight: 60,
  buffer: 5,
  keyField: 'id'
})

const emit = defineEmits<{
  (e: 'scroll', event: Event): void
  (e: 'reach-end'): void
}>()

const containerRef = ref<HTMLElement | null>(null)
const scrollTop = ref(0)
const containerHeight = ref(0)

// 计算可见区域
const visibleCount = computed(() => 
  Math.ceil(containerHeight.value / props.itemHeight) + props.buffer * 2
)

const startIndex = computed(() => 
  Math.max(0, Math.floor(scrollTop.value / props.itemHeight) - props.buffer)
)

const endIndex = computed(() => 
  Math.min(props.items.length, startIndex.value + visibleCount.value)
)

const visibleItems = computed(() => 
  props.items.slice(startIndex.value, endIndex.value).map((item, index) => ({
    item,
    index: startIndex.value + index,
    style: {
      position: 'absolute' as const,
      top: `${(startIndex.value + index) * props.itemHeight}px`,
      height: `${props.itemHeight}px`,
      left: 0,
      right: 0
    }
  }))
)

const totalHeight = computed(() => props.items.length * props.itemHeight)

// 滚动处理
let ticking = false
function handleScroll(event: Event) {
  if (!ticking) {
    requestAnimationFrame(() => {
      const target = event.target as HTMLElement
      scrollTop.value = target.scrollTop
      emit('scroll', event)
      
      // 检测是否滚动到底部
      if (target.scrollHeight - target.scrollTop - target.clientHeight < 100) {
        emit('reach-end')
      }
      
      ticking = false
    })
    ticking = true
  }
}

// 更新容器高度
function updateContainerHeight() {
  if (containerRef.value) {
    containerHeight.value = containerRef.value.clientHeight
  }
}

// ResizeObserver 监听容器大小变化
let resizeObserver: ResizeObserver | null = null

onMounted(() => {
  updateContainerHeight()
  
  resizeObserver = new ResizeObserver(() => {
    updateContainerHeight()
  })
  
  if (containerRef.value) {
    resizeObserver.observe(containerRef.value)
  }
})

onUnmounted(() => {
  resizeObserver?.disconnect()
})

// 滚动到指定位置
function scrollToIndex(index: number) {
  if (containerRef.value) {
    containerRef.value.scrollTop = index * props.itemHeight
  }
}

// 滚动到顶部
function scrollToTop() {
  scrollToIndex(0)
}

// 滚动到底部
function scrollToBottom() {
  scrollToIndex(props.items.length - 1)
}

// 暴露方法
defineExpose({
  scrollToIndex,
  scrollToTop,
  scrollToBottom
})
</script>

<template>
  <div 
    ref="containerRef"
    class="virtual-list-container"
    @scroll="handleScroll"
  >
    <div class="virtual-list-content" :style="{ height: `${totalHeight}px` }">
      <div 
        v-for="{ item, index, style } in visibleItems" 
        :key="item[keyField] || index"
        :style="style"
        class="virtual-list-item"
      >
        <slot :item="item" :index="index"></slot>
      </div>
    </div>
  </div>
</template>

<style scoped>
.virtual-list-container {
  height: 100%;
  overflow-y: auto;
  position: relative;
}

.virtual-list-content {
  position: relative;
}

.virtual-list-item {
  box-sizing: border-box;
  overflow: hidden;
}
</style>
