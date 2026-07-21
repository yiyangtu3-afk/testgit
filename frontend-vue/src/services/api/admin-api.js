import { withApiFallback } from "./auth-api";

export function createAdminApi({ http, mockAdmin }) {
  const call = (path, options, fallback) => withApiFallback(() => http.request(path, options), fallback);
  return {
    metrics: () => call("/api/admin/metrics", {}, () => mockAdmin.metrics()),
    activityMetrics: () => call("/api/admin/activity-metrics", {}, () => mockAdmin.activityMetrics()),
    pendingActivities: () => call("/api/admin/activities/pending", {}, () => mockAdmin.pendingActivities()),
    reviewActivity: (id, decision, reason = "") => call(`/api/admin/activities/${id}/reviews`, { method: "POST", body: JSON.stringify({ decision, reason: reason || null }) }, () => mockAdmin.reviewActivity(id, decision, reason)),
    moderationItems: (includeResolved = false) => call(`/api/admin/moderation?includeResolved=${includeResolved}`, {}, () => mockAdmin.moderationItems(includeResolved)),
    moderationAssistance: (id) => call(`/api/admin/moderation/${encodeURIComponent(id)}/assistance`, {}, () => mockAdmin.moderationAssistance(id)),
    resolveModeration: (id, decision, comment = "") => call(`/api/admin/moderation/${id}/${decision}`, { method: "POST", body: JSON.stringify({ comment: comment || null }) }, () => mockAdmin.resolveModeration(id, decision, comment)),
    deleteModerationItems: (itemIds) => call("/api/admin/moderation", { method: "DELETE", body: JSON.stringify({ itemIds }) }, () => mockAdmin.deleteModerationItems(itemIds)),
    auditEvents: () => call("/api/admin/audit-events", {}, () => mockAdmin.auditEvents()),
    deleteAuditEvents: (eventIds) => call("/api/admin/audit-events", { method: "DELETE", body: JSON.stringify({ eventIds }) }, () => mockAdmin.deleteAuditEvents(eventIds)),
    adminReport: (range) => call(`/api/admin/report?range=${encodeURIComponent(range)}`, {}, () => mockAdmin.adminReport(range))
  };
}
