import { ApiUnavailableError } from "./errors";

export async function withApiFallback(liveCall, mockCall) {
    try {
      return { mode: "api", data: await liveCall() };
    } catch (error) {
      if (!(error instanceof ApiUnavailableError)) throw error;
      return { mode: "mock", data: await mockCall() };
    }
}

export function createAuthApi({ http, mockAuth }) {

  return {
    createCode(phone) {
      return withApiFallback(
        () => http.request("/api/auth/code", { method: "POST", body: JSON.stringify({ phone }) }),
        () => mockAuth.createCode(phone)
      );
    },
    login(phone, code) {
      return withApiFallback(
        () => http.request("/api/auth/login", { method: "POST", body: JSON.stringify({ phone, code }) }),
        () => mockAuth.login(phone, code)
      );
    },
    register(name, phone, code) {
      return withApiFallback(
        () => http.request("/api/auth/register", { method: "POST", body: JSON.stringify({ name, phone, code }) }),
        () => mockAuth.register(name, phone, code)
      );
    },
    demoLogin(userId) {
      return withApiFallback(
        () => http.request("/api/auth/demo-login", { method: "POST", body: JSON.stringify({ userId }) }),
        () => mockAuth.demoLogin(userId)
      );
    },
    logout() {
      return withApiFallback(
        () => http.request("/api/auth/logout", { method: "POST" }),
        () => mockAuth.logout()
      );
    }
  };
}
