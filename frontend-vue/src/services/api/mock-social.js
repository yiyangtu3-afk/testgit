const people = [
  { id: "u-1001", name: "林一", role: "学生账号", phone: "13800000001", status: "online" },
  { id: "u-2001", name: "陈老师", role: "教师", phone: "13800000002", status: "online" },
  { id: "u-2002", name: "周同学", role: "学生", phone: "13800000003", status: "offline" },
  { id: "u-2003", name: "教务管理员", role: "管理员", phone: "13800000004", status: "online" }
];

export function createMockSocialApi(getUser) {
  const requests = [{ id: "fr-1001", fromUserId: "u-2002", toUserId: "u-1001", status: "pending" }];
  const friendships = [["u-1001", "u-2001"], ["u-1001", "u-2003"]];
  const messages = {
    "u-2001": [
      { id: 1, from: "u-2001", text: "毕设题目可以再收敛一点，先把第一版 demo 跑起来。", time: "09:12", deleted: false, attachments: [] },
      { id: 2, from: "u-1001", text: "收到，我先做登录、好友和单聊。", time: "09:14", deleted: false, attachments: [] }
    ],
    "u-2003": [{ id: 4, from: "u-2003", text: "后台需要能看到基础统计和审计记录。", time: "周一", deleted: false, attachments: [] }]
  };
  const reads = {};
  const user = () => getUser() || people[0];
  const person = (id) => people.find((item) => item.id === id);
  const friends = () => friendships.filter((pair) => pair.includes(user().id)).map((pair) => person(pair.find((id) => id !== user().id))).filter(Boolean);
  const areFriends = (id) => friends().some((friend) => friend.id === id);
  const page = (peerId, beforeId, limit = 30) => {
    if (!areFriends(peerId)) throw new Error("仅能与已建立好友关系的用户聊天");
    const newest = [...(messages[peerId] || [])].filter((item) => beforeId == null || item.id < beforeId).sort((a, b) => b.id - a.id);
    const result = newest.slice(0, limit + 1); const hasMore = result.length > limit; const selected = result.slice(0, limit);
    if (beforeId == null && selected[0]) reads[peerId] = selected[0].id;
    return { messages: selected.reverse(), hasMore, nextBeforeId: hasMore ? selected.at(-1).id : null };
  };
  return {
    async users(keyword = "") { const text = keyword.trim(); return people.filter((item) => item.id !== user().id && (!text || item.name.includes(text) || item.phone.includes(text))); },
    async friends() { return friends(); },
    async friendRequests() { return requests.filter((item) => item.fromUserId === user().id || item.toUserId === user().id).map((item) => { const incoming = item.toUserId === user().id; const userId = incoming ? item.fromUserId : item.toUserId; return { ...item, userId, direction: incoming ? "incoming" : "outgoing", user: person(userId) }; }); },
    async sendFriendRequest(userId) { if (areFriends(userId)) return { userId, status: "accepted" }; const existing = requests.find((item) => item.fromUserId === user().id && item.toUserId === userId) || { id: `fr-${Date.now()}`, fromUserId: user().id, toUserId: userId, status: "pending" }; if (!requests.includes(existing)) requests.unshift(existing); return { userId, status: "pending" }; },
    async resolveFriendRequest(requestId, decision) { const request = requests.find((item) => item.id === requestId); if (!request || request.toUserId !== user().id) throw new Error("只能处理发给当前账号的好友申请"); request.status = decision === "accept" ? "accepted" : "rejected"; if (request.status === "accepted") { friendships.push([request.fromUserId, request.toUserId]); messages[request.fromUserId] ||= []; messages[request.fromUserId].push({ id: Date.now(), from: user().id, text: "我们已经是好友了，之后可以直接在这里沟通。", time: "刚刚", deleted: false, attachments: [] }); } return { ...request, userId: request.fromUserId, direction: "incoming", user: person(request.fromUserId) }; },
    async messages(peerId, beforeId, limit) { return page(peerId, beforeId, limit); },
    async unreadCounts() { const counts = {}; friends().forEach((friend) => { const count = (messages[friend.id] || []).filter((item) => item.from === friend.id && !item.deleted && item.id > (reads[friend.id] || 0)).length; if (count) counts[friend.id] = count; }); return { counts }; },
    async conversationPreviews() { return { previews: Object.fromEntries(friends().map((friend) => [friend.id, (messages[friend.id] || []).at(-1)]).filter(([, item]) => item)) }; },
    async sendMessage(peerId, text, attachments = []) { if (!areFriends(peerId)) throw new Error("仅能与已建立好友关系的用户聊天"); const message = { id: Date.now(), from: user().id, text, time: "刚刚", deleted: false, attachments }; (messages[peerId] ||= []).push(message); return message; },
    async attachment(peerId, attachmentId) { const attachment = (messages[peerId] || []).flatMap((message) => message.attachments || []).find((item) => item.id === attachmentId); if (!attachment?.dataUrl) throw new Error("图片附件不存在或尚未上传"); const [header, encoded] = attachment.dataUrl.split(",", 2); const binary = atob(encoded); const bytes = Uint8Array.from(binary, (character) => character.charCodeAt(0)); return new Blob([bytes], { type: header.slice(5, header.indexOf(";")) }); },
    async withdrawMessage(peerId, messageId) { const message = (messages[peerId] || []).find((item) => item.id === messageId); if (!message || message.from !== user().id) throw new Error("只能撤回自己发送的消息"); message.deleted = true; return message; }
  };
}
