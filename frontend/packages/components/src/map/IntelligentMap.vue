<template>
  <div class="gn-intelligentmap">
    <slot />
  </div>
</template>

<script setup lang="ts">
defineOptions({
  name: 'GnIntelligentMap'
})
</script>

<style scoped>
.gn-intelligentmap {
  width: 100%;
  height: 100%;
}
</style>
