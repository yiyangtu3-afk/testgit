import { $ } from "../utils/dom.js?v=20260712-activity-operations-v1";
import { escapeHtml } from "../utils/format.js?v=20260712-activity-operations-v1";
import { activityNotificationState } from "./state.js?v=20260712-activity-operations-v1";

const typeLabels = {
  "activity.review.approved": "审核通过",
  "activity.review.rejected": "审核退回",
  "activity.registration.registered": "报名成功",
  "activity.registration.waitlisted": "进入候补",
  "activity.registration.promoted": "候补递补"
};

export function renderActivityNotifications() {
  const notificationState = activityNotificationState();
  const badge = $("#activityNotificationBadge");
  badge.hidden = notificationState.unreadCount === 0;
  badge.textContent = String(notificationState.unreadCount);
  $("#activityNotificationUnreadCount").textContent = String(notificationState.unreadCount);
  $("#markAllActivityNotifications").disabled = notificationState.unreadCount === 0;
  renderNotice(notificationState.notice);
  $("#activityNotificationList").innerHTML = notificationState.items.length
    ? notificationState.items.map(notificationCard).join("")
    : `<div class="notification-empty">
        <strong>还没有活动通知</strong>
        <p>活动审核、报名、候补和递补结果会保留在这里。</p>
      </div>`;
}

function notificationCard(notification) {
  const typeLabel = typeLabels[notification.type] || "活动更新";
  return `<article class="notification-card${notification.read ? "" : " notification-card--unread"}">
    <div class="notification-marker" aria-hidden="true"></div>
    <div class="notification-copy">
      <div class="notification-meta">
        <span>${escapeHtml(typeLabel)}</span>
        <time>${escapeHtml(notificationTime(notification.createdAt))}</time>
      </div>
      <h3>${escapeHtml(notification.title)}</h3>
      <p>${escapeHtml(notification.body)}</p>
    </div>
    <span class="notification-read-state">${notification.read ? "已读" : "未读"}</span>
  </article>`;
}

function renderNotice(noticeState) {
  const notice = $("#activityNotificationNotice");
  notice.hidden = !noticeState;
  notice.className = noticeState
    ? `activity-feedback activity-feedback--${noticeState.kind}`
    : "activity-feedback";
  notice.textContent = noticeState ? noticeState.message : "";
}

function notificationTime(value) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "刚刚";
  return new Intl.DateTimeFormat("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false
  }).format(date);
}
