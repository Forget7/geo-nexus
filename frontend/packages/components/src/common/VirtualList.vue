<template>
  <el-virtuallist v-bind="$attrs">
    <slot />
  </el-virtuallist>
</template>

<script setup lang="ts">
defineOptions({
  name: 'GnVirtualList',
  inheritAttrs: false
})
</script>
