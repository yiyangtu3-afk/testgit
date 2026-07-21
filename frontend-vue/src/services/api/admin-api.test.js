import { describe, expect, it, vi } from "vitest";
import { createHttpClient } from "./http";
import { createAdminApi } from "./admin-api";
import { ApiHttpError } from "./errors";
const response = (status) => ({ ok: status >= 200 && status < 300, status, json: async () => ({ message: "denied" }) });
describe("admin API boundary", () => {
  it.each([401, 403, 500])("keeps HTTP %i instead of using Mock", async (status) => { const mockAdmin = { metrics: vi.fn() }; const api = createAdminApi({ http: createHttpClient({ fetchImpl: vi.fn().mockResolvedValue(response(status)) }), mockAdmin }); await expect(api.metrics()).rejects.toMatchObject({ name: ApiHttpError.name, status }); expect(mockAdmin.metrics).not.toHaveBeenCalled(); });
  it("uses Mock when the API is unavailable", async () => { const api = createAdminApi({ http: createHttpClient({ fetchImpl: vi.fn().mockRejectedValue(new TypeError("offline")) }), mockAdmin: { metrics: vi.fn().mockResolvedValue({}) } }); await expect(api.metrics()).resolves.toMatchObject({ mode: "mock" }); });
  it("keeps moderation assistance HTTP failures instead of using Mock", async () => { const mockAdmin = { moderationAssistance: vi.fn() }; const api = createAdminApi({ http: createHttpClient({ fetchImpl: vi.fn().mockResolvedValue(response(500)) }), mockAdmin }); await expect(api.moderationAssistance("m-1")).rejects.toMatchObject({ name: ApiHttpError.name, status: 500 }); expect(mockAdmin.moderationAssistance).not.toHaveBeenCalled(); });
});
