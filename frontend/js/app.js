import { bindAppEvents } from "./app-events.js?v=20260712-activity-filters-v1";
import { setApiMode } from "./ui/status.js?v=20260712-activity-filters-v1";

bindAppEvents();
setApiMode("mock");
