package com.campuslink.ratelimit;

public class RateLimitExceededException extends RuntimeException {

  public RateLimitExceededException(String message) {
    super(message);
  }
}
