<script setup lang="ts">
/**
 * AttributeTable - 属性数据表组件
 * 借鉴SuperMap iClient的数据分析功能
 */

import { ref, computed, watch } from 'vue'

const props = defineProps<{
  data: any[]
  loading?: boolean
  title?: string
}>()

const emit = defineEmits<{
  (e: 'row-click', row: any): void
  (e: 'sort', field: string, order: 'asc' | 'desc'): void
  (e: 'filter', filters: Record<string, string>): void
  (e: 'export', format: string): void
}>()

// 表格状态
const sortField = ref<string | null>(null)
const sortOrder = ref<'asc' | 'desc'>('asc')
const selectedRows = ref<Set<number>>(new Set())
const currentPage = ref(1)
const pageSize = ref(50)
const searchQuery = ref('')
const columnFilters = ref<Record<string, string>>({})

// 提取列信息
const columns = computed(() => {
  if (!props.data || props.data.length === 0) return []
  
  const firstRow = props.data[0]
  return Object.keys(firstRow).map(key => ({
    key,
    label: key,
    sortable: true,
    width: key === 'geometry' ? 200 : 'auto'
  }))
})

// 过滤后的数据
const filteredData = computed(() => {
  if (!props.data) return []
  
  let result = [...props.data]
  
  // 全局搜索
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    result = result.filter(row => 
      Object.values(row).some(val => 
        String(val).toLowerCase().includes(query)
      )
    )
  }
  
  // 列过滤
  for (const [key, value] of Object.entries(columnFilters.value)) {
    if (value) {
      result = result.filter(row => 
        String(row[key]).toLowerCase().includes(value.toLowerCase())
      )
    }
  }
  
  // 排序
  if (sortField.value) {
    result.sort((a, b) => {
      const aVal = a[sortField.value!]
      const bVal = b[sortField.value!]
      
      if (aVal === bVal) return 0
      
      const comparison = aVal < bVal ? -1 : 1
      return sortOrder.value === 'asc' ? comparison : -comparison
    })
  }
  
  return result
})

// 分页数据
const paginatedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredData.value.slice(start, end)
})

// 总页数
const totalPages = computed(() => 
  Math.ceil(filteredData.value.length / pageSize.value)
)

// 分页范围
const pageRange = computed(() => {
  const range: number[] = []
  const maxVisible = 5
  
  let start = Math.max(1, currentPage.value - Math.floor(maxVisible / 2))
  let end = Math.min(totalPages.value, start + maxVisible - 1)
  
  if (end - start < maxVisible - 1) {
    start = Math.max(1, end - maxVisible + 1)
  }
  
  for (let i = start; i <= end; i++) {
    range.push(i)
  }
  
  return range
})

// 排序
function handleSort(field: string) {
  if (sortField.value === field) {
    sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortField.value = field
    sortOrder.value = 'asc'
  }
  emit('sort', field, sortOrder.value)
}

// 行选择
function toggleRow(index: number) {
  if (selectedRows.value.has(index)) {
    selectedRows.value.delete(index)
  } else {
    selectedRows.value.add(index)
  }
  selectedRows.value = new Set(selectedRows.value)
}

function selectAll() {
  if (selectedRows.value.size === paginatedData.value.length) {
    selectedRows.value.clear()
  } else {
    paginatedData.value.forEach((_, i) => selectedRows.value.add(i))
  }
  selectedRows.value = new Set(selectedRows.value)
}

// 导出
function exportData(format: string) {
  emit('export', format)
}

// 列过滤
function setColumnFilter(key: string, value: string) {
  if (value) {
    columnFilters.value[key] = value
  } else {
    delete columnFilters.value[key]
  }
  emit('filter', columnFilters.value)
}

// 格式化值显示
function formatValue(value: any): string {
  if (value === null || value === undefined) return '-'
  if (typeof value === 'number') {
    return value.toLocaleString('en-US', { maximumFractionDigits: 4 })
  }
  if (typeof value === 'object') {
    return JSON.stringify(value)
  }
  return String(value)
}
</script>

<template>
  <div class="attribute-table">
    <!-- 表格头部 -->
    <div class="table-header">
      <div class="header-left">
        <h4 v-if="title">{{ title }}</h4>
        <span class="record-count">
          共 {{ filteredData.length }} 条记录
          <span v-if="selectedRows.size > 0">
            (已选择 {{ selectedRows.size }} 条)
          </span>
        </span>
      </div>
      
      <div class="header-right">
        <!-- 搜索 -->
        <div class="search-box">
          <span class="search-icon">🔍</span>
          <input 
            v-model="searchQuery" 
            type="text" 
            placeholder="搜索..."
            class="search-input"
          />
        </div>
        
        <!-- 导出 -->
        <div class="export-dropdown">
          <button class="export-btn">
            📥 导出
          </button>
          <div class="dropdown-menu">
            <button @click="exportData('csv')">导出 CSV</button>
            <button @click="exportData('xlsx')">导出 Excel</button>
            <button @click="exportData('json')">导出 JSON</button>
            <button @click="exportData('geojson')">导出 GeoJSON</button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <span>加载中...</span>
    </div>
    
    <!-- 数据表 -->
    <div v-else class="table-container">
      <table class="data-table">
        <thead>
          <tr>
            <th class="checkbox-col">
              <input 
                type="checkbox" 
                :checked="selectedRows.size === paginatedData.length && paginatedData.length > 0"
                @change="selectAll"
              />
            </th>
            <th 
              v-for="col in columns" 
              :key="col.key"
              :style="{ width: col.width }"
              :class="{ sortable: col.sortable, sorted: sortField === col.key }"
              @click="col.sortable && handleSort(col.key)"
            >
              <div class="th-content">
                <span>{{ col.label }}</span>
                <span v-if="col.sortable" class="sort-icon">
                  <span v-if="sortField === col.key">
                    {{ sortOrder === 'asc' ? '↑' : '↓' }}
                  </span>
                  <span v-else class="sort-inactive">↕</span>
                </span>
              </div>
              <!-- 过滤输入 -->
              <input 
                v-if="columnFilters[col.key] !== undefined"
                type="text"
                :value="columnFilters[col.key]"
                @input="setColumnFilter(col.key, ($event.target as HTMLInputElement).value)"
                class="column-filter"
                placeholder="过滤..."
                @click.stop
              />
            </th>
          </tr>
        </thead>
        <tbody>
          <tr 
            v-for="(row, index) in paginatedData" 
            :key="index"
            :class="{ selected: selectedRows.has(index) }"
            @click="emit('row-click', row)"
          >
            <td class="checkbox-col" @click.stop>
              <input 
                type="checkbox" 
                :checked="selectedRows.has(index)"
                @change="toggleRow(index)"
              />
            </td>
            <td 
              v-for="col in columns" 
              :key="col.key"
              :class="{ 'geometry-cell': col.key === 'geometry' }"
            >
              <span v-if="col.key === 'geometry'" class="geometry-badge">
                📍 {{ row[col.key]?.type || 'Geometry' }}
              </span>
              <span v-else>{{ formatValue(row[col.key]) }}</span>
            </td>
          </tr>
          <tr v-if="paginatedData.length === 0">
            <td :colspan="columns.length + 1" class="empty-cell">
              暂无数据
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    
    <!-- 分页 -->
    <div class="table-footer">
      <div class="page-size-selector">
        <span>每页</span>
        <select v-model="pageSize" @change="currentPage = 1">
          <option :value="25">25</option>
          <option :value="50">50</option>
          <option :value="100">100</option>
          <option :value="500">500</option>
        </select>
        <span>条</span>
      </div>
      
      <div class="pagination">
        <button 
          class="page-btn" 
          :disabled="currentPage === 1"
          @click="currentPage = 1"
        >
          首页
        </button>
        <button 
          class="page-btn" 
          :disabled="currentPage === 1"
          @click="currentPage--"
        >
          ‹
        </button>
        
        <button 
          v-for="page in pageRange" 
          :key="page"
          :class="['page-btn', { active: currentPage === page }]"
          @click="currentPage = page"
        >
          {{ page }}
        </button>
        
        <button 
          class="page-btn" 
          :disabled="currentPage === totalPages"
          @click="currentPage++"
        >
          ›
        </button>
        <button 
          class="page-btn" 
          :disabled="currentPage === totalPages"
          @click="currentPage = totalPages"
        >
          末页
        </button>
      </div>
      
      <div class="page-info">
        第 {{ currentPage }} / {{ totalPages }} 页
      </div>
    </div>
  </div>
</template>

<style scoped>
.attribute-table {
  display: flex;
  flex-direction: column;
  background: rgba(30, 41, 59, 0.98);
  border-radius: 12px;
  overflow: hidden;
  height: 100%;
}

/* 头部 */
.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.header-left h4 {
  margin: 0;
  font-size: 1rem;
  font-weight: 600;
  color: white;
}

.record-count {
  font-size: 0.875rem;
  color: #64748b;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.search-box {
  display: flex;
  align-items: center;
  background: rgba(0, 0, 0, 0.2);
  border-radius: 8px;
  padding: 0 0.75rem;
}

.search-icon {
  font-size: 0.875rem;
  opacity: 0.5;
}

.search-input {
  background: transparent;
  border: none;
  color: white;
  padding: 0.5rem;
  font-size: 0.875rem;
  width: 150px;
}

.search-input::placeholder {
  color: #64748b;
}

.search-input:focus {
  outline: none;
}

/* 导出下拉 */
.export-dropdown {
  position: relative;
}

.export-btn {
  padding: 0.5rem 1rem;
  background: #2563eb;
  border: none;
  border-radius: 8px;
  color: white;
  font-size: 0.875rem;
  cursor: pointer;
  transition: background 0.2s;
}

.export-btn:hover {
  background: #1d4ed8;
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 0.5rem;
  background: rgba(30, 41, 59, 0.98);
  border-radius: 8px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.5);
  overflow: hidden;
  display: none;
  z-index: 100;
  min-width: 150px;
}

.export-dropdown:hover .dropdown-menu {
  display: block;
}

.dropdown-menu button {
  width: 100%;
  padding: 0.75rem 1rem;
  background: transparent;
  border: none;
  color: #94a3b8;
  font-size: 0.875rem;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s;
}

.dropdown-menu button:hover {
  background: rgba(255, 255, 255, 0.1);
  color: white;
}

/* 加载状态 */
.loading-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  padding: 2rem;
  color: #64748b;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(255, 255, 255, 0.1);
  border-top-color: #2563eb;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 表格容器 */
.table-container {
  flex: 1;
  overflow: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.875rem;
}

.data-table th,
.data-table td {
  padding: 0.75rem 1rem;
  text-align: left;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  white-space: nowrap;
}

.data-table th {
  position: sticky;
  top: 0;
  background: rgba(15, 23, 42, 0.95);
  color: #94a3b8;
  font-weight: 600;
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  z-index: 10;
}

.data-table th.sortable {
  cursor: pointer;
  user-select: none;
}

.data-table th.sortable:hover {
  color: white;
}

.data-table th.sorted {
  color: #60a5fa;
}

.th-content {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.sort-icon {
  font-size: 0.75rem;
}

.sort-inactive {
  opacity: 0.3;
}

.column-filter {
  width: 100%;
  margin-top: 0.5rem;
  padding: 0.375rem;
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  color: white;
  font-size: 0.75rem;
}

.column-filter:focus {
  outline: none;
  border-color: #2563eb;
}

.data-table td {
  color: #e2e8f0;
}

.data-table tr {
  transition: background 0.15s;
}

.data-table tbody tr:hover {
  background: rgba(255, 255, 255, 0.05);
}

.data-table tbody tr.selected {
  background: rgba(37, 99, 235, 0.2);
}

.checkbox-col {
  width: 40px;
  text-align: center;
}

.checkbox-col input[type="checkbox"] {
  width: 16px;
  height: 16px;
  accent-color: #2563eb;
  cursor: pointer;
}

.geometry-cell {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.geometry-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.25rem 0.5rem;
  background: rgba(37, 99, 235, 0.2);
  border-radius: 4px;
  font-size: 0.75rem;
  color: #60a5fa;
}

.empty-cell {
  text-align: center;
  color: #64748b;
  padding: 2rem !important;
}

/* 表尾 */
.table-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 1rem;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(15, 23, 42, 0.5);
}

.page-size-selector {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: #64748b;
  font-size: 0.875rem;
}

.page-size-selector select {
  padding: 0.375rem 0.5rem;
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  color: white;
  font-size: 0.875rem;
}

.pagination {
  display: flex;
  gap: 0.25rem;
}

.page-btn {
  min-width: 32px;
  height: 32px;
  padding: 0 0.5rem;
  background: rgba(255, 255, 255, 0.05);
  border: none;
  border-radius: 6px;
  color: #94a3b8;
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s;
}

.page-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.1);
  color: white;
}

.page-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.page-btn.active {
  background: #2563eb;
  color: white;
}

.page-info {
  color: #64748b;
  font-size: 0.875rem;
}
</style>
