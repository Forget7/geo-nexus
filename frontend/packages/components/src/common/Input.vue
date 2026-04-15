<template>
  <el-input v-bind="$attrs">
    <slot />
  </el-input>
</template>

<script setup lang="ts">
defineOptions({
  name: 'GnInput',
  inheritAttrs: false
})
</script>
