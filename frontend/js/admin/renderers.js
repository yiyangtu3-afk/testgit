import { reportRanges, state } from "../state.js";
import { $ } from "../utils/dom.js";
import { exportSummary, reportPreviewRows } from "../utils/format.js?v=20260708-user-moderation-scroll-v2";

export function renderMetrics() {
  $("#metricGrid").innerHTML = Object.entries(state.metrics)
    .map(([label, value]) => `<article class="metric-card"><p>${label}</p><strong>${value}</strong></article>`)
    .join("");
}

export function renderAuditEvents() {
  const selectedCount = state.auditEvents.filter((item) => state.selectedAuditEventIds.has(item.id)).length;
  const allSelected = state.auditEvents.length > 0 && selectedCount === state.auditEvents.length;
  const rows = state.auditEvents
    .map(
      (item) => `
        <div class="table-row">
          <label class="audit-check" aria-label="选择审计记录">
            <input type="checkbox" data-select-audit-event="${item.id}" ${state.selectedAuditEventIds.has(item.id) ? "checked" : ""} />
          </label>
          <span>${item.time}</span>
          <span>${item.module}</span>
          <span>${item.event}</span>
          <button class="small-button" data-delete-audit-event="${item.id}" type="button">删除</button>
        </div>
      `
    )
    .join("");
  $("#auditTable").innerHTML = `
    ${moderationWorkbenchMarkup()}
    <section class="audit-log-panel">
      <div class="admin-section-title">
        <div>
          <p class="section-kicker">Audit Log</p>
          <h3>审计记录</h3>
        </div>
        <strong>${state.auditEvents.length}</strong>
      </div>
      <div class="audit-toolbar">
        <button class="text-action" data-toggle-all-audit-events type="button" ${state.auditEvents.length === 0 ? "disabled" : ""}>
          ${allSelected ? "取消全选" : "全选"}
        </button>
        <button class="text-action danger-action" data-delete-selected-audit-events type="button" ${selectedCount === 0 ? "disabled" : ""}>
          删除所选
        </button>
        <span>${selectedCount} 条已选择</span>
      </div>
      <div class="table-row table-head">
        <span></span>
        <span>时间</span>
        <span>模块</span>
        <span>事件</span>
        <span>操作</span>
      </div>
      ${rows || `<p class="empty-copy">暂无审计记录。</p>`}
    </section>
  `;
}

export function renderExportPanel() {
  const panel = $("#exportPanel");
  if (!state.reportExport) {
    panel.hidden = true;
    panel.innerHTML = "";
    return;
  }
  if (state.reportExport.status === "loading") {
    panel.hidden = false;
    panel.innerHTML = `
      <div>
        <p class="section-kicker">Preparing Report</p>
        <h3>正在生成后台报表</h3>
        <p>正在整理指标、待审内容和最近审计记录。</p>
      </div>
      <span class="export-state">生成中</span>
    `;
    return;
  }
  if (state.reportExport.status === "error") {
    panel.hidden = false;
    panel.innerHTML = `
      <div>
        <p class="section-kicker">Export Failed</p>
        <h3>报表生成失败</h3>
        <p>${state.reportExport.message}</p>
      </div>
      <span class="export-state">失败</span>
    `;
    return;
  }
  const { report, url } = state.reportExport;
  const summary = exportSummary(report);
  const rangeLabel = report.range?.label || reportRanges[state.reportRange].label;
  const previewRows = reportPreviewRows(report)
    .map((row) => `
      <div class="report-preview-row">
        <span>${row.label}</span>
        <strong>${row.value}</strong>
        <small>${row.meta}</small>
      </div>
    `)
    .join("");
  panel.hidden = false;
  panel.innerHTML = `
    <div class="report-card">
      <div class="report-card-head">
        <div>
          <p class="section-kicker">Report Ready</p>
          <h3>后台运营报表</h3>
          <p>${report.generatedAt} 生成，范围：${rangeLabel}，已整理为可下载 CSV。</p>
        </div>
        <span class="export-state">已生成</span>
      </div>
      <div class="report-chip-grid">
        <span><strong>${rangeLabel}</strong> 报表范围</span>
        <span><strong>${summary.metricCount}</strong> 项指标</span>
        <span><strong>${summary.moderationCount}</strong> 条待审</span>
        <span><strong>${summary.auditCount}</strong> 条审计</span>
      </div>
      <div class="report-preview">
        ${previewRows || `<p class="empty-copy">暂无报表明细。</p>`}
      </div>
    </div>
    <div class="report-actions">
      <a class="download-link" href="${url}" download="${report.fileName}">下载 CSV</a>
      <button class="print-link" data-print-report type="button">打印预览</button>
    </div>
  `;
  panel.scrollIntoView({ behavior: "smooth", block: "nearest" });
}

export function renderModerationItems() {
  $("#moderationPanel").hidden = true;
  $("#moderationPanel").innerHTML = "";
}

function moderationWorkbenchMarkup() {
  const pending = state.moderationItems.filter((item) => item.status === "pending");
  const visiblePending = filteredModerationItems(pending);
  const selectedCount = visiblePending.filter((item) => state.selectedModerationIds.has(String(item.id))).length;
  const allSelected = visiblePending.length > 0 && selectedCount === visiblePending.length;
  const statusLabels = {
    pending: "待审核",
    approved: "已通过",
    rejected: "已拒绝"
  };
  const rows = visiblePending.length
    ? visiblePending
        .map((item) => `
          <article class="review-card ${state.reviewingModerationId === String(item.id) ? "is-open" : ""}">
            <div class="review-card-main">
              <label class="moderation-check" aria-label="选择审核记录">
                <input type="checkbox" data-select-moderation="${item.id}" ${state.selectedModerationIds.has(String(item.id)) ? "checked" : ""} />
              </label>
              <div class="review-content">
                <div class="review-card-head">
                  <span class="moderation-type">${item.type === "post" ? "动态" : "评论"}</span>
                  <span class="moderation-status moderation-status--${item.status}">
                    ${statusLabels[item.status] || item.status}
                  </span>
                </div>
                <h3>${item.title || item.body || "待审核内容"}</h3>
                <p>${item.body || item.reason || "暂无内容摘要"}</p>
                <div class="review-meta">
                  <span>提交人：${item.author || "演示用户"}</span>
                  <span>提交时间：${item.submittedAt || item.time || "--"}</span>
                  <span>原因：${item.reason || "内容待审核"}</span>
                </div>
              </div>
            </div>
            <div class="moderation-actions">
              <button class="small-button" data-moderation="${item.id}" data-decision="approve" type="button">同意</button>
              <button class="small-button" data-moderation="${item.id}" data-decision="reject" type="button">拒绝</button>
              <button class="small-button" data-review-moderation="${item.id}" type="button">
                ${state.reviewingModerationId === String(item.id) ? "收起" : "查看"}
              </button>
            </div>
            ${state.reviewingModerationId === String(item.id) ? moderationDetailMarkup(item, statusLabels[item.status] || item.status) : ""}
          </article>
        `)
        .join("")
    : `<p class="empty-copy">${pending.length === 0 ? "当前没有待审核内容。" : "当前筛选下没有待审核内容。"}</p>`;
  return `
    <section class="review-workbench">
    <div class="admin-section-title">
      <div>
        <p class="section-kicker">Content Review</p>
        <h3>待审核内容</h3>
      </div>
      <strong>${pending.length}</strong>
    </div>
    ${adminNoticeMarkup()}
    <div class="moderation-bulk-actions">
      <button class="text-action" data-toggle-all-moderation type="button" ${visiblePending.length === 0 ? "disabled" : ""}>
        ${allSelected ? "取消全选" : "全选"}
      </button>
      <button class="text-action danger-action" data-delete-selected-moderation type="button" ${selectedCount === 0 ? "disabled" : ""}>
        删除所选
      </button>
      ${moderationFilterButtons(pending)}
      <span>${selectedCount} 条已选择</span>
      <span>${visiblePending.length} / ${pending.length} 条待审核</span>
    </div>
    <div class="review-field-head">
      <span>审核内容</span>
      <span>提交人 / 提交时间 / 内容类型 / 当前状态</span>
      <span>操作</span>
    </div>
    <div class="review-list">
      ${rows}
    </div>
    </section>
  `;
}

function filteredModerationItems(items) {
  if (state.moderationFilter === "post") {
    return items.filter((item) => item.type === "post");
  }
  if (state.moderationFilter === "comment") {
    return items.filter((item) => item.type === "comment");
  }
  return items;
}

function moderationFilterButtons(items) {
  const filters = [
    { key: "all", label: "全部", count: items.length },
    { key: "post", label: "动态", count: items.filter((item) => item.type === "post").length },
    { key: "comment", label: "评论", count: items.filter((item) => item.type === "comment").length }
  ];
  return filters
    .map((filter) => `
      <button
        class="review-filter ${state.moderationFilter === filter.key ? "is-active" : ""}"
        data-moderation-filter="${filter.key}"
        type="button"
      >
        ${filter.label} ${filter.count}
      </button>
    `)
    .join("");
}

function adminNoticeMarkup() {
  if (!state.adminNotice) {
    return "";
  }
  return `
    <div class="admin-feedback admin-feedback--${state.adminNotice.kind}" role="status">
      ${state.adminNotice.message}
    </div>
  `;
}

function moderationDetailMarkup(item, statusLabel) {
  const typeLabel = item.type === "post" ? "动态" : "评论";
  const sourceLabel = item.type === "comment" && item.postId ? `关联动态：${item.postId}` : `内容编号：${item.targetId || item.id}`;
  return `
    <div class="review-detail-panel">
      <div>
        <p class="section-kicker">Review Detail</p>
        <h4>审核详情</h4>
      </div>
      <dl>
        <div>
          <dt>内容类型</dt>
          <dd>${typeLabel}</dd>
        </div>
        <div>
          <dt>当前状态</dt>
          <dd>${statusLabel}</dd>
        </div>
        <div>
          <dt>提交人</dt>
          <dd>${item.author || "演示用户"}</dd>
        </div>
        <div>
          <dt>提交时间</dt>
          <dd>${item.submittedAt || item.time || "--"}</dd>
        </div>
        <div>
          <dt>审核原因</dt>
          <dd>${item.reason || "内容待审核"}</dd>
        </div>
        <div>
          <dt>来源</dt>
          <dd>${sourceLabel}</dd>
        </div>
      </dl>
      <p>${item.body || "暂无内容正文。"}</p>
    </div>
  `;
}

export function renderAdminAccessDenied() {
  $("#exportPanel").hidden = true;
  $("#exportPanel").innerHTML = "";
  $("#metricGrid").innerHTML = `
    <article class="metric-card"><p>后台权限</p><strong>管理员</strong></article>
  `;
  $("#moderationPanel").hidden = true;
  $("#moderationPanel").innerHTML = "";
  $("#auditTable").innerHTML = `
    <section class="review-workbench">
      <div class="admin-section-title">
        <div>
          <p class="section-kicker">Admin Only</p>
          <h3>待审核内容</h3>
        </div>
        <strong>--</strong>
      </div>
      <p class="empty-copy">请切换到教务管理员账号。</p>
    </section>
    <section class="audit-log-panel">
      <div class="table-row table-head"><span></span><span>时间</span><span>模块</span><span>事件</span><span>操作</span></div>
      <div class="table-row"><span></span><span>--</span><span>权限</span><span>请切换到教务管理员账号。</span><span>--</span></div>
    </section>
  `;
}
