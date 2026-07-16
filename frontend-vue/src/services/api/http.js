import { ApiHttpError, ApiUnavailableError } from "./errors";

export function createHttpClient({ fetchImpl = fetch, getToken = () => null } = {}) {
  async function request(path, options = {}) {
    let response;
    try {
      response = await fetchImpl(path, {
        ...options,
        headers: {
          "Content-Type": "application/json",
          ...(getToken() ? { Authorization: `Bearer ${getToken()}` } : {}),
          ...options.headers
        }
      });
    } catch (error) {
      throw new ApiUnavailableError(error);
    }

    const payload = await response.json().catch(() => null);
    if (!response.ok) {
      throw new ApiHttpError(
        response.status,
        payload?.message || `HTTP ${response.status}`
      );
    }
    return payload;
  }

  return { request };
}
