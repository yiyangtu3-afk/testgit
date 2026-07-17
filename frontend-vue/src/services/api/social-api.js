import { withApiFallback } from "./auth-api";

export function createSocialApi({ http, mockSocial }) {
  const call = (path, options, fallback) => withApiFallback(() => http.request(path, options), fallback);
  return {
    users: (keyword = "") => call(`/api/users?keyword=${encodeURIComponent(keyword)}`, {}, () => mockSocial.users(keyword)),
    friends: () => call("/api/friends", {}, () => mockSocial.friends()),
    friendRequests: () => call("/api/friends/requests", {}, () => mockSocial.friendRequests()),
    sendFriendRequest: (userId) => call("/api/friends/requests", { method: "POST", body: JSON.stringify({ userId }) }, () => mockSocial.sendFriendRequest(userId)),
    resolveFriendRequest: (id, decision) => call(`/api/friends/requests/${id}/${decision}`, { method: "POST" }, () => mockSocial.resolveFriendRequest(id, decision)),
    messages: (peerId, beforeId = null, limit = 30) => { const query = new URLSearchParams({ limit: String(limit) }); if (beforeId != null) query.set("beforeId", String(beforeId)); return call(`/api/conversations/${peerId}/messages?${query}`, {}, () => mockSocial.messages(peerId, beforeId, limit)); },
    unreadCounts: () => call("/api/conversations/unread-counts", {}, () => mockSocial.unreadCounts()),
    conversationPreviews: () => call("/api/conversations/previews", {}, () => mockSocial.conversationPreviews()),
    sendMessage: (peerId, text, attachments) => call(`/api/conversations/${peerId}/messages`, { method: "POST", body: JSON.stringify({ text, attachments }) }, () => mockSocial.sendMessage(peerId, text, attachments)),
    withdrawMessage: (peerId, messageId) => call(`/api/conversations/${peerId}/messages/${messageId}/withdraw`, { method: "POST" }, () => mockSocial.withdrawMessage(peerId, messageId))
  };
}
