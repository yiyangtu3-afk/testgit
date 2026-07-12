import { bindAppEvents } from "./app-events.js?v=20260710-activity-review-ui-v1";
import { setApiMode } from "./ui/status.js?v=20260710-activity-review-ui-v1";

bindAppEvents();
setApiMode("mock");
