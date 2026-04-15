<template>
  <div class="layout-panel">
    <div class="panel-header">
      <h3>📐 {{ t('layout.title') }}</h3>
    </div>

    <!-- 模板选择 -->
    <div class="template-section">
      <label>{{ t('layout.template') }}</label>
      <div class="template-grid">
        <div
          v-for="tmpl in templates"
          :key="tmpl.id"
          :class="['template-item', { active: selectedTemplate === tmpl.id }]"
          @click="selectTemplate(tmpl)"
        >
          <div class="template-preview" :class="tmpl.id"></div>
          <span>{{ tmpl.name }}</span>
        </div>
      </div>
    </div>

    <!-- 标题设置 -->
    <div class="section">
      <label class="checkbox-row">
        <input type="checkbox" v-model="showTitle" />
        {{ t('layout.showTitle') }}
      </label>
      <div v-if="showTitle">
        <input v-model="title" :placeholder="t('layout.titlePlaceholder')" class="text-input" />
        <input v-model="subtitle" :placeholder="t('layout.subtitlePlaceholder')" class="text-input" />
      </div>
    </div>

    <!-- 图例设置 -->
    <div class="section">
      <label class="checkbox-row">
        <input type="checkbox" v-model="showLegend" />
        {{ t('layout.showLegend') }}
      </label>
    </div>

    <!-- 指北针设置 -->
    <div class="section">
      <label class="checkbox-row">
        <input type="checkbox" v-model="showNorthArrow" />
        {{ t('layout.showNorthArrow') }}
      </label>
      <div v-if="showNorthArrow" class="sub-options">
        <label>{{ t('layout.northStyle') }}</label>
        <select v-model="northStyle" class="select-input">
          <option value="arrow">↑ 箭头</option>
          <option value="compass">🧭 罗盘</option>
          <option value="star">★ 星形</option>
        </select>
      </div>
    </div>

    <!-- 比例尺设置 -->
    <div class="section">
      <label class="checkbox-row">
        <input type="checkbox" v-model="showScaleBar" />
        {{ t('layout.showScaleBar') }}
      </label>
      <div v-if="showScaleBar" class="sub-options">
        <label>{{ t('layout.scaleUnit') }}</label>
        <select v-model="scaleUnit" class="select-input">
          <option value="km">公里</option>
          <option value="m">米</option>
        </select>
      </div>
    </div>

    <!-- 元数据设置 -->
    <div class="section">
      <label class="checkbox-row">
        <input type="checkbox" v-model="showMetadata" />
        {{ t('layout.showMetadata') }}
      </label>
    </div>

    <!-- 导出按钮 -->
    <div class="export-buttons">
      <button class="export-btn" @click="exportHtml" :disabled="loading">
        📄 {{ t('layout.exportHtml') }}
      </button>
      <button class="export-btn" @click="exportPng" :disabled="loading">
        🖼️ {{ t('layout.exportPng') }}
      </button>
      <button class="export-btn primary" @click="exportPdf" :disabled="loading">
        📑 {{ t('layout.exportPdf') }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();

const loading = ref(false);
const selectedTemplate = ref('a4-landscape');
const showTitle = ref(true);
const title = ref('');
const subtitle = ref('');
const showLegend = ref(true);
const showNorthArrow = ref(true);
const northStyle = ref('arrow');
const showScaleBar = ref(true);
const scaleUnit = ref('km');
const showMetadata = ref(false);

const templates = [
  { id: 'a4-landscape', name: 'A4 横版', width: 297, height: 210 },
  { id: 'a4-portrait', name: 'A4 竖版', width: 210, height: 297 },
  { id: 'a3-landscape', name: 'A3 横版', width: 420, height: 297 },
  { id: 'a3-portrait', name: 'A3 竖版', width: 297, height: 420 },
  { id: 'poster', name: '海报', width: 600, height: 400 },
  { id: 'custom', name: '自定义', width: 0, height: 0 },
];

function selectTemplate(tmpl: any) {
  selectedTemplate.value = tmpl.id;
}

function buildRequest() {
  const req: any = {
    width: 297,
    height: 210,
    mapImageUrl: '',
    showTitle: showTitle.value,
    showLegend: showLegend.value,
    showMetadata: showMetadata.value,
  };

  // Apply template dimensions
  const tmpl = templates.find(t => t.id === selectedTemplate.value);
  if (tmpl && tmpl.width > 0) {
    req.width = tmpl.width;
    req.height = tmpl.height;
  }

  if (showTitle.value) {
    req.title = title.value;
    req.subtitle = subtitle.value;
  }

  if (showNorthArrow.value) {
    req.northArrow = {
      style: northStyle.value,
      position: 'top: 70px; right: 20px;',
      size: 40,
    };
  }

  if (showScaleBar.value) {
    req.scaleBar = {
      position: 'bottom: 50px; left: 40px;',
      unit: scaleUnit.value,
      barWidth: 100,
    };
  }

  // Placeholder map image URL
  req.mapImageUrl = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==';

  return req;
}

async function exportHtml() {
  loading.value = true;
  try {
    const req = buildRequest();
    const res = await fetch('/api/v1/layout/generate', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(req),
    });
    const html = await res.text();
    const blob = new Blob([html], { type: 'text/html' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'map-layout.html';
    a.click();
    URL.revokeObjectURL(url);
  } catch (e) {
    console.error('Export HTML failed:', e);
  } finally {
    loading.value = false;
  }
}

async function exportPng() {
  loading.value = true;
  try {
    const req = buildRequest();
    const res = await fetch('/api/v1/layout/export/png', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(req),
    });
    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'map.png';
    a.click();
    URL.revokeObjectURL(url);
  } catch (e) {
    console.error('Export PNG failed:', e);
  } finally {
    loading.value = false;
  }
}

async function exportPdf() {
  loading.value = true;
  try {
    const req = buildRequest();
    const res = await fetch('/api/v1/layout/export/pdf', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(req),
    });
    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'map.pdf';
    a.click();
    URL.revokeObjectURL(url);
  } catch (e) {
    console.error('Export PDF failed:', e);
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.layout-panel { padding: 16px; }
.panel-header h3 { font-size: 15px; font-weight: 600; margin-bottom: 12px; }
.template-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin: 8px 0;
}
.template-item {
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 8px;
  cursor: pointer;
  text-align: center;
  font-size: 11px;
}
.template-item.active { border-color: #2563eb; background: #eff6ff; }
.template-preview {
  height: 40px;
  margin-bottom: 4px;
  border: 1px solid #ccc;
  border-radius: 3px;
}
.a4-landscape { aspect-ratio: 297/210; }
.a4-portrait { aspect-ratio: 210/297; }
.checkbox-row { display: flex; align-items: center; gap: 6px; cursor: pointer; }
.section { margin: 12px 0; padding-bottom: 12px; border-bottom: 1px solid #f1f5f9; }
.sub-options { margin-top: 8px; padding-left: 12px; display: flex; flex-direction: column; gap: 4px; }
.sub-options label { font-size: 12px; color: #64748b; }
.select-input {
  width: 100%;
  padding: 6px 8px;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
  font-size: 13px;
  background: white;
}
.text-input {
  width: 100%;
  padding: 6px 8px;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
  margin: 4px 0;
  font-size: 13px;
}
.export-buttons {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 16px;
}
.export-btn {
  padding: 10px;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  background: white;
  cursor: pointer;
  font-size: 13px;
}
.export-btn:hover { background: #f8fafc; }
.export-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.export-btn.primary {
  background: #2563eb;
  color: white;
  border-color: #2563eb;
}
.export-btn.primary:hover { background: #1d4ed8; }
</style>