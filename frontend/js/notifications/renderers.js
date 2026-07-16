import { $ } from "../utils/dom.js?v=20260715-real-dashboard-metrics-v1";
import { escapeHtml } from "../utils/format.js?v=20260715-real-dashboard-metrics-v1";
import { activityNotificationState, socialNotificationState } from "./state.js?v=20260715-real-dashboard-metrics-v1";

const typeLabels = {
  "activity.review.approved": "审核通过",
  "activity.review.rejected": "审核退回",
  "activity.registration.registered": "报名成功",
  "activity.registration.waitlisted": "进入候补",
  "activity.registration.promoted": "候补递补",
  "social.post.liked": "动态点赞",
  "social.post.commented": "动态评论",
  "social.friend.requested": "好友申请",
  "social.friend.accepted": "好友已添加",
  "social.friend.rejected": "好友申请结果"
};

export function renderActivityNotifications() {
  const notificationState = activityNotificationState();
  const socialState = socialNotificationState();
  const unreadCount = notificationState.unreadCount + socialState.unreadCount;
  const items = [...notificationState.items, ...socialState.items]
    .sort((left, right) => new Date(right.createdAt) - new Date(left.createdAt));
  const badge = $("#activityNotificationBadge");
  badge.hidden = unreadCount === 0;
  badge.textContent = String(unreadCount);
  $("#activityNotificationUnreadCount").textContent = String(unreadCount);
  $("#markAllActivityNotifications").disabled = unreadCount === 0;
  renderNotice(socialState.notice || notificationState.notice);
  $("#activityNotificationList").innerHTML = items.length
    ? items.map(notificationCard).join("")
    : `<div class="notification-empty">
        <strong>还没有站内通知</strong>
        <p>活动状态和动态互动结果会保留在这里。</p>
      </div>`;
}

function notificationCard(notification) {
  const typeLabel = typeLabels[notification.type] || "站内更新";
  const isActivity = notification.type.startsWith("activity.");
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
    <div class="notification-actions">
      ${notificationActionMarkup(notification, isActivity)}
    </div>
    <span class="notification-read-state">${notification.read ? "已读" : "未读"}</span>
  </article>`;
}

function notificationActionMarkup(notification, isActivity) {
  const actions = [];
  if (isActivity) {
    actions.push(`<button class="small-button" type="button" data-open-activity-notification="${escapeHtml(notification.id)}" data-activity-id="${escapeHtml(notification.activityId)}">查看活动</button>`);
  } else if (notification.type.startsWith("social.post.")) {
    actions.push(`<button class="small-button" type="button" data-open-social-notification="${escapeHtml(notification.id)}">查看动态</button>`);
  } else if (notification.type === "social.friend.requested") {
    actions.push(`<button class="small-button" type="button" data-open-friend-request-notification="${escapeHtml(notification.id)}">处理申请</button>`);
  }
  if (!notification.read) {
    actions.push(`<button class="small-button" type="button" data-mark-notification="${escapeHtml(notification.id)}" data-notification-kind="${isActivity ? "activity" : "social"}">标为已读</button>`);
  }
  return actions.join("");
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
