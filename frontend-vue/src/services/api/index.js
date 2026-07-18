import { createAuthApi } from "./auth-api";
import { createHttpClient } from "./http";
import { createMockAuthApi } from "./mock-auth";
import { createMockSocialApi } from "./mock-social";
import { createSocialApi } from "./social-api";
import { createFeedApi } from "./feed-api";
import { createMockFeedApi } from "./mock-feed";
import { createActivityApi } from "./activity-api";
import { createMockActivityApi } from "./mock-activity";
import { createNotificationApi } from "./notification-api";
import { createMockNotificationApi } from "./mock-notifications";

export function createApi({ getToken, getUser, fetchImpl } = {}) {
  const http = createHttpClient({ getToken, fetchImpl });
  return {
    ...createAuthApi({ http, mockAuth: createMockAuthApi() }),
    ...createSocialApi({ http, mockSocial: createMockSocialApi(getUser || (() => null)) }),
    ...createFeedApi({ http, mockFeed: createMockFeedApi(getUser || (() => null)) }),
    ...createActivityApi({ http, mockActivity: createMockActivityApi(getUser || (() => null)) }),
    ...createNotificationApi({ http, mockNotifications: createMockNotificationApi() })
  };
}
