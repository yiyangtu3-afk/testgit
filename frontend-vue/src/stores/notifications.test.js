import { describe, expect, it, vi } from "vitest";
import { createNotificationStore } from "./notifications";
describe("notification store", () => {
  it("combines summaries, marks one notification, and de-duplicates realtime delivery", async () => {
    const api = {
      activityNotifications: vi.fn().mockResolvedValue({ mode: "api", data: { items: [{ id: "a-1", title: "活动", read: false, createdAt: "2026-07-16T09:00:00" }] } }),
      socialNotifications: vi.fn().mockResolvedValue({ mode: "api", data: { items: [{ id: "s-1", title: "互动", read: false, createdAt: "2026-07-16T10:00:00" }] } }),
      markActivityNotificationRead: vi.fn().mockResolvedValue({ mode: "api", data: { items: [{ id: "a-1", title: "活动", read: true, createdAt: "2026-07-16T09:00:00" }] } })
    };
    const store = createNotificationStore({ api })(); await store.load();
    expect(store.items.value.map((item) => item.id)).toEqual(["s-1", "a-1"]);
    await store.mark(store.activity.value[0]); expect(store.unreadCount.value).toBe(1);
    expect(store.receive({ type: "social.notification.created", notification: { id: "s-1", title: "更新后的互动", read: false, createdAt: "2026-07-16T10:30:00" } })).toBe(true);
    expect(store.social.value).toHaveLength(1); expect(store.social.value[0].title).toBe("更新后的互动");
  });
  it("marks activity and social summaries as read together", async () => {
    const api = {
      markAllActivityNotificationsRead: vi.fn().mockResolvedValue({ mode: "mock", data: { items: [{ id: "a-1", read: true, createdAt: "2026-07-16T09:00:00" }] } }),
      markAllSocialNotificationsRead: vi.fn().mockResolvedValue({ mode: "mock", data: { items: [{ id: "s-1", read: true, createdAt: "2026-07-16T10:00:00" }] } })
    };
    const store = createNotificationStore({ api })();
    await store.markAll();
    expect(store.unreadCount.value).toBe(0);
    expect(store.notice.value).toContain("全部标为已读");
  });
});
