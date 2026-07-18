<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
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
const accountMenuOpen = ref(false);
const switchError = ref("");
const demoAccounts = [
  { id: "u-1001", name: "林一", role: "学生账号" },
  { id: "u-2001", name: "陈老师", role: "教师账号" },
  { id: "u-2003", name: "教务管理员", role: "管理员" }
];
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

async function switchAccount(userId) {
  if (userId === session.user?.id || session.busy) return;
  switchError.value = "";
  try {
    await session.switchDemoAccount(userId);
    window.location.assign("/workspace");
  } catch (error) {
    switchError.value = error.message || "账号切换失败，当前会话未改变。";
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
          <div class="account-actions">
            <button
              type="button"
              class="text-button"
              :aria-expanded="accountMenuOpen"
              aria-controls="account-switcher"
              @click="accountMenuOpen = !accountMenuOpen"
            >切换账号</button>
            <button type="button" class="text-button" @click="logout">退出</button>
          </div>
          <section v-if="accountMenuOpen" id="account-switcher" class="account-switcher" aria-label="切换演示账号">
            <p>切换演示身份</p>
            <button
              v-for="account in demoAccounts"
              :key="account.id"
              type="button"
              :class="{ 'is-current': account.id === session.user?.id }"
              :disabled="account.id === session.user?.id || session.busy"
              @click="switchAccount(account.id)"
            >
              <span>{{ account.name.slice(0, 1) }}</span>
              <strong>{{ account.name }}</strong>
              <small>{{ account.id === session.user?.id ? "当前账号" : account.role }}</small>
            </button>
            <p v-if="switchError" class="account-switcher-error" role="alert">{{ switchError }}</p>
          </section>
        </div>
      </header>

      <StatusNotice :mode="source" :notice="shell.notice" />
      <RouterView />
    </section>
  </main>
</template>
