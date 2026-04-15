<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const router = useRouter()

const features = [
  {
    icon: '💬',
    title: 'home.feature.chat',
    description: 'home.feature.chatDesc',
    route: '/chat'
  },
  {
    icon: '🗺️',
    title: 'home.feature.map',
    description: 'home.feature.mapDesc',
    route: '/map'
  },
  {
    icon: '📊',
    title: 'home.feature.analysis',
    description: 'home.feature.analysisDesc',
    route: '/tools'
  },
  {
    icon: '🔄',
    title: 'home.feature.convert',
    description: 'home.feature.convertDesc',
    route: '/data'
  },
  {
    icon: '🗂️',
    title: 'home.feature.template',
    description: 'home.feature.templateDesc',
    route: '/templates'
  }
]

const quickActions = [
  { text: 'home.action.distance', action: () => router.push('/chat?msg=计算北京到上海的距离') },
  { text: 'home.action.mapNearby', action: () => router.push('/chat?msg=生成天安门附近地图') },
  { text: 'home.action.buffer', action: () => router.push('/tools?tool=buffer') }
]
</script>

<template>
  <div class="home-view">
    <!-- Hero Section -->
    <section class="hero">
      <div class="hero-content">
        <h1 class="hero-title">🌐 GeoNexus</h1>
        <p class="hero-subtitle">{{ t('home.subtitle') }}</p>
        <p class="hero-description">
          {{ t('home.description') }}
        </p>
        <div class="hero-actions">
          <button class="btn-primary" @click="router.push('/chat')">
            {{ t('home.startChat') }}
          </button>
          <button class="btn-secondary" @click="router.push('/map')">
            {{ t('home.viewMap') }}
          </button>
        </div>
      </div>
    </section>

    <!-- Features Section -->
    <section class="features">
      <h2 class="section-title">{{ t('home.featuresTitle') }}</h2>
      <div class="features-grid">
        <div 
          v-for="feature in features" 
          :key="feature.title"
          class="feature-card"
          @click="router.push(feature.route)"
        >
          <div class="feature-icon">{{ feature.icon }}</div>
          <h3>{{ t(feature.title) }}</h3>
          <p>{{ t(feature.description) }}</p>
        </div>
      </div>
    </section>

    <!-- Quick Actions -->
    <section class="quick-actions">
      <h2 class="section-title">{{ t('home.quickActions') }}</h2>
      <div class="actions-list">
        <button 
          v-for="action in quickActions" 
          :key="action.text"
          class="action-btn"
          @click="action.action"
        >
          {{ t(action.text) }}
        </button>
      </div>
    </section>

    <!-- Supported Models -->
    <section class="models">
      <h2 class="section-title">{{ t('home.supportedModels') }}</h2>
      <div class="models-list">
        <span class="model-tag">GPT-4o</span>
        <span class="model-tag">Claude-3.5</span>
        <span class="model-tag">Gemini-1.5</span>
        <span class="model-tag">Llama-3</span>
        <span class="model-tag">Qwen2</span>
        <span class="model-tag">Ollama</span>
      </div>
    </section>
  </div>
</template>

<style scoped>
.home-view {
  min-height: 100vh;
  background: #f8fafc;
}

.hero {
  background: linear-gradient(135deg, #1e3a5f 0%, #0f1c2e 100%);
  color: white;
  padding: 4rem 2rem;
  text-align: center;
}

.hero-title {
  font-size: 3rem;
  margin: 0 0 0.5rem;
  font-weight: 700;
}

.hero-subtitle {
  font-size: 1.5rem;
  margin: 0 0 1rem;
  opacity: 0.9;
}

.hero-description {
  font-size: 1.1rem;
  margin: 0 0 2rem;
  opacity: 0.8;
  max-width: 600px;
  margin-left: auto;
  margin-right: auto;
}

.hero-actions {
  display: flex;
  gap: 1rem;
  justify-content: center;
}

.btn-primary {
  padding: 0.875rem 2rem;
  background: #2563eb;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-primary:hover {
  background: #1d4ed8;
}

.btn-secondary {
  padding: 0.875rem 2rem;
  background: transparent;
  color: white;
  border: 2px solid white;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-secondary:hover {
  background: rgba(255, 255, 255, 0.1);
}

.section-title {
  font-size: 1.5rem;
  color: #1e293b;
  margin: 0 0 1.5rem;
  text-align: center;
}

.features {
  padding: 3rem 2rem;
  max-width: 1200px;
  margin: 0 auto;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1.5rem;
}

.feature-card {
  background: white;
  border-radius: 12px;
  padding: 1.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: all 0.2s;
}

.feature-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.feature-icon {
  font-size: 2.5rem;
  margin-bottom: 1rem;
}

.feature-card h3 {
  margin: 0 0 0.5rem;
  color: #1e293b;
}

.feature-card p {
  margin: 0;
  color: #64748b;
  font-size: 0.9rem;
  line-height: 1.5;
}

.quick-actions {
  padding: 2rem;
  max-width: 800px;
  margin: 0 auto;
}

.actions-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  justify-content: center;
}

.action-btn {
  padding: 0.625rem 1.25rem;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  font-size: 0.875rem;
  color: #475569;
  cursor: pointer;
  transition: all 0.2s;
}

.action-btn:hover {
  background: #2563eb;
  color: white;
  border-color: #2563eb;
}

.models {
  padding: 2rem;
  text-align: center;
  background: white;
}

.models-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  justify-content: center;
}

.model-tag {
  padding: 0.375rem 0.875rem;
  background: #f1f5f9;
  border-radius: 16px;
  font-size: 0.8rem;
  color: #64748b;
}
</style>
