import { describe, expect, it, vi } from "vitest";
import { createActivityStore } from "./activity";

describe("activity store", () => {
  it("submits an organizer activity as pending without adding it to the public list", async () => {
    const api = { createActivity: vi.fn().mockResolvedValue({ mode: "api", data: { id: "a-2", title: "验收活动", reviewDecision: "pending" } }) };
    const store = createActivityStore({ api, getUser: () => ({ role: "教师" }) })();
    const created = await store.create({ title: "验收活动", startsAt: "2026-08-20T09:00", endsAt: "2026-08-20T10:00", capacity: 20 });
    expect(created.id).toBe("a-2"); expect(store.managed.value[0].reviewDecision).toBe("pending"); expect(store.notice.value).toContain("提交审核");
  });
  it("keeps an invalid time range on the client and does not call the API", async () => {
    const api = { createActivity: vi.fn() };
    const store = createActivityStore({ api, getUser: () => ({ role: "教师" }) })();
    await expect(store.create({ startsAt: "2026-08-20T10:00", endsAt: "2026-08-20T09:00" })).resolves.toBeNull();
    expect(api.createActivity).not.toHaveBeenCalled();
  });
});
