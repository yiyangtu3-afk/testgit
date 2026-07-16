export class ApiUnavailableError extends Error {
  constructor(cause) {
    super("Java API 暂不可用。");
    this.name = "ApiUnavailableError";
    this.cause = cause;
  }
}

export class ApiHttpError extends Error {
  constructor(status, message) {
    super(message || `HTTP ${status}`);
    this.name = "ApiHttpError";
    this.status = status;
  }
}
