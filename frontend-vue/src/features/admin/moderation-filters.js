export function filterModerationItems(items, { type = "all", author = "", status = "all" } = {}) {
  const keyword = author.trim().toLowerCase();
  return items.filter((item) => {
    const matchesType = type === "all" || item.type === type;
    const matchesAuthor = !keyword || String(item.author || "").toLowerCase().includes(keyword);
    const matchesStatus = status === "all" || (item.status || "pending") === status;
    return matchesType && matchesAuthor && matchesStatus;
  });
}
