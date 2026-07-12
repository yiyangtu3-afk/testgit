import { api } from "../api/client.js?v=20260711-activity-review-layout-v2";
import { loadActivities, loadPendingActivities } from "../loaders.js?v=20260711-activity-review-layout-v2";
import { state } from "../state.js";
import { $ } from "../utils/dom.js?v=20260711-activity-review-layout-v2";
import { renderActivities, renderPendingActivities } from "./renderers.js?v=20260711-activity-review-layout-v2";

export function bindActivityEvents() {
  $("#activityForm").addEventListener("submit", submitActivity);
  $("#activityReviewPanel").addEventListener("click", reviewActivity);
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
