import { api } from "./api/client.js?v=20260711-activity-review-layout-v2";
import { state } from "./state.js";
import { isAdminUser } from "./utils/auth.js?v=20260711-activity-review-layout-v2";
import { normalizePost } from "./utils/format.js?v=20260711-activity-review-layout-v2";
import {
  renderAdminAccessDenied,
  renderActivities,
  renderAuditEvents,
  renderConversations,
  renderFeed,
  renderFriendRequests,
  renderMessages,
  renderMetrics,
  renderModerationItems,
  renderPersonalPostManager,
  renderPendingActivities,
  renderSearchResults
} from "./ui/renderers.js?v=20260711-activity-review-layout-v2";

export async function loadUsers(keyword = "") {
  state.users = await api.users(keyword);
  renderSearchResults();
}

export async function loadFriends() {
  state.friends = await api.friends();
  if (!state.friends.some((user) => user.id === state.selectedConversation)) {
    state.selectedConversation = state.friends[0] ? state.friends[0].id : "";
  }
  renderSearchResults();
  renderConversations();
}

export async function loadConversationPreviews() {
  const response = await api.conversationPreviews();
  state.conversationPreviews = response.previews || {};
  renderConversations();
}

export async function loadFriendRequests() {
  state.friendRequestItems = await api.friendRequests();
  state.friendRequests = state.friendRequestItems.reduce((result, request) => {
    result[request.userId] = request.status;
    return result;
  }, {});
  renderSearchResults();
  renderFriendRequests();
}

export async function loadMessages(peerId = state.selectedConversation, beforeId = null) {
  if (!peerId) return;
  const page = await api.messages(peerId, beforeId);
  state.conversations[peerId] = beforeId === null
    ? page.messages
    : [...page.messages, ...(state.conversations[peerId] || [])];
  state.conversationPaging[peerId] = {
    hasMore: page.hasMore,
    nextBeforeId: page.nextBeforeId
  };
  if (beforeId === null && page.messages.length > 0) {
    state.conversationPreviews[peerId] = page.messages.at(-1);
  }
  if (beforeId === null) {
    await loadUnreadCounts();
  }
  renderMessages({ preserveScroll: beforeId !== null });
}

export async function loadUnreadCounts() {
  const response = await api.unreadCounts();
  state.unread = response.counts || {};
  renderConversations();
}

export async function loadFeed() {
  state.posts = (await api.feed()).map(normalizePost);
  renderFeed();
  if (state.personalPostManagerOpen) {
    await loadPersonalPosts();
  }
}

export async function loadPersonalPosts() {
  state.personalPosts = (await api.personalPosts())
    .map(normalizePost)
    .filter(isCurrentUserPost);
  renderPersonalPostManager();
}

function isCurrentUserPost(post) {
  if (post.authorId) {
    return post.authorId === state.currentUser.id;
  }
  return post.author === state.currentUser.name;
}

export async function loadComments(postId) {
  state.postComments[postId] = await api.comments(postId);
  renderFeed();
}

export async function loadActivities() {
  state.activities = await api.activities();
  renderActivities();
}

export async function loadPendingActivities() {
  if (!isAdminUser()) {
    state.pendingActivities = [];
    renderPendingActivities();
    return;
  }
  renderPendingActivities();
  try {
    state.pendingActivities = await api.pendingActivities();
  } catch (error) {
    state.pendingActivities = [];
    state.activityReviewNotice = {
      kind: "error",
      message: error.message || "待审核活动暂时无法加载，请稍后重试。"
    };
  }
  renderPendingActivities();
}

export async function loadMetrics() {
  state.metrics = await api.metrics();
  renderMetrics();
}

export async function loadAuditEvents() {
  state.auditEvents = await api.auditEvents();
  const availableIds = new Set(state.auditEvents.map((item) => item.id));
  state.selectedAuditEventIds = new Set([...state.selectedAuditEventIds].filter((itemId) => availableIds.has(itemId)));
  renderAuditEvents();
}

export async function loadModerationItems() {
  state.moderationItems = await api.moderationItems();
  const availableIds = new Set(state.moderationItems.map((item) => String(item.id)));
  state.selectedModerationIds = new Set([...state.selectedModerationIds].filter((itemId) => availableIds.has(itemId)));
  if (state.reviewingModerationId && !availableIds.has(state.reviewingModerationId)) {
    state.reviewingModerationId = "";
  }
  renderModerationItems();
}

export async function loadAdminData() {
  if (!isAdminUser()) {
    state.metrics = {};
    state.auditEvents = [];
    state.moderationItems = [];
    state.moderationFilter = "all";
    state.selectedModerationIds = new Set();
    state.reviewingModerationId = "";
    state.selectedAuditEventIds = new Set();
    state.adminNotice = null;
    state.reportExport = null;
    state.pendingActivities = [];
    state.activityReviewNotice = null;
    renderPendingActivities();
    renderAdminAccessDenied();
    return;
  }
  renderPendingActivities();
  await loadMetrics();
  await loadPendingActivities();
  await loadModerationItems();
  await backfillModerationItemsWhenMetricsDisagree();
  await loadAuditEvents();
}

async function backfillModerationItemsWhenMetricsDisagree() {
  const expectedPending = Number(state.metrics["待审内容"] || 0);
  const actualPending = state.moderationItems.filter((item) => item.status === "pending").length;
  if (expectedPending === 0 || actualPending > 0) {
    return;
  }
  const report = await api.adminReport("all");
  state.moderationItems = report.moderation || [];
  const availableIds = new Set(state.moderationItems.map((item) => String(item.id)));
  state.selectedModerationIds = new Set([...state.selectedModerationIds].filter((itemId) => availableIds.has(itemId)));
  if (state.reviewingModerationId && !availableIds.has(state.reviewingModerationId)) {
    state.reviewingModerationId = "";
  }
  renderModerationItems();
}
