import { mockStore, state } from "../state.js";
import { $ } from "../utils/dom.js?v=20260710-conversation-previews-v1";
import { escapeHtml } from "../utils/format.js?v=20260710-conversation-previews-v1";

export function renderSearchResults() {
  $("#resultList").innerHTML = state.users
    .map((user) => {
      const userId = escapeHtml(user.id);
      const name = escapeHtml(user.name);
      const role = escapeHtml(user.role);
      const phone = escapeHtml(user.phone);
      const status = state.friendRequests[user.id];
      const friend = state.friends.some((item) => item.id === user.id);
      const disabled = friend || status === "pending" || status === "accepted";
      const label = friend || status === "accepted" ? "已好友" : status === "pending" ? "已申请" : "申请";
      return `
        <article class="person-row">
          <span class="avatar">${escapeHtml((user.name || "?").slice(0, 1))}</span>
          <div>
            <h3>${name}</h3>
            <p>${role} / ${phone}</p>
          </div>
          <button class="small-button" data-add="${userId}" type="button" ${disabled ? "disabled" : ""}>
            ${label}
          </button>
        </article>
      `;
    })
    .join("");
}

export function renderFriendRequests() {
  const pendingCount = state.friendRequestItems.filter((request) => {
    return request.direction === "incoming" && request.status === "pending";
  }).length;
  $("#friendRequestCount").textContent = pendingCount;
  $("#friendRequestList").innerHTML = state.friendRequestItems
    .map((request) => {
      const user = request.user || state.users.find((item) => item.id === request.userId) || {};
      const incoming = request.direction !== "outgoing";
      const directionText = incoming
        ? `${user.name || request.userId}申请${state.currentUser.name}`
        : `${state.currentUser.name}申请${user.name || request.userId}`;
      const statusText = {
        pending: "待处理",
        accepted: "已同意",
        rejected: "已拒绝"
      }[request.status] || request.status;
      const userId = escapeHtml(request.userId);
      const requestId = escapeHtml(request.id);
      const name = escapeHtml(user.name || request.userId);
      return `
        <article class="friend-request-row">
          <span class="avatar">${escapeHtml((user.name || "?").slice(0, 1))}</span>
          <div class="request-copy">
            <strong>${name}</strong>
            <small>${escapeHtml(directionText)} / ${escapeHtml(statusText)}</small>
          </div>
          ${
            request.status === "pending" && incoming
              ? `
                <div class="request-actions">
                  <button class="small-button" data-resolve-request="${requestId}" data-decision="accept" type="button">
                    同意
                  </button>
                  <button class="small-button" data-resolve-request="${requestId}" data-decision="reject" type="button">
                    拒绝
                  </button>
                </div>
              `
              : request.status === "accepted"
                ? `<button class="small-button" data-open-chat="${userId}" type="button">聊天</button>`
                : `<span class="request-waiting">${incoming ? "已处理" : "等待对方"}</span>`
          }
        </article>
      `;
    })
    .join("");
}

export function renderAccountSwitch() {
  $("#accountSelect").innerHTML = mockStore.accounts
    .map((account) => {
      return `<option value="${escapeHtml(account.id)}" ${account.id === state.currentUser.id ? "selected" : ""}>${escapeHtml(account.name)}</option>`;
    })
    .join("");
}

export function renderConversations() {
  $("#friendCount").textContent = state.friends.length;
  if (state.friends.length === 0) {
    $("#conversationList").innerHTML = `<p class="empty-copy">暂无联系人。</p>`;
    $("#chatTitle").textContent = "暂无联系人";
    $("#messageStream").innerHTML = "";
    return;
  }
  $("#conversationList").innerHTML = state.friends
    .map((user) => {
      const messages = state.conversations[user.id] || [];
      const last = state.conversationPreviews[user.id] || messages.at(-1);
      const active = user.id === state.selectedConversation ? " is-active" : "";
      const unread = state.unread[user.id] || 0;
      const userId = escapeHtml(user.id);
      const name = escapeHtml(user.name);
      const lastText = last ? (last.deleted ? "消息已撤回" : last.text) : "暂无消息";
      return `
        <button class="conversation-row${active}" data-open-chat="${userId}" type="button">
          <span class="avatar">${escapeHtml((user.name || "?").slice(0, 1))}</span>
          <span class="conversation-copy">
            <strong>${name}</strong>
            <small>${escapeHtml(lastText)}</small>
          </span>
          ${unread > 0 ? `<span class="unread">${unread}</span>` : ""}
        </button>
      `;
    })
    .join("");
}
