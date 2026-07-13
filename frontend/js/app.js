import { bindAppEvents } from "./app-events.js?v=20260711-activity-registration-v1";
import { setApiMode } from "./ui/status.js?v=20260711-activity-registration-v1";

bindAppEvents();
setApiMode("mock");
