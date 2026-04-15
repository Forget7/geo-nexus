<template>
  <el-dialog v-bind="$attrs">
    <slot />
  </el-dialog>
</template>

<script setup lang="ts">
defineOptions({
  name: 'GnDialog',
  inheritAttrs: false
})
</script>
