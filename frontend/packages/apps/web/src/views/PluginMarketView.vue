<template>
  <div class="plugin-market">
    <div class="market-header">
      <h2>🧩 {{ $t('plugin.market') }}</h2>
      <div class="search-bar">
        <input v-model="searchQuery" :placeholder="$t('plugin.searchPlaceholder')" @input="onSearch" />
        <select v-model="selectedCategory" @change="onCategoryChange">
          <option value="">{{ $t('plugin.allCategories') }}</option>
          <option v-for="cat in categories" :key="cat" :value="cat">{{ cat }}</option>
        </select>
      </div>
    </div>

    <div class="plugin-grid">
      <div v-for="plugin in marketplace" :key="plugin.pluginId" class="plugin-card">
        <div class="plugin-icon">{{ plugin.iconUrl ? '🖼️' : '🧩' }}</div>
        <div class="plugin-info">
          <h3>{{ plugin.name }}</h3>
          <p class="plugin-desc">{{ plugin.description }}</p>
          <div class="plugin-meta">
            <span class="tag" v-for="tag in plugin.tags.slice(0,3)" :key="tag">{{ tag }}</span>
            <span class="stats">{{ plugin.installCount }} {{ $t('plugin.installs') }}</span>
          </div>
        </div>
        <button
          v-if="!isInstalled(plugin.pluginId)"
          @click="doInstall(plugin.pluginId)"
          class="install-btn"
        >
          {{ $t('plugin.install') }}
        </button>
        <div v-else class="installed-badge">
          ✅ {{ $t('plugin.installed') }}
        </div>
      </div>
    </div>

    <div v-if="marketplace.length === 0" class="empty-state">
      <p>🧩 {{ $t('plugin.empty') }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { usePluginStore } from '@geonex/stores'

const store = usePluginStore()
const searchQuery = ref('')
const selectedCategory = ref('')
const categories = ['visualization', 'ai', 'data', 'utility', 'security', 'export']

onMounted(() => {
  store.fetchMarketplace()
})

function onSearch() {
  store.fetchMarketplace(searchQuery.value, selectedCategory.value || undefined)
}

function onCategoryChange() {
  store.fetchMarketplace(searchQuery.value || undefined, selectedCategory.value || undefined)
}

function isInstalled(pluginId: string) {
  return store.installed.some(p => p.pluginId === pluginId)
}

async function doInstall(pluginId: string) {
  await store.install(pluginId, 'current-user-id')
}
</script>

<style scoped>
.plugin-market { padding: 24px; max-width: 1200px; margin: 0 auto; }
.market-header { margin-bottom: 24px; }
.market-header h2 { font-size: 24px; margin-bottom: 16px; }
.search-bar { display: flex; gap: 12px; }
.search-bar input {
  flex: 1; padding: 10px 16px; border: 1px solid #e2e8f0;
  border-radius: 8px; font-size: 14px;
}
.search-bar select {
  padding: 10px 16px; border: 1px solid #e2e8f0;
  border-radius: 8px; font-size: 14px;
}
.plugin-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}
.plugin-card {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px;
  background: white;
  display: flex;
  gap: 12px;
}
.plugin-icon {
  width: 48px; height: 48px;
  background: #f1f5f9;
  border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}
.plugin-info { flex: 1; min-width: 0; }
.plugin-info h3 { font-size: 16px; margin-bottom: 4px; }
.plugin-desc {
  font-size: 13px; color: #64748b;
  overflow: hidden; text-overflow: ellipsis;
  white-space: nowrap; margin-bottom: 8px;
}
.plugin-meta { display: flex; flex-wrap: wrap; gap: 4px; align-items: center; }
.tag {
  background: #eff6ff; color: #2563eb;
  font-size: 11px; padding: 2px 8px; border-radius: 10px;
}
.stats { font-size: 11px; color: #94a3b8; margin-left: auto; }
.install-btn {
  align-self: center;
  padding: 8px 16px;
  background: #2563eb; color: white;
  border: none; border-radius: 6px; cursor: pointer; font-size: 13px;
}
.installed-badge {
  align-self: center;
  font-size: 13px; color: #16a34a;
}
.empty-state {
  text-align: center; padding: 60px;
  color: #94a3b8; font-size: 16px;
}
</style>
