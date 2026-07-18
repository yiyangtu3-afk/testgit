<script setup>
import { onMounted } from "vue";
import { useRouter } from "vue-router";
import { useNotificationStore } from "../../stores/notifications";

const router = useRouter();
const notifications = useNotificationStore();
onMounted(() => notifications.load());

const labels = {
  "activity.review.approved": "审核通过", "activity.review.rejected": "审核退回",
  "activity.registration.registered": "报名成功", "activity.registration.waitlisted": "进入候补", "activity.registration.promoted": "候补递补",
  "social.post.liked": "动态点赞", "social.post.commented": "动态评论", "social.friend.requested": "好友申请", "social.friend.accepted": "好友已添加", "social.friend.rejected": "好友申请结果"
};
function label(notification) { return labels[notification.type] || "站内更新"; }
function formatTime(value) { const date = new Date(value); return Number.isNaN(date.getTime()) ? "刚刚" : new Intl.DateTimeFormat("zh-CN", { month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit", hour12: false }).format(date); }
function actionLabel(notification) { if (notification.kind === "activity") return "查看活动"; if (notification.type.startsWith("social.post.")) return "查看动态"; if (notification.type === "social.friend.requested") return "处理申请"; return ""; }
async function open(notification) { const target = await notifications.resolveTarget(notification); if (target) await router.push(target); }
</script>

<template>
  <section class="notifications-workspace">
    <header class="notifications-heading"><div><p class="eyebrow">NOTIFICATION DESK</p><h2>站内通知</h2></div><div class="notification-summary"><strong>{{ notifications.unreadCount }}</strong><span>条未读</span></div></header>
    <div class="notification-toolbar"><p>活动状态、动态互动和好友申请都会保留在这里。</p><button :disabled="notifications.unreadCount === 0" @click="notifications.markAll">全部标为已读</button></div>
    <p v-if="notifications.notice" class="feed-notice">{{ notifications.notice }}</p>
    <div v-if="notifications.loading" class="notification-empty"><strong>正在整理通知…</strong></div>
    <div v-else-if="notifications.items.length" class="notification-list">
      <article v-for="notification in notifications.items" :key="notification.id" class="notification-card" :class="{ unread: !notification.read }">
        <span class="notification-dot" aria-hidden="true"></span>
        <div class="notification-copy"><div><span class="notification-type">{{ label(notification) }}</span><time>{{ formatTime(notification.createdAt) }}</time></div><h3>{{ notification.title }}</h3><p>{{ notification.body }}</p></div>
        <footer><button v-if="actionLabel(notification)" @click="open(notification)">{{ actionLabel(notification) }}</button><button v-if="!notification.read" class="quiet" @click="notifications.mark(notification)">标为已读</button><span>{{ notification.read ? "已读" : "未读" }}</span></footer>
      </article>
    </div>
    <div v-else class="notification-empty"><strong>还没有站内通知</strong><p>活动状态和社交互动结果会保留在这里。</p></div>
  </section>
</template>
