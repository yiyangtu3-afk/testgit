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
});
