package com.campuslink.ratelimit;

/** Protects public authentication actions without making Redis a source of truth. */
public interface AuthRateLimiter {

  void acquireVerificationCode(String phone);

  void checkLoginAllowed(String phone);

  void recordLoginFailure(String phone);

  void clearLoginFailures(String phone);
}
