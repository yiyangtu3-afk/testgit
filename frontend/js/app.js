import { bindAppEvents } from "./app-events.js?v=20260709-feed-visibility-v1";
import { setApiMode } from "./ui/status.js?v=20260709-feed-visibility-v1";

bindAppEvents();
setApiMode("mock");
