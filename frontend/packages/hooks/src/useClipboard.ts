import { ref, onMounted, onUnmounted } from 'vue'

export interface UseClipboardOptions {
  timeout?: number
}

export function useClipboard(options: UseClipboardOptions = {}) {
  const text = ref('')
  const copied = ref(false)
  const error = ref<Error | null>(null)

  async function copy(textToCopy: string) {
    try {
      await navigator.clipboard.writeText(textToCopy)
      text.value = textToCopy
      copied.value = true
      error.value = null

      setTimeout(() => {
        copied.value = false
      }, options.timeout || 2000)
    } catch (e) {
      error.value = e as Error
      copied.value = false
    }
  }

  return {
    text,
    copied,
    error,
    copy
  }
}
