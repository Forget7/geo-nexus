<template>
  <div class="gn-sanitization-panel">
    <div class="panel-header">
      <h3>🔒 {{ t('sanitization.title') || '数据脱敏' }}</h3>
    </div>

    <el-tabs v-model="activeTab">
      <!-- 批量脱敏 -->
      <el-tab-pane :label="t('sanitization.batchSanitize')" name="sanitize">
        <div class="tool-section">
          <label>{{ t('sanitization.gisData') }} (GeoJSON)</label>
          <el-input
            v-model="gisData"
            type="textarea"
            rows="8"
            :placeholder="t('sanitization.geojsonHint')"
          />

          <div class="rules-section">
            <label>{{ t('sanitization.rules') }}</label>
            <el-checkbox v-model="rules.phone">{{ t('sanitization.rulePhone') }}</el-checkbox>
            <el-checkbox v-model="rules.email">{{ t('sanitization.ruleEmail') }}</el-checkbox>
            <el-checkbox v-model="rules.coordinate">{{ t('sanitization.ruleCoordinate') }}</el-checkbox>
            <el-checkbox v-model="rules.address">{{ t('sanitization.ruleAddress') }}</el-checkbox>
          </div>

          <el-button type="primary" :loading="loading" @click="executeSanitize">
            {{ t('sanitization.sanitize') }}
          </el-button>
        </div>
      </el-tab-pane>

      <!-- 单条工具 -->
      <el-tab-pane :label="t('sanitization.tools')" name="tools">
        <div class="tool-section">
          <!-- 手机号 -->
          <label>{{ t('sanitization.phone') }}</label>
          <div class="inline-row">
            <el-input v-model="phoneInput" :placeholder="t('sanitization.phoneHint')" />
            <el-button type="primary" :disabled="!phoneInput" @click="maskPhone">
              {{ t('sanitization.mask') }}
            </el-button>
          </div>
          <div v-if="phoneOutput" class="output-box">{{ phoneOutput }}</div>

          <!-- 邮箱 -->
          <label>{{ t('sanitization.email') }}</label>
          <div class="inline-row">
            <el-input v-model="emailInput" :placeholder="t('sanitization.emailHint')" />
            <el-button type="primary" :disabled="!emailInput" @click="maskEmail">
              {{ t('sanitization.mask') }}
            </el-button>
          </div>
          <div v-if="emailOutput" class="output-box">{{ emailOutput }}</div>

          <!-- 坐标 -->
          <label>{{ t('sanitization.coordinate') }}</label>
          <div class="inline-row">
            <el-input v-model.number="coordInput" type="number" :placeholder="t('sanitization.coordHint')" />
            <el-button type="primary" :disabled="coordInput === null" @click="roundCoord">
              {{ t('sanitization.round') }}
            </el-button>
          </div>
          <div v-if="coordOutput !== null" class="output-box">{{ coordOutput }}</div>

          <!-- 门牌号 -->
          <label>{{ t('sanitization.address') }}</label>
          <div class="inline-row">
            <el-input v-model="addressInput" :placeholder="t('sanitization.addressHint')" />
            <el-button type="primary" :disabled="!addressInput" @click="maskAddress">
              {{ t('sanitization.mask') }}
            </el-button>
          </div>
          <div v-if="addressOutput" class="output-box">{{ addressOutput }}</div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <div v-if="sanitizeResult" class="result-section">
      <h4>{{ t('sanitization.result') }}</h4>
      <pre>{{ JSON.stringify(sanitizeResult, null, 2) }}</pre>
    </div>

    <div v-if="error" class="error-section">
      <el-alert type="error" :title="error" show-icon :closable="false" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useI18n } from 'vue-i18n'
import { sanitizationApi } from '@/service/api'

defineOptions({ name: 'GnDataSanitizationPanel' })

const { t } = useI18n()

const activeTab = ref('sanitize')
const loading = ref(false)
const error = ref('')

// Batch sanitize
const gisData = ref('')
const rules = reactive({
  phone: true,
  email: true,
  coordinate: true,
  address: true
})
const sanitizeResult = ref<any>(null)

// Single tools
const phoneInput = ref('')
const phoneOutput = ref('')
const emailInput = ref('')
const emailOutput = ref('')
const coordInput = ref<number | null>(null)
const coordOutput = ref<number | null>(null)
const addressInput = ref('')
const addressOutput = ref('')

async function executeSanitize() {
  error.value = ''
  sanitizeResult.value = null
  if (!gisData.value) {
    error.value = t('sanitization.gisDataRequired')
    return
  }
  loading.value = true
  try {
    const data = JSON.parse(gisData.value)
    const response = await sanitizationApi.sanitize({ gisData: data, rules: { ...rules } })
    if (response.data?.success) {
      sanitizeResult.value = response.data.data
    } else {
      error.value = response.data?.message || 'Sanitization failed'
    }
  } catch (e: any) {
    error.value = e.message || t('sanitization.parseError')
  } finally {
    loading.value = false
  }
}

async function maskPhone() {
  if (!phoneInput.value) return
  const response = await sanitizationApi.maskPhone({ phone: phoneInput.value })
  if (response.data?.success) {
    phoneOutput.value = response.data.data
  }
}

async function maskEmail() {
  if (!emailInput.value) return
  const response = await sanitizationApi.maskEmail({ email: emailInput.value })
  if (response.data?.success) {
    emailOutput.value = response.data.data
  }
}

async function roundCoord() {
  if (coordInput.value === null) return
  const response = await sanitizationApi.roundCoord({ value: coordInput.value })
  if (response.data?.success) {
    coordOutput.value = response.data.data
  }
}

async function maskAddress() {
  if (!addressInput.value) return
  const response = await sanitizationApi.maskAddress({ address: addressInput.value })
  if (response.data?.success) {
    addressOutput.value = response.data.data
  }
}
</script>

<style scoped>
.gn-sanitization-panel {
  padding: 16px;
}

.panel-header {
  margin-bottom: 16px;
}

.panel-header h3 {
  margin: 0;
  font-size: 16px;
}

.tool-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tool-section label {
  font-weight: 500;
  font-size: 13px;
}

.rules-section {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 8px;
  background: var(--bg-secondary, #f5f5f5);
  border-radius: 6px;
}

.inline-row {
  display: flex;
  gap: 8px;
}

.inline-row .el-input {
  flex: 1;
}

.output-box {
  padding: 8px 12px;
  background: var(--bg-secondary, #f5f5f5);
  border-radius: 4px;
  font-family: monospace;
  font-size: 13px;
}

.result-section {
  margin-top: 16px;
  padding: 12px;
  background: var(--bg-secondary, #f5f5f5);
  border-radius: 8px;
}

.result-section h4 {
  margin: 0 0 8px;
  font-size: 14px;
}

.result-section pre {
  margin: 0;
  font-size: 12px;
  overflow-x: auto;
}

.error-section {
  margin-top: 16px;
}
</style>
