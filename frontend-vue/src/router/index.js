import { createRouter, createWebHistory } from "vue-router";
import LoginView from "../features/auth/LoginView.vue";
import WorkspaceView from "../features/auth/WorkspaceView.vue";

export default createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", name: "login", component: LoginView },
    { path: "/workspace", name: "workspace", component: WorkspaceView }
  ]
});
