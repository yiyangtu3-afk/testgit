import { api } from "../api/client.js?v=20260715-notification-actions-v1";
import { $ } from "../utils/dom.js?v=20260715-notification-actions-v1";
import { renderActivityNotifications } from "./renderers.js?v=20260715-notification-actions-v1";
import { activityNotificationState, socialNotificationState } from "./state.js?v=20260715-notification-actions-v1";
import { state } from "../state.js";
import { loadActivities, loadFeed } from "../loaders.js?v=20260715-notification-actions-v1";
import { switchTab } from "../ui/shell.js?v=20260715-notification-actions-v1";

export function bindActivityNotificationEvents() {
  $("#markAllActivityNotifications").addEventListener("click", markAllRead);
  $("#activityNotificationList").addEventListener("click", handleNotificationAction);
}

async function handleNotificationAction(event) {
  const markButton = event.target.closest("[data-mark-notification]");
  if (markButton) {
    await markNotification(markButton.dataset.notificationKind, markButton.dataset.markNotification);
    return;
  }
  const activityButton = event.target.closest("[data-open-activity-notification]");
  if (activityButton) {
    await markNotification("activity", activityButton.dataset.openActivityNotification);
    state.notificationActivityFocusId = activityButton.dataset.activityId;
    switchTab("activities");
    await loadActivities();
    return;
  }
  const socialButton = event.target.closest("[data-open-social-notification]");
  if (socialButton) {
    await openSocialNotification(socialButton.dataset.openSocialNotification);
  }
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

async function markNotification(kind, notificationId) {
  const notificationState = kind === "activity"
    ? activityNotificationState()
    : socialNotificationState();
  try {
    const summary = kind === "activity"
      ? await api.markActivityNotificationRead(notificationId)
      : await api.markSocialNotificationRead(notificationId);
    notificationState.items = summary.items || [];
    notificationState.unreadCount = Number(summary.unreadCount || 0);
    notificationState.notice = { kind: "success", message: "通知已标为已读。" };
  } catch (error) {
    notificationState.notice = {
      kind: "error",
      message: error.message || "通知状态更新失败，请稍后重试。"
    };
  }
  renderActivityNotifications();
}

async function openSocialNotification(notificationId) {
  const notificationState = socialNotificationState();
  try {
    const target = await api.socialNotificationPostTarget(notificationId);
    await markNotification("social", notificationId);
    state.notificationPostFocusId = Number(target.postId);
    state.personalPostManagerOpen = false;
    switchTab("feed");
    await loadFeed();
  } catch (error) {
    notificationState.notice = {
      kind: "error",
      message: error.message || "关联动态暂时无法打开，请稍后重试。"
    };
    renderActivityNotifications();
  }
}
