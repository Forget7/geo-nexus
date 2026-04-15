<script setup lang="ts">
import { ref } from 'vue'
import { setLocale, getLocale, type Locale } from '../../../../src/locales'

const currentLocale = ref<Locale>(getLocale())

const languages = [
  { value: 'zh-CN' as Locale, label: '中文', flag: '🇨🇳' },
  { value: 'en-US' as Locale, label: 'English', flag: '🇺🇸' },
]

function switchLanguage(locale: Locale) {
  currentLocale.value = locale
  setLocale(locale)
}
</script

<template>
  <el-dropdown trigger="click" @command="switchLanguage">
    <span class="lang-switcher">
      <span class="lang-flag">{{ languages.find(l => l.value === currentLocale)?.flag }}</span>
      <span class="lang-label">{{ languages.find(l => l.value === currentLocale)?.label }}</span>
    </span>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item
          v-for="lang in languages"
          :key="lang.value"
          :command="lang.value"
          :class="{ 'is-active': lang.value === currentLocale }"
        >
          <span class="lang-flag">{{ lang.flag }}</span>
          {{ lang.label }}
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<style scoped>
.lang-switcher {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 14px;
  color: var(--el-text-color-regular);
  transition: background-color 0.2s;
}

.lang-switcher:hover {
  background-color: var(--el-fill-color-light);
}

.lang-flag {
  font-size: 16px;
}

.lang-label {
  font-size: 13px;
}

:deep(.el-dropdown-menu__item.is-active) {
  color: var(--el-color-primary);
  font-weight: 500;
}
</style>
