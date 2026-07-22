import { computed, ref } from "vue";
import { defineStore } from "pinia";
import { createApi } from "../services/api";
import { useSessionStore } from "./session";

export function createChatStore({ api, setMode = () => {} } = {}) {
  return () => {
    const friends = ref([]); const requests = ref([]); const results = ref([]); const previews = ref({}); const unread = ref({});
    const conversations = ref({}); const paging = ref({}); const selectedId = ref(""); const notice = ref(""); const loading = ref(false); const attachmentUrls = ref({});
    const selectedFriend = computed(() => friends.value.find((item) => item.id === selectedId.value));
    async function run(call) { const result = await call(); setMode(result.mode); return result.data; }
    async function refreshLists() { const [nextFriends, nextRequests, nextUnread, nextPreviews] = await Promise.all([run(() => api.friends()), run(() => api.friendRequests()), run(() => api.unreadCounts()), run(() => api.conversationPreviews())]); friends.value = nextFriends; requests.value = nextRequests; unread.value = nextUnread.counts || {}; previews.value = nextPreviews.previews || {}; if (!friends.value.some((item) => item.id === selectedId.value)) selectedId.value = friends.value[0]?.id || ""; }
    async function loadMessages(peerId = selectedId.value, beforeId = null) { if (!peerId) return; const page = await run(() => api.messages(peerId, beforeId)); conversations.value = { ...conversations.value, [peerId]: beforeId == null ? page.messages : [...(conversations.value[peerId] || []), ...page.messages] }; paging.value = { ...paging.value, [peerId]: { hasMore: page.hasMore, nextBeforeId: page.nextBeforeId } }; if (beforeId == null) { unread.value = (await run(() => api.unreadCounts())).counts || {}; } }
    async function initialize() { loading.value = true; notice.value = ""; try { await refreshLists(); await loadMessages(); } catch (error) { notice.value = error.message || "联系人暂时无法加载。"; } finally { loading.value = false; } }
    async function select(peerId) { selectedId.value = peerId; await loadMessages(peerId); }
    async function search(keyword) { try { results.value = keyword.trim() ? await run(() => api.users(keyword)) : []; } catch (error) { notice.value = error.message || "搜索失败。"; } }
    async function requestFriend(userId) { await run(() => api.sendFriendRequest(userId)); notice.value = "好友申请已发送。"; await refreshLists(); }
    async function resolveRequest(id, decision) { const item = await run(() => api.resolveFriendRequest(id, decision)); notice.value = decision === "accept" ? "已同意好友申请。" : "已拒绝好友申请。"; await refreshLists(); if (decision === "accept") { selectedId.value = item.userId; await loadMessages(item.userId); } }
    function attachmentKey(peerId, attachmentId) { return `${peerId}:${attachmentId}`; }
    function attachmentUrl(peerId, attachmentId) { return attachmentUrls.value[attachmentKey(peerId, attachmentId)] || ""; }
    function rememberLocalAttachments(peerId, attachments) { const next = { ...attachmentUrls.value }; attachments.filter((attachment) => attachment.dataUrl).forEach((attachment) => { next[attachmentKey(peerId, attachment.id)] = attachment.dataUrl; }); attachmentUrls.value = next; }
    async function loadAttachmentUrl(peerId, attachmentId) { const key = attachmentKey(peerId, attachmentId); if (attachmentUrls.value[key]) return attachmentUrls.value[key]; try { const blob = await run(() => api.attachment(peerId, attachmentId)); const url = URL.createObjectURL(blob); attachmentUrls.value = { ...attachmentUrls.value, [key]: url }; return url; } catch (error) { notice.value = error.message || "图片暂时无法加载。"; return ""; } }
    function clearAttachmentUrls() { Object.values(attachmentUrls.value).filter((url) => url.startsWith("blob:")).forEach((url) => URL.revokeObjectURL(url)); attachmentUrls.value = {}; }
    async function send(text, attachments = []) { const peerId = selectedId.value; if (!peerId || (!text.trim() && !attachments.length)) return; const message = await run(() => api.sendMessage(peerId, text.trim() || "图片", attachments)); rememberLocalAttachments(peerId, attachments); conversations.value = { ...conversations.value, [peerId]: [...(conversations.value[peerId] || []), message] }; previews.value = { ...previews.value, [peerId]: message }; }
    async function withdraw(messageId) { const peerId = selectedId.value; const message = await run(() => api.withdrawMessage(peerId, messageId)); conversations.value = { ...conversations.value, [peerId]: (conversations.value[peerId] || []).map((item) => item.id === messageId ? message : item) }; }
    return { friends, requests, results, previews, unread, conversations, paging, selectedId, selectedFriend, notice, loading, initialize, select, search, requestFriend, resolveRequest, loadMessages, send, withdraw, refreshLists, attachmentUrl, loadAttachmentUrl, clearAttachmentUrls };
  };
}

export const useChatStore = defineStore("chat", () => {
  const session = useSessionStore();
  const api = createApi({ getToken: () => session.token, getUser: () => session.user });
  return createChatStore({ api, setMode: (mode) => { session.mode = mode; } })();
});
