import { state } from "../state.js";
import { $ } from "../utils/dom.js?v=20260711-activity-registration-v1";

export function setApiMode(mode) {
  state.apiMode = mode;
  const badge = $("#apiMode");
  if (!badge) return;
  badge.textContent = mode === "live" ? "Java API" : "Mock";
  badge.classList.toggle("is-live", mode === "live");
}

export function setRealtimeMode(mode) {
  state.realtimeMode = mode;
  const badge = $("#realtimeMode");
  if (!badge) return;
  const labels = {
    connecting: "连接中",
    live: "实时",
    offline: "离线"
  };
  badge.textContent = labels[mode] || labels.offline;
  badge.classList.toggle("is-live", mode === "live");
  badge.classList.toggle("is-connecting", mode === "connecting");
}

export function setStatus(message, isError = false) {
  const status = $("#loginStatus");
  status.textContent = message;
  status.classList.toggle("is-error", isError);
}

export function setLoginBusy(isBusy) {
  $("#loginButton").disabled = isBusy;
  $("#sendCodeButton").disabled = isBusy;
  $("#demoLoginButton").disabled = isBusy;
}
