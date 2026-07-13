import { api } from "../api/client.js?v=20260712-activity-filters-v1";
import { loadActivities, loadPendingActivities } from "../loaders.js?v=20260712-activity-filters-v1";
import { state } from "../state.js";
import { $ } from "../utils/dom.js?v=20260712-activity-filters-v1";
import { renderActivities, renderPendingActivities } from "./renderers.js?v=20260712-activity-filters-v1";

export function bindActivityEvents() {
  $("#activityForm").addEventListener("submit", submitActivity);
  $("#activityFilterForm").addEventListener("submit", filterActivities);
  $("#clearActivityFilters").addEventListener("click", clearActivityFilters);
  $("#activityList").addEventListener("click", updateRegistration);
  $("#activityReviewPanel").addEventListener("click", reviewActivity);
}

async function filterActivities(event) {
  event.preventDefault();
  const formData = new FormData(event.currentTarget);
  await applyActivityFilters({
    from: String(formData.get("from") || ""),
    to: String(formData.get("to") || ""),
    category: String(formData.get("category") || "").trim()
  });
}

async function clearActivityFilters() {
  $("#activityFilterForm").reset();
  await applyActivityFilters({ from: "", to: "", category: "" });
}

async function applyActivityFilters(filters) {
  if (filters.from && filters.to && filters.to < filters.from) {
    state.activityNotice = { kind: "error", message: "筛选结束日期不能早于开始日期。" };
    renderActivities();
    return;
  }
  state.activityFilters = filters;
  state.activityNotice = null;
  try {
    await loadActivities();
  } catch (error) {
    state.activityNotice = { kind: "error", message: error.message || "活动筛选失败，请稍后重试。" };
    renderActivities();
  }
}

async function updateRegistration(event) {
  const button = event.target.closest("[data-activity-registration]");
  if (!button) return;
  const activityId = button.dataset.activityRegistration;
  const action = button.dataset.action;
  button.disabled = true;
  try {
    const registration = action === "cancel"
      ? await api.cancelActivityRegistration(activityId)
      : await api.registerActivity(activityId);
    state.activityRegistrations[activityId] = registration;
    state.activityNotice = { kind: "success", message: registration.status === "registered"
      ? "报名成功，已为你保留名额。" : registration.status === "waitlisted"
        ? `活动已满，你已进入候补队列（第 ${registration.queuePosition} 位）。`
        : "已取消报名。" };
    await loadActivities();
  } catch (error) {
    state.activityNotice = { kind: "error", message: error.message || "报名操作失败，请稍后重试。" };
    renderActivities();
  } finally {
    button.disabled = false;
  }
}

async function submitActivity(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const formData = new FormData(form);
  const activity = {
    title: String(formData.get("title") || "").trim(),
    description: String(formData.get("description") || "").trim(),
    category: String(formData.get("category") || "").trim(),
    location: String(formData.get("location") || "").trim(),
    startsAt: String(formData.get("startsAt") || ""),
    endsAt: String(formData.get("endsAt") || ""),
    capacity: Number(formData.get("capacity"))
  };
  if (activity.endsAt <= activity.startsAt) {
    state.activityNotice = { kind: "error", message: "活动结束时间必须晚于开始时间。" };
    renderActivities();
    return;
  }

  const button = $("#submitActivityButton");
  button.disabled = true;
  button.textContent = "提交中";
  try {
    const created = await api.createActivity(activity);
    state.activitySubmissions = [created, ...state.activitySubmissions.filter((item) => item.id !== created.id)];
    state.activityNotice = {
      kind: "success",
      message: "活动已提交审核。管理员通过前，它不会出现在公开活动列表。"
    };
    form.reset();
    $("#activityCapacity").value = "40";
    await loadActivities();
  } catch (error) {
    state.activityNotice = { kind: "error", message: error.message || "活动提交失败，请稍后重试。" };
    renderActivities();
  } finally {
    button.disabled = false;
    button.textContent = "提交审核";
  }
}

async function reviewActivity(event) {
  const button = event.target.closest("[data-review-activity]");
  if (!button) return;
  const activityId = button.dataset.reviewActivity;
  const decision = button.dataset.decision;
  const reasonInput = document.querySelector(`[data-activity-rejection-reason="${CSS.escape(activityId)}"]`);
  const reason = reasonInput ? reasonInput.value.trim() : "";
  if (decision === "reject" && !reason) {
    state.activityReviewNotice = { kind: "error", message: "拒绝活动时必须填写原因。" };
    renderPendingActivities();
    return;
  }

  button.disabled = true;
  try {
    const reviewed = await api.reviewActivity(activityId, decision, decision === "reject" ? reason : null);
    state.activitySubmissions = state.activitySubmissions.map((item) => {
      return item.id === reviewed.id ? reviewed : item;
    });
    state.activityReviewNotice = {
      kind: "success",
      message: decision === "approve" ? "活动已发布。" : "活动已拒绝并保留审核原因。"
    };
    await Promise.all([loadPendingActivities(), loadActivities()]);
  } catch (error) {
    state.activityReviewNotice = { kind: "error", message: error.message || "活动审核失败，请稍后重试。" };
    renderPendingActivities();
  } finally {
    button.disabled = false;
  }
}
