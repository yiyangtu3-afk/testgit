import { state } from "../state.js";
import { isActivityOrganizer, isAdminUser, isStudentUser } from "../utils/auth.js?v=20260711-activity-registration-v1";
import { $ } from "../utils/dom.js?v=20260711-activity-registration-v1";
import { escapeHtml } from "../utils/format.js?v=20260711-activity-registration-v1";

const statusLabels = {
  draft: "需修改",
  pending: "待审核",
  published: "已发布",
  full: "已满员",
  closed: "已结束",
  cancelled: "已取消"
};

export function renderActivities() {
  $("#activityCreatorPanel").hidden = !isActivityOrganizer();
  renderActivityNotice();
  renderActivitySubmissions();
  $("#publishedActivityCount").textContent = String(state.activities.length);
  $("#activityList").innerHTML = state.activities.length
    ? state.activities.map((activity) => activityCard(activity)).join("")
    : `
      <div class="activity-empty">
        <strong>还没有已发布活动</strong>
        <p>管理员审核通过的活动会出现在这里。</p>
      </div>
    `;
}

export function renderPendingActivities() {
  const panel = $("#activityReviewPanel");
  if (!isAdminUser()) {
    panel.hidden = true;
    panel.innerHTML = "";
    return;
  }
  panel.hidden = false;
  panel.innerHTML = `
    <div class="activity-section-head">
      <div>
        <p class="section-kicker">Activity Review</p>
        <h3>待审核活动</h3>
      </div>
      <strong>${state.pendingActivities.length}</strong>
    </div>
    ${activityReviewNoticeMarkup()}
    <div class="activity-review-list">
      ${state.pendingActivities.length
        ? state.pendingActivities.map((activity) => activityReviewCard(activity)).join("")
        : `<div class="activity-empty"><strong>活动队列已清空</strong><p>新的活动提案会显示在这里。</p></div>`}
    </div>
  `;
}

function renderActivityNotice() {
  const notice = $("#activityNotice");
  notice.hidden = !state.activityNotice;
  notice.className = state.activityNotice
    ? `activity-feedback activity-feedback--${state.activityNotice.kind}`
    : "activity-feedback";
  notice.textContent = state.activityNotice ? state.activityNotice.message : "";
}

function renderActivitySubmissions() {
  const submissions = state.activitySubmissions.filter((activity) => {
    return activity.organizerId === state.currentUser.id;
  });
  $("#activitySubmissionPanel").hidden = submissions.length === 0;
  $("#activitySubmissionList").innerHTML = submissions
    .map((activity) => activityCard(activity, true))
    .join("");
}

function activityCard(activity, showReview = false) {
  const status = statusLabels[activity.status] || activity.status;
  const rejection = showReview && activity.reviewDecision === "rejected"
    ? `<p class="activity-rejection"><strong>拒绝原因：</strong>${escapeHtml(activity.reviewReason || "未填写")}</p>`
    : "";
  const registration = state.activityRegistrations[activity.id];
  const registrationAction = !showReview && isStudentUser()
    ? activityRegistrationMarkup(activity, registration)
    : "";
  return `
    <article class="activity-card activity-card--${escapeHtml(activity.status)}">
      <div class="activity-card-date" aria-label="活动日期">
        <strong>${escapeHtml(datePart(activity.startsAt, "day"))}</strong>
        <span>${escapeHtml(datePart(activity.startsAt, "month"))}</span>
      </div>
      <div class="activity-card-copy">
        <div class="activity-card-topline">
          <span>${escapeHtml(activity.category)}</span>
          <span class="activity-status activity-status--${escapeHtml(activity.status)}">${escapeHtml(status)}</span>
        </div>
        <h4>${escapeHtml(activity.title)}</h4>
        <p>${escapeHtml(activity.description)}</p>
        <div class="activity-meta">
          <span>${escapeHtml(dateRange(activity.startsAt, activity.endsAt))}</span>
          <span>${escapeHtml(activity.location)}</span>
          <span>${escapeHtml(activity.organizerName)} · ${escapeHtml(activity.capacity)} 人</span>
        </div>
        ${rejection}
        ${registrationAction}
      </div>
    </article>
  `;
}

function activityRegistrationMarkup(activity, registration) {
  if (registration && ["registered", "waitlisted"].includes(registration.status)) {
    const label = registration.status === "registered" ? "已报名" : `候补第 ${registration.queuePosition} 位`;
    return `<div class="activity-registration activity-registration--${escapeHtml(registration.status)}">
      <strong>${escapeHtml(label)}</strong>
      <button class="ghost-button" type="button" data-activity-registration="${escapeHtml(activity.id)}" data-action="cancel">取消报名</button>
    </div>`;
  }
  const label = activity.status === "full" ? "加入候补" : "立即报名";
  return `<div class="activity-registration"><span>${activity.status === "full" ? "名额已满，按报名顺序候补" : "名额开放中"}</span>
    <button class="primary-button" type="button" data-activity-registration="${escapeHtml(activity.id)}" data-action="register">${label}</button>
  </div>`;
}

function activityReviewCard(activity) {
  return `
    <article class="activity-review-card">
      <div class="activity-review-main">
        <div class="activity-card-topline">
          <span>${escapeHtml(activity.category)}</span>
          <span class="activity-status activity-status--pending">待审核</span>
        </div>
        <h4>${escapeHtml(activity.title)}</h4>
        <p>${escapeHtml(activity.description)}</p>
        <div class="activity-meta">
          <span>${escapeHtml(dateRange(activity.startsAt, activity.endsAt))}</span>
          <span>${escapeHtml(activity.location)}</span>
          <span>组织者：${escapeHtml(activity.organizerName)}</span>
          <span>名额：${escapeHtml(activity.capacity)}</span>
        </div>
      </div>
      <div class="activity-review-actions">
        <label for="activityReason-${escapeHtml(activity.id)}">
          拒绝原因
          <input
            id="activityReason-${escapeHtml(activity.id)}"
            data-activity-rejection-reason="${escapeHtml(activity.id)}"
            maxlength="500"
            placeholder="拒绝时必须填写"
          />
        </label>
        <div>
          <button class="small-button" type="button" data-review-activity="${escapeHtml(activity.id)}" data-decision="approve">同意发布</button>
          <button class="small-button danger-action" type="button" data-review-activity="${escapeHtml(activity.id)}" data-decision="reject">拒绝</button>
        </div>
      </div>
    </article>
  `;
}

function activityReviewNoticeMarkup() {
  if (!state.activityReviewNotice) return "";
  return `
    <div class="admin-feedback admin-feedback--${state.activityReviewNotice.kind}" role="status">
      ${escapeHtml(state.activityReviewNotice.message)}
    </div>
  `;
}

function datePart(value, part) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "--";
  if (part === "day") return String(date.getDate()).padStart(2, "0");
  return `${date.getMonth() + 1}月`;
}

function dateRange(startValue, endValue) {
  const start = new Date(startValue);
  const end = new Date(endValue);
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) return "时间待定";
  const date = start.toLocaleDateString("zh-CN", { month: "long", day: "numeric" });
  const startTime = start.toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit", hour12: false });
  const endTime = end.toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit", hour12: false });
  return `${date} ${startTime}–${endTime}`;
}
