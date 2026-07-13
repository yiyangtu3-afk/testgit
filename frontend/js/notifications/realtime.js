import { renderActivityNotifications } from "./renderers.js?v=20260713-social-like-notifications-v1";
import { activityNotificationState } from "./state.js?v=20260713-social-like-notifications-v1";

export function handleActivityNotificationEvent(payload) {
  if (payload.type !== "activity.notification.created" || !payload.notification) {
    return false;
  }
  const notificationState = activityNotificationState();
  const incoming = payload.notification;
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
    message: `收到新的活动通知：${incoming.title}`
  };
  renderActivityNotifications();
  return true;
}
