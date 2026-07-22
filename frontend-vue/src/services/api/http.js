import { ApiHttpError, ApiUnavailableError } from "./errors";

export function createHttpClient({ fetchImpl = fetch, getToken = () => null } = {}) {
  async function fetchResponse(path, { anonymous = false, ...options } = {}) {
    try {
      return await fetchImpl(path, {
        ...options,
        headers: {
          "Content-Type": "application/json",
          ...(!anonymous && getToken() ? { Authorization: `Bearer ${getToken()}` } : {}),
          ...options.headers
        }
      });
    } catch (error) {
      throw new ApiUnavailableError(error);
    }
  }

  async function request(path, { anonymous = false, ...options } = {}) {
    const response = await fetchResponse(path, { anonymous, ...options });

    const payload = await response.json().catch(() => null);
    if (!response.ok) {
      throw new ApiHttpError(
        response.status,
        payload?.message || `HTTP ${response.status}`
      );
    }
    return payload;
  }

  async function requestBlob(path, options = {}) {
    const response = await fetchResponse(path, options);
    if (!response.ok) {
      const payload = await response.json().catch(() => null);
      throw new ApiHttpError(
        response.status,
        payload?.message || `HTTP ${response.status}`
      );
    }
    return response.blob();
  }

  return { request, requestBlob };
}
