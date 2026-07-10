import { state } from "../state.js";
import { $ } from "../utils/dom.js";
import { formatFileSize } from "../utils/format.js";
import { renderConversations } from "../ui/contacts-renderers.js";

export function currentPeer() {
  return (
    state.friends.find((user) => user.id === state.selectedConversation) ||
    state.users.find((user) => user.id === state.selectedConversation) ||
    state.friends[0] ||
    state.users[0]
  );
}
export function renderMessages({ preserveScroll = false } = {}) {
  const peer = currentPeer();
  if (!peer) return;
  $("#chatTitle").textContent = peer.name;
  const stream = $("#messageStream");
  const previousScrollTop = stream.scrollTop;
  const previousScrollHeight = stream.scrollHeight;
  const paging = state.conversationPaging[peer.id];
  const loadOlder = paging?.hasMore
    ? `<button class="small-button load-older-messages" data-load-older-messages="${peer.id}" type="button">加载更早消息</button>`
    : "";
  stream.innerHTML = `${loadOlder}${(state.conversations[peer.id] || [])
    .map((message) => {
      const mine = message.from === state.currentUser.id;
      const attachments = message.deleted ? [] : message.attachments || [];
      const attachmentRows = attachments
        .map((attachment) => `
          <div class="message-attachment">
            <span class="attachment-icon">${attachment.kind ? attachment.kind.slice(0, 1) : "文"}</span>
            <span>
              <strong>${attachment.name}</strong>
              <small>${attachment.kind || "文件"} / ${formatFileSize(attachment.size || 0)}</small>
            </span>
          </div>
        `)
        .join("");
      return `
        <article class="message ${mine ? "is-mine" : ""}">
          <span>${mine ? state.currentUser.name : peer.name}</span>
          <p>${message.deleted ? "消息已撤回" : message.text}</p>
          ${attachmentRows ? `<div class="message-attachments">${attachmentRows}</div>` : ""}
          <time>${message.time}</time>
        </article>
      `;
    })
    .join("")}`;
  stream.scrollTop = preserveScroll
    ? previousScrollTop + stream.scrollHeight - previousScrollHeight
    : stream.scrollHeight;
  renderConversations();
}

export function renderAttachmentTray() {
  const tray = $("#attachmentTray");
  if (state.pendingAttachments.length === 0) {
    tray.hidden = true;
    tray.innerHTML = "";
    return;
  }
  tray.hidden = false;
  tray.innerHTML = state.pendingAttachments
    .map((attachment) => `
      <span class="attachment-chip">
        <strong>${attachment.name}</strong>
        <small>${formatFileSize(attachment.size)}</small>
        <button type="button" data-remove-attachment="${attachment.id}" aria-label="移除 ${attachment.name}"></button>
      </span>
    `)
    .join("");
}
