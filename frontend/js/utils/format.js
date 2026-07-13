export function validPhone(phone) {
  return /^1\d{10}$/.test(phone);
}

export function escapeHtml(value) {
  return String(value ?? "").replace(/[&<>"']/g, (character) => {
    return {
      "&": "&amp;",
      "<": "&lt;",
      ">": "&gt;",
      "\"": "&quot;",
      "'": "&#39;"
    }[character];
  });
}

export function normalizePost(post) {
  return {
    ...post,
    comments: Array.isArray(post.comments) ? post.comments.length : post.comments
  };
}

export function formatFileSize(bytes) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

export function fileKind(type, name) {
  if (type.startsWith("image/")) return "图片";
  if (type.includes("pdf") || name.toLowerCase().endsWith(".pdf")) return "PDF";
  if (type.includes("zip") || name.toLowerCase().endsWith(".zip")) return "压缩包";
  return "文件";
}

export function filesToAttachments(files) {
  return Array.from(files).map((file) => ({
    id: `att-${Date.now()}-${Math.random().toString(16).slice(2)}`,
    name: file.name,
    size: file.size,
    type: file.type || "application/octet-stream",
    kind: fileKind(file.type || "", file.name)
  }));
}

export function csvCell(value) {
  const text = String(value ?? "");
  return `"${text.replaceAll("\"", "\"\"")}"`;
}

export function reportToCsv(report) {
  const rows = [
    ["section", "label", "value", "status", "time"],
    ["generated", "生成时间", report.generatedAt, "", ""],
    ["range", "报表范围", report.range?.label || "今日", report.range?.key || "today", ""],
    ...Object.entries(report.metrics || {}).map(([label, value]) => {
      return ["metric", label, value, "", ""];
    }),
    ...(report.moderation || []).map((item) => {
      return [
        "moderation",
        item.title || `${item.type === "post" ? "动态" : "评论"} / ${item.author}`,
        item.body,
        item.status,
        item.submittedAt || item.time
      ];
    }),
    ...(report.auditEvents || []).map((item) => {
      return ["audit", item.module, item.event, "", item.time];
    })
  ];
  return rows.map((row) => row.map(csvCell).join(",")).join("\n");
}

export function activityRosterToCsv(roster) {
  const statusLabels = {
    registered: "已报名",
    waitlisted: "候补",
    checked_in: "已签到"
  };
  const rows = [
    ["活动", "报名ID", "用户ID", "姓名", "状态", "候补顺序", "报名时间", "签到时间"],
    ...(roster.entries || []).map((entry) => [
      roster.title,
      entry.registrationId,
      entry.attendeeId,
      entry.attendeeName,
      statusLabels[entry.status] || entry.status,
      entry.queuePosition || "",
      entry.registeredAt || entry.waitlistedAt || "",
      entry.checkedInAt || ""
    ])
  ];
  return rows.map((row) => row.map(csvCell).join(",")).join("\n");
}

export function exportSummary(report) {
  const metricCount = Object.keys(report.metrics || {}).length;
  const moderationCount = (report.moderation || []).length;
  const auditCount = (report.auditEvents || []).length;
  return {
    metricCount,
    moderationCount,
    auditCount,
    rowCount: 3 + metricCount + moderationCount + auditCount
  };
}

export function reportPreviewRows(report) {
  const metrics = Object.entries(report.metrics || {}).slice(0, 3).map(([label, value]) => ({
    label,
    value,
    meta: "核心指标"
  }));
  const moderation = (report.moderation || []).slice(0, 2).map((item) => ({
    label: item.type === "post" ? "待审动态" : "待审评论",
    value: item.author,
    meta: item.title || item.body
  }));
  const audit = (report.auditEvents || []).slice(0, 3).map((item) => ({
    label: item.module,
    value: item.time,
    meta: item.event
  }));
  return [...metrics, ...moderation, ...audit];
}
