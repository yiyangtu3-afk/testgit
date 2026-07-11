import { state } from "../state.js";
import { $ } from "../utils/dom.js?v=20260710-conversation-previews-v1";

export function renderIdentity() {
  $("#currentUser").textContent = state.currentUser.name;
  $("#currentRole").textContent = state.currentUser.role;
  const hidden = state.currentUser.presence === "invisible";
  $("#presenceBadge").textContent = hidden ? "隐身" : "在线";
  $("#presenceBadge").classList.toggle("is-online", !hidden);
}

export function switchTab(tab) {
  ["chat", "feed", "admin"].forEach((name) => {
    $(`#${name}Panel`).hidden = name !== tab;
    document.querySelector(`[data-tab="${name}"]`).classList.toggle("is-active", name === tab);
  });
}
