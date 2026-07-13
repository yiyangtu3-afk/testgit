import { api } from "./api/client.js?v=20260712-activity-filters-v1";
import { bindActivityEvents } from "./activities/events.js?v=20260712-activity-filters-v1";
import { bindAdminAuditEvents } from "./admin/audit-events.js?v=20260712-activity-filters-v1";
import { bindPersonalPostEvents } from "./posts/personal-post-events.js?v=20260712-activity-filters-v1";
import { state, reportRanges } from "./state.js";
import { $ } from "./utils/dom.js?v=20260712-activity-filters-v1";
import { isAdminUser } from "./utils/auth.js?v=20260712-activity-filters-v1";
import { filesToAttachments, reportToCsv } from "./utils/format.js?v=20260712-activity-filters-v1";
import { loadActivities, loadAdminData, loadComments, loadFeed, loadFriendRequests, loadFriends, loadMessages, loadPersonalPosts, loadUsers } from "./loaders.js?v=20260712-activity-filters-v1";
import { enterDemoWorkspace, loginWithCode, sendLoginCode } from "./auth/session.js?v=20260712-activity-filters-v1";
import { logout, switchAccount } from "./auth/workspace.js?v=20260712-activity-filters-v1";
import { currentPeer, renderAttachmentTray, renderExportPanel, renderFeed, renderIdentity, renderMessages, switchTab } from "./ui/renderers.js?v=20260712-activity-filters-v1";

function successNotice(message) {
  return { kind: "success", message };
}

function errorNotice(message) {
  return { kind: "error", message };
}

function confirmAdminAction(message) {
  return typeof window.confirm !== "function" || window.confirm(message);
}

export function bindAppEvents() {
  bindActivityEvents();
  $("#loginForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    await loginWithCode();
  });
  
  $("#sendCodeButton").addEventListener("click", sendLoginCode);
  $("#demoLoginButton").addEventListener("click", enterDemoWorkspace);
  $("#accountSelect").addEventListener("change", (event) => switchAccount(event.target.value));
  $("#logoutButton").addEventListener("click", logout);
  $("#attachmentButton").addEventListener("click", () => $("#attachmentInput").click());
  $("#attachmentInput").addEventListener("change", (event) => {
    state.pendingAttachments = [...state.pendingAttachments, ...filesToAttachments(event.target.files)];
    event.target.value = "";
    renderAttachmentTray();
  });
  
  $("#searchButton").addEventListener("click", () => loadUsers($("#searchInput").value));
  $("#searchInput").addEventListener("input", (event) => loadUsers(event.target.value));
  $("#reportRange").addEventListener("click", (event) => {
    const button = event.target.closest("[data-report-range]");
    if (!button) return;
    const range = button.dataset.reportRange;
    if (!reportRanges[range]) return;
    state.reportRange = range;
    $("#reportRange").querySelectorAll("[data-report-range]").forEach((item) => {
      item.classList.toggle("is-active", item.dataset.reportRange === range);
    });
    state.reportExport = null;
    renderExportPanel();
  });
  $("#exportReportButton").addEventListener("click", async () => {
    if (!isAdminUser()) {
      state.reportExport = {
        status: "error",
        message: "请切换到教务管理员账号后再导出报表。"
      };
      renderExportPanel();
      return;
    }
    const button = $("#exportReportButton");
    button.disabled = true;
    button.textContent = "生成中";
    state.reportExport = { status: "loading" };
    renderExportPanel();
    try {
      const report = await api.adminReport(state.reportRange);
      const csv = reportToCsv(report);
      const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
      if (state.reportExport && state.reportExport.url) {
        URL.revokeObjectURL(state.reportExport.url);
      }
      state.reportExport = {
        status: "ready",
        report,
        url: URL.createObjectURL(blob)
      };
      renderExportPanel();
      await loadAdminData();
    } catch (error) {
      state.reportExport = {
        status: "error",
        message: error.message || "请稍后重试。"
      };
      renderExportPanel();
    } finally {
      button.disabled = false;
      button.textContent = "打印报表";
    }
  });
  
  document.addEventListener("click", async (event) => {
    const addButton = event.target.closest("[data-add]");
    if (addButton) {
      addButton.disabled = true;
      addButton.textContent = "申请中";
      const response = await api.sendFriendRequest(addButton.dataset.add);
      state.friendRequests[response.userId] = response.status;
      await loadFriendRequests();
      await loadAdminData();
    }
  
    const resolveButton = event.target.closest("[data-resolve-request]");
    if (resolveButton) {
      const decision = resolveButton.dataset.decision;
      const response = await api.resolveFriendRequest(resolveButton.dataset.resolveRequest, decision);
      state.friendRequests[response.userId] = response.status;
      await Promise.all([loadFriendRequests(), loadFriends(), loadMessages(response.userId), loadAdminData()]);
      if (response.status === "accepted") {
        state.selectedConversation = response.userId;
        switchTab("chat");
        renderMessages();
      }
    }
  
    const openChat = event.target.closest("[data-open-chat]");
    if (openChat) {
      state.selectedConversation = openChat.dataset.openChat;
      switchTab("chat");
      await loadMessages(state.selectedConversation);
    }

    const loadOlderMessages = event.target.closest("[data-load-older-messages]");
    if (loadOlderMessages) {
      const peerId = loadOlderMessages.dataset.loadOlderMessages;
      const paging = state.conversationPaging[peerId];
      if (paging?.hasMore) {
        await loadMessages(peerId, paging.nextBeforeId);
      }
    }
  
    const tabButton = event.target.closest("[data-tab]");
    if (tabButton) {
      switchTab(tabButton.dataset.tab);
      if (tabButton.dataset.tab === "admin") {
        await loadAdminData();
      }
      if (tabButton.dataset.tab === "activities") {
        await loadActivities();
      }
    }

    if (await bindPersonalPostEvents(event)) return;
  
    const likeButton = event.target.closest("[data-like]");
    if (likeButton) {
      await api.likePost(Number(likeButton.dataset.like));
      await Promise.all([loadFeed(), loadAdminData()]);
    }

    const commentsButton = event.target.closest("[data-comments]");
    if (commentsButton) {
      const postId = Number(commentsButton.dataset.comments);
      state.expandedPostId = state.expandedPostId === postId ? null : postId;
      if (state.expandedPostId) {
        await loadComments(postId);
      } else {
        renderFeed();
      }
    }
  
    const removeAttachment = event.target.closest("[data-remove-attachment]");
    if (removeAttachment) {
      state.pendingAttachments = state.pendingAttachments.filter((attachment) => {
        return attachment.id !== removeAttachment.dataset.removeAttachment;
      });
      renderAttachmentTray();
    }

    if (await bindAdminAuditEvents(event)) return;

    const moderationFilter = event.target.closest("[data-moderation-filter]");
    if (moderationFilter) {
      state.moderationFilter = moderationFilter.dataset.moderationFilter;
      state.selectedModerationIds = new Set();
      state.reviewingModerationId = "";
      await loadAdminData();
    }

    const toggleAllModeration = event.target.closest("[data-toggle-all-moderation]");
    if (toggleAllModeration) {
      const pendingIds = state.moderationItems
        .filter((item) => item.status === "pending")
        .filter((item) => state.moderationFilter === "all" || item.type === state.moderationFilter)
        .map((item) => String(item.id));
      const allSelected = pendingIds.length > 0 && pendingIds.every((itemId) => state.selectedModerationIds.has(itemId));
      state.selectedModerationIds = allSelected ? new Set() : new Set(pendingIds);
      await loadAdminData();
    }

    const selectModeration = event.target.closest("[data-select-moderation]");
    if (selectModeration) {
      const next = new Set(state.selectedModerationIds);
      if (selectModeration.checked) {
        next.add(selectModeration.dataset.selectModeration);
      } else {
        next.delete(selectModeration.dataset.selectModeration);
      }
      state.selectedModerationIds = next;
      await loadAdminData();
    }

    const deleteModeration = event.target.closest("[data-delete-moderation]");
    if (deleteModeration) {
      if (!isAdminUser()) return;
      const itemId = deleteModeration.dataset.deleteModeration;
      if (!confirmAdminAction("确认删除这条待审核内容？删除后不会进入同意或拒绝流程。")) return;
      try {
        const response = await api.deleteModerationItems([itemId]);
        if (state.reviewingModerationId === itemId) {
          state.reviewingModerationId = "";
        }
        state.selectedModerationIds.delete(itemId);
        state.adminNotice = successNotice(`已删除 ${response.deleted || 1} 条待审核内容。`);
      } catch (error) {
        console.error(error);
        state.adminNotice = errorNotice("待审核内容删除失败，请刷新页面后重试。");
      }
      await loadAdminData();
    }

    const deleteSelectedModeration = event.target.closest("[data-delete-selected-moderation]");
    if (deleteSelectedModeration) {
      if (!isAdminUser() || state.selectedModerationIds.size === 0) return;
      const itemIds = [...state.selectedModerationIds];
      if (!confirmAdminAction(`确认删除已选择的 ${itemIds.length} 条待审核内容？删除后不会进入同意或拒绝流程。`)) return;
      try {
        const response = await api.deleteModerationItems(itemIds);
        if (state.selectedModerationIds.has(state.reviewingModerationId)) {
          state.reviewingModerationId = "";
        }
        state.selectedModerationIds = new Set();
        state.adminNotice = successNotice(`已删除 ${response.deleted || itemIds.length} 条待审核内容。`);
      } catch (error) {
        console.error(error);
        state.adminNotice = errorNotice("待审核内容批量删除失败，请刷新页面后重试。");
      }
      await loadAdminData();
    }

    const reviewModeration = event.target.closest("[data-review-moderation]");
    if (reviewModeration) {
      if (!isAdminUser()) return;
      const itemId = reviewModeration.dataset.reviewModeration;
      state.reviewingModerationId = state.reviewingModerationId === itemId ? "" : itemId;
      await loadAdminData();
    }

    const moderationButton = event.target.closest("[data-moderation]");
    if (moderationButton) {
      if (!isAdminUser()) return;
      const decisionLabel = moderationButton.dataset.decision === "approve" ? "同意" : "拒绝";
      if (state.reviewingModerationId === moderationButton.dataset.moderation) {
        state.reviewingModerationId = "";
      }
      try {
        await api.resolveModeration(moderationButton.dataset.moderation, moderationButton.dataset.decision);
        state.adminNotice = successNotice(`已${decisionLabel}该待审核内容。`);
        await Promise.all([loadFeed(), loadAdminData()]);
        if (state.expandedPostId) {
          await loadComments(state.expandedPostId);
        }
      } catch (error) {
        console.error(error);
        state.adminNotice = errorNotice("审核操作失败，请刷新页面后重试。");
        await loadAdminData();
      }
    }
  
    const printButton = event.target.closest("[data-print-report]");
    if (printButton) {
      window.print();
    }
  });
  
  document.addEventListener("submit", async (event) => {
    const commentForm = event.target.closest("[data-comment-form]");
    if (!commentForm) return;
    event.preventDefault();
    const postId = Number(commentForm.dataset.commentForm);
    const input = commentForm.elements.comment;
    const body = input.value.trim();
    if (!body) return;
    input.value = "";
    await api.publishComment(postId, body);
    state.feedNotice = successNotice("评论已提交审核，通过后会显示在动态下。");
    await Promise.all([loadFeed(), loadComments(postId), loadAdminData()]);
  });
  
  $("#composerForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    const input = $("#messageInput");
    const text = input.value.trim();
    const attachments = [...state.pendingAttachments];
    if (!text && attachments.length === 0) return;
    input.value = "";
    state.pendingAttachments = [];
    renderAttachmentTray();
    const peer = currentPeer();
    if (!peer) return;
    await api.sendMessage(peer.id, text || "发送了附件", attachments);
    await Promise.all([loadMessages(peer.id), loadAdminData()]);
  });
  
  $("#withdrawLast").addEventListener("click", async () => {
    const peer = currentPeer();
    const mine = [...(state.conversations[peer.id] || [])].reverse().find((message) => {
      return message.from === state.currentUser.id && !message.deleted;
    });
    if (mine) {
      await api.withdrawMessage(peer.id, mine.id);
      await Promise.all([loadMessages(peer.id), loadAdminData()]);
    }
  });
  
  $("#togglePresence").addEventListener("click", async () => {
    const next = state.currentUser.presence === "online" ? "invisible" : "online";
    const response = await api.updatePresence(next);
    state.currentUser.presence = response.presence;
    renderIdentity();
    await loadAdminData();
  });
  
  $("#publishPost").addEventListener("click", async () => {
    const body = $("#postInput").value.trim();
    if (!body) return;
    await api.publishPost(body, $("#visibilitySelect").value);
    $("#postInput").value = "";
    state.feedNotice = successNotice("动态已提交审核，可在我的动态中查看进度。");
    state.personalPostManagerOpen = true;
    state.editingPersonalPostId = null;
    await Promise.all([loadFeed(), loadPersonalPosts(), loadAdminData()]);
  });
  
}
