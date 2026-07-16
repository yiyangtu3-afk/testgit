import { renderActivityNotifications } from "./renderers.js?v=20260715-signed-jwt-logout-v1";
import {
  activityNotificationState,
  socialNotificationState
} from "./state.js?v=20260715-signed-jwt-logout-v1";

export function handleNotificationRealtimeEvent(payload) {
  if (!payload.notification) {
    return false;
  }
  if (payload.type === "activity.notification.created") {
    applyIncomingNotification(activityNotificationState(), payload.notification, "活动");
    return true;
  }
  if (payload.type === "social.notification.created") {
    applyIncomingNotification(socialNotificationState(), payload.notification, "站内");
    return true;
  }
  return false;
}

function applyIncomingNotification(notificationState, incoming, category) {
  const existed = notificationState.items.some((item) => item.id === incoming.id);
  notificationState.items = [
    incoming,
    ...notificationState.items.filter((item) => item.id !== incoming.id)
  ];
  if (!existed && !incoming.read) {
    notificationState.unreadCount += 1;
  }
  notificationState.notice = {
    kind: "success",
    message: `收到新的${category}通知：${incoming.title}`
  };
  renderActivityNotifications();
}
