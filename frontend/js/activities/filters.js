import { state } from "../state.js";

export function activityFilterState() {
  if (!state.activityFilters || typeof state.activityFilters !== "object") {
    state.activityFilters = { from: "", to: "", category: "" };
  }
  if (!Array.isArray(state.activityCategories)) {
    state.activityCategories = [];
  }
  return {
    filters: state.activityFilters,
    categories: state.activityCategories
  };
}

export function rememberActivityCategories(activities) {
  const { categories } = activityFilterState();
  state.activityCategories = [...new Set([
    ...categories,
    ...activities.map((activity) => activity.category).filter(Boolean)
  ])].sort((left, right) => left.localeCompare(right, "zh-CN"));
}
