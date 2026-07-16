import { api } from "../api/client.js?v=20260715-comment-notifications-v1";
import { $ } from "../utils/dom.js?v=20260715-comment-notifications-v1";
import { renderActivityNotifications } from "./renderers.js?v=20260715-comment-notifications-v1";
import { activityNotificationState, socialNotificationState } from "./state.js?v=20260715-comment-notifications-v1";

export function bindActivityNotificationEvents() {
  $("#markAllActivityNotifications").addEventListener("click", markAllRead);
}

async function markAllRead() {
  const button = $("#markAllActivityNotifications");
  const notificationState = activityNotificationState();
  const socialState = socialNotificationState();
  button.disabled = true;
  try {
    const [activitySummary, socialSummary] = await Promise.all([
      api.markAllActivityNotificationsRead(),
      api.markAllSocialNotificationsRead()
    ]);
    notificationState.items = activitySummary.items || [];
    notificationState.unreadCount = Number(activitySummary.unreadCount || 0);
    socialState.items = socialSummary.items || [];
    socialState.unreadCount = Number(socialSummary.unreadCount || 0);
    notificationState.notice = { kind: "success", message: "站内通知已全部标为已读。" };
    socialState.notice = null;
  } catch (error) {
    notificationState.notice = {
      kind: "error",
      message: error.message || "通知状态更新失败，请稍后重试。"
    };
  }
  renderActivityNotifications();
}
