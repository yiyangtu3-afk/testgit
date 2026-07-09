import { mockStore, state } from "../state.js";
import { $ } from "../utils/dom.js";

export function renderSearchResults() {
  $("#resultList").innerHTML = state.users
    .map((user) => {
      const status = state.friendRequests[user.id];
      const friend = state.friends.some((item) => item.id === user.id);
      const disabled = friend || status === "pending" || status === "accepted";
      const label = friend || status === "accepted" ? "已好友" : status === "pending" ? "已申请" : "申请";
      return `
        <article class="person-row">
          <span class="avatar">${user.name.slice(0, 1)}</span>
          <div>
            <h3>${user.name}</h3>
            <p>${user.role} / ${user.phone}</p>
          </div>
          <button class="small-button" data-add="${user.id}" type="button" ${disabled ? "disabled" : ""}>
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
      return `
        <article class="friend-request-row">
          <span class="avatar">${(user.name || "?").slice(0, 1)}</span>
          <div class="request-copy">
            <strong>${user.name || request.userId}</strong>
            <small>${directionText} / ${statusText}</small>
          </div>
          ${
            request.status === "pending" && incoming
              ? `
                <div class="request-actions">
                  <button class="small-button" data-resolve-request="${request.id}" data-decision="accept" type="button">
                    同意
                  </button>
                  <button class="small-button" data-resolve-request="${request.id}" data-decision="reject" type="button">
                    拒绝
                  </button>
                </div>
              `
              : request.status === "accepted"
                ? `<button class="small-button" data-open-chat="${request.userId}" type="button">聊天</button>`
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
      return `<option value="${account.id}" ${account.id === state.currentUser.id ? "selected" : ""}>${account.name}</option>`;
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
      const last = messages.at(-1);
      const active = user.id === state.selectedConversation ? " is-active" : "";
      const unread = state.unread[user.id] || 0;
      return `
        <button class="conversation-row${active}" data-open-chat="${user.id}" type="button">
          <span class="avatar">${user.name.slice(0, 1)}</span>
          <span class="conversation-copy">
            <strong>${user.name}</strong>
            <small>${last ? (last.deleted ? "消息已撤回" : last.text) : "暂无消息"}</small>
          </span>
          ${unread > 0 ? `<span class="unread">${unread}</span>` : ""}
        </button>
      `;
    })
    .join("");
}
