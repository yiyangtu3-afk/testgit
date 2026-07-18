import { describe, expect, it, vi } from "vitest";
import { createApi } from "./index";

const response = (body) => ({ ok: true, status: 200, json: async () => body });

describe("composed API boundary", () => {
  it("keeps student registration separate from activity registration", async () => {
    const fetchImpl = vi.fn().mockResolvedValue(response({ token: "jwt", user: { id: "u-new" } }));
    const api = createApi({ fetchImpl, getToken: () => "saved-token" });

    await api.registerStudent("新同学", "13900000001", "123456");
    await api.register("a-1");

    expect(fetchImpl.mock.calls[0][0]).toBe("/api/auth/register");
    expect(fetchImpl.mock.calls[0][1].headers.Authorization).toBeUndefined();
    expect(fetchImpl.mock.calls[1][0]).toBe("/api/activities/a-1/registrations");
    expect(fetchImpl.mock.calls[1][1].headers.Authorization).toBe("Bearer saved-token");
  });
});
