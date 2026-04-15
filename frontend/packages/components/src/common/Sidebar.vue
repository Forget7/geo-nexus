<template>
  <el-sidebar v-bind="$attrs">
    <slot />
  </el-sidebar>
</template>

<script setup lang="ts">
defineOptions({
  name: 'GnSidebar',
  inheritAttrs: false
})
</script>
