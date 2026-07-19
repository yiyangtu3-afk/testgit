import { describe, expect, it } from "vitest";
import {
  captureScrollPosition,
  restoreScrollPosition,
  scrollToLatest
} from "./scroll-position";

describe("chat scroll position", () => {
  it("moves a selected or sent conversation to its latest message", () => {
    const stream = { scrollHeight: 840, scrollTop: 20 };

    scrollToLatest(stream);

    expect(stream.scrollTop).toBe(840);
  });

  it("preserves the reader position when older messages extend the stream", () => {
    const stream = { scrollHeight: 600, scrollTop: 180 };
    const position = captureScrollPosition(stream);
    stream.scrollHeight = 960;

    restoreScrollPosition(stream, position);

    expect(stream.scrollTop).toBe(540);
  });
});
