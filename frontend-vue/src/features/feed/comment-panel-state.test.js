import { describe, expect, it } from "vitest";
import { toggleCommentPanel } from "./comment-panel-state";

describe("comment panel state", () => {
  it("opens after comments load and closes without clearing a draft", () => {
    const open = toggleCommentPanel({}, 12);
    expect(open).toEqual({ 12: true });
    expect(toggleCommentPanel(open, 12)).toEqual({});
  });
});
