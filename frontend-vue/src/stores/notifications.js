import { computed, ref } from "vue";
import { defineStore } from "pinia";
import { createApi } from "../services/api";
import { useSessionStore } from "./session";

function withKind(summary, kind) { return (summary.items || []).map((item) => ({ ...item, kind })); }
export function createNotificationStore({ api, setMode = () => {} } = {}) {
  return () => {
    const activity = ref([]), social = ref([]), notice = ref(""), loading = ref(false), initialized = ref(false);
    const items = computed(() => [...activity.value, ...social.value].sort((left, right) => new Date(right.createdAt) - new Date(left.createdAt)));
    const unreadCount = computed(() => items.value.filter((item) => !item.read).length);
    async function run(call) { const result = await call(); setMode(result.mode); return result.data; }
    function applyActivity(summary) { activity.value = withKind(summary, "activity"); }
    function applySocial(summary) { social.value = withKind(summary, "social"); }
    async function load() { loading.value = true; notice.value = ""; try { const [activitySummary, socialSummary] = await Promise.all([run(() => api.activityNotifications()), run(() => api.socialNotifications())]); applyActivity(activitySummary); applySocial(socialSummary); initialized.value = true; } catch (error) { notice.value = error.message || "站内通知暂时无法加载。"; } finally { loading.value = false; } }
    async function mark(notification) { try { const summary = notification.kind === "activity" ? await run(() => api.markActivityNotificationRead(notification.id)) : await run(() => api.markSocialNotificationRead(notification.id)); notification.kind === "activity" ? applyActivity(summary) : applySocial(summary); notice.value = "通知已标为已读。"; return true; } catch (error) { notice.value = error.message || "通知状态更新失败。"; return false; } }
    async function markAll() { try { const [activitySummary, socialSummary] = await Promise.all([run(() => api.markAllActivityNotificationsRead()), run(() => api.markAllSocialNotificationsRead())]); applyActivity(activitySummary); applySocial(socialSummary); notice.value = "站内通知已全部标为已读。"; } catch (error) { notice.value = error.message || "通知状态更新失败。"; } }
    async function resolveTarget(notification) { try { if (notification.kind === "activity") return await mark(notification) ? { name: "activities", query: { activity: notification.activityId } } : null; const result = notification.type.startsWith("social.post.") ? await run(() => api.socialNotificationPostTarget(notification.id)) : notification.type === "social.friend.requested" ? await run(() => api.socialNotificationFriendRequestTarget(notification.id)) : null; if (!result || !await mark(notification)) return null; return notification.type.startsWith("social.post.") ? { name: "feed", query: { post: result.postId } } : { name: "contacts", query: { request: result.requestId } }; } catch (error) { notice.value = error.message || "关联内容暂时无法打开。"; return null; } }
    function receive(payload) { const kind = payload.type === "activity.notification.created" ? "activity" : payload.type === "social.notification.created" ? "social" : null; if (!kind || !payload.notification) return false; const target = kind === "activity" ? activity : social; target.value = [{ ...payload.notification, kind }, ...target.value.filter((item) => item.id !== payload.notification.id)]; notice.value = `收到新的${kind === "activity" ? "活动" : "站内"}通知：${payload.notification.title}`; return true; }
    return { activity, social, items, unreadCount, notice, loading, initialized, load, mark, markAll, resolveTarget, receive };
  };
}
export const useNotificationStore = defineStore("notifications", () => { const session = useSessionStore(); const api = createApi({ getToken: () => session.token, getUser: () => session.user }); return createNotificationStore({ api, setMode: (mode) => { session.mode = mode; } })(); });
