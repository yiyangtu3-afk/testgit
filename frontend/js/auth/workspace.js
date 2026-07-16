import { mockStore, state } from "../state.js";
import { api } from "../api/client.js?v=20260715-social-realtime-v1";
import { accountById, pushMockAudit } from "../api/mock-api.js?v=20260715-social-realtime-v1";
import { $ } from "../utils/dom.js?v=20260715-social-realtime-v1";
import { setApiMode, setRealtimeMode, setStatus } from "../ui/status.js?v=20260715-social-realtime-v1";
import { renderAccountSwitch, renderAttachmentTray, renderExportPanel, renderIdentity, renderMessages } from "../ui/renderers.js?v=20260715-social-realtime-v1";
import { loadActivities, loadAdminData, loadConversationPreviews, loadFeed, loadFriendRequests, loadFriends, loadMessages, loadNotifications, loadUnreadCounts, loadUsers } from "../loaders.js?v=20260715-social-realtime-v1";
import { connectChatRealtime, disconnectChatRealtime } from "../chat/realtime.js?v=20260715-social-realtime-v1";

export async function bootstrapWorkspace() {
  renderAccountSwitch();
  renderIdentity();
  await loadUsers();
  await loadFriendRequests();
  await loadFriends();
  await loadConversationPreviews();
  await loadUnreadCounts();
  await loadMessages();
  await Promise.all([loadFeed(), loadActivities(), loadNotifications(), loadAdminData()]);
  renderMessages();
}

export async function enterWorkspace(response) {
  state.token = response.token;
  state.currentUser = { ...state.currentUser, ...response.user, presence: "online" };
  $("#loginView").hidden = true;
  $("#workspaceView").hidden = false;
  await bootstrapWorkspace();
  connectChatRealtime();
}

export async function switchAccount(userId) {
  const account = accountById(userId);
  if (!account) return;
  disconnectChatRealtime();
  state.currentUser = { ...account, presence: state.currentUser.presence || "online" };
  state.selectedConversation = "";
  state.conversations = {};
  state.conversationPreviews = {};
  state.conversationPaging = {};
  state.unread = {};
  state.pendingAttachments = [];
  state.feedNotice = null;
  state.pendingActivities = [];
  state.activityNotice = null;
  state.activityReviewNotice = null;
  state.managedActivities = [];
  state.activityRosters = {};
  state.expandedActivityRosterId = "";
  state.activityOperationsNotice = null;
  state.activityNotifications = [];
  state.activityNotificationUnreadCount = 0;
  state.activityNotificationNotice = null;
  state.socialNotifications = [];
  state.socialNotificationUnreadCount = 0;
  state.socialNotificationNotice = null;
  state.moderationFilter = "all";
  state.selectedModerationIds = new Set();
  state.reviewingModerationId = "";
  state.adminNotice = null;
  $("#searchInput").value = "";
  $("#attachmentInput").value = "";
  renderAttachmentTray();
  pushMockAudit("用户", `${state.currentUser.name}切换到演示账号`);
  try {
    const response = await api.loginAsDemo(userId);
    state.token = response.token;
    state.currentUser = { ...state.currentUser, ...response.user, presence: state.currentUser.presence || "online" };
  } catch {
    // Keep local account switching usable when the Java API is unavailable.
  }
  await bootstrapWorkspace();
  connectChatRealtime();
}

export function logout() {
  disconnectChatRealtime();
  state.token = "";
  setRealtimeMode("offline");
  state.verificationCode = "";
  state.users = [];
  state.selectedConversation = "u-2001";
  state.conversations = {};
  state.conversationPreviews = {};
  state.conversationPaging = {};
  state.unread = {};
  state.friendRequests = {};
  state.friendRequestItems = [];
  state.friends = [];
  state.pendingAttachments = [];
  state.posts = [];
  state.personalPosts = [];
  state.personalPostManagerOpen = false;
  state.editingPersonalPostId = null;
  state.feedNotice = null;
  state.postComments = {};
  state.expandedPostId = null;
  state.activities = [];
  state.activitySubmissions = [];
  state.managedActivities = [];
  state.activityRosters = {};
  state.expandedActivityRosterId = "";
  state.activityOperationsNotice = null;
  state.pendingActivities = [];
  state.activityNotice = null;
  state.activityReviewNotice = null;
  state.activityNotifications = [];
  state.activityNotificationUnreadCount = 0;
  state.activityNotificationNotice = null;
  state.socialNotifications = [];
  state.socialNotificationUnreadCount = 0;
  state.socialNotificationNotice = null;
  state.metrics = {};
  state.moderationItems = [];
  state.moderationFilter = "all";
  state.selectedModerationIds = new Set();
  state.reviewingModerationId = "";
  state.adminNotice = null;
  state.reportRange = "today";
  state.reportExport = null;
  state.auditEvents = [];
  state.currentUser = {
    id: "u-1001",
    name: "林一",
    role: "学生账号",
    phone: "13800000001",
    presence: "online"
  };
  $("#workspaceView").hidden = true;
  $("#loginView").hidden = false;
  $("#codeInput").value = "";
  $("#attachmentInput").value = "";
  renderAttachmentTray();
  renderExportPanel();
  setStatus("已退出登录，可重新获取验证码或快速进入。");
  setApiMode("mock");
}
