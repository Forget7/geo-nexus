import { Ref, onMounted, onUnmounted } from 'vue'

/**
 * 移动端地图手势支持
 * 支持双指缩放和惯性滑动
 */
export function useMapGestures(element: Ref<HTMLElement | null>, map: Ref<any>) {
  let startX = 0
  let startY = 0
  let initialZoom = 0
  let lastX = 0
  let lastY = 0
  let velocityX = 0
  let velocityY = 0
  let animationFrame: number | null = null

  function handleTouchStart(e: TouchEvent) {
    if (e.touches.length !== 2) return
    e.preventDefault()

    startX = (e.touches[0].clientX + e.touches[1].clientX) / 2
    startY = (e.touches[0].clientY + e.touches[1].clientY) / 2
    lastX = startX
    lastY = startY
    initialZoom = map.value?.getZoom() || 10
    velocityX = 0
    velocityY = 0

    // Stop any ongoing animation
    if (animationFrame !== null) {
      cancelAnimationFrame(animationFrame)
      animationFrame = null
    }
  }

  function handleTouchMove(e: TouchEvent) {
    if (e.touches.length !== 2) return
    e.preventDefault()

    const currentX = (e.touches[0].clientX + e.touches[1].clientX) / 2
    const currentY = (e.touches[0].clientY + e.touches[1].clientY) / 2
    const deltaX = currentX - startX
    const deltaY = currentY - startY

    // 计算速度用于惯性滑动
    velocityX = currentX - lastX
    velocityY = currentY - lastY
    lastX = currentX
    lastY = currentY

    // 惯性滑动
    map.value?.panBy({ x: -deltaX * 0.5, y: -deltaY * 0.5 })
  }

  function handlePinch(e: TouchEvent) {
    e.preventDefault()
    const scale = e.scale
    if (map.value) {
      map.value.setZoom(initialZoom + Math.log2(scale) * 2)
    }
  }

  function applyInertia() {
    if (Math.abs(velocityX) < 0.5 && Math.abs(velocityY) < 0.5) {
      animationFrame = null
      return
    }

    map.value?.panBy({ x: -velocityX * 0.5, y: -velocityY * 0.5 })

    // 衰减
    velocityX *= 0.9
    velocityY *= 0.9

    animationFrame = requestAnimationFrame(applyInertia)
  }

  function handleTouchEnd(e: TouchEvent) {
    if (e.touches.length < 2) {
      // 启动惯性动画
      if (Math.abs(velocityX) > 2 || Math.abs(velocityY) > 2) {
        applyInertia()
      }
    }
  }

  onMounted(() => {
    const el = element.value
    if (!el) return
    el.addEventListener('touchstart', handleTouchStart, { passive: false })
    el.addEventListener('touchmove', handleTouchMove, { passive: false })
    el.addEventListener('touchmove', handlePinch, { passive: false })
    el.addEventListener('touchend', handleTouchEnd, { passive: false })
  })

  onUnmounted(() => {
    const el = element.value
    if (!el) return
    el.removeEventListener('touchstart', handleTouchStart)
    el.removeEventListener('touchmove', handleTouchMove)
    el.removeEventListener('touchmove', handlePinch)
    el.removeEventListener('touchend', handleTouchEnd)
    if (animationFrame !== null) {
      cancelAnimationFrame(animationFrame)
    }
  })
}
