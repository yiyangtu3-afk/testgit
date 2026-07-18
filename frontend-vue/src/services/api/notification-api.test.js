import { describe, expect, it, vi } from "vitest";
import { createHttpClient } from "./http";
import { createNotificationApi } from "./notification-api";
import { ApiHttpError } from "./errors";
const response = (status, body) => ({ ok: status >= 200 && status < 300, status, json: async () => body });
describe("notification API boundary", () => {
  it.each([401, 403, 500])("does not fall back to Mock for HTTP %i", async (status) => {
    const mockNotifications = { activityNotifications: vi.fn() };
    const api = createNotificationApi({ http: createHttpClient({ fetchImpl: vi.fn().mockResolvedValue(response(status, { message: "denied" })) }), mockNotifications });
    await expect(api.activityNotifications()).rejects.toMatchObject({ name: ApiHttpError.name, status });
    expect(mockNotifications.activityNotifications).not.toHaveBeenCalled();
  });
  it("uses Mock only when the notification API is unreachable", async () => {
    const mockNotifications = { activityNotifications: vi.fn().mockResolvedValue({ items: [], unreadCount: 0 }) };
    const api = createNotificationApi({ http: createHttpClient({ fetchImpl: vi.fn().mockRejectedValue(new TypeError("offline")) }), mockNotifications });
    await expect(api.activityNotifications()).resolves.toMatchObject({ mode: "mock" });
  });
});
