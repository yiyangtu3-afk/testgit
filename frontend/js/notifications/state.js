import { state } from "../state.js";

export function activityNotificationState() {
  if (!Array.isArray(state.activityNotifications)) {
    state.activityNotifications = [];
  }
  if (!Number.isInteger(state.activityNotificationUnreadCount)) {
    state.activityNotificationUnreadCount = 0;
  }
  if (!("activityNotificationNotice" in state)) {
    state.activityNotificationNotice = null;
  }
  return {
    get items() {
      return state.activityNotifications;
    },
    set items(value) {
      state.activityNotifications = value;
    },
    get unreadCount() {
      return state.activityNotificationUnreadCount;
    },
    set unreadCount(value) {
      state.activityNotificationUnreadCount = value;
    },
    get notice() {
      return state.activityNotificationNotice;
    },
    set notice(value) {
      state.activityNotificationNotice = value;
    }
  };
}

export function socialNotificationState() {
  if (!Array.isArray(state.socialNotifications)) {
    state.socialNotifications = [];
  }
  if (!Number.isInteger(state.socialNotificationUnreadCount)) {
    state.socialNotificationUnreadCount = 0;
  }
  if (!("socialNotificationNotice" in state)) {
    state.socialNotificationNotice = null;
  }
  return {
    get items() {
      return state.socialNotifications;
    },
    set items(value) {
      state.socialNotifications = value;
    },
    get unreadCount() {
      return state.socialNotificationUnreadCount;
    },
    set unreadCount(value) {
      state.socialNotificationUnreadCount = value;
    },
    get notice() {
      return state.socialNotificationNotice;
    },
    set notice(value) {
      state.socialNotificationNotice = value;
    }
  };
}
