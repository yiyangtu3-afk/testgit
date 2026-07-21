function clone(value) { return JSON.parse(JSON.stringify(value)); }
export function createMockAdminApi(getUser) {
  const metrics = { "注册用户": "4", "今日消息": "12", "动态总数": "8", "待审内容": "2" };
  const activities = [{ id: "a-review-1", title: "夏日校园摄影展", category: "艺术", organizerName: "陈老师", location: "图书馆展厅", startsAt: "2026-08-20T09:00:00", capacity: 60, status: "pending", reviewDecision: "pending", description: "面向全校征集校园主题摄影作品。" }];
  let moderation = [{ id: "m-1", type: "post", targetId: 8, title: "社团招新分享", author: "周同学", body: "分享本周社团招新的活动时间和地点。", status: "pending", reason: "内容待审核", submittedAt: "2026-07-17 10:20" }, { id: "m-2", type: "comment", targetId: 12, postId: 3, title: "评论待审核", author: "林一", body: "报名信息已同步到班群。", status: "pending", reason: "内容待审核", submittedAt: "2026-07-17 09:40" }];
  let audits = [{ id: "audit-1", time: "2026-07-17 10:00", module: "审核", event: "教务管理员通过了一条动态" }, { id: "audit-2", time: "2026-07-17 09:10", module: "活动", event: "陈老师提交活动审核" }];
  const requireAdmin = () => { if (!(getUser()?.role || "").includes("管理员")) throw new Error("需要管理员账号"); };
  const addAudit = (event) => audits = [{ id: `audit-${Date.now()}`, time: "刚刚", module: "后台", event }, ...audits];
  const moderationAssistance = (item) => {
    const body = item.body || "";
    const signals = [];
    if (/1[3-9]\d{9}/.test(body)) signals.push("包含疑似手机号");
    if (/代写|刷单|赌博|色情|诈骗|违法交易/.test(body)) signals.push("命中高风险词");
    if (/加微信|加vx|扫码|进群/.test(body)) signals.push("包含引流提示");
    if (signals.some((signal) => signal.includes("高风险"))) return { suggestedDecision: "reject", riskLevel: "high", signals, suggestedComment: "建议拒绝：内容出现高风险信号，请管理员核实上下文后填写审核意见。", provider: "local-policy-v1" };
    if (signals.length) return { suggestedDecision: "manual_review", riskLevel: "medium", signals, suggestedComment: "建议人工复核：请确认联系方式或引流信息是否符合校园内容规范。", provider: "local-policy-v1" };
    return { suggestedDecision: "approve", riskLevel: "low", signals: ["未检测到预设高风险信号"], suggestedComment: "建议通过：仍请管理员结合内容语境作出最终决定。", provider: "local-policy-v1" };
  };
  return {
    async metrics() { requireAdmin(); return { ...metrics, "待审内容": String(moderation.filter((item) => item.status === "pending").length) }; },
    async activityMetrics() { requireAdmin(); return { registrationCount: 5, checkedInCount: 1 }; },
    async pendingActivities() { requireAdmin(); return clone(activities.filter((item) => item.reviewDecision === "pending")); },
    async reviewActivity(id, decision, reason) { requireAdmin(); const item = activities.find((entry) => entry.id === id); if (!item) throw new Error("活动已完成审核"); if (decision === "reject" && !String(reason).trim()) throw new Error("拒绝活动时必须填写原因"); item.reviewDecision = decision === "approve" ? "approved" : "rejected"; item.status = decision === "approve" ? "published" : "draft"; item.reviewReason = reason || null; addAudit(`完成活动“${item.title}”审核`); return clone(item); },
    async moderationItems(includeResolved = false) { requireAdmin(); return clone(includeResolved ? moderation : moderation.filter((item) => item.status === "pending")); },
    async moderationAssistance(id) { requireAdmin(); const item = moderation.find((entry) => entry.id === id); if (!item) throw new Error("审核记录不存在"); if (item.status !== "pending") throw new Error("已完成审核的内容无需生成辅助建议"); return moderationAssistance(item); },
    async resolveModeration(id, decision, comment = "") { requireAdmin(); const item = moderation.find((entry) => entry.id === id); if (!item) throw new Error("审核记录不存在"); if (item.status !== "pending") throw new Error("内容已完成审核"); if (decision === "reject" && !String(comment).trim()) throw new Error("拒绝内容时必须填写审核意见"); item.status = decision === "approve" ? "approved" : "rejected"; item.reviewerName = "教务管理员"; item.reviewedAt = "刚刚"; item.reviewComment = String(comment).trim() || null; addAudit(`教务管理员${decision === "approve" ? "通过" : "拒绝"}${item.type === "post" ? "动态" : "评论"}；审核意见：${item.reviewComment || "未填写"}`); return clone(item); },
    async deleteModerationItems(ids) { requireAdmin(); const before = moderation.length; moderation = moderation.filter((item) => !ids.includes(item.id)); const deleted = before - moderation.length; if (deleted) addAudit(`删除${deleted}条审核记录`); return { deleted }; },
    async auditEvents() { requireAdmin(); return clone(audits); },
    async deleteAuditEvents(ids) { requireAdmin(); const before = audits.length; audits = audits.filter((item) => !ids.includes(item.id)); return { deleted: before - audits.length }; },
    async adminReport(range) { requireAdmin(); return { generatedAt: "2026-07-17 10:30", fileName: `campuslink-admin-report-${range}.csv`, range: { key: range, label: ({ today: "今日", week: "本周", all: "全部" })[range] || "今日" }, metrics: await this.metrics(), moderation: await this.moderationItems(), auditEvents: await this.auditEvents() }; }
  };
}
