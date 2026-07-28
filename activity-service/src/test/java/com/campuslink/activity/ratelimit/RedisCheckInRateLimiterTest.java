package com.campuslink.activity.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class RedisCheckInRateLimiterTest {
  private static final String SECRET = "campuslink-test-signing-secret-must-have-32-bytes";

  @Test
  void rejectsTheRequestAfterTheConfiguredLimit() {
    StringRedisTemplate redis = org.mockito.Mockito.mock(StringRedisTemplate.class);
    when(redis.execute(any(), anyList(), any())).thenReturn(1L, 2L, 3L);
    SimpleMeterRegistry metrics = new SimpleMeterRegistry();
    RedisCheckInRateLimiter limiter = new RedisCheckInRateLimiter(redis, metrics, SECRET, 2, 10,
        Duration.ofMinutes(1));

    limiter.acquireCredentialIssue("student-1", "activity-1");
    limiter.acquireCredentialIssue("student-1", "activity-1");
    Throwable error = catchThrowable(
        () -> limiter.acquireCredentialIssue("student-1", "activity-1"));

    assertThat(error).isInstanceOf(ResponseStatusException.class);
    assertThat(((ResponseStatusException) error).getStatusCode())
        .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    assertThat(metrics.get("campuslink.redis.check_in_rate_limit.allowed")
        .tag("action", "credential_issue").counter().count()).isEqualTo(2);
    assertThat(metrics.get("campuslink.redis.check_in_rate_limit.rejected")
        .tag("action", "credential_issue").counter().count()).isEqualTo(1);
  }

  @Test
  void failsOpenAndRecordsAnErrorWhenRedisIsUnavailable() {
    StringRedisTemplate redis = org.mockito.Mockito.mock(StringRedisTemplate.class);
    when(redis.execute(any(), anyList(), any()))
        .thenThrow(new RedisConnectionFailureException("Redis unavailable"));
    SimpleMeterRegistry metrics = new SimpleMeterRegistry();
    RedisCheckInRateLimiter limiter = new RedisCheckInRateLimiter(redis, metrics, SECRET, 2, 10,
        Duration.ofMinutes(1));

    limiter.acquireCredentialVerification("teacher-1", "activity-1");

    assertThat(metrics.get("campuslink.redis.check_in_rate_limit.error")
        .tag("action", "credential_verification").counter().count()).isEqualTo(1);
  }

  @Test
  void fingerprintsUserAndActivityBeforeUsingThemInARedisKey() {
    StringRedisTemplate redis = org.mockito.Mockito.mock(StringRedisTemplate.class);
    when(redis.execute(any(), anyList(), any())).thenAnswer(invocation -> {
      List<String> keys = invocation.getArgument(1);
      assertThat(keys).singleElement().satisfies(key -> assertThat(key)
          .startsWith("campuslink:activities:check-in-rate-limit:")
          .doesNotContain("student-1", "activity-1", "credential_issue"));
      return 1L;
    });
    RedisCheckInRateLimiter limiter = new RedisCheckInRateLimiter(redis,
        new SimpleMeterRegistry(), SECRET, 2, 10, Duration.ofMinutes(1));

    limiter.acquireCredentialIssue("student-1", "activity-1");
  }
}
