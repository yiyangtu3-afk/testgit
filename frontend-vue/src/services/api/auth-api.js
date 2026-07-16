import { ApiUnavailableError } from "./errors";

export function createAuthApi({ http, mockAuth }) {
  async function withFallback(liveCall, mockCall) {
    try {
      return { mode: "api", data: await liveCall() };
    } catch (error) {
      if (!(error instanceof ApiUnavailableError)) throw error;
      return { mode: "mock", data: await mockCall() };
    }
  }

  return {
    createCode(phone) {
      return withFallback(
        () => http.request("/api/auth/code", { method: "POST", body: JSON.stringify({ phone }) }),
        () => mockAuth.createCode(phone)
      );
    },
    login(phone, code) {
      return withFallback(
        () => http.request("/api/auth/login", { method: "POST", body: JSON.stringify({ phone, code }) }),
        () => mockAuth.login(phone, code)
      );
    },
    demoLogin(userId) {
      return withFallback(
        () => http.request("/api/auth/demo-login", { method: "POST", body: JSON.stringify({ userId }) }),
        () => mockAuth.demoLogin(userId)
      );
    },
    logout() {
      return withFallback(
        () => http.request("/api/auth/logout", { method: "POST" }),
        () => mockAuth.logout()
      );
    }
  };
}
