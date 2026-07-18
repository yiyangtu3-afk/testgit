export function toggleCommentPanel(openPanels, postId) {
  if (!openPanels[postId]) return { ...openPanels, [postId]: true };
  const next = { ...openPanels };
  delete next[postId];
  return next;
}
