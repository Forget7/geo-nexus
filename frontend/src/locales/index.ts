import { createI18n } from 'vue-i18n'
import zhCN from './zh-CN.json'
import enUS from './en-US.json'

export type Locale = 'zh-CN' | 'en-US'

const savedLocale = localStorage.getItem('geonexus-locale') as Locale | null

export const i18n = createI18n({
  legacy: false,
  locale: savedLocale ?? 'zh-CN',
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS,
  },
})

export function setLocale(locale: Locale) {
  i18n.global.locale.value = locale
  localStorage.setItem('geonexus-locale', locale)
  document.documentElement.lang = locale
}

export function getLocale(): Locale {
  return i18n.global.locale.value as Locale
}
