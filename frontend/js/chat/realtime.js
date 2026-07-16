import { API_BASE, state } from "../state.js";
import { loadFriends, loadMessages, loadUnreadCounts } from "../loaders.js?v=20260715-social-realtime-v1";
import { renderConversations } from "../ui/contacts-renderers.js?v=20260715-social-realtime-v1";
import { setRealtimeMode } from "../ui/status.js?v=20260715-social-realtime-v1";
import { handleNotificationRealtimeEvent } from "../notifications/realtime.js?v=20260715-social-realtime-v1";

const HEARTBEAT_INTERVAL_MS = 15000;
const HEARTBEAT_TIMEOUT_MS = 6000;

let socket = null;
let reconnectTimer = 0;
let heartbeatTimer = 0;
let heartbeatTimeoutTimer = 0;

export function connectChatRealtime() {
  disconnectChatRealtime();
  if (state.apiMode !== "live" || !state.token || typeof WebSocket === "undefined") {
    setRealtimeMode("offline");
    return;
  }

  setRealtimeMode("connecting");
  socket = new WebSocket(`${chatSocketUrl()}?token=${encodeURIComponent(state.token)}`);
  socket.addEventListener("open", () => {
    setRealtimeMode("live");
    startHeartbeat();
  });
  socket.addEventListener("message", handleRealtimeMessage);
  socket.addEventListener("close", scheduleReconnect);
  socket.addEventListener("error", () => setRealtimeMode("offline"));
}

export function disconnectChatRealtime() {
  window.clearTimeout(reconnectTimer);
  reconnectTimer = 0;
  stopHeartbeat();
  if (!socket) {
    return;
  }
  socket.removeEventListener("message", handleRealtimeMessage);
  socket.removeEventListener("close", scheduleReconnect);
  socket.close();
  socket = null;
  setRealtimeMode("offline");
}

async function handleRealtimeMessage(event) {
  const payload = parsePayload(event.data);
  if (!payload) {
    return;
  }

  if (payload.type === "heartbeat.pong") {
    window.clearTimeout(heartbeatTimeoutTimer);
    heartbeatTimeoutTimer = 0;
    setRealtimeMode("live");
    return;
  }

  if (handleNotificationRealtimeEvent(payload)) {
    return;
  }

  if (!isConversationEvent(payload) || !payload.peerId) {
    return;
  }

  await refreshConversation(payload.peerId);
}

function scheduleReconnect() {
  socket = null;
  stopHeartbeat();
  setRealtimeMode("offline");
  if (state.apiMode !== "live" || !state.token) {
    return;
  }
  setRealtimeMode("connecting");
  reconnectTimer = window.setTimeout(connectChatRealtime, 2000);
}

function startHeartbeat() {
  stopHeartbeat();
  sendHeartbeat();
  heartbeatTimer = window.setInterval(sendHeartbeat, HEARTBEAT_INTERVAL_MS);
}

function stopHeartbeat() {
  window.clearInterval(heartbeatTimer);
  window.clearTimeout(heartbeatTimeoutTimer);
  heartbeatTimer = 0;
  heartbeatTimeoutTimer = 0;
}

function sendHeartbeat() {
  if (!socket || socket.readyState !== WebSocket.OPEN) {
    return;
  }
  socket.send(JSON.stringify({ type: "heartbeat.ping" }));
  window.clearTimeout(heartbeatTimeoutTimer);
  heartbeatTimeoutTimer = window.setTimeout(() => {
    setRealtimeMode("connecting");
    if (socket) {
      socket.close();
    }
  }, HEARTBEAT_TIMEOUT_MS);
}

async function refreshConversation(peerId) {
  if (!state.friends.some((friend) => friend.id === peerId)) {
    await loadFriends();
  }
  if (peerId === state.selectedConversation) {
    await loadMessages(peerId);
    return;
  }
  await loadUnreadCounts();
  renderConversations();
}

function chatSocketUrl() {
  return API_BASE.replace(/^http/, "ws").replace(/\/api$/, "/ws/chat");
}

function parsePayload(data) {
  try {
    return JSON.parse(data);
  } catch {
    return null;
  }
}

function isConversationEvent(payload) {
  return payload.type === "message.created" || payload.type === "message.withdrawn";
}
