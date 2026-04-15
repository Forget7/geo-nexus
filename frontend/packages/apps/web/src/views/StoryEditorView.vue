<template>
  <div class="story-editor">
    <!-- 顶部工具栏 -->
    <div class="editor-toolbar">
      <input v-model="story.title" :placeholder="t('story.titlePlaceholder')" class="title-input" />
      <div class="toolbar-actions">
        <button @click="save" class="save-btn">💾 {{ t('story.save') }}</button>
        <button @click="publish" class="publish-btn" :disabled="!story.title">
          🚀 {{ t('story.publish') }}
        </button>
      </div>
    </div>

    <!-- 章节列表 -->
    <div class="chapters-sidebar">
      <div class="chapter-list">
        <div
          v-for="(chapter, idx) in story.chapters"
          :key="chapter.id"
          :class="['chapter-item', { active: activeChapter === idx }]"
          @click="selectChapter(idx)"
        >
          <span class="chapter-num">{{ idx + 1 }}</span>
          <span class="chapter-title">{{ chapter.title || t('story.untitledChapter') }}</span>
          <button @click.stop="removeChapter(idx)" class="remove-btn">×</button>
        </div>
      </div>
      <button @click="addChapter" class="add-chapter-btn">
        ➕ {{ t('story.addChapter') }}
      </button>
    </div>

    <!-- 章节编辑器 -->
    <div class="chapter-editor" v-if="activeChapter !== null">
      <div class="chapter-header">
        <input
          v-model="story.chapters[activeChapter].title"
          :placeholder="t('story.chapterTitlePlaceholder')"
          class="chapter-title-input"
        />
      </div>

      <!-- 地图预览 -->
      <div class="map-preview">
        <div class="map-placeholder">
          <span>🗺️ {{ t('story.mapPreview') }}</span>
          <div class="map-coords">
            <input type="number" v-model.number="story.chapters[activeChapter].centerLat"
              :placeholder="t('story.latitude')" />
            <input type="number" v-model.number="story.chapters[activeChapter].centerLng"
              :placeholder="t('story.longitude')" />
            <input type="number" v-model.number="story.chapters[activeChapter].zoom"
              :placeholder="t('story.zoom')" min="1" max="18" />
          </div>
        </div>
      </div>

      <!-- 富文本编辑器 -->
      <div class="rich-editor">
        <div class="editor-toolbar-btns">
          <button @click="editor?.chain().focus().toggleBold().run()" :class="{ active: editor?.isActive('bold') }"><b>B</b></button>
          <button @click="editor?.chain().focus().toggleItalic().run()" :class="{ active: editor?.isActive('italic') }"><i>I</i></button>
          <button @click="editor?.chain().focus().toggleHeading({ level: 2 }).run()">H2</button>
          <button @click="editor?.chain().focus().toggleBulletList().run()">• List</button>
          <button @click="editor?.chain().focus().toggleBlockquote().run()">Quote</button>
        </div>
        <editor-content :editor="editor" class="editor-content" />
      </div>

      <!-- 媒体上传 -->
      <div class="media-section">
        <label class="checkbox-row">
          <input type="checkbox" v-model="story.chapters[activeChapter].hasMedia" />
          {{ t('story.addMedia') }}
        </label>
        <div v-if="story.chapters[activeChapter].hasMedia" class="media-url-input">
          <input v-model="story.chapters[activeChapter].mediaUrl"
            :placeholder="t('story.mediaUrlPlaceholder')" />
        </div>
      </div>
    </div>

    <div v-else class="no-chapter">
      <p>{{ t('story.selectOrAddChapter') }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onBeforeUnmount } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Link from '@tiptap/extension-link'
import Image from '@tiptap/extension-image'
import Placeholder from '@tiptap/extension-placeholder'

const t = (key: string) => key // simplified, use i18n in real use

const story = reactive({
  id: '',
  title: '',
  description: '',
  chapters: [] as any[],
  content: ''
})

const activeChapter = ref<number | null>(null)

const editor = useEditor({
  extensions: [
    StarterKit,
    Link.configure({ openOnClick: false }),
    Image,
    Placeholder.configure({ placeholder: '开始书写...' })
  ],
  content: '',
  onUpdate: ({ editor }) => {
    if (activeChapter.value !== null) {
      story.chapters[activeChapter.value].content = editor.getHTML()
    }
  }
})

function addChapter() {
  story.chapters.push({
    id: Date.now().toString(),
    title: '',
    content: '',
    centerLat: 39.9042,
    centerLng: 116.4074,
    zoom: 10,
    tileType: 'osm',
    visibleLayerIds: [],
    mediaType: 'none',
    mediaUrl: ''
  })
  activeChapter.value = story.chapters.length - 1
}

function removeChapter(idx: number) {
  story.chapters.splice(idx, 1)
  if (activeChapter.value === idx) {
    activeChapter.value = story.chapters.length > 0 ? 0 : null
  } else if (activeChapter.value !== null && activeChapter.value > idx) {
    activeChapter.value--
  }
}

function selectChapter(idx: number) {
  if (activeChapter.value !== null && editor.value) {
    story.chapters[activeChapter.value].content = editor.value.getHTML()
  }
  activeChapter.value = idx
  if (editor.value) {
    editor.value.commands.setContent(story.chapters[idx].content || '')
  }
}

async function save() {
  if (activeChapter.value !== null && editor.value) {
    story.chapters[activeChapter.value].content = editor.value.getHTML()
  }
  console.log('Saving story:', story)
  // TODO: call API to save
}

async function publish() {
  if (activeChapter.value !== null && editor.value) {
    story.chapters[activeChapter.value].content = editor.value.getHTML()
  }
  console.log('Publishing story:', story)
  // TODO: call API to publish
}

onBeforeUnmount(() => {
  editor.value?.destroy()
})
</script>

<style scoped>
.story-editor {
  display: grid;
  grid-template-columns: 250px 1fr;
  height: 100vh;
  background: #f8fafc;
}
.editor-toolbar {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  background: white;
  border-bottom: 1px solid #e2e8f0;
}
.title-input {
  font-size: 18px;
  font-weight: 600;
  border: none;
  outline: none;
  width: 400px;
}
.chapters-sidebar {
  background: white;
  border-right: 1px solid #e2e8f0;
  padding: 12px;
  overflow-y: auto;
}
.chapter-list { display: flex; flex-direction: column; gap: 4px; }
.chapter-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
}
.chapter-item:hover { background: #f1f5f9; }
.chapter-item.active { background: #eff6ff; color: #2563eb; }
.chapter-num { font-weight: 600; color: #94a3b8; font-size: 11px; }
.chapter-title { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.remove-btn { background: none; border: none; cursor: pointer; color: #94a3b8; font-size: 16px; }
.add-chapter-btn {
  width: 100%; margin-top: 8px; padding: 8px;
  border: 1px dashed #cbd5e1; border-radius: 6px;
  background: none; cursor: pointer; color: #64748b; font-size: 13px;
}
.add-chapter-btn:hover { border-color: #2563eb; color: #2563eb; }
.chapter-editor { padding: 20px; overflow-y: auto; }
.chapter-title-input {
  width: 100%; font-size: 20px; font-weight: 600;
  border: none; border-bottom: 2px solid #e2e8f0;
  outline: none; margin-bottom: 16px; padding: 8px 0;
}
.map-preview {
  margin-bottom: 16px;
  background: #1e293b;
  border-radius: 8px;
  padding: 20px;
  color: white;
}
.map-coords {
  display: flex; gap: 8px; margin-top: 12px;
}
.map-coords input {
  flex: 1; padding: 6px 8px; border-radius: 4px;
  border: 1px solid #334155; background: #0f172a;
  color: white; font-size: 12px;
}
.editor-toolbar-btns {
  display: flex; gap: 4px; margin-bottom: 8px;
}
.editor-toolbar-btns button {
  padding: 4px 10px; border: 1px solid #e2e8f0;
  border-radius: 4px; background: white; cursor: pointer; font-size: 12px;
}
.editor-toolbar-btns button.active { background: #2563eb; color: white; border-color: #2563eb; }
.editor-content {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 16px;
  min-height: 200px;
  background: white;
}
.media-section { margin-top: 16px; }
.checkbox-row { display: flex; align-items: center; gap: 6px; cursor: pointer; }
.media-url-input input {
  width: 100%; margin-top: 8px; padding: 8px;
  border: 1px solid #e2e8f0; border-radius: 6px;
}
.no-chapter {
  display: flex; align-items: center; justify-content: center;
  color: #94a3b8; font-size: 16px;
}
</style>
