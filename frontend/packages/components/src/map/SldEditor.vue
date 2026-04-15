<script setup lang="ts">
/**
 * SldEditor.vue - SLD 样式编辑器
 * 
 * 左右布局：
 * - 左侧：代码编辑器（textarea + 语法高亮）+ 模板选择
 * - 右侧：实时地图预览 + 保存/导出按钮
 */
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  /** 要编辑样式的图层名 */
  layerName?: string
  /** 初始 SLD 内容 */
  initialSld?: string
}>()

const emit = defineEmits<{
  save: [sldContent: string, styleName: string]
  cancel: []
}>()

// 状态
const sldContent = ref(props.initialSld || getDefaultSLD())
const selectedTemplateId = ref<string | null>(null)
const previewUrl = ref('')
const styleName = ref(props.layerName || 'my-style')
const isLoading = ref(false)
const sldVersion = ref<'1.0' | '1.1'>('1.0')

// 预设模板
const templates = [
  {
    id: 'point-default',
    name: '点-默认圆形',
    description: '蓝色圆形点符号',
    sld: createPointSLD('#2563EB', 6)
  },
  {
    id: 'line-default',
    name: '线-默认',
    description: '蓝色实线',
    sld: createLineSLD('#2563EB', 2)
  },
  {
    id: 'polygon-default',
    name: '面-默认填充',
    description: '蓝色半透明填充+边框',
    sld: createPolygonSLD('#2563EB', '#1D4ED8', 0.7)
  },
  {
    id: 'point-gradient',
    name: '点-热力样式',
    description: '红色渐变热力点',
    sld: createPointSLD('#EF4444', 12)
  },
  {
    id: 'line-dashed',
    name: '线-虚线',
    description: '蓝色虚线',
    sld: createDashedLineSLD()
  },
  {
    id: 'polygon-pattern',
    name: '面-纹理填充',
    description: '绿色纹理填充面',
    sld: createPatternPolygonSLD()
  }
]

// 加载模板
function loadTemplate(templateId: string) {
  const tpl = templates.find(t => t.id === templateId)
  if (tpl) {
    sldContent.value = tpl.sld
    selectedTemplateId.value = templateId
    updatePreview()
  }
}

// 检测 SLD 版本
function detectVersion(content: string) {
  if (content.includes('version="1.1.0"')) {
    sldVersion.value = '1.1'
  } else {
    sldVersion.value = '1.0'
  }
}

// 更新预览 URL
function updatePreview() {
  if (!props.layerName) return
  detectVersion(sldContent.value)
  // 简单的 WMS GetMap URL 用于预览
  const encodedStyle = encodeURIComponent(sldContent.value)
  previewUrl.value = '' // 预览将通过 GeoServer 的 WMS 实现，此处预留
}

// 验证 SLD XML
function validateSLD(): boolean {
  try {
    // 基本格式检查
    const content = sldContent.value.trim()
    if (!content.includes('StyledLayerDescriptor')) {
      ElMessage.warning('SLD 内容缺少 <StyledLayerDescriptor> 根元素')
      return false
    }
    if (!content.includes('<') + !content.includes('>')) {
      ElMessage.warning('SLD 内容不是有效的 XML 格式')
      return false
    }
    return true
  } catch {
    ElMessage.warning('SLD 内容格式错误')
    return false
  }
}

// 保存样式（emit 到父组件，由 MapView 调用 API）
function saveStyle() {
  if (!validateSLD()) return
  emit('save', sldContent.value, styleName.value)
}

// 导出 SLD 文件
function exportSLD() {
  if (!validateSLD()) return
  const blob = new Blob([sldContent.value], { type: 'application/xml' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${styleName.value}.sld`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('SLD 文件已导出')
}

// 导入 SLD 文件
function importSLD(event: Event) {
  const input = event.target as HTMLInputElement
  if (!input.files?.length) return

  const reader = new FileReader()
  reader.onload = (e) => {
    const content = e.target?.result as string
    if (content) {
      sldContent.value = content
      detectVersion(content)
      selectedTemplateId.value = null
      updatePreview()
      ElMessage.success('SLD 文件已导入')
    }
  }
  reader.readAsText(input.files[0])
  input.value = '' // reset
}

// 切换 SLD 版本
function switchVersion(version: '1.0' | '1.1') {
  sldContent.value = sldContent.value.replace(
    /version="1\.\d+\.\d+"/,
    `version="${version}.0"`
  )
  sldVersion.value = version
}

// SLD 模板生成函数
function createPointSLD(color: string, size: number): string {
  return `<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0"
  xmlns="http://www.opengis.net/sld"
  xmlns:ogc="http://www.opengis.net/ogc">
  <NamedLayer>
    <Name>Point Style</Name>
    <UserStyle>
      <Title>Point Style</Title>
      <FeatureTypeStyle>
        <Rule>
          <PointSymbolizer>
            <Graphic>
              <Mark>
                <WellKnownName>circle</WellKnownName>
                <Fill>
                  <CssParameter name="fill">${color}</CssParameter>
                </Fill>
              </Mark>
              <Size>${size}</Size>
            </Graphic>
          </PointSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>`
}

function createLineSLD(color: string, width: number): string {
  return `<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0"
  xmlns="http://www.opengis.net/sld"
  xmlns:ogc="http://www.opengis.net/ogc">
  <NamedLayer>
    <Name>Line Style</Name>
    <UserStyle>
      <Title>Line Style</Title>
      <FeatureTypeStyle>
        <Rule>
          <LineSymbolizer>
            <Stroke>
              <CssParameter name="stroke">${color}</CssParameter>
              <CssParameter name="stroke-width">${width}</CssParameter>
            </Stroke>
          </LineSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>`
}

function createPolygonSLD(fillColor: string, strokeColor: string, opacity: number): string {
  return `<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0"
  xmlns="http://www.opengis.net/sld"
  xmlns:ogc="http://www.opengis.net/ogc">
  <NamedLayer>
    <Name>Polygon Style</Name>
    <UserStyle>
      <Title>Polygon Style</Title>
      <FeatureTypeStyle>
        <Rule>
          <PolygonSymbolizer>
            <Fill>
              <CssParameter name="fill">${fillColor}</CssParameter>
              <CssParameter name="fill-opacity">${opacity}</CssParameter>
            </Fill>
            <Stroke>
              <CssParameter name="stroke">${strokeColor}</CssParameter>
              <CssParameter name="stroke-width">1</CssParameter>
            </Stroke>
          </PolygonSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>`
}

function createDashedLineSLD(): string {
  return `<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0"
  xmlns="http://www.opengis.net/sld"
  xmlns:ogc="http://www.opengis.net/ogc">
  <NamedLayer>
    <Name>Dashed Line</Name>
    <UserStyle>
      <Title>Dashed Line Style</Title>
      <FeatureTypeStyle>
        <Rule>
          <LineSymbolizer>
            <Stroke>
              <CssParameter name="stroke">#3B82F6</CssParameter>
              <CssParameter name="stroke-width">2</CssParameter>
              <CssParameter name="stroke-dasharray">5 3</CssParameter>
            </Stroke>
          </LineSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>`
}

function createPatternPolygonSLD(): string {
  return `<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0"
  xmlns="http://www.opengis.net/sld"
  xmlns:ogc="http://www.opengis.net/ogc">
  <NamedLayer>
    <Name>Pattern Polygon</Name>
    <UserStyle>
      <Title>Pattern Fill Polygon</Title>
      <FeatureTypeStyle>
        <Rule>
          <PolygonSymbolizer>
            <Fill>
              <GraphicFill>
                <Graphic>
                  <Mark><WellKnownName>circle</WellKnownName>
                    <Fill><CssParameter name="fill">#10B981</CssParameter></Fill>
                    <Size>4</Size>
                  </Mark>
                </Graphic>
              </GraphicFill>
            </Fill>
            <Stroke>
              <CssParameter name="stroke">#047857</CssParameter>
              <CssParameter name="stroke-width">1</CssParameter>
            </Stroke>
          </PolygonSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>`
}

function getDefaultSLD(): string {
  return createPointSLD('#2563EB', 6)
}

// 监听内容变化
watch(sldContent, () => {
  detectVersion(sldContent.value)
})

onMounted(() => {
  detectVersion(sldContent.value)
})
</script>

<template>
  <div class="sld-editor">
    <!-- 左侧：代码编辑区 -->
    <div class="editor-panel">
      <div class="editor-header">
        <h3>🎨 SLD 样式编辑器</h3>
        <span class="version-badge">v{{ sldVersion }}</span>
      </div>

      <!-- 样式名称 -->
      <div class="form-row">
        <label>样式名称</label>
        <input v-model="styleName" type="text" placeholder="my-style" />
      </div>

      <!-- 模板选择 -->
      <div class="templates-section">
        <label>预设模板</label>
        <div class="templates-grid">
          <button
            v-for="tpl in templates"
            :key="tpl.id"
            :class="['tpl-btn', { active: selectedTemplateId === tpl.id }]"
            :title="tpl.description"
            @click="loadTemplate(tpl.id)"
          >
            {{ tpl.name }}
          </button>
        </div>
      </div>

      <!-- 代码编辑器 -->
      <div class="code-section">
        <div class="code-header">
          <span>SLD XML</span>
          <div class="version-switch">
            <button
              :class="{ active: sldVersion === '1.0' }"
              @click="switchVersion('1.0')"
            >SLD 1.0</button>
            <button
              :class="{ active: sldVersion === '1.1' }"
              @click="switchVersion('1.1')"
            >SLD 1.1</button>
          </div>
        </div>
        <textarea
          v-model="sldContent"
          class="code-textarea"
          spellcheck="false"
          placeholder="输入 SLD XML 内容..."
        />
      </div>

      <!-- 操作按钮 -->
      <div class="editor-actions">
        <button class="btn-primary" @click="saveStyle">
          💾 保存
        </button>
        <button class="btn-secondary" @click="exportSLD">
          📤 导出
        </button>
        <label class="btn-secondary import-btn">
          📥 导入
          <input type="file" accept=".sld,.xml" @change="importSLD" hidden />
        </label>
        <button class="btn-ghost" @click="emit('cancel')">
          取消
        </button>
      </div>
    </div>

    <!-- 右侧：预览区 -->
    <div class="preview-panel">
      <div class="preview-header">
        <h4>实时预览</h4>
        <span v-if="layerName" class="layer-label">{{ layerName }}</span>
      </div>
      <div class="preview-map">
        <div v-if="!layerName" class="preview-placeholder">
          <p>选择图层后在此预览</p>
        </div>
        <div v-else class="preview-wms">
          <p>WMS 预览加载中...</p>
          <small>样式将应用于图层: {{ layerName }}</small>
        </div>
      </div>
      <div class="preview-info">
        <span>字数: {{ sldContent.length }}</span>
        <span>版本: SLD {{ sldVersion }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.sld-editor {
  display: flex;
  height: 100%;
  gap: 1px;
  background: #e2e8f0;
}

/* 左侧编辑面板 */
.editor-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: white;
  padding: 1rem;
  overflow-y: auto;
  min-width: 0;
}

.editor-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.editor-header h3 {
  margin: 0;
  font-size: 1rem;
  color: #1e293b;
}

.version-badge {
  font-size: 0.7rem;
  padding: 2px 6px;
  background: #dbeafe;
  color: #1d4ed8;
  border-radius: 4px;
}

.form-row {
  margin-bottom: 0.75rem;
}

.form-row label {
  display: block;
  font-size: 0.75rem;
  color: #64748b;
  margin-bottom: 4px;
}

.form-row input {
  width: 100%;
  padding: 0.5rem;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 0.875rem;
  box-sizing: border-box;
}

.form-row input:focus {
  outline: none;
  border-color: #2563eb;
}

/* 模板选择 */
.templates-section {
  margin-bottom: 0.75rem;
}

.templates-section label {
  display: block;
  font-size: 0.75rem;
  color: #64748b;
  margin-bottom: 4px;
}

.templates-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.tpl-btn {
  padding: 4px 8px;
  font-size: 0.7rem;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.15s;
  color: #475569;
}

.tpl-btn:hover {
  background: #e0f2fe;
  border-color: #38bdf8;
}

.tpl-btn.active {
  background: #dbeafe;
  border-color: #2563eb;
  color: #1d4ed8;
}

/* 代码区 */
.code-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 200px;
}

.code-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 8px;
  background: #1e293b;
  border-radius: 6px 6px 0 0;
  font-size: 0.75rem;
  color: #94a3b8;
}

.version-switch {
  display: flex;
  gap: 2px;
}

.version-switch button {
  padding: 2px 6px;
  font-size: 0.65rem;
  background: transparent;
  border: 1px solid #475569;
  border-radius: 3px;
  color: #94a3b8;
  cursor: pointer;
}

.version-switch button.active {
  background: #2563eb;
  border-color: #2563eb;
  color: white;
}

.code-textarea {
  flex: 1;
  width: 100%;
  padding: 0.75rem;
  background: #0f172a;
  color: #e2e8f0;
  border: none;
  border-radius: 0 0 6px 6px;
  font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
  font-size: 0.8rem;
  line-height: 1.5;
  resize: none;
  box-sizing: border-box;
  min-height: 250px;
}

.code-textarea:focus {
  outline: none;
}

/* 操作按钮 */
.editor-actions {
  display: flex;
  gap: 0.5rem;
  margin-top: 1rem;
  flex-wrap: wrap;
}

.btn-primary {
  padding: 0.5rem 1rem;
  background: #2563eb;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 0.875rem;
  cursor: pointer;
}

.btn-primary:hover {
  background: #1d4ed8;
}

.btn-secondary {
  padding: 0.5rem 1rem;
  background: white;
  color: #475569;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 0.875rem;
  cursor: pointer;
}

.btn-secondary:hover {
  background: #f8fafc;
  border-color: #2563eb;
}

.import-btn {
  display: inline-flex;
  align-items: center;
}

.btn-ghost {
  padding: 0.5rem 1rem;
  background: transparent;
  color: #64748b;
  border: none;
  font-size: 0.875rem;
  cursor: pointer;
}

.btn-ghost:hover {
  color: #1e293b;
}

/* 右侧预览面板 */
.preview-panel {
  width: 320px;
  display: flex;
  flex-direction: column;
  background: white;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid #f1f5f9;
}

.preview-header h4 {
  margin: 0;
  font-size: 0.875rem;
  color: #1e293b;
}

.layer-label {
  font-size: 0.7rem;
  padding: 2px 6px;
  background: #f0fdf4;
  color: #16a34a;
  border-radius: 4px;
}

.preview-map {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8fafc;
  min-height: 200px;
}

.preview-placeholder {
  text-align: center;
  color: #94a3b8;
  font-size: 0.875rem;
}

.preview-wms {
  text-align: center;
  color: #64748b;
  font-size: 0.875rem;
}

.preview-wms small {
  display: block;
  margin-top: 0.5rem;
  color: #94a3b8;
  font-size: 0.75rem;
}

.preview-info {
  display: flex;
  gap: 1rem;
  padding: 0.5rem 1rem;
  background: #f8fafc;
  border-top: 1px solid #f1f5f9;
  font-size: 0.7rem;
  color: #94a3b8;
}
</style>
