const activitySeed = [{ id: "an-1001", activityId: "a-1", type: "activity.registration.registered", title: "活动报名成功", body: "你已成功报名“校园开源协作日”。", read: false, createdAt: "2026-07-16T09:15:00" }];
const socialSeed = [
  { id: "sn-1001", targetId: "fr-1001", type: "social.friend.requested", title: "新的好友申请", body: "周同学向你发送了好友申请。", read: false, createdAt: "2026-07-16T10:00:00" },
  { id: "sn-1002", targetId: "1", type: "social.post.liked", title: "动态收到新点赞", body: "陈老师赞了你的动态。", read: true, createdAt: "2026-07-15T14:20:00" }
];
function summary(items) { return { items: items.map((item) => ({ ...item })), unreadCount: items.filter((item) => !item.read).length }; }
export function createMockNotificationApi() {
  const activityItems = activitySeed.map((item) => ({ ...item })); const socialItems = socialSeed.map((item) => ({ ...item }));
  return {
    async activityNotifications() { return summary(activityItems); },
    async markAllActivityNotificationsRead() { activityItems.forEach((item) => { item.read = true; }); return summary(activityItems); },
    async markActivityNotificationRead(id) { const item = activityItems.find((entry) => entry.id === id); if (item) item.read = true; return summary(activityItems); },
    async socialNotifications() { return summary(socialItems); },
    async markAllSocialNotificationsRead() { socialItems.forEach((item) => { item.read = true; }); return summary(socialItems); },
    async markSocialNotificationRead(id) { const item = socialItems.find((entry) => entry.id === id); if (item) item.read = true; return summary(socialItems); },
    async socialNotificationPostTarget(id) { const item = socialItems.find((entry) => entry.id === id); if (!item?.type.startsWith("social.post.")) throw new Error("该通知未关联动态"); return { postId: Number(item.targetId) }; },
    async socialNotificationFriendRequestTarget(id) { const item = socialItems.find((entry) => entry.id === id); if (item?.type !== "social.friend.requested") throw new Error("该通知未关联好友申请"); return { requestId: item.targetId }; }
  };
}
