<template>
  <el-button v-bind="$attrs">
    <slot />
  </el-button>
</template>

<script setup lang="ts">
defineOptions({
  name: 'GnButton',
  inheritAttrs: false
})
</script>
