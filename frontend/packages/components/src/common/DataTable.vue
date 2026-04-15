<template>
  <el-datatable v-bind="$attrs">
    <slot />
  </el-datatable>
</template>

<script setup lang="ts">
defineOptions({
  name: 'GnDataTable',
  inheritAttrs: false
})
</script>
