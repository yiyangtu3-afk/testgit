<script setup>
defineProps({
  mode: { type: String, default: "unknown" },
  notice: { type: Object, required: true }
});
</script>

<template>
  <aside class="status-notice" :class="`is-${notice.tone}`" aria-live="polite">
    <span class="status-orb" aria-hidden="true"></span>
    <div>
      <p class="status-label">
        {{ mode === "api" ? "Java API" : mode === "mock" ? "Mock 模式" : "连接状态" }}
      </p>
      <p class="status-copy">{{ notice.message }}</p>
    </div>
  </aside>
</template>
