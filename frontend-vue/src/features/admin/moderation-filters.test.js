import { describe, expect, it } from "vitest";
import { filterModerationItems } from "./moderation-filters";

const items = [
  { id: "1", type: "post", author: "林一", status: "pending" },
  { id: "2", type: "comment", author: "周同学", status: "rejected" },
  { id: "3", type: "post", author: "王老师", status: "approved" },
];

describe("filterModerationItems", () => {
  it("combines type, author, and status filters", () => {
    expect(filterModerationItems(items, { type: "post", author: "王", status: "approved" })).toEqual([items[2]]);
  });

  it("treats missing status as pending", () => {
    expect(filterModerationItems([{ id: "1", author: "林一" }], { status: "pending" })).toHaveLength(1);
  });
});
