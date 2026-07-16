import { bindAppEvents } from "./app-events.js?v=20260715-friend-notifications-v1";
import { setApiMode } from "./ui/status.js?v=20260715-friend-notifications-v1";

bindAppEvents();
setApiMode("mock");
