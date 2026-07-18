import { describe, expect, it, vi } from "vitest";
import { createSessionStore } from "./session";

function storage() { const values = new Map(); return { getItem: (key) => values.get(key) || null, setItem: (key, value) => values.set(key, value), removeItem: (key) => values.delete(key) }; }

describe("session store", () => {
  it("persists an API session and clears it after logout", async () => {
    const api = { login: vi.fn().mockResolvedValue({ mode: "api", data: { token: "jwt", user: { id: "u-1" } } }), logout: vi.fn().mockResolvedValue({ mode: "api" }) };
    const session = createSessionStore({ api, storage: storage() })();
    await session.login("13800000001", "123456");
    expect(session.isAuthenticated.value).toBe(true); expect(session.mode.value).toBe("api");
    await session.logout(); expect(session.isAuthenticated.value).toBe(false);
  });

  it("replaces the stored session only after revoking the previous demo session", async () => {
    const api = {
      demoLogin: vi.fn().mockResolvedValue({ mode: "api", data: { token: "teacher-jwt", user: { id: "u-2001", name: "陈老师" } } }),
      logout: vi.fn().mockResolvedValue({ mode: "api" })
    };
    const session = createSessionStore({ api, storage: storage() })();
    session.token.value = "student-jwt";
    session.user.value = { id: "u-1001", name: "林一" };

    await session.switchDemoAccount("u-2001");

    expect(api.demoLogin).toHaveBeenCalledWith("u-2001");
    expect(api.logout).toHaveBeenCalledOnce();
    expect(session.token.value).toBe("teacher-jwt");
    expect(session.user.value.id).toBe("u-2001");
    expect(session.feedback.value).toContain("陈老师");
  });

  it("keeps the current session when revoking it fails", async () => {
    const api = {
      demoLogin: vi.fn().mockResolvedValue({ mode: "api", data: { token: "teacher-jwt", user: { id: "u-2001", name: "陈老师" } } }),
      logout: vi.fn().mockRejectedValue(new Error("会话已失效"))
    };
    const session = createSessionStore({ api, storage: storage() })();
    session.token.value = "student-jwt";
    session.user.value = { id: "u-1001", name: "林一" };

    await expect(session.switchDemoAccount("u-2001")).rejects.toThrow("会话已失效");
    expect(session.token.value).toBe("student-jwt");
    expect(session.user.value.id).toBe("u-1001");
  });
});
