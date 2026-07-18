const HEARTBEAT_INTERVAL = 15_000;

export function createChatRealtime({ token, onConversationEvent = async () => {}, onNotificationEvent = async () => {}, onMode = () => {}, WebSocketImpl = WebSocket }) {
  let socket; let heartbeat; let reconnect; let stopped = false;
  const url = () => `${location.protocol === "https:" ? "wss" : "ws"}://${location.host}/ws/chat?token=${encodeURIComponent(token())}`;
  function stopTimers() { clearInterval(heartbeat); clearTimeout(reconnect); heartbeat = null; reconnect = null; }
  function connect() {
    if (stopped || !token()) return;
    onMode("connecting"); socket = new WebSocketImpl(url());
    socket.addEventListener("open", () => { onMode("live"); heartbeat = setInterval(() => socket?.readyState === WebSocketImpl.OPEN && socket.send(JSON.stringify({ type: "heartbeat.ping" })), HEARTBEAT_INTERVAL); });
    socket.addEventListener("message", async (event) => { try { const payload = JSON.parse(event.data); if (["message.created", "message.withdrawn"].includes(payload.type) && payload.peerId) await onConversationEvent(payload.peerId); if (["activity.notification.created", "social.notification.created"].includes(payload.type)) await onNotificationEvent(payload); } catch { /* Ignore malformed realtime events. */ } });
    socket.addEventListener("close", () => { stopTimers(); if (!stopped) { onMode("offline"); reconnect = setTimeout(connect, 2000); } });
    socket.addEventListener("error", () => onMode("offline"));
  }
  function disconnect() { stopped = true; stopTimers(); socket?.close(); socket = null; onMode("offline"); }
  return { connect, disconnect };
}
