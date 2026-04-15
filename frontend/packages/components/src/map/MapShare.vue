<template>
  <div class="gn-mapshare">
    <slot />
  </div>
</template>

<script setup lang="ts">
defineOptions({
  name: 'GnMapShare'
})
</script>

<style scoped>
.gn-mapshare {
  width: 100%;
  height: 100%;
}
</style>
