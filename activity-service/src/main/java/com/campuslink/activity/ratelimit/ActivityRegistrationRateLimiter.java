package com.campuslink.activity.ratelimit;

/** Protects one hot activity's transactional registration path from request bursts. */
public interface ActivityRegistrationRateLimiter {
  void acquireRegistration(String activityId);
}
