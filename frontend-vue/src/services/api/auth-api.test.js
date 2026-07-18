import { describe, expect, it, vi } from "vitest";
import { createAuthApi } from "./auth-api";
import { createHttpClient } from "./http";
import { ApiHttpError } from "./errors";
import { createMockAuthApi } from "./mock-auth";

const response = (status, body) => ({ ok: status >= 200 && status < 300, status, json: async () => body });

describe("authentication API boundary", () => {
  it("injects the bearer token for live requests", async () => {
    const fetchImpl = vi.fn().mockResolvedValue(response(200, { token: "new", user: {} }));
    const http = createHttpClient({ fetchImpl, getToken: () => "saved-token" });
    await http.request("/api/auth/logout", { method: "POST" });
    expect(fetchImpl.mock.calls[0][1].headers.Authorization).toBe("Bearer saved-token");
  });
  it.each([401, 403, 500])("does not fall back to mock for HTTP %i", async (status) => {
    const mockAuth = { login: vi.fn() };
    const api = createAuthApi({ http: createHttpClient({ fetchImpl: vi.fn().mockResolvedValue(response(status, { message: "denied" })) }), mockAuth });
    await expect(api.login("13800000001", "123456")).rejects.toMatchObject({ name: ApiHttpError.name, status });
    expect(mockAuth.login).not.toHaveBeenCalled();
  });
  it("does not fall back to mock when registration conflicts", async () => {
    const mockAuth = { register: vi.fn() };
    const api = createAuthApi({ http: createHttpClient({ fetchImpl: vi.fn().mockResolvedValue(response(409, { message: "该手机号已注册" })) }), mockAuth });

    await expect(api.register("新同学", "13900000001", "123456")).rejects.toMatchObject({ name: ApiHttpError.name, status: 409 });
    expect(mockAuth.register).not.toHaveBeenCalled();
  });
  it("uses mock only when the API is unreachable", async () => {
    const mockAuth = { login: vi.fn().mockResolvedValue({ token: "mock", user: { id: "u-1" } }) };
    const api = createAuthApi({ http: createHttpClient({ fetchImpl: vi.fn().mockRejectedValue(new TypeError("offline")) }), mockAuth });
    await expect(api.login("13800000001", "123456")).resolves.toMatchObject({ mode: "mock" });
  });
  it("registers a new student in the Mock fallback session", async () => {
    const mockAuth = createMockAuthApi();
    const code = await mockAuth.createCode("13900000999");

    const registered = await mockAuth.register("Mock 新同学", "13900000999", code.code);
    const loggedIn = await mockAuth.login("13900000999", code.code);

    expect(registered.user).toMatchObject({ name: "Mock 新同学", role: "学生账号" });
    expect(loggedIn.user.id).toBe(registered.user.id);
  });
});
