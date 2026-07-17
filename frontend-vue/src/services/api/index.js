import { createAuthApi } from "./auth-api";
import { createHttpClient } from "./http";
import { createMockAuthApi } from "./mock-auth";
import { createMockSocialApi } from "./mock-social";
import { createSocialApi } from "./social-api";
import { createFeedApi } from "./feed-api";
import { createMockFeedApi } from "./mock-feed";

export function createApi({ getToken, getUser, fetchImpl } = {}) {
  const http = createHttpClient({ getToken, fetchImpl });
  return {
    ...createAuthApi({ http, mockAuth: createMockAuthApi() }),
    ...createSocialApi({ http, mockSocial: createMockSocialApi(getUser || (() => null)) }),
    ...createFeedApi({ http, mockFeed: createMockFeedApi(getUser || (() => null)) })
  };
}
