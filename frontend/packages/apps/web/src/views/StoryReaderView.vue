<template>
  <div class="story-reader" v-if="story">
    <!-- 封面 -->
    <div class="story-cover" v-if="!started">
      <img v-if="story.coverImageUrl" :src="story.coverImageUrl" class="cover-img" />
      <div class="cover-overlay">
        <h1>{{ story.title }}</h1>
        <p>{{ story.description }}</p>
        <p class="author">{{ t('story.by') }} {{ story.authorName }}</p>
        <button @click="startReading" class="start-btn">
          {{ t('story.startReading') }} →
        </button>
      </div>
    </div>

    <!-- 内容 -->
    <div v-else class="story-content">
      <div class="chapter-map">
        <!-- TODO: integrate CesiumView component when available -->
        <div class="map-placeholder">
          <span>🗺️ {{ chapter.centerLat }}, {{ chapter.centerLng }} | Zoom: {{ chapter.zoom }}</span>
        </div>
      </div>

      <div class="chapter-text">
        <h2>{{ chapter.title }}</h2>
        <div v-html="chapter.content"></div>
      </div>

      <!-- 章节导航 -->
      <div class="chapter-nav">
        <button v-if="chapterIndex > 0" @click="prevChapter">
          ← {{ t('story.prevChapter') }}
        </button>
        <span class="chapter-indicator">{{ chapterIndex + 1 }} / {{ story.chapters.length }}</span>
        <button v-if="chapterIndex < story.chapters.length - 1" @click="nextChapter">
          {{ t('story.nextChapter') }} →
        </button>
        <button v-else @click="finishReading" class="finish-btn">
          {{ t('story.finish') }}
        </button>
      </div>
    </div>
  </div>
  <div v-else class="loading">{{ t('story.loading') }}</div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

const props = defineProps<{ token?: string }>()

const story = ref<any>(null)
const started = ref(false)
const chapterIndex = ref(0)

const chapter = computed(() => story.value?.chapters?.[chapterIndex.value])

const t = (key: string) => key // simplified, use i18n in real use

function startReading() { started.value = true }
function prevChapter() { chapterIndex.value-- }
function nextChapter() { chapterIndex.value++ }
function finishReading() { started.value = false }

// TODO: fetch story by token when token prop is provided
// async function fetchStory() {
//   if (props.token) {
//     const res = await fetch(`/api/v1/stories/shared/${props.token}`)
//     story.value = await res.json()
//   }
// }
</script>

<style scoped>
.story-cover {
  position: relative;
  height: 100vh;
  overflow: hidden;
}
.cover-img { width: 100%; height: 100%; object-fit: cover; }
.cover-overlay {
  position: absolute; inset: 0;
  background: rgba(0,0,0,0.5);
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  color: white; text-align: center;
}
.cover-overlay h1 { font-size: 48px; margin-bottom: 16px; }
.cover-overlay p { font-size: 18px; margin-bottom: 8px; }
.start-btn {
  margin-top: 32px; padding: 14px 32px;
  background: #2563eb; color: white;
  border: none; border-radius: 8px; font-size: 16px; cursor: pointer;
}
.story-content { display: flex; flex-direction: column; min-height: 100vh; }
.chapter-map { height: 50vh; }
.map-placeholder {
  height: 100%;
  background: #1e293b;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 16px;
}
.chapter-text { max-width: 800px; margin: 40px auto; padding: 0 20px; }
.chapter-text h2 { font-size: 28px; margin-bottom: 20px; }
.chapter-nav {
  display: flex; justify-content: center; align-items: center;
  gap: 24px; padding: 24px; background: #f8fafc; border-top: 1px solid #e2e8f0;
}
.chapter-nav button {
  padding: 10px 20px; border: 1px solid #e2e8f0;
  border-radius: 6px; background: white; cursor: pointer;
}
.finish-btn { background: #2563eb !important; color: white !important; border-color: #2563eb !important; }
.loading { display: flex; align-items: center; justify-content: center; height: 100vh; color: #94a3b8; }
</style>
