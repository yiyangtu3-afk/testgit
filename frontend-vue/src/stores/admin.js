import { computed, ref } from "vue";
import { defineStore } from "pinia";
import { createApi } from "../services/api";
import { useSessionStore } from "./session";

export function createAdminStore({ api, getUser = () => null, setMode = () => {} } = {}) {
  return () => {
    const metrics = ref({}), activities = ref([]), moderation = ref([]), audits = ref([]), notice = ref(""), loading = ref(false), report = ref(null), selectedModeration = ref([]), selectedAudits = ref([]), moderationAssistance = ref({}), assistanceLoadingId = ref("");
    const isAdmin = computed(() => (getUser()?.role || "").includes("管理员"));
    async function run(call) { const result = await call(); setMode(result.mode); return result.data; }
    function clear() { metrics.value = {}; activities.value = []; moderation.value = []; audits.value = []; selectedModeration.value = []; selectedAudits.value = []; moderationAssistance.value = {}; assistanceLoadingId.value = ""; report.value = null; }
    async function load() { clear(); notice.value = ""; if (!isAdmin.value) return; loading.value = true; try { const [base, activityMetrics, pendingActivities, moderationItems, auditEvents] = await Promise.all([run(() => api.metrics()), run(() => api.activityMetrics()), run(() => api.pendingActivities()), run(() => api.moderationItems(true)), run(() => api.auditEvents())]); metrics.value = { ...base, "活动报名": String(activityMetrics.registrationCount), "活动签到": String(activityMetrics.checkedInCount) }; activities.value = pendingActivities; moderation.value = moderationItems; audits.value = auditEvents; } catch (error) { notice.value = error.message || "管理员数据暂时无法加载。"; } finally { loading.value = false; } }
    function toggle(list, id) { return list.includes(id) ? list.filter((item) => item !== id) : [...list, id]; }
    function toggleAll(list, items) { const ids = items.map((item) => item.id); if (!ids.length) return []; return ids.every((id) => list.includes(id)) ? [] : ids; }
    async function reviewActivity(id, decision, reason = "") { try { await run(() => api.reviewActivity(id, decision, reason)); notice.value = "活动审核已完成。"; await load(); } catch (error) { notice.value = error.message || "活动审核失败。"; } }
    async function resolveModeration(id, decision, comment = "") { try { await run(() => api.resolveModeration(id, decision, comment)); notice.value = "内容审核已完成。"; await load(); } catch (error) { notice.value = error.message || "内容审核失败。"; } }
    async function loadModerationAssistance(id) { assistanceLoadingId.value = id; try { moderationAssistance.value = { ...moderationAssistance.value, [id]: await run(() => api.moderationAssistance(id)) }; notice.value = "审核辅助建议已生成，仍需管理员人工决定。"; } catch (error) { notice.value = error.message || "审核辅助建议生成失败。"; } finally { assistanceLoadingId.value = ""; } }
    async function deleteModeration() { if (!selectedModeration.value.length) return; try { const result = await run(() => api.deleteModerationItems(selectedModeration.value)); notice.value = `已删除 ${result.deleted} 条审核记录。`; await load(); } catch (error) { notice.value = error.message || "删除审核记录失败。"; } }
    async function deleteAudits() { if (!selectedAudits.value.length) return; try { const result = await run(() => api.deleteAuditEvents(selectedAudits.value)); notice.value = `已删除 ${result.deleted} 条审计记录。`; await load(); } catch (error) { notice.value = error.message || "删除审计记录失败。"; } }
    async function generateReport(range) { try { report.value = await run(() => api.adminReport(range)); notice.value = "报表已生成。"; } catch (error) { notice.value = error.message || "报表生成失败。"; } }
    return { metrics, activities, moderation, audits, notice, loading, report, selectedModeration, selectedAudits, moderationAssistance, assistanceLoadingId, isAdmin, load, toggle, toggleAll, reviewActivity, resolveModeration, loadModerationAssistance, deleteModeration, deleteAudits, generateReport };
  };
}
export const useAdminStore = defineStore("admin", () => { const session = useSessionStore(); const api = createApi({ getToken: () => session.token, getUser: () => session.user }); return createAdminStore({ api, getUser: () => session.user, setMode: (mode) => { session.mode = mode; } })(); });
