package com.campuslink.activity.ratelimit;

public interface CheckInRateLimiter {
  void acquireCredentialIssue(String userId, String activityId);

  void acquireCredentialVerification(String userId, String activityId);
}
