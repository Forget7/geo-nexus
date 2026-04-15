<template>
  <div class="gn-chatmessage">
    <slot />
  </div>
</template>

<script setup lang="ts">
defineOptions({
  name: 'GnChatMessage'
})
</script>

<style scoped>
.gn-chatmessage {
  width: 100%;
}
</style>
