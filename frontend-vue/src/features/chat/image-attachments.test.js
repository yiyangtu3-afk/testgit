import { describe, expect, it } from "vitest";
import { isPreviewableImage } from "./image-attachments";

describe("chat image attachment state", () => {
  it("only previews stored or in-memory images", () => {
    expect(isPreviewableImage({ type: "image/png", hasContent: true })).toBe(true);
    expect(isPreviewableImage({ type: "image/jpeg", dataUrl: "data:image/jpeg;base64,AA==" })).toBe(true);
    expect(isPreviewableImage({ type: "image/png", hasContent: false })).toBe(false);
    expect(isPreviewableImage({ type: "application/pdf", hasContent: true })).toBe(false);
  });
});
