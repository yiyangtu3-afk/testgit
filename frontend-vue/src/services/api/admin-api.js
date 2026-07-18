import { withApiFallback } from "./auth-api";

export function createAdminApi({ http, mockAdmin }) {
  const call = (path, options, fallback) => withApiFallback(() => http.request(path, options), fallback);
  return {
    metrics: () => call("/api/admin/metrics", {}, () => mockAdmin.metrics()),
    activityMetrics: () => call("/api/admin/activity-metrics", {}, () => mockAdmin.activityMetrics()),
    pendingActivities: () => call("/api/admin/activities/pending", {}, () => mockAdmin.pendingActivities()),
    reviewActivity: (id, decision, reason = "") => call(`/api/admin/activities/${id}/reviews`, { method: "POST", body: JSON.stringify({ decision, reason: reason || null }) }, () => mockAdmin.reviewActivity(id, decision, reason)),
    moderationItems: () => call("/api/admin/moderation", {}, () => mockAdmin.moderationItems()),
    resolveModeration: (id, decision) => call(`/api/admin/moderation/${id}/${decision}`, { method: "POST" }, () => mockAdmin.resolveModeration(id, decision)),
    deleteModerationItems: (itemIds) => call("/api/admin/moderation", { method: "DELETE", body: JSON.stringify({ itemIds }) }, () => mockAdmin.deleteModerationItems(itemIds)),
    auditEvents: () => call("/api/admin/audit-events", {}, () => mockAdmin.auditEvents()),
    deleteAuditEvents: (eventIds) => call("/api/admin/audit-events", { method: "DELETE", body: JSON.stringify({ eventIds }) }, () => mockAdmin.deleteAuditEvents(eventIds)),
    adminReport: (range) => call(`/api/admin/report?range=${encodeURIComponent(range)}`, {}, () => mockAdmin.adminReport(range))
  };
}
