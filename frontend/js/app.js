import { bindAppEvents } from "./app-events.js?v=20260710-conversation-previews-v1";
import { setApiMode } from "./ui/status.js?v=20260710-conversation-previews-v1";

bindAppEvents();
setApiMode("mock");
