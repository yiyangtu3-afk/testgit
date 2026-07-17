import { createRouter, createWebHistory } from "vue-router";
import LoginView from "../features/auth/LoginView.vue";
import WorkspaceView from "../features/auth/WorkspaceView.vue";
import AppShell from "../features/shell/AppShell.vue";
import ContactsChatView from "../features/chat/ContactsChatView.vue";

export default createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", name: "login", component: LoginView },
    {
      path: "/workspace",
      component: AppShell,
      children: [
        { path: "", name: "workspace", component: WorkspaceView },
        { path: "contacts", name: "contacts", component: ContactsChatView }
      ]
    }
  ]
});
