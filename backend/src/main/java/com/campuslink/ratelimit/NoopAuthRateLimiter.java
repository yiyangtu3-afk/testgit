package com.campuslink.ratelimit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Keeps native development independent from a locally running Redis instance. */
@Component
@ConditionalOnProperty(
    name = "campuslink.redis.auth-rate-limit.enabled",
    havingValue = "false",
    matchIfMissing = true)
public class NoopAuthRateLimiter implements AuthRateLimiter {

  @Override
  public void acquireVerificationCode(String phone) {
  }

  @Override
  public void checkLoginAllowed(String phone) {
  }

  @Override
  public void recordLoginFailure(String phone) {
  }

  @Override
  public void clearLoginFailures(String phone) {
  }
}
