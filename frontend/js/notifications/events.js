import { api } from "../api/client.js?v=20260712-activity-notifications-v1";
import { $ } from "../utils/dom.js?v=20260712-activity-notifications-v1";
import { renderActivityNotifications } from "./renderers.js?v=20260712-activity-notifications-v1";
import { activityNotificationState } from "./state.js?v=20260712-activity-notifications-v1";

export function bindActivityNotificationEvents() {
  $("#markAllActivityNotifications").addEventListener("click", markAllRead);
}

async function markAllRead() {
  const button = $("#markAllActivityNotifications");
  const notificationState = activityNotificationState();
  button.disabled = true;
  try {
    const summary = await api.markAllActivityNotificationsRead();
    notificationState.items = summary.items || [];
    notificationState.unreadCount = Number(summary.unreadCount || 0);
    notificationState.notice = { kind: "success", message: "活动通知已全部标为已读。" };
  } catch (error) {
    notificationState.notice = {
      kind: "error",
      message: error.message || "通知状态更新失败，请稍后重试。"
    };
  }
  renderActivityNotifications();
}
