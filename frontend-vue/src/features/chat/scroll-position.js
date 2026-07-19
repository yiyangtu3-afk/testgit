export function scrollToLatest(stream) {
  if (stream) stream.scrollTop = stream.scrollHeight;
}

export function captureScrollPosition(stream) {
  return stream ? { height: stream.scrollHeight, top: stream.scrollTop } : null;
}

export function restoreScrollPosition(stream, position) {
  if (!stream || !position) return;
  stream.scrollTop = position.top + stream.scrollHeight - position.height;
}
