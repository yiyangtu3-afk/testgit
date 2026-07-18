import { computed, ref } from "vue";
import { defineStore } from "pinia";
import { createApi } from "../services/api";

const TOKEN_KEY = "campuslink-vue.token";
const USER_KEY = "campuslink-vue.user";

function browserStorage() {
  return typeof localStorage === "undefined" ? null : localStorage;
}

export function createSessionStore({ api, storage = browserStorage() } = {}) {
  return () => {
    const token = ref(storage?.getItem(TOKEN_KEY) || "");
    const user = ref(JSON.parse(storage?.getItem(USER_KEY) || "null"));
    const mode = ref("unknown");
    const feedback = ref("");
    const busy = ref(false);
    const isAuthenticated = computed(() => Boolean(token.value && user.value));

    function saveSession(response) {
      token.value = response.token;
      user.value = response.user;
      storage?.setItem(TOKEN_KEY, token.value);
      storage?.setItem(USER_KEY, JSON.stringify(user.value));
    }
    function clearSession() {
      token.value = "";
      user.value = null;
      storage?.removeItem(TOKEN_KEY);
      storage?.removeItem(USER_KEY);
    }
    async function run(action, successMessage) {
      busy.value = true;
      try {
        const result = await action();
        mode.value = result.mode;
        saveSession(result.data);
        feedback.value = `${successMessage}（${result.mode === "api" ? "Java API" : "Mock"}）`;
        return result.data;
      } finally {
        busy.value = false;
      }
    }
    return {
      token, user, mode, feedback, busy, isAuthenticated,
      requestCode(phone) {
        return api.createCode(phone).then((result) => {
          mode.value = result.mode;
          feedback.value = `验证码已发送（${result.mode === "api" ? "Java API" : "Mock"}）。`;
          return result.data;
        });
      },
      login: (phone, code) => run(() => api.login(phone, code), "登录成功"),
      register: (name, phone, code) => run(() => api.register(name, phone, code), "注册成功，欢迎加入 CampusLink"),
      demoLogin: (userId = "u-1001") => run(() => api.demoLogin(userId), "已进入演示"),
      async switchDemoAccount(userId) {
        const previousToken = token.value;
        busy.value = true;
        try {
          const nextSession = await api.demoLogin(userId);
          if (previousToken) await api.logout();
          mode.value = nextSession.mode;
          saveSession(nextSession.data);
          feedback.value = `已切换为 ${nextSession.data.user.name}（${nextSession.mode === "api" ? "Java API" : "Mock"}）。`;
          return nextSession.data;
        } finally {
          busy.value = false;
        }
      },
      async logout() {
        try {
          const result = await api.logout();
          mode.value = result.mode;
        } finally {
          clearSession();
          feedback.value = "已退出当前会话。";
        }
      },
      clearSession
    };
  };
}

export const useSessionStore = defineStore("session", () => {
  const api = createApi({
    getToken: () => browserStorage()?.getItem(TOKEN_KEY) || "",
    getUser: () => JSON.parse(browserStorage()?.getItem(USER_KEY) || "null")
  });
  return createSessionStore({ api })();
});
