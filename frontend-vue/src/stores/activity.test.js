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
  it("loads an organizer's managed activities without requesting student registrations", async () => {
    const api = {
      activities: vi.fn().mockResolvedValue({ mode: "api", data: [{ id: "a-1" }] }),
      current: vi.fn(),
      managed: vi.fn().mockResolvedValue({ mode: "api", data: [{ id: "a-1", title: "揍康鹏" }] })
    };
    const store = createActivityStore({ api, getUser: () => ({ role: "社团负责人" }) })();

    await store.load();

    expect(api.current).not.toHaveBeenCalled();
    expect(api.managed).toHaveBeenCalledOnce();
    expect(store.managed.value).toMatchObject([{ title: "揍康鹏" }]);
  });
  it("keeps a student credential in memory and refreshes the organizer roster after verification", async () => {
    const api = {
      credential: vi.fn().mockResolvedValue({ mode: "api", data: { activityId: "a-1", code: "opaque-code" } }),
      verifyCredential: vi.fn().mockResolvedValue({ mode: "api", data: { attendeeName: "林一", status: "checked_in" } }),
      roster: vi.fn().mockResolvedValue({ mode: "api", data: { entries: [{ registrationId: "r-1", status: "checked_in" }] } })
    };
    const store = createActivityStore({ api })();

    await store.credential("a-1");
    await expect(store.verifyCredential("a-1", "opaque-code")).resolves.toMatchObject({ status: "checked_in" });

    expect(store.credentials.value["a-1"].code).toBe("opaque-code");
    expect(api.verifyCredential).toHaveBeenCalledWith("a-1", "opaque-code");
    expect(store.rosters.value["a-1"].entries[0].status).toBe("checked_in");
    expect(store.notice.value).toContain("林一");
  });
});
