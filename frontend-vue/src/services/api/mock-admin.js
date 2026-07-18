function clone(value) { return JSON.parse(JSON.stringify(value)); }
export function createMockAdminApi(getUser) {
  const metrics = { "注册用户": "4", "今日消息": "12", "动态总数": "8", "待审内容": "2" };
  const activities = [{ id: "a-review-1", title: "夏日校园摄影展", category: "艺术", organizerName: "陈老师", location: "图书馆展厅", startsAt: "2026-08-20T09:00:00", capacity: 60, status: "pending", reviewDecision: "pending", description: "面向全校征集校园主题摄影作品。" }];
  let moderation = [{ id: "m-1", type: "post", targetId: 8, title: "社团招新分享", author: "周同学", body: "分享本周社团招新的活动时间和地点。", status: "pending", reason: "内容待审核", submittedAt: "2026-07-17 10:20" }, { id: "m-2", type: "comment", targetId: 12, postId: 3, title: "评论待审核", author: "林一", body: "报名信息已同步到班群。", status: "pending", reason: "内容待审核", submittedAt: "2026-07-17 09:40" }];
  let audits = [{ id: "audit-1", time: "2026-07-17 10:00", module: "审核", event: "教务管理员通过了一条动态" }, { id: "audit-2", time: "2026-07-17 09:10", module: "活动", event: "陈老师提交活动审核" }];
  const requireAdmin = () => { if (!(getUser()?.role || "").includes("管理员")) throw new Error("需要管理员账号"); };
  const addAudit = (event) => audits = [{ id: `audit-${Date.now()}`, time: "刚刚", module: "后台", event }, ...audits];
  return {
    async metrics() { requireAdmin(); return { ...metrics, "待审内容": String(moderation.filter((item) => item.status === "pending").length) }; },
    async activityMetrics() { requireAdmin(); return { registrationCount: 5, checkedInCount: 1 }; },
    async pendingActivities() { requireAdmin(); return clone(activities.filter((item) => item.reviewDecision === "pending")); },
    async reviewActivity(id, decision, reason) { requireAdmin(); const item = activities.find((entry) => entry.id === id); if (!item) throw new Error("活动已完成审核"); if (decision === "reject" && !String(reason).trim()) throw new Error("拒绝活动时必须填写原因"); item.reviewDecision = decision === "approve" ? "approved" : "rejected"; item.status = decision === "approve" ? "published" : "draft"; item.reviewReason = reason || null; addAudit(`完成活动“${item.title}”审核`); return clone(item); },
    async moderationItems() { requireAdmin(); return clone(moderation.filter((item) => item.status === "pending")); },
    async resolveModeration(id, decision) { requireAdmin(); const item = moderation.find((entry) => entry.id === id); if (!item) throw new Error("审核记录不存在"); item.status = decision === "approve" ? "approved" : "rejected"; addAudit(`完成${item.type === "post" ? "动态" : "评论"}审核`); return clone(item); },
    async deleteModerationItems(ids) { requireAdmin(); const before = moderation.length; moderation = moderation.filter((item) => !ids.includes(item.id)); const deleted = before - moderation.length; if (deleted) addAudit(`删除${deleted}条审核记录`); return { deleted }; },
    async auditEvents() { requireAdmin(); return clone(audits); },
    async deleteAuditEvents(ids) { requireAdmin(); const before = audits.length; audits = audits.filter((item) => !ids.includes(item.id)); return { deleted: before - audits.length }; },
    async adminReport(range) { requireAdmin(); return { generatedAt: "2026-07-17 10:30", fileName: `campuslink-admin-report-${range}.csv`, range: { key: range, label: ({ today: "今日", week: "本周", all: "全部" })[range] || "今日" }, metrics: await this.metrics(), moderation: await this.moderationItems(), auditEvents: await this.auditEvents() }; }
  };
}
