import { createRouter, createWebHistory } from "vue-router";
import LoginView from "../features/auth/LoginView.vue";
import WorkspaceView from "../features/auth/WorkspaceView.vue";
import AppShell from "../features/shell/AppShell.vue";

export default createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", name: "login", component: LoginView },
    {
      path: "/workspace",
      component: AppShell,
      children: [
        { path: "", name: "workspace", component: WorkspaceView }
      ]
    }
  ]
});
