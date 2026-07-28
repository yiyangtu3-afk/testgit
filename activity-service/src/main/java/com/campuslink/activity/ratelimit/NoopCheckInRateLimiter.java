package com.campuslink.activity.ratelimit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    name = "campuslink.redis.check-in-rate-limit.enabled",
    havingValue = "false",
    matchIfMissing = true)
public class NoopCheckInRateLimiter implements CheckInRateLimiter {
  @Override
  public void acquireCredentialIssue(String userId, String activityId) {
    // Native development keeps Redis optional, so credential requests stay unlimited.
  }

  @Override
  public void acquireCredentialVerification(String userId, String activityId) {
    // Native development keeps Redis optional, so credential requests stay unlimited.
  }
}
