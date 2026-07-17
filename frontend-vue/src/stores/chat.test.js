import { describe, expect, it, vi } from "vitest";
import { createChatStore } from "./chat";

describe("chat store", () => {
  it("loads a paged conversation and refreshes read counts", async () => {
    const api = { friends: vi.fn().mockResolvedValue({ mode: "api", data: [{ id: "u-2", name: "陈老师" }] }), friendRequests: vi.fn().mockResolvedValue({ mode: "api", data: [] }), unreadCounts: vi.fn().mockResolvedValue({ mode: "api", data: { counts: {} } }), conversationPreviews: vi.fn().mockResolvedValue({ mode: "api", data: { previews: {} } }), messages: vi.fn().mockResolvedValue({ mode: "api", data: { messages: [{ id: 2, from: "u-2", text: "你好" }], hasMore: true, nextBeforeId: 1 } }) };
    const store = createChatStore({ api })(); await store.initialize();
    expect(store.selectedId.value).toBe("u-2"); expect(store.conversations.value["u-2"]).toHaveLength(1); expect(store.paging.value["u-2"].hasMore).toBe(true);
  });
});
