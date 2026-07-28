package com.campuslink.ratelimit;

public class RateLimitUnavailableException extends RuntimeException {

  public RateLimitUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
