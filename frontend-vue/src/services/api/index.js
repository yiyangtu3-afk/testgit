import { createAuthApi } from "./auth-api";
import { createHttpClient } from "./http";
import { createMockAuthApi } from "./mock-auth";

export function createApi({ getToken, fetchImpl } = {}) {
  return createAuthApi({
    http: createHttpClient({ getToken, fetchImpl }),
    mockAuth: createMockAuthApi()
  });
}
