import { createRouter, createWebHistory } from "vue-router";
import LoginView from "../features/auth/LoginView.vue";
import WorkspaceView from "../features/auth/WorkspaceView.vue";
import AppShell from "../features/shell/AppShell.vue";
import ContactsChatView from "../features/chat/ContactsChatView.vue";
import FeedView from "../features/feed/FeedView.vue";
import ActivitiesView from "../features/activities/ActivitiesView.vue";
import NotificationsView from "../features/notifications/NotificationsView.vue";
import AdminView from "../features/admin/AdminView.vue";

export default createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", name: "login", component: LoginView },
    {
      path: "/workspace",
      component: AppShell,
      children: [
        { path: "", name: "workspace", component: WorkspaceView },
        { path: "contacts", name: "contacts", component: ContactsChatView },
        { path: "feed", name: "feed", component: FeedView },
        { path: "activities", name: "activities", component: ActivitiesView },
        { path: "notifications", name: "notifications", component: NotificationsView },
        { path: "admin", name: "admin", component: AdminView }
      ]
    }
  ]
});
