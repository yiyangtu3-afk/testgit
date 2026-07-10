import { API_BASE, state } from "../state.js";
import { setApiMode } from "../ui/status.js";
import { mockApi } from "./mock-api.js?v=20260709-feed-visibility-v1";

class ApiUnavailableError extends Error {
  constructor(cause) {
    super("Java API 暂不可用。");
    this.name = "ApiUnavailableError";
    this.cause = cause;
  }
}

async function request(path, options = {}) {
  let response;
  try {
    response = await fetch(`${API_BASE}${path}`, {
      headers: {
        "Content-Type": "application/json",
        ...(state.token ? { Authorization: `Bearer ${state.token}` } : {})
      },
      ...options
    });
  } catch (error) {
    throw new ApiUnavailableError(error);
  }

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }

  return response.json();
}
async function withApi(liveCall, fallbackCall) {
  try {
    const result = await liveCall();
    setApiMode("live");
    return result;
  } catch (error) {
    if (!(error instanceof ApiUnavailableError)) {
      throw error;
    }
    const result = await fallbackCall();
    setApiMode("mock");
    return result;
  }
}

export const api = {
  createCode(phone) {
    return withApi(
      () => request("/auth/code", { method: "POST", body: JSON.stringify({ phone }) }),
      () => mockApi.createCode(phone)
    );
  },
  login(phone, code) {
    return withApi(
      () => request("/auth/login", { method: "POST", body: JSON.stringify({ phone, code }) }),
      () => mockApi.login(phone, code)
    );
  },
  loginAsDemo(userId = state.currentUser.id) {
    return withApi(
      () => request("/auth/demo-login", { method: "POST", body: JSON.stringify({ userId }) }),
      () => mockApi.loginAsDemo(userId)
    );
  },
  users(keyword) {
    return withApi(
      () => request(`/users?keyword=${encodeURIComponent(keyword)}`),
      () => mockApi.users(keyword)
    );
  },
  sendFriendRequest(userId) {
    return withApi(
      () => request("/friends/requests", {
        method: "POST",
        body: JSON.stringify({ userId })
      }),
      () => mockApi.sendFriendRequest(userId)
    );
  },
  friendRequests() {
    return withApi(
      () => request("/friends/requests"),
      () => mockApi.friendRequests()
    );
  },
  friends() {
    return withApi(
      () => request("/friends"),
      () => mockApi.friends()
    );
  },
  resolveFriendRequest(requestId, decision) {
    return withApi(
      () => request(`/friends/requests/${requestId}/${decision}`, { method: "POST" }),
      () => mockApi.resolveFriendRequest(requestId, decision)
    );
  },
  messages(peerId) {
    return withApi(
      () => request(`/conversations/${peerId}/messages`),
      () => mockApi.messages(peerId)
    );
  },
  sendMessage(peerId, text, attachments = []) {
    return withApi(
      () => request(`/conversations/${peerId}/messages`, {
        method: "POST",
        body: JSON.stringify({ text, attachments })
      }),
      () => mockApi.sendMessage(peerId, text, attachments)
    );
  },
  withdrawMessage(peerId, messageId) {
    return withApi(
      () => request(`/conversations/${peerId}/messages/${messageId}/withdraw`, { method: "POST" }),
      () => mockApi.withdrawMessage(peerId, messageId)
    );
  },
  updatePresence(presence) {
    return withApi(
      () => request("/presence", { method: "POST", body: JSON.stringify({ presence }) }),
      () => mockApi.updatePresence(presence)
    );
  },
  feed() {
    return withApi(() => request("/feed"), () => mockApi.feed());
  },
  publishPost(body, visibility) {
    return withApi(
      () => request("/feed", { method: "POST", body: JSON.stringify({ body, visibility }) }),
      () => mockApi.publishPost(body, visibility)
    );
  },
  personalPosts() {
    return withApi(() => request("/feed/personal-posts"), () => mockApi.personalPosts());
  },
  updatePersonalPost(postId, body) {
    return withApi(
      () => request(`/feed/personal-posts/${postId}`, {
        method: "PATCH",
        body: JSON.stringify({ body })
      }),
      () => mockApi.updatePersonalPost(postId, body)
    );
  },
  deletePersonalPost(postId) {
    return withApi(
      () => request(`/feed/personal-posts/${postId}`, { method: "DELETE" }),
      () => mockApi.deletePersonalPost(postId)
    );
  },
  likePost(postId) {
    return withApi(
      () => request(`/feed/${postId}/likes`, { method: "POST" }),
      () => mockApi.likePost(postId)
    );
  },
  comments(postId) {
    return withApi(
      () => request(`/feed/${postId}/comments`),
      () => mockApi.comments(postId)
    );
  },
  publishComment(postId, body) {
    return withApi(
      () => request(`/feed/${postId}/comments`, { method: "POST", body: JSON.stringify({ body }) }),
      () => mockApi.publishComment(postId, body)
    );
  },
  metrics() {
    return withApi(() => request("/admin/metrics"), () => mockApi.metrics());
  },
  auditEvents() {
    return withApi(() => request("/admin/audit-events"), () => mockApi.auditEvents());
  },
  deleteAuditEvents(eventIds) {
    return withApi(
      () => request("/admin/audit-events", {
        method: "DELETE",
        body: JSON.stringify({ eventIds })
      }),
      () => mockApi.deleteAuditEvents(eventIds)
    );
  },
  moderationItems() {
    return withApi(() => request("/admin/moderation"), () => mockApi.moderationItems());
  },
  resolveModeration(itemId, decision) {
    return withApi(
      () => request(`/admin/moderation/${itemId}/${decision}`, { method: "POST" }),
      () => mockApi.resolveModeration(itemId, decision)
    );
  },
  deleteModerationItems(itemIds) {
    return withApi(
      () => request("/admin/moderation", {
        method: "DELETE",
        body: JSON.stringify({ itemIds })
      }),
      () => mockApi.deleteModerationItems(itemIds)
    );
  },
  adminReport(range = state.reportRange) {
    return withApi(() => request(`/admin/report?range=${encodeURIComponent(range)}`), () => mockApi.adminReport());
  }
};
