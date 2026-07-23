import { withApiFallback } from "./auth-api";

export function createActivityApi({ http, mockActivity }) {
  const call = (path, options, mockCall) =>
    withApiFallback(() => http.request(path, options), mockCall);

  return {
    activities(filters = {}) {
      const query = new URLSearchParams(Object.entries(filters).filter(([, value]) => value));
      return call(`/api/activities${query.size ? `?${query}` : ""}`, {},
        () => mockActivity.activities(filters));
    },
    createActivity(activity) {
      return call("/api/activities", { method: "POST", body: JSON.stringify(activity) },
        () => mockActivity.createActivity(activity));
    },
    managed: () => call("/api/activities/managed", {}, () => mockActivity.managed()),
    current: (id) => call(`/api/activities/${id}/registrations/current`, {}, () => mockActivity.current(id)),
    register: (id) => call(`/api/activities/${id}/registrations`, { method: "POST" }, () => mockActivity.register(id)),
    cancel: (id) => call(`/api/activities/${id}/registrations/current`, { method: "DELETE" }, () => mockActivity.cancel(id)),
    roster: (id) => call(`/api/activities/${id}/registrations/roster`, {}, () => mockActivity.roster(id)),
    checkIn: (id, registrationId) => call(`/api/activities/${id}/registrations/${registrationId}/check-in`,
      { method: "POST" }, () => mockActivity.checkIn(id, registrationId)),
    credential: (id) => call(`/api/activities/${id}/registrations/current/check-in-credential`,
      { method: "POST" }, () => mockActivity.credential(id)),
    verifyCredential: (id, code) => call(`/api/activities/${id}/registrations/check-in-credential`,
      { method: "POST", body: JSON.stringify({ code }) }, () => mockActivity.verifyCredential(id, code))
  };
}
