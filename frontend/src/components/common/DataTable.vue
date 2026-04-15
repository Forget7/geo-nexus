<script setup lang="ts">
/**
 * DataTable - 数据表格组件
 */

import { ref, computed } from 'vue'

export interface Column {
  key: string
  title: string
  width?: string
  align?: 'left' | 'center' | 'right'
  sortable?: boolean
  formatter?: (value: unknown, row: Record<string, unknown>) => string
}

export interface TableRow extends Record<string, unknown> {
  [key: string]: unknown
}

const props = defineProps<{
  columns: Column[]
  data: TableRow[]
  loading?: boolean
  emptyText?: string
  rowClass?: (row: TableRow, index: number) => string
}>()

const emit = defineEmits<{
  (e: 'row-click', row: TableRow, index: number): void
  (e: 'sort', key: string, order: 'asc' | 'desc'): void
}>()

const sortKey = ref('')
const sortOrder = ref<'asc' | 'desc'>('asc')

const sortedData = computed(() => {
  if (!sortKey.value) return props.data
  
  return [...props.data].sort((a, b) => {
    const aVal = a[sortKey.value]
    const bVal = b[sortKey.value]
    
    if (aVal === bVal) return 0
    
    const comparison = aVal > bVal ? 1 : -1
    return sortOrder.value === 'asc' ? comparison : -comparison
  })
})

function handleSort(column: Column) {
  if (!column.sortable) return
  
  if (sortKey.value === column.key) {
    sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortKey.value = column.key
    sortOrder.value = 'asc'
  }
  
  emit('sort', sortKey.value, sortOrder.value)
}

function handleRowClick(row: TableRow, index: number) {
  emit('row-click', row, index)
}
</script>

<template>
  <div class="data-table-wrapper">
    <table class="data-table">
      <thead>
        <tr>
          <th 
            v-for="column in columns" 
            :key="column.key"
            :style="{ width: column.width, textAlign: column.align || 'left' }"
            :class="{ sortable: column.sortable, sorted: sortKey === column.key }"
            @click="handleSort(column)"
          >
            {{ column.title }}
            <span v-if="column.sortable" class="sort-icon">
              <template v-if="sortKey === column.key">
                {{ sortOrder === 'asc' ? '↑' : '↓' }}
              </template>
              <template v-else>
                ↕
              </template>
            </span>
          </th>
        </tr>
      </thead>
      
      <tbody v-if="!loading && data.length > 0">
        <tr 
          v-for="(row, index) in sortedData" 
          :key="index"
          :class="rowClass?.(row, index)"
          @click="handleRowClick(row, index)"
        >
          <td 
            v-for="column in columns" 
            :key="column.key"
            :style="{ textAlign: column.align || 'left' }"
          >
            <template v-if="column.formatter">
              <span v-html="column.formatter(row[column.key], row)"></span>
            </template>
            <template v-else>
              {{ row[column.key] }}
            </template>
          </td>
        </tr>
      </tbody>
      
      <tbody v-else-if="loading">
        <tr>
          <td :colspan="columns.length" class="empty-cell">
            <div class="loading-placeholder">
              <span class="loading-spinner"></span>
              加载中...
            </div>
          </td>
        </tr>
      </tbody>
      
      <tbody v-else>
        <tr>
          <td :colspan="columns.length" class="empty-cell">
            {{ emptyText || '暂无数据' }}
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.data-table-wrapper {
  overflow-x: auto;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.875rem;
}

.data-table th {
  padding: 0.75rem 1rem;
  background: #f8fafc;
  font-weight: 600;
  color: #475569;
  border-bottom: 1px solid #e2e8f0;
  white-space: nowrap;
}

.data-table th.sortable {
  cursor: pointer;
  user-select: none;
}

.data-table th.sortable:hover {
  background: #f1f5f9;
}

.data-table th.sorted {
  color: #2563eb;
}

.sort-icon {
  margin-left: 0.25rem;
  opacity: 0.5;
}

.data-table th.sorted .sort-icon {
  opacity: 1;
}

.data-table td {
  padding: 0.75rem 1rem;
  border-bottom: 1px solid #f1f5f9;
  color: #1e293b;
}

.data-table tbody tr:hover {
  background: #f8fafc;
}

.data-table tbody tr {
  cursor: pointer;
}

.empty-cell {
  text-align: center;
  color: #94a3b8;
  padding: 2rem !important;
}

.loading-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

.loading-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid #e2e8f0;
  border-top-color: #2563eb;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
