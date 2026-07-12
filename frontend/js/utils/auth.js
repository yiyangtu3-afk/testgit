import { state } from "../state.js";

export function isAdminUser(user = state.currentUser) {
  return (user.role || "").includes("管理员");
}

export function isActivityOrganizer(user = state.currentUser) {
  const role = user && user.role || "";
  return role.includes("教师") || role.includes("社团负责人");
}
