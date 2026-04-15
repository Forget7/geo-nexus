<template>
  <el-loading v-bind="$attrs">
    <slot />
  </el-loading>
</template>

<script setup lang="ts">
defineOptions({
  name: 'GnLoading',
  inheritAttrs: false
})
</script>
