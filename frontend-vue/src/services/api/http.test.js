import { describe, expect, it, vi } from "vitest";
import { ApiHttpError } from "./errors";
import { createHttpClient } from "./http";

describe("HTTP binary requests", () => {
  it("loads protected image bytes with the current bearer token", async () => {
    const image = new Blob(["image"], { type: "image/png" });
    const fetchImpl = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      blob: async () => image
    });
    const http = createHttpClient({ fetchImpl, getToken: () => "session-token" });

    await expect(http.requestBlob("/api/conversations/u-2001/attachments/att-1"))
      .resolves.toBe(image);
    expect(fetchImpl).toHaveBeenCalledWith(
      "/api/conversations/u-2001/attachments/att-1",
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: "Bearer session-token" })
      })
    );
  });

  it("keeps image endpoint failures as real API errors", async () => {
    const http = createHttpClient({
      fetchImpl: vi.fn().mockResolvedValue({
        ok: false,
        status: 403,
        json: async () => ({ message: "仅能与已建立好友关系的用户聊天" })
      })
    });

    await expect(http.requestBlob("/api/conversations/u-2001/attachments/att-1"))
      .rejects.toEqual(expect.objectContaining({
        status: 403,
        message: "仅能与已建立好友关系的用户聊天"
      }));
    await expect(http.requestBlob("/api/conversations/u-2001/attachments/att-1"))
      .rejects.toBeInstanceOf(ApiHttpError);
  });
});
