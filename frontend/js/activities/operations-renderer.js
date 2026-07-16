import { state } from "../state.js";
import { isActivityOrganizer } from "../utils/auth.js?v=20260715-notification-actions-v1";
import { $ } from "../utils/dom.js?v=20260715-notification-actions-v1";
import { escapeHtml } from "../utils/format.js?v=20260715-notification-actions-v1";

const activityStatusLabels = {
  draft: "需修改",
  pending: "待审核",
  published: "已发布",
  full: "已满员",
  closed: "已结束",
  cancelled: "已取消"
};

const registrationStatusLabels = {
  registered: "待签到",
  waitlisted: "候补",
  checked_in: "已签到"
};

export function renderActivityOperations() {
  const panel = $("#activitySubmissionPanel");
  if (!isActivityOrganizer()) {
    panel.hidden = true;
    $("#activitySubmissionList").innerHTML = "";
    $("#activityRosterPanel").hidden = true;
    return;
  }
  panel.hidden = false;
  $("#managedActivityCount").textContent = String(state.managedActivities.length);
  renderOperationsNotice();
  $("#activitySubmissionList").innerHTML = state.managedActivities.length
    ? state.managedActivities.map(managedActivityCard).join("")
    : `<div class="activity-empty"><strong>还没有提交活动</strong><p>创建活动后，可在这里跟进审核并管理报名名单。</p></div>`;
  renderRoster();
}

function managedActivityCard(activity) {
  const expanded = state.expandedActivityRosterId === activity.id;
  const rejection = activity.reviewDecision === "rejected"
    ? `<p class="activity-rejection"><strong>拒绝原因：</strong>${escapeHtml(activity.reviewReason || "未填写")}</p>`
    : "";
  return `
    <article class="activity-operation-card ${expanded ? "is-open" : ""}${state.notificationActivityFocusId === activity.id ? " is-notification-target" : ""}" data-activity-id="${escapeHtml(activity.id)}">
      <div class="activity-operation-copy">
        <div class="activity-card-topline">
          <span>${escapeHtml(activity.category)}</span>
          <span class="activity-status activity-status--${escapeHtml(activity.status)}">
            ${escapeHtml(activityStatusLabels[activity.status] || activity.status)}
          </span>
        </div>
        <h4>${escapeHtml(activity.title)}</h4>
        <p>${escapeHtml(formatActivityTime(activity.startsAt))} · ${escapeHtml(activity.location)}</p>
        ${rejection}
      </div>
      <div class="activity-operation-actions">
        <span>${escapeHtml(activity.capacity)} 人上限</span>
        <button class="ghost-button" type="button" data-open-activity-roster="${escapeHtml(activity.id)}">
          ${expanded ? "收起名单" : "查看报名名单"}
        </button>
      </div>
    </article>`;
}

function renderRoster() {
  const panel = $("#activityRosterPanel");
  const activityId = state.expandedActivityRosterId;
  if (!activityId) {
    panel.hidden = true;
    panel.innerHTML = "";
    return;
  }
  panel.hidden = false;
  const roster = state.activityRosters[activityId];
  if (!roster) {
    panel.innerHTML = `<div class="activity-roster-loading"><span></span><p>正在读取报名名单…</p></div>`;
    return;
  }
  panel.innerHTML = `
    <div class="activity-roster-head">
      <div>
        <p class="section-kicker">Attendance Ledger</p>
        <h4>${escapeHtml(roster.title)}</h4>
      </div>
      <button class="primary-button" type="button" data-export-activity-roster="${escapeHtml(activityId)}">
        导出 CSV
      </button>
    </div>
    <div class="activity-roster-metrics">
      <span><strong>${escapeHtml(roster.registeredCount)}</strong> 待签到</span>
      <span><strong>${escapeHtml(roster.checkedInCount)}</strong> 已签到</span>
      <span><strong>${escapeHtml(roster.waitlistedCount)}</strong> 候补</span>
      <span><strong>${escapeHtml(roster.capacity)}</strong> 活动上限</span>
    </div>
    <div class="activity-roster-table">
      <div class="activity-roster-row activity-roster-row--head">
        <span>参与者</span><span>报名状态</span><span>时间记录</span><span>现场操作</span>
      </div>
      ${roster.entries.length ? roster.entries.map(rosterRow).join("")
        : `<div class="activity-empty"><strong>暂无报名记录</strong><p>学生报名后会实时出现在这里。</p></div>`}
    </div>`;
}

function rosterRow(entry) {
  const time = entry.checkedInAt || entry.registeredAt || entry.waitlistedAt;
  const statusDetail = entry.status === "waitlisted" ? `第 ${entry.queuePosition} 位` : "";
  return `
    <div class="activity-roster-row">
      <span><strong>${escapeHtml(entry.attendeeName)}</strong><small>${escapeHtml(entry.attendeeId)}</small></span>
      <span><em class="activity-roster-status activity-roster-status--${escapeHtml(entry.status)}">
        ${escapeHtml(registrationStatusLabels[entry.status] || entry.status)}
      </em>${statusDetail ? `<small>${escapeHtml(statusDetail)}</small>` : ""}</span>
      <span>${escapeHtml(formatRosterTime(time))}</span>
      <span>${entry.status === "registered"
        ? `<button class="small-button" type="button" data-check-in-registration="${escapeHtml(entry.registrationId)}">确认签到</button>`
        : `<small>${entry.status === "checked_in" ? "签到完成" : "等待空余名额"}</small>`}</span>
    </div>`;
}

function renderOperationsNotice() {
  const notice = $("#activityOperationsNotice");
  notice.hidden = !state.activityOperationsNotice;
  notice.className = state.activityOperationsNotice
    ? `activity-feedback activity-feedback--${state.activityOperationsNotice.kind}`
    : "activity-feedback";
  notice.textContent = state.activityOperationsNotice?.message || "";
}

function formatActivityTime(value) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "时间待定";
  return date.toLocaleString("zh-CN", {
    month: "long", day: "numeric", hour: "2-digit", minute: "2-digit", hour12: false
  });
}

function formatRosterTime(value) {
  if (!value) return "--";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "--";
  return date.toLocaleString("zh-CN", {
    month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit", hour12: false
  });
}
