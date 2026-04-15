<template>
  <div class="gn-chatpanel">
    <slot />
  </div>
</template>

<script setup lang="ts">
defineOptions({
  name: 'GnChatPanel'
})
</script>

<style scoped>
.gn-chatpanel {
  width: 100%;
}
</style>
