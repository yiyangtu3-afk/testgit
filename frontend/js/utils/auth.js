import { state } from "../state.js";

export function isAdminUser(user = state.currentUser) {
  return (user.role || "").includes("管理员");
}
