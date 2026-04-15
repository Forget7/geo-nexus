<template>
  <div class="geo-panel">
    <div class="panel-header">
      <h3>📐 {{ t('geo.title') }}</h3>
    </div>

    <!-- 工具选择 -->
    <div class="section">
      <h4>{{ t('geo.tools') }}</h4>
      <div class="tool-grid">
        <button v-for="tool in tools" :key="tool.id" @click="activeTool = tool.id"
          :class="{ active: activeTool === tool.id }">
          {{ tool.icon }} {{ tool.label }}
        </button>
      </div>
    </div>

    <!-- 参数输入 -->
    <div class="section">
      <h4>{{ t('geo.parameters') }}</h4>
      <div class="param-grid">
        <!-- Delaunay, Voronoi, Buffer -->
        <template v-if="['delaunay', 'voronoi', 'buffer'].includes(activeTool)">
          <label>{{ t('geo.points') }} (JSON)</label>
          <textarea v-model="pointsJson" rows="4" :placeholder="t('geo.pointsHint')" class="input"></textarea>
        </template>

        <!-- Voronoi bounds -->
        <template v-if="activeTool === 'voronoi'">
          <label>{{ t('geo.bounds') }} (JSON)</label>
          <textarea v-model="boundsJson" rows="3" :placeholder="t('geo.boundsHint')" class="input"></textarea>
        </template>

        <!-- Buffer distance -->
        <template v-if="activeTool === 'buffer'">
          <label>{{ t('geo.distance') }} (km)</label>
          <input v-model.number="distance" type="number" class="input" />
          <label>{{ t('geo.endcapStyle') }}</label>
          <select v-model="endcapStyle" class="input">
            <option value="ROUND">ROUND</option>
            <option value="FLAT">FLAT</option>
            <option value="SQUARE">SQUARE</option>
          </select>
        </template>

        <!-- Merge Lines -->
        <template v-if="activeTool === 'merge-lines'">
          <label>{{ t('geo.lineStrings') }} (JSON)</label>
          <textarea v-model="lineStringsJson" rows="5" :placeholder="t('geo.lineStringsHint')" class="input"></textarea>
        </template>

        <!-- Dissolve -->
        <template v-if="activeTool === 'dissolve'">
          <label>{{ t('geo.polygons') }} (JSON)</label>
          <textarea v-model="polygonsJson" rows="5" :placeholder="t('geo.polygonsHint')" class="input"></textarea>
          <label>{{ t('geo.dissolveField') }}</label>
          <input v-model="dissolveField" type="text" class="input" placeholder="e.g. region" />
          <label>{{ t('geo.fieldValues') }} (JSON Array)</label>
          <input v-model="fieldValuesJson" type="text" class="input" placeholder='["A","A","B"]' />
        </template>

        <!-- Clip -->
        <template v-if="activeTool === 'clip'">
          <label>{{ t('geo.target') }} (JSON)</label>
          <textarea v-model="targetJson" rows="3" :placeholder="t('geo.targetHint')" class="input"></textarea>
          <label>{{ t('geo.clipper') }} (JSON)</label>
          <textarea v-model="clipperJson" rows="3" :placeholder="t('geo.clipperHint')" class="input"></textarea>
        </template>

        <!-- IDW -->
        <template v-if="activeTool === 'interpolate-idw'">
          <label>{{ t('geo.samplePoints') }} (JSON)</label>
          <textarea v-model="samplePointsJson" rows="3" :placeholder="t('geo.samplePointsHint')" class="input"></textarea>
          <label>{{ t('geo.values') }} (JSON Array)</label>
          <input v-model="valuesJson" type="text" class="input" placeholder="[10, 20, 30]" />
          <label>{{ t('geo.grid') }} (JSON)</label>
          <textarea v-model="gridJson" rows="3" :placeholder="t('geo.gridHint')" class="input"></textarea>
          <label>{{ t('geo.power') }}</label>
          <input v-model.number="power" type="number" class="input" />
        </template>

        <!-- Contours -->
        <template v-if="activeTool === 'contours'">
          <label>{{ t('geo.grid') }} (JSON)</label>
          <textarea v-model="gridJson" rows="5" :placeholder="t('geo.gridHint')" class="input"></textarea>
          <label>{{ t('geo.levels') }} (JSON Array)</label>
          <input v-model="levelsJson" type="text" class="input" placeholder="[10, 20, 30, 40]" />
        </template>

        <!-- Validate -->
        <template v-if="activeTool === 'validate'">
          <label>{{ t('geo.polygons') }} (JSON)</label>
          <textarea v-model="polygonsJson" rows="5" :placeholder="t('geo.polygonsHint')" class="input"></textarea>
        </template>
      </div>

      <button @click="execute" class="action-btn" :disabled="loading">
        {{ loading ? t('geo.running') : t('geo.execute') }}
      </button>
    </div>

    <!-- 结果 -->
    <div v-if="result" class="section result-section">
      <h4>{{ t('geo.result') }}</h4>
      <div class="result-info">{{ result.info || JSON.stringify(result.data, null, 2) }}</div>
      <div v-if="result.data" class="result-meta">
        <span v-for="(v, k) in getMeta(result.data)" :key="k" class="meta-tag">{{ k }}: {{ v }}</span>
      </div>
      <div v-if="result.message" class="result-error">{{ result.message }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

const t = (key: string) => key
const activeTool = ref('delaunay')
const loading = ref(false)
const result = ref<any>(null)

// Shared
const pointsJson = ref('[[0,0],[1,0],[0.5,1],[1,1],[0.5,0.5]]')
const boundsJson = ref('[0,10,0,10]')
const distance = ref(1)
const endcapStyle = ref('ROUND')

// merge-lines
const lineStringsJson = ref('[[[0,0],[1,0],[1,1]],[[1,1],[2,1],[2,0]]]')

// dissolve
const polygonsJson = ref('[[[0,0],[1,0],[1,1],[0,1]],[[1,0],[2,0],[2,1],[1,1]]]')
const dissolveField = ref('region')
const fieldValuesJson = ref('["A","A"]')

// clip
const targetJson = ref('[[0,0],[5,0],[5,5],[0,5]]')
const clipperJson = ref('[[[2,2],[3,2],[3,3],[2,3]]]')

// idw
const samplePointsJson = ref('[[0,0],[1,0],[0,1],[1,1]]')
const valuesJson = ref('[10, 20, 30, 40]')
const gridJson = ref('[[0.5,0.5],[0.5,1.5]]')
const power = ref(2)

// contours
const levelsJson = ref('[1, 2, 3, 4]')

const tools = [
  { id: 'delaunay', label: 'Delaunay', icon: '△' },
  { id: 'voronoi', label: 'Voronoi', icon: '⬡' },
  { id: 'buffer', label: 'Buffer', icon: '◯' },
  { id: 'contours', label: 'Contours', icon: '〰' },
  { id: 'merge-lines', label: 'Merge Lines', icon: '╱' },
  { id: 'dissolve', label: 'Dissolve', icon: '⊞' },
  { id: 'clip', label: 'Clip', icon: '✂' },
  { id: 'interpolate-idw', label: 'IDW', icon: '⊕' },
  { id: 'validate', label: 'Validate', icon: '✓' },
]

function getMeta(data: any) {
  const meta: Record<string, any> = {}
  if (data.count !== undefined) meta.count = data.count
  if (data.totalArea !== undefined) meta.totalArea = data.totalArea.toFixed(4)
  if (data.dissolvedCount !== undefined) meta.dissolved = data.dissolvedCount
  if (data.valid !== undefined) meta.valid = data.valid
  if (data.repaired !== undefined) meta.repaired = data.repaired
  if (data.method !== undefined) meta.method = data.method
  return meta
}

async function execute() {
  loading.value = true
  result.value = null
  try {
    let endpoint = '/geo-processing/' + activeTool.value
    let body: any = {}

    switch (activeTool.value) {
      case 'delaunay':
        body = JSON.parse(pointsJson.value)
        break
      case 'voronoi':
        body = { points: JSON.parse(pointsJson.value), bounds: JSON.parse(boundsJson.value) }
        break
      case 'buffer':
        body = { points: JSON.parse(pointsJson.value), distance: distance.value, endcapType: endcapStyle.value }
        break
      case 'merge-lines':
        body = JSON.parse(lineStringsJson.value)
        break
      case 'dissolve':
        body = {
          polygons: JSON.parse(polygonsJson.value),
          dissolveField: dissolveField.value,
          fieldValues: JSON.parse(fieldValuesJson.value)
        }
        break
      case 'clip':
        body = { target: JSON.parse(targetJson.value), clipper: JSON.parse(clipperJson.value) }
        break
      case 'interpolate-idw':
        body = {
          samplePoints: JSON.parse(samplePointsJson.value),
          values: JSON.parse(valuesJson.value),
          grid: JSON.parse(gridJson.value),
          power: power.value
        }
        break
      case 'contours':
        body = { grid: JSON.parse(gridJson.value), levels: JSON.parse(levelsJson.value) }
        break
      case 'validate':
        body = JSON.parse(polygonsJson.value)
        break
    }

    const response = await fetch(endpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    })
    const data = await response.json()
    result.value = data
  } catch (e: any) {
    result.value = { message: 'Error: ' + e.message }
  }
  loading.value = false
}
</script>

<style scoped>
.geo-panel { background: white; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.15); }
.panel-header { padding: 12px 16px; border-bottom: 1px solid #e2e8f0; }
.panel-header h3 { font-size: 15px; font-weight: 600; margin: 0; }
.section { padding: 12px 16px; border-bottom: 1px solid #f1f5f9; }
.section h4 { font-size: 13px; font-weight: 600; margin: 0 0 8px 0; }
.tool-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 6px; }
.tool-grid button {
  padding: 8px 6px; border: 1px solid #e2e8f0; border-radius: 6px;
  background: white; cursor: pointer; font-size: 11px; text-align: center;
}
.tool-grid button:hover { background: #eff6ff; }
.tool-grid button.active { background: #2563eb; color: white; border-color: #2563eb; }
.param-grid { display: flex; flex-direction: column; gap: 6px; margin-bottom: 10px; }
.param-grid label { font-size: 12px; color: #64748b; font-weight: 500; }
.input {
  width: 100%; padding: 8px; border: 1px solid #e2e8f0; border-radius: 6px;
  font-size: 12px; font-family: monospace; box-sizing: border-box; background: white;
}
.action-btn {
  width: 100%; padding: 10px; background: #2563eb; color: white;
  border: none; border-radius: 6px; cursor: pointer; font-size: 13px;
}
.action-btn:disabled { opacity: 0.6; }
.result-section { background: #f8fafc; }
.result-info {
  font-size: 12px; font-family: monospace; background: white;
  padding: 8px; border-radius: 6px; border: 1px solid #e2e8f0;
  margin-bottom: 8px; word-break: break-all; white-space: pre-wrap; max-height: 200px; overflow-y: auto;
}
.result-meta { display: flex; flex-wrap: wrap; gap: 4px; margin-bottom: 6px; }
.meta-tag { padding: 2px 8px; background: #e2e8f0; border-radius: 4px; font-size: 11px; }
.result-error { color: #ef4444; font-size: 12px; }
</style>
