<script setup>
import { computed, onBeforeUnmount, onMounted, watch } from "vue";
import { RouterLink, RouterView, useRouter } from "vue-router";
import { useSessionStore } from "../../stores/session";
import { useAppShellStore } from "../../stores/app-shell";
import { navigationItems } from "./navigation";
import StatusNotice from "./StatusNotice.vue";
import { useChatStore } from "../../stores/chat";
import { useNotificationStore } from "../../stores/notifications";
import { createChatRealtime } from "../../services/realtime/chat-realtime";

const router = useRouter();
const session = useSessionStore();
const shell = useAppShellStore();
const chat = useChatStore();
const notifications = useNotificationStore();
const source = computed(() => session.mode || "unknown");
let realtime;

onMounted(() => {
  if (!session.isAuthenticated) router.replace("/");
  if (session.mode === "api") {
    realtime = createChatRealtime({
      token: () => session.token,
      onMode: () => {},
      onConversationEvent: async (peerId) => {
        await chat.refreshLists();
        if (peerId === chat.selectedId) await chat.loadMessages(peerId);
      },
      onNotificationEvent: (payload) => notifications.receive(payload)
    });
    realtime.connect();
  }
});

onBeforeUnmount(() => realtime?.disconnect());

watch(
  () => session.feedback,
  (feedback) => {
    if (feedback) shell.setNotice(feedback, session.mode === "mock" ? "info" : "success");
  },
  { immediate: true }
);

function explainPending(label) {
  shell.setNotice(`${label} 尚未迁移；请继续使用旧版功能基线。`, "info");
}

async function logout() {
  try {
    await session.logout();
  } finally {
    realtime?.disconnect();
    router.replace("/");
  }
}
</script>

<template>
  <main class="app-shell">
    <aside class="side-rail">
      <RouterLink class="wordmark" to="/workspace" aria-label="CampusLink 工作台">
        <span class="wordmark-mark">CL</span>
        <span>CampusLink<small>Vue preview</small></span>
      </RouterLink>

      <nav class="primary-nav" aria-label="校园工作台导航">
        <template v-for="item in navigationItems" :key="item.label">
          <RouterLink
            v-if="item.available"
            :to="item.to"
            class="nav-item"
            active-class="is-active"
          >
            <span>{{ item.label }}</span><small>{{ item.caption }}</small>
          </RouterLink>
          <button
            v-else
            type="button"
            class="nav-item is-pending"
            @click="explainPending(item.label)"
          >
            <span>{{ item.label }}</span><small>{{ item.caption }}</small>
          </button>
        </template>
      </nav>

      <div class="rail-footer">
        <p>迁移进度</p>
        <strong>07 / 07</strong>
        <span>领域迁移完成</span>
      </div>
    </aside>

    <section class="shell-main">
      <header class="shell-header">
        <div><p class="eyebrow">CAMPUSLINK / WORKSPACE</p><h1>校园工作台</h1></div>
        <div class="account-cluster">
          <div class="account-avatar">{{ session.user?.name?.slice(0, 1) || "访" }}</div>
          <div><strong>{{ session.user?.name || "未登录" }}</strong><span>{{ session.user?.role || "" }}</span></div>
          <button type="button" class="text-button" @click="logout">退出</button>
        </div>
      </header>

      <StatusNotice :mode="source" :notice="shell.notice" />
      <RouterView />
    </section>
  </main>
</template>
