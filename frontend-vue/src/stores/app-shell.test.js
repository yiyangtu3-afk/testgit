import { describe, expect, it } from "vitest";
import { createPinia, setActivePinia } from "pinia";
import { useAppShellStore } from "./app-shell";

describe("app shell store", () => {
  it("replaces the shared status notice with its severity", () => {
    setActivePinia(createPinia());
    const shell = useAppShellStore();

    shell.setNotice("聊天将在后续切片迁移。", "info");

    expect(shell.notice).toEqual({
      message: "聊天将在后续切片迁移。",
      tone: "info"
    });
  });
});
