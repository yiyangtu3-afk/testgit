import { api } from "../api/client.js?v=20260715-friend-request-actions-v1";
import { API_BASE, state } from "../state.js";
import { isAdminUser } from "../utils/auth.js?v=20260715-friend-request-actions-v1";
import { loadAdminData } from "../loaders.js?v=20260715-friend-request-actions-v1";

export async function bindAdminAuditEvents(event) {
  const toggleAll = event.target.closest("[data-toggle-all-audit-events]");
  if (toggleAll) {
    const eventIds = state.auditEvents.map((item) => item.id);
    const allSelected = eventIds.length > 0 && eventIds.every((eventId) => state.selectedAuditEventIds.has(eventId));
    state.selectedAuditEventIds = allSelected ? new Set() : new Set(eventIds);
    await loadAdminData();
    return true;
  }

  const selectEvent = event.target.closest("[data-select-audit-event]");
  if (selectEvent) {
    const next = new Set(state.selectedAuditEventIds);
    if (selectEvent.checked) {
      next.add(selectEvent.dataset.selectAuditEvent);
    } else {
      next.delete(selectEvent.dataset.selectAuditEvent);
    }
    state.selectedAuditEventIds = next;
    await loadAdminData();
    return true;
  }

  const deleteEvent = event.target.closest("[data-delete-audit-event]");
  if (deleteEvent) {
    await deleteAuditEvents([deleteEvent.dataset.deleteAuditEvent]);
    return true;
  }

  const deleteSelectedEvents = event.target.closest("[data-delete-selected-audit-events]");
  if (deleteSelectedEvents) {
    await deleteAuditEvents([...state.selectedAuditEventIds]);
    return true;
  }

  return false;
}

async function deleteAuditEvents(eventIds) {
  if (!isAdminUser() || eventIds.length === 0) return;
  if (!confirmAuditDeletion(eventIds.length)) return;
  try {
    const response = await requestAuditEventDeletion(eventIds);
    eventIds.forEach((eventId) => state.selectedAuditEventIds.delete(eventId));
    state.adminNotice = {
      kind: "success",
      message: `已删除 ${response.deleted || eventIds.length} 条审计记录。`
    };
    await loadAdminData();
  } catch (error) {
    console.error(error);
    state.adminNotice = {
      kind: "error",
      message: "审计记录删除失败，请刷新页面后重试。"
    };
    await loadAdminData();
  }
}

function confirmAuditDeletion(count) {
  const message = count === 1 ? "确认删除这条审计记录？" : `确认删除已选择的 ${count} 条审计记录？`;
  return typeof window.confirm !== "function" || window.confirm(message);
}

async function requestAuditEventDeletion(eventIds) {
  if (typeof api.deleteAuditEvents === "function") {
    return api.deleteAuditEvents(eventIds);
  }

  const response = await fetch(`${API_BASE}/admin/audit-events`, {
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
      ...(state.token ? { Authorization: `Bearer ${state.token}` } : {})
    },
    body: JSON.stringify({ eventIds })
  });
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }
  return response.json();
}
