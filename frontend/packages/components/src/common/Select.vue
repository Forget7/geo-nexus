<template>
  <el-select v-bind="$attrs">
    <slot />
  </el-select>
</template>

<script setup lang="ts">
defineOptions({
  name: 'GnSelect',
  inheritAttrs: false
})
</script>
