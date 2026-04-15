/**
 * 防抖与节流工具
 */

/**
 * 防抖函数 - 在事件触发n毫秒后执行，n毫秒内再次触发则重新计时
 */
export function debounce<T extends (...args: any[]) => any>(
  func: T,
  wait: number,
  immediate = false
): (...args: Parameters<T>) => void {
  let timeout: ReturnType<typeof setTimeout> | null = null
  let lastResult: ReturnType<T>

  return function (this: any, ...args: Parameters<T>) {
    const context = this

    const later = () => {
      timeout = null
      if (!immediate) {
        lastResult = func.apply(context, args)
      }
    }

    const callNow = immediate && !timeout

    if (timeout) {
      clearTimeout(timeout)
    }

    timeout = setTimeout(later, wait)

    if (callNow) {
      lastResult = func.apply(context, args)
    }

    return lastResult
  }
}

/**
 * 节流函数 - 在n毫秒内只执行一次
 */
export function throttle<T extends (...args: any[]) => any>(
  func: T,
  limit: number
): (...args: Parameters<T>) => void {
  let inThrottle = false
  let lastResult: ReturnType<T>

  return function (this: any, ...args: Parameters<T>) {
    const context = this

    if (!inThrottle) {
      lastResult = func.apply(context, args)
      inThrottle = true

      setTimeout(() => {
        inThrottle = false
      }, limit)
    }

    return lastResult
  }
}

/**
 * 带取消的防抖
 */
export function createDebounced<T extends (...args: any[]) => any>(
  func: T,
  wait: number
): {
  run: (...args: Parameters<T>) => ReturnType<T>
  cancel: () => void
} {
  let timeout: ReturnType<typeof setTimeout> | null = null

  return {
    run: (...args: Parameters<T>) => {
      if (timeout) {
        clearTimeout(timeout)
      }
      timeout = setTimeout(() => {
        func(...args)
        timeout = null
      }, wait)
    },
    cancel: () => {
      if (timeout) {
        clearTimeout(timeout)
        timeout = null
      }
    }
  }
}

/**
 * 带取消的节流
 */
export function createThrottled<T extends (...args: any[]) => any>(
  func: T,
  limit: number
): {
  run: (...args: Parameters<T>) => ReturnType<T> | undefined
  cancel: () => void
} {
  let lastRun = 0
  let timeout: ReturnType<typeof setTimeout> | null = null
  let lastResult: ReturnType<T>

  return {
    run: (...args: Parameters<T>) => {
      const now = Date.now()
      const remaining = limit - (now - lastRun)

      if (remaining <= 0) {
        if (timeout) {
          clearTimeout(timeout)
          timeout = null
        }
        lastRun = now
        lastResult = func(...args)
      } else if (!timeout) {
        timeout = setTimeout(() => {
          lastRun = Date.now()
          timeout = null
          lastResult = func(...args)
        }, remaining)
      }

      return lastResult
    },
    cancel: () => {
      if (timeout) {
        clearTimeout(timeout)
        timeout = null
      }
      lastRun = 0
    }
  }
}

/**
 * 动画帧节流 - 使用requestAnimationFrame
 */
export function rafThrottle<T extends (...args: any[]) => any>(
  func: T
): (...args: Parameters<T>) => void {
  let ticking = false
  let lastArgs: Parameters<T>

  return function (this: any, ...args: Parameters<T>) {
    lastArgs = args

    if (!ticking) {
      requestAnimationFrame(() => {
        func.apply(this, lastArgs)
        ticking = false
      })
      ticking = true
    }
  }
}

/**
 * 组合防抖（首次立即执行，后续防抖）
 */
export function debounceLeading<T extends (...args: any[]) => any>(
  func: T,
  wait: number
): (...args: Parameters<T>) => void {
  let timeout: ReturnType<typeof setTimeout> | null = null

  return function (this: any, ...args: Parameters<T>) {
    const context = this

    if (!timeout) {
      func.apply(context, args)
    }

    if (timeout) {
      clearTimeout(timeout)
    }

    timeout = setTimeout(() => {
      timeout = null
    }, wait)
  }
}
