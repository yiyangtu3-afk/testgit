import { withApiFallback } from "./auth-api";

export function createNotificationApi({ http, mockNotifications }) {
  const call = (path, options, fallback) => withApiFallback(
    () => http.request(path, options),
    fallback
  );

  return {
    activityNotifications: () => call("/api/activity-notifications", {}, () => mockNotifications.activityNotifications()),
    markAllActivityNotificationsRead: () => call("/api/activity-notifications/read-all", { method: "POST" }, () => mockNotifications.markAllActivityNotificationsRead()),
    markActivityNotificationRead: (notificationId) => call(`/api/activity-notifications/${notificationId}/read`, { method: "POST" }, () => mockNotifications.markActivityNotificationRead(notificationId)),
    socialNotifications: () => call("/api/social-notifications", {}, () => mockNotifications.socialNotifications()),
    markAllSocialNotificationsRead: () => call("/api/social-notifications/read-all", { method: "POST" }, () => mockNotifications.markAllSocialNotificationsRead()),
    markSocialNotificationRead: (notificationId) => call(`/api/social-notifications/${notificationId}/read`, { method: "POST" }, () => mockNotifications.markSocialNotificationRead(notificationId)),
    socialNotificationPostTarget: (notificationId) => call(`/api/social-notifications/${notificationId}/post-target`, {}, () => mockNotifications.socialNotificationPostTarget(notificationId)),
    socialNotificationFriendRequestTarget: (notificationId) => call(`/api/social-notifications/${notificationId}/friend-request-target`, {}, () => mockNotifications.socialNotificationFriendRequestTarget(notificationId))
  };
}
