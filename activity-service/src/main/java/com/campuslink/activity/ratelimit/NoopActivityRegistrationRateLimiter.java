package com.campuslink.activity.ratelimit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    name = "campuslink.redis.registration-rate-limit.enabled",
    havingValue = "false",
    matchIfMissing = true)
public class NoopActivityRegistrationRateLimiter implements ActivityRegistrationRateLimiter {
  @Override
  public void acquireRegistration(String activityId) {
    // Native development keeps Redis optional, so registration requests stay unlimited.
  }
}
